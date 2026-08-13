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
import org.bukkit.Material

internal data class DisplayParticleBookStringChoice(
    val value: String,
    val descriptionKey: String
)

internal class DisplayParticleBookStringChoiceException(
    val field: String,
    val choices: List<DisplayParticleBookStringChoice>,
    message: String
) : IllegalArgumentException(message)

internal data class ParsedDisplayParticleBook(
    val preset: DisplayParticlePreset,
    val request: DisplayParticleEmissionRequest,
    val offset: DisplayEffectVector3
)

internal data class PreparedDisplayParticleBook(
    val parsed: ParsedDisplayParticleBook,
    val pages: List<String>,
    val addedPaths: List<String>,
    val removedPaths: List<String>
)

/** 本のJSONを一時的な外観・動作スナップショットへ変換します。 */
internal object DisplayParticleBookJsonParser {
    fun preparePages(
        pages: List<String>,
        blockChoices: List<DisplayParticleBookStringChoice> = emptyList()
    ): PreparedDisplayParticleBook {
        val defaults = sampleRoot()
        val selected = linkedMapOf<String, JsonElement>()
        val added = mutableListOf<String>()
        val removed = mutableListOf<String>()

        pages.forEachIndexed { index, page ->
            val extracted = extractAnyTopLevelFragment(page, index + 1)
            if (extracted == null) {
                if (page.isNotBlank()) removed += "page[${index + 1}]"
                return@forEachIndexed
            }
            if (extracted.hasOutsideText) removed += "page[${index + 1}].outside"
            if (extracted.key !in TOP_LEVEL_FIELDS) {
                removed += extracted.key
                return@forEachIndexed
            }
            if (selected.putIfAbsent(extracted.key, extracted.value) != null) {
                removed += "${extracted.key}(duplicate)"
            }
        }

        TOP_LEVEL_FIELDS.forEach { key ->
            if (key !in selected) {
                selected[key] = defaults[key].deepCopy()
                added += key
            }
        }
        TOP_LEVEL_FIELDS.forEach { key ->
            selected[key] = mergeWithDefaults(key, selected.getValue(key), defaults.get(key), added, removed)
        }

        val correctedPages = TOP_LEVEL_FIELDS.map { key -> "\"$key\":${selected.getValue(key)}" }
        val parsed = parse("{${correctedPages.joinToString(",")}}", blockChoices)
        return PreparedDisplayParticleBook(parsed, correctedPages, added, removed)
    }

    /**
     * 各ページを外側の波括弧を持たないトップレベル項目として検証し、
     * ページ間に空白を加えず単一のJSONオブジェクトへ組み立てます。
     */
    fun parsePages(
        pages: List<String>,
        blockChoices: List<DisplayParticleBookStringChoice> = emptyList()
    ): ParsedDisplayParticleBook {
        return preparePages(pages, blockChoices).parsed
    }

