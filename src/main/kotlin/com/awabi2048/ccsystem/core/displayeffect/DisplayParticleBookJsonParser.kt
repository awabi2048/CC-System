package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectQuaternion
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionMode
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionProperties
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleEmissionRequest
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionPresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionProperties
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleVisibilityMode
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class ParsedDisplayParticleBook(
    val preset: DisplayParticlePreset,
    val request: DisplayParticleEmissionRequest,
    val offset: DisplayEffectVector3
)

/** 本のJSONを一時的な外観・動作スナップショットへ変換します。 */
internal object DisplayParticleBookJsonParser {
    fun parse(json: String): ParsedDisplayParticleBook {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "JSONは${MAX_JSON_BYTES}byte以内です" }
        val root = JsonParser.parseString(json).objectValue("root")
        root.requireOnly("textures", "scale", "rotation", "lifetime", "motion", "collision", "emission")

        val scale = root.requiredObject("scale").also {
            it.requireOnly("initial", "peak", "peak_progress", "scale_in_ticks", "variation")
        }
        val rotation = root.requiredObject("rotation").also {
            it.requireOnly("random_initial", "angular_velocity", "variation")
        }
        val lifetime = root.requiredObject("lifetime").also {
            it.requireOnly("ticks", "variation", "fade_out_ticks", "fade_variation", "spawn_delay")
        }
        val motion = root.requiredObject("motion").also {
            it.requireOnly(
                "preset", "initial_velocity", "acceleration", "retention", "turbulence", "frequency",
                "radial_speed", "spawn_radius", "orbit_speed", "radial_pull", "attraction", "max_speed"
            )
        }
        val collision = root.requiredObject("collision").also { it.requireOnly("mode", "restitution") }
        val emission = root.requiredObject("emission").also {
            it.requireOnly("offset", "delta", "speed", "count", "visibility")
        }

        val preset = DisplayParticlePreset(
            DisplayParticlePresetId(TRANSIENT_PRESET_ID),
            root.requiredArray("textures").mapIndexed { index, element ->
                val texture = element.objectValue("textures[$index]").also { it.requireOnly("block", "weight") }
                DisplayParticleTexture(
                    DisplayEffectAssetId(texture.requiredString("block")),
                    texture.requiredInt("weight")
                )
            },
            scale.requiredScale("initial"),
            scale.requiredScale("peak"),
            scale.requiredDouble("peak_progress"),
            scale.requiredInt("scale_in_ticks"),
            DisplayEffectQuaternion.IDENTITY,
            rotation.requiredVector("angular_velocity"),
            scale.requiredDouble("variation"),
            rotation.requiredDouble("variation"),
            rotation.requiredBoolean("random_initial"),
            lifetime.requiredInt("ticks"),
            lifetime.requiredInt("variation"),
            lifetime.requiredInt("fade_out_ticks"),
            lifetime.requiredInt("fade_variation"),
            lifetime.requiredInt("spawn_delay")
        )

