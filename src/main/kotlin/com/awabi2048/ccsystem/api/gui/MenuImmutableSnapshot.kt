package com.awabi2048.ccsystem.api.gui

import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID
import java.math.BigDecimal
import java.math.BigInteger

/**
 * 可逆 token と監査証跡へ保存できる値の、切り離し済み不変 snapshot です。
 *
 * [value] は [MenuSnapshotCodec] が許可した値だけです。Map は String key のキー順、
 * List は入力順、Set は値の正規表現順で固定され、いずれも変更不能です。
 */
class MenuImmutableSnapshot private constructor(
    val value: Any?,
) {
    /** JSON証跡へそのまま渡せる、順序固定済みの値です。UUID・enum・routeもJSON表現へ正規化します。 */
    fun jsonEvidence(): Any? = MenuSnapshotCodec.jsonEvidence(value)

    /** ログや監査artifactへ保存できる決定的なJSON文字列です。 */
    fun jsonEvidenceText(): String = MenuSnapshotCodec.canonicalJson(jsonEvidence())

    override fun equals(other: Any?): Boolean =
        other is MenuImmutableSnapshot && value == other.value

    override fun hashCode(): Int = value?.hashCode() ?: 0

    override fun toString(): String = "MenuImmutableSnapshot(${MenuSnapshotCodec.canonicalJson(jsonEvidence())})"

    internal companion object {
        fun detached(value: Any?): MenuImmutableSnapshot = MenuImmutableSnapshot(value)
    }
}

/** [MenuSnapshotCodec] が受理できない値です。可逆状態はこの値を token に保存できません。 */
class MenuSnapshotValueException internal constructor(
    message: String,
) : IllegalArgumentException(message)

/**
 * token保存・監査出力用の値codecです。任意のobject graphを一般に安全コピーすることはしません。
 * Bukkit handle、Player、World、mutable collection、独自classはIDまたはallowlist値へ明示変換してください。
 */
object MenuSnapshotCodec {
    /** 許可値を再帰的に切り離します。未知型は [MenuSnapshotValueException] で拒否します。 */
    fun snapshot(value: Any?): MenuImmutableSnapshot =
        MenuImmutableSnapshot.detached(copy(value, "$", IdentityHashMap()))

    /** 許可値なら切り離したsnapshot、未知型ならnullです。診断用attributesで利用します。 */
    fun snapshotOrNull(value: Any?): MenuImmutableSnapshot? = try {
        snapshot(value)
    } catch (_: MenuSnapshotValueException) {
        null
    }

    private fun copy(value: Any?, path: String, ancestors: IdentityHashMap<Any, Unit>): Any? = when (value) {
        null,
        is String,
        is Boolean,
        is Byte,
        is Short,
        is Int,
        is Long,
        is BigInteger,
        is BigDecimal -> value
        is Float -> if (value.isFinite()) value else throw MenuSnapshotValueException("$path contains a non-finite Float")
        is Double -> if (value.isFinite()) value else throw MenuSnapshotValueException("$path contains a non-finite Double")
        is UUID -> value
        is Enum<*> -> value
        is MenuRoute -> MenuRoute(value.owner, value.id, value.payload)
        is MenuRuntimeRouteSnapshot -> MenuRuntimeRouteSnapshot(value.owner, value.id, value.payload)
        is Map<*, *> -> withAncestor(value, path, ancestors) { copyMap(value, path, ancestors) }
        is List<*> -> withAncestor(value, path, ancestors) {
            Collections.unmodifiableList(value.mapIndexed { index, nested -> copy(nested, "$path[$index]", ancestors) })
        }
        is Set<*> -> withAncestor(value, path, ancestors) {
            val copied = value.mapIndexed { index, nested -> copy(nested, "$path[$index]", ancestors) }
                .sortedBy { canonicalJson(jsonEvidence(it)) }
            Collections.unmodifiableSet(LinkedHashSet(copied))
        }
        else -> throw MenuSnapshotValueException(
            "$path has unsupported snapshot value type: ${value.javaClass.name}",
        )
    }