    fun parse(
        json: String,
        blockChoices: List<DisplayParticleBookStringChoice> = emptyList()
    ): ParsedDisplayParticleBook {
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
                    DisplayEffectAssetId(texture.requiredBlockId("block", blockChoices)),
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

        val collisionMode = choiceEnumValue<DisplayParticleCollisionMode>(
            collision.requiredString("mode"),
            "collision.mode",
            "management.debug.particle_test_choice.collision"
        )
        val motionPresetId = DisplayParticleMotionPresetId(namespaced(motion.requiredString("preset")))
        requireMotionPreset(motionPresetId)
        val request = DisplayParticleEmissionRequest(
            presetId = DisplayParticlePresetId(TRANSIENT_PRESET_ID),
            motionPresetId = motionPresetId,
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
            visibilityMode = choiceEnumValue(
                emission.requiredString("visibility"),
                "emission.visibility",
                "management.debug.particle_test_choice.visibility"
            )
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
    private fun JsonObject.requiredBlockId(
        name: String,
        blockChoices: List<DisplayParticleBookStringChoice>
    ): String {
        val value = namespaced(requiredString(name))
        val syntacticallyValid = MINECRAFT_BLOCK_ID_PATTERN.matches(value)
        if (!syntacticallyValid || blockChoices.isNotEmpty() && blockChoices.none { it.value == value }) {
            throw DisplayParticleBookStringChoiceException(
                "textures.$name",
                // Materialは数が多くチャットを埋めるため、候補一覧には表示しません。
                emptyList(),
                "textures.$name が不正です: $value"
            )
        }
        return value
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
    private inline fun <reified T : Enum<T>> choiceEnumValue(
        value: String,
        name: String,
        descriptionKeyPrefix: String
    ): T = enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: throw DisplayParticleBookStringChoiceException(
            name,
            enumValues<T>().map {
                DisplayParticleBookStringChoice(it.name.lowercase(), "$descriptionKeyPrefix.${it.name.lowercase()}")
            },
            "$name が不正です: $value"
        )

    private fun requireMotionPreset(id: DisplayParticleMotionPresetId) {
        if (DisplayParticleMotionCatalog.list().none { it.id == id.value }) {
            throw DisplayParticleBookStringChoiceException(
                "motion.preset",
                DisplayParticleMotionCatalog.list().map {
                    val value = it.id.substringAfter(':')
                    DisplayParticleBookStringChoice(value, "management.debug.particle_test_choice.motion.$value")
                },
                "motion.preset が不正です: ${id.value}"
            )
        }
    }

    private fun extractAnyTopLevelFragment(page: String, pageNumber: Int): ExtractedFragment? {
        val matches = ANY_FIELD_PATTERN.findAll(page).toList()
        val match = matches.firstOrNull { it.groupValues[1] in TOP_LEVEL_FIELDS }
            ?: matches.firstOrNull()
            ?: return null
        val key = match.groupValues[1]
        val valueStart = page.indexOfFirstFrom(match.range.last + 1) { !it.isWhitespace() }
        require(valueStart >= 0 && page[valueStart] in charArrayOf('{', '[')) {
            "${pageNumber}ページ目のJSON値はオブジェクトまたは配列で記述してください"
        }
        val closing = if (page[valueStart] == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escaped = false
        for (index in valueStart until page.length) {
            val character = page[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                page[valueStart] -> depth++
                closing -> {
                    depth--
                    if (depth == 0) {
                        val fragment = page.substring(match.range.first, index + 1)
                        val value = JsonParser.parseString("{$fragment}").asJsonObject.get(key)
                        val outside = page.substring(0, match.range.first).isNotBlank() ||
                            page.substring(index + 1).isNotBlank()
                        return ExtractedFragment(key, value, outside)
                    }
                }
            }
        }
        throw IllegalArgumentException("${pageNumber}ページ目のJSON値が閉じられていません")
    }

    private fun mergeWithDefaults(
        path: String,
        current: JsonElement,
        defaults: JsonElement,
        added: MutableList<String>,
        removed: MutableList<String>
    ): JsonElement {
        if (path == "textures" && current.isJsonArray) {
            val defaultEntry = defaults.asJsonArray.first().asJsonObject
            if (current.asJsonArray.isEmpty) {
                current.asJsonArray.add(defaultEntry.deepCopy())
                added += "textures[0]"
            }
            current.asJsonArray.forEachIndexed { index, element ->
                if (!element.isJsonObject) return@forEachIndexed
                mergeObject(
                    "textures[$index]", element.asJsonObject, defaultEntry,
                    TEXTURE_FIELDS, TEXTURE_FIELDS, added, removed
                )
            }
            return current
        }
        if (current.isJsonObject && defaults.isJsonObject) {
            mergeObject(
                path, current.asJsonObject, defaults.asJsonObject,
                CHILD_FIELDS.getValue(path), REQUIRED_CHILD_FIELDS.getValue(path), added, removed
            )
        }
        return current
    }

    private fun mergeObject(
        path: String,
        current: JsonObject,
        defaults: JsonObject,
        allowedFields: Set<String>,
        requiredFields: Set<String>,
        added: MutableList<String>,
        removed: MutableList<String>
    ) {
        (current.keySet() - allowedFields).forEach { unknown ->
            current.remove(unknown)
            removed += "$path.$unknown"
        }
        defaults.entrySet().filter { it.key in requiredFields }.forEach { (key, defaultValue) ->
            if (!current.has(key)) {
                current.add(key, defaultValue.deepCopy())
                added += "$path.$key"
            }
        }
    }

    private fun sampleRoot(): JsonObject = JsonParser.parseString(
        "{${DisplayParticleBookSample.pages.joinToString(",")}}"
    ).asJsonObject

    private inline fun String.indexOfFirstFrom(startIndex: Int, predicate: (Char) -> Boolean): Int {
        for (index in startIndex until length) if (predicate(this[index])) return index
        return -1
    }
    private fun namespaced(value: String) = if (':' in value) value else "cc:$value"

    private const val TRANSIENT_PRESET_ID = "cc:book-test"
    private const val MAX_JSON_BYTES = 16_384
    private val TOP_LEVEL_FIELDS = linkedSetOf("textures", "scale", "rotation", "lifetime", "motion", "collision", "emission")
    private val ANY_FIELD_PATTERN = Regex("\\\"([a-z_]+)\\\"\\s*:")
    private val MINECRAFT_BLOCK_ID_PATTERN = Regex("minecraft:[a-z0-9_./-]+")
    private val TEXTURE_FIELDS = setOf("block", "weight")
    private val CHILD_FIELDS = mapOf(
        "scale" to setOf("initial", "peak", "peak_progress", "scale_in_ticks", "variation"),
        "rotation" to setOf("random_initial", "angular_velocity", "variation"),
        "lifetime" to setOf("ticks", "variation", "fade_out_ticks", "fade_variation", "spawn_delay"),
        "motion" to setOf(
            "preset", "initial_velocity", "acceleration", "retention", "turbulence", "frequency",
            "radial_speed", "spawn_radius", "orbit_speed", "radial_pull", "attraction", "max_speed"
        ),
        "collision" to setOf("mode", "restitution"),
        "emission" to setOf("offset", "delta", "speed", "count", "visibility")
    )
    private val REQUIRED_CHILD_FIELDS = mapOf(
        "scale" to CHILD_FIELDS.getValue("scale"),
        "rotation" to CHILD_FIELDS.getValue("rotation"),
        "lifetime" to CHILD_FIELDS.getValue("lifetime"),
        "motion" to setOf("preset"),
        "collision" to setOf("mode"),
        "emission" to CHILD_FIELDS.getValue("emission")
    )

    /** Bukkit Registryが利用可能なサーバー起動後にだけ呼び出します。 */
    fun availableBlockChoices(): List<DisplayParticleBookStringChoice> = Material.entries.asSequence()
        .filter { material ->
            material.isBlock && material != Material.AIR && !material.isLegacy &&
                runCatching(material::createBlockData).isSuccess
        }
        .map {
            DisplayParticleBookStringChoice(
                "minecraft:${it.key.key}",
                "management.debug.particle_test_choice.block"
            )
        }
        .sortedBy(DisplayParticleBookStringChoice::value)
        .toList()

    private data class ExtractedFragment(
        val key: String,
        val value: JsonElement,
        val hasOutsideText: Boolean
    )
}