        val collisionMode = enumValue<DisplayParticleCollisionMode>(collision.requiredString("mode"), "collision.mode")
        val request = DisplayParticleEmissionRequest(
            presetId = DisplayParticlePresetId(TRANSIENT_PRESET_ID),
            motionPresetId = DisplayParticleMotionPresetId(namespaced(motion.requiredString("preset"))),
            collisionMode = collisionMode,
            motionProperties = DisplayParticleMotionProperties(
                initialVelocity = motion.optionalVector("initial_velocity"),
                acceleration = motion.optionalVector("acceleration"),
                velocityRetention = motion.optionalDouble("retention"),
                turbulenceStrength = motion.optionalDouble("turbulence"),
                turbulenceFrequency = motion.optionalDouble("frequency"),
                radialSpeed = motion.optionalDouble("radial_speed"),
                spawnRadius = motion.optionalDouble("spawn_radius"),
                orbitSpeed = motion.optionalDouble("orbit_speed"),
                radialPull = motion.optionalDouble("radial_pull"),
                attraction = motion.optionalDouble("attraction"),
                maxSpeed = motion.optionalDouble("max_speed")
            ),
            collisionProperties = DisplayParticleCollisionProperties(collision.optionalDouble("restitution")),
            delta = emission.requiredVector("delta"),
            speed = emission.requiredDouble("speed"),
            count = emission.requiredInt("count"),
            visibilityMode = enumValue(emission.requiredString("visibility"), "emission.visibility")
        )
        // 動作ごとの無効プロパティも、本をクリックする前と同じ検証規則で確定させます。
        DisplayParticleMotionCatalog.resolve(
            request.motionPresetId,
            request.motionProperties,
            request.collisionMode,
            request.collisionProperties
        )
        return ParsedDisplayParticleBook(preset, request, emission.requiredVector("offset"))
    }

    private fun JsonObject.requireOnly(vararg names: String) {
        val unknown = keySet() - names.toSet()
        require(unknown.isEmpty()) { "未対応のJSON項目です: ${unknown.sorted().joinToString()}" }
    }

    private fun JsonObject.required(name: String): JsonElement =
        requireNotNull(get(name)) { "JSON項目がありません: $name" }.also { require(!it.isJsonNull) { "${name}はnullにできません" } }

    private fun JsonObject.requiredObject(name: String) = required(name).objectValue(name)
    private fun JsonObject.requiredArray(name: String) = required(name).let {
        require(it.isJsonArray) { "${name}は配列です" }
        it.asJsonArray.toList()
    }
    private fun JsonObject.requiredString(name: String) = required(name).let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "${name}は文字列です" }
        it.asString
    }
    private fun JsonObject.requiredBoolean(name: String) = required(name).let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) { "${name}は真偽値です" }
        it.asBoolean
    }
    private fun JsonObject.requiredDouble(name: String) = required(name).numberValue(name)
    private fun JsonObject.optionalDouble(name: String) = get(name)?.takeUnless(JsonElement::isJsonNull)?.numberValue(name)
    private fun JsonObject.requiredInt(name: String): Int {
        val value = requiredDouble(name)
        require(value == value.toInt().toDouble()) { "${name}は整数です" }
        return value.toInt()
    }
    private fun JsonObject.requiredVector(name: String) = required(name).vectorValue(name)
    private fun JsonObject.optionalVector(name: String) = get(name)?.takeUnless(JsonElement::isJsonNull)?.vectorValue(name)
    private fun JsonObject.requiredScale(name: String): DisplayEffectVector3 {
        val element = required(name)
        return if (element.isJsonArray) element.vectorValue(name) else {
            val value = element.numberValue(name)
            DisplayEffectVector3(value, value, value)
        }
    }
    private fun JsonElement.objectValue(name: String): JsonObject {
        require(isJsonObject) { "${name}はオブジェクトです" }
        return asJsonObject
    }
    private fun JsonElement.numberValue(name: String): Double {
        require(isJsonPrimitive && asJsonPrimitive.isNumber) { "${name}は数値です" }
        return asDouble.also { require(it.isFinite()) { "${name}は有限値です" } }
    }
    private fun JsonElement.vectorValue(name: String): DisplayEffectVector3 {
        require(isJsonArray && asJsonArray.size() == 3) { "${name}は[x,y,z]形式です" }
        return DisplayEffectVector3(
            asJsonArray[0].numberValue("$name[0]"),
            asJsonArray[1].numberValue("$name[1]"),
            asJsonArray[2].numberValue("$name[2]")
        )
    }
    private inline fun <reified T : Enum<T>> enumValue(value: String, name: String): T =
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("${name}が不正です: $value")
    private fun namespaced(value: String) = if (':' in value) value else "cc:$value"

    private const val TRANSIENT_PRESET_ID = "cc:book-test"
    private const val MAX_JSON_BYTES = 16_384
}