    private inline fun <T> withAncestor(
        value: Any,
        path: String,
        ancestors: IdentityHashMap<Any, Unit>,
        block: () -> T,
    ): T {
        if (ancestors.put(value, Unit) != null) {
            throw MenuSnapshotValueException("$path contains a cyclic collection reference")
        }
        return try {
            block()
        } finally {
            ancestors.remove(value)
        }
    }

    private fun copyMap(value: Map<*, *>, path: String, ancestors: IdentityHashMap<Any, Unit>): Map<String, Any?> {
        val entries = value.entries.map { entry ->
            val key = entry.key as? String
                ?: throw MenuSnapshotValueException("$path has a non-String map key: ${entry.key?.javaClass?.name ?: "null"}")
            key to copy(entry.value, "$path.$key", ancestors)
        }.sortedBy { it.first }
        return Collections.unmodifiableMap(LinkedHashMap<String, Any?>(entries.size).also { target ->
            entries.forEach { (key, nested) -> target[key] = nested }
        })
    }

    internal fun jsonEvidence(value: Any?): Any? = when (value) {
        null,
        is String,
        is Boolean,
        is Byte,
        is Short,
        is Int,
        is Long,
        is Float,
        is Double,
        is BigInteger,
        is BigDecimal -> value
        is UUID -> linkedMapOf("${'$'}type" to "uuid", "value" to value.toString())
        is Enum<*> -> linkedMapOf("${'$'}type" to "enum", "class" to value.declaringJavaClass.name, "name" to value.name)
        is MenuRoute -> linkedMapOf(
            "${'$'}type" to "menu_route",
            "owner" to value.owner,
            "id" to value.id,
            "payload" to jsonEvidence(value.payload),
        )
        is MenuRuntimeRouteSnapshot -> linkedMapOf(
            "${'$'}type" to "menu_runtime_route",
            "owner" to value.owner,
            "id" to value.id,
            "payload" to jsonEvidence(value.payload),
        )
        is Map<*, *> -> Collections.unmodifiableMap(LinkedHashMap<String, Any?>(value.size).also { target ->
            value.entries.sortedBy { it.key as String }.forEach { (key, nested) ->
                target[key as String] = jsonEvidence(nested)
            }
        })
        is List<*> -> Collections.unmodifiableList(value.map(::jsonEvidence))
        is Set<*> -> Collections.unmodifiableList(value.map(::jsonEvidence).sortedBy(::canonicalJson))
        else -> error("internal snapshot codec invariant violated: ${value.javaClass.name}")
    }

    internal fun canonicalJson(value: Any?): String = when (value) {
        null -> "null"
        is String -> jsonString(value)
        is Boolean,
        is Byte,
        is Short,
        is Int,
        is Long,
        is Float,
        is Double,
        is BigInteger,
        is BigDecimal -> value.toString()
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, nested) ->
            canonicalJson(key as String) + ":" + canonicalJson(nested)
        }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]", separator = ",", transform = ::canonicalJson)
        else -> error("JSON evidence must contain JSON-compatible values, got ${value.javaClass.name}")
    }

    private fun jsonString(value: String): String = buildString(value.length + 2) {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
            }
        }
        append('"')
    }
}

internal object MenuImmutableCollections {
    @Suppress("UNCHECKED_CAST")
    fun strings(source: Map<String, String>): Map<String, String> =
        MenuSnapshotCodec.snapshot(source).value as Map<String, String>

    fun <T> orderedSet(source: Set<T>, comparator: Comparator<in T>? = null): Set<T> {
        val values = source.toList().let { values -> comparator?.let { values.sortedWith(it) } ?: values }
        return Collections.unmodifiableSet(LinkedHashSet(values))
    }

    fun <K, V> orderedMap(source: Map<K, V>, comparator: Comparator<in K>? = null): Map<K, V> {
        val entries = source.entries.toList().let { entries -> comparator?.let { entries.sortedWith(compareBy(it) { entry -> entry.key }) } ?: entries }
        return Collections.unmodifiableMap(LinkedHashMap<K, V>(entries.size).also { target ->
            entries.forEach { entry -> target[entry.key] = entry.value }
        })
    }

    fun <T> list(source: List<T>): List<T> = Collections.unmodifiableList(ArrayList(source))
}
