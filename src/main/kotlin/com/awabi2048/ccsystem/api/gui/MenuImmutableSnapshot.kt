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
enum class MenuSnapshotFailureReason { INVALID_VALUE_TYPE, INVALID_STATE_DEPTH, INVALID_STATE_SIZE }

class MenuSnapshotValueException internal constructor(
    val reason: MenuSnapshotFailureReason,
    message: String,
) : IllegalArgumentException(message)

/**
 * token保存・監査出力用の値codecです。任意のobject graphを一般に安全コピーすることはしません。
 * Bukkit handle、Player、World、mutable collection、独自classはIDまたはallowlist値へ明示変換してください。
 */
object MenuSnapshotCodec {
    const val MAX_DEPTH = 32
    const val MAX_NODES = 4096
    const val MAX_STRING_UTF8_BYTES = 65_536
    const val MAX_TOTAL_ENCODED_BYTES = 262_144
    const val MAX_COLLECTION_SIZE = 1024

    /** 許可値を再帰的に切り離します。未知型は [MenuSnapshotValueException] で拒否します。 */
    fun snapshot(value: Any?): MenuImmutableSnapshot =
        MenuImmutableSnapshot.detached(copy(value, "$", IdentityHashMap(), Budget(), 0)).also { snapshot ->
            val bytes = snapshot.jsonEvidenceText().toByteArray(Charsets.UTF_8).size
            if (bytes > MAX_TOTAL_ENCODED_BYTES) sizeFailure("encoded snapshot is $bytes bytes; maximum is $MAX_TOTAL_ENCODED_BYTES")
        }

    /** 許可値なら切り離したsnapshot、未知型ならnullです。診断用attributesで利用します。 */
    fun snapshotOrNull(value: Any?): MenuImmutableSnapshot? = try {
        snapshot(value)
    } catch (_: MenuSnapshotValueException) {
        null
    }

    private class Budget(var nodes: Int = 0, var encodedBytes: Int = 0) {
        fun charge(bytes: Int, message: String) {
            if (bytes > MAX_TOTAL_ENCODED_BYTES - encodedBytes) {
                sizeFailure("$message exceeds maximum encoded snapshot size $MAX_TOTAL_ENCODED_BYTES")
            }
            encodedBytes += bytes
        }
    }

    private fun copy(value: Any?, path: String, ancestors: IdentityHashMap<Any, Unit>, budget: Budget, depth: Int): Any? {
        if (depth > MAX_DEPTH) throw MenuSnapshotValueException(MenuSnapshotFailureReason.INVALID_STATE_DEPTH, "$path exceeds maximum depth $MAX_DEPTH")
        if (++budget.nodes > MAX_NODES) sizeFailure("snapshot exceeds maximum node count $MAX_NODES")
        return when (value) {
        null -> value.also { budget.charge(4, path) }
        is Boolean -> value.also { budget.charge(if (value) 4 else 5, path) }
        is Byte, is Short, is Int, is Long -> value.also { budget.charge(value.toString().length, path) }
        is BigInteger -> value.also {
            val estimated = ((value.bitLength().toLong() * 30103L) / 100000L + 2L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            budget.charge(estimated, path)
        }
        is BigDecimal -> value.also {
            val estimated = (value.unscaledValue().bitLength().toLong() * 30103L / 100000L + kotlin.math.abs(value.scale().toLong()) + 8L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            budget.charge(estimated, path)
        }
        is String -> value.also { checkString(it, path, budget) }
        is Float -> if (value.isFinite()) value.also { budget.charge(value.toString().length, path) } else throw MenuSnapshotValueException(MenuSnapshotFailureReason.INVALID_VALUE_TYPE, "$path contains a non-finite Float")
        is Double -> if (value.isFinite()) value.also { budget.charge(value.toString().length, path) } else throw MenuSnapshotValueException(MenuSnapshotFailureReason.INVALID_VALUE_TYPE, "$path contains a non-finite Double")
        is UUID -> value.also { budget.charge(64, path) }
        is Enum<*> -> value.also {
            budget.charge(48, path)
            chargeString(value.declaringJavaClass.name, "$path enum class", budget)
            chargeString(value.name, "$path enum name", budget)
        }
        is MenuRoute -> {
            budget.charge(64, path)
            chargeString(value.owner, "$path.owner", budget)
            chargeString(value.id, "$path.id", budget)
            MenuRoute(value.owner, value.id, copyMap(value.payload, "$path.payload", ancestors, budget, depth + 1).mapValues { it.value as String })
        }
        is MenuRuntimeRouteSnapshot -> {
            budget.charge(72, path)
            chargeString(value.owner, "$path.owner", budget)
            chargeString(value.id, "$path.id", budget)
            MenuRuntimeRouteSnapshot(value.owner, value.id, copyMap(value.payload, "$path.payload", ancestors, budget, depth + 1).mapValues { it.value as String })
        }
        is Map<*, *> -> withAncestor(value, path, ancestors) { copyMap(value, path, ancestors, budget, depth) }
        is List<*> -> withAncestor(value, path, ancestors) {
            checkCollection(value.size, path)
            budget.charge(2 + (value.size - 1).coerceAtLeast(0), path)
            Collections.unmodifiableList(value.mapIndexed { index, nested -> copy(nested, "$path[$index]", ancestors, budget, depth + 1) })
        }
        is Set<*> -> withAncestor(value, path, ancestors) {
            checkCollection(value.size, path)
            budget.charge(2 + (value.size - 1).coerceAtLeast(0), path)
            val copied = value.mapIndexed { index, nested -> copy(nested, "$path[$index]", ancestors, budget, depth + 1) }
                .sortedBy { canonicalJson(jsonEvidence(it)) }
            Collections.unmodifiableSet(LinkedHashSet(copied))
        }
        else -> throw MenuSnapshotValueException(MenuSnapshotFailureReason.INVALID_VALUE_TYPE,
            "$path has unsupported snapshot value type: ${value.javaClass.name}",
        )
        }
    }

    private inline fun <T> withAncestor(
        value: Any,
        path: String,
        ancestors: IdentityHashMap<Any, Unit>,
        block: () -> T,
    ): T {
        if (ancestors.put(value, Unit) != null) {
            throw MenuSnapshotValueException(MenuSnapshotFailureReason.INVALID_VALUE_TYPE, "$path contains a cyclic collection reference")
        }
        return try {
            block()
        } finally {
            ancestors.remove(value)
        }
    }

    private fun copyMap(value: Map<*, *>, path: String, ancestors: IdentityHashMap<Any, Unit>, budget: Budget, depth: Int): Map<String, Any?> {
        checkCollection(value.size, path)
        budget.charge(2 + (value.size - 1).coerceAtLeast(0), path)
        val entries = value.entries.map { entry ->
            val key = entry.key as? String
                ?: throw MenuSnapshotValueException(MenuSnapshotFailureReason.INVALID_VALUE_TYPE, "$path has a non-String map key: ${entry.key?.javaClass?.name ?: "null"}")
            chargeString(key, "$path key", budget)
            budget.charge(1, "$path key separator")
            key to copy(entry.value, "$path.$key", ancestors, budget, depth + 1)
        }.sortedBy { it.first }
        return Collections.unmodifiableMap(LinkedHashMap<String, Any?>(entries.size).also { target ->
            entries.forEach { (key, nested) -> target[key] = nested }
        })
    }

    private fun checkCollection(size: Int, path: String) {
        if (size > MAX_COLLECTION_SIZE) sizeFailure("$path has $size entries; maximum is $MAX_COLLECTION_SIZE")
    }

    private fun checkString(value: String, path: String, budget: Budget) {
        val bytes = utf8Bytes(value)
        if (bytes > MAX_STRING_UTF8_BYTES) sizeFailure("$path is $bytes UTF-8 bytes; maximum is $MAX_STRING_UTF8_BYTES")
        budget.charge(jsonStringBytes(value), path)
    }

    private fun chargeString(value: String, path: String, budget: Budget) = checkString(value, path, budget)

    private fun utf8Bytes(value: String): Int = countCodePoints(value, 0) { codePoint -> when {
        codePoint <= 0x7f -> 1
        codePoint <= 0x7ff -> 2
        codePoint <= 0xffff -> 3
        else -> 4
    } }

    private fun jsonStringBytes(value: String): Int = countCodePoints(value, 2) { codePoint -> when (codePoint) {
        0x22, 0x5c, 0x08, 0x0c, 0x0a, 0x0d, 0x09 -> 2
        in 0..0x1f -> 6
        in 0..0x7f -> 1
        in 0..0x7ff -> 2
        in 0..0xffff -> 3
        else -> 4
    } }

    private inline fun countCodePoints(value: String, initial: Int, bytes: (Int) -> Int): Int {
        var total = initial.toLong()
        val iterator = value.codePoints().iterator()
        while (iterator.hasNext()) {
            total += bytes(iterator.nextInt())
            if (total > Int.MAX_VALUE) return Int.MAX_VALUE
        }
        return total.toInt()
    }

    private fun sizeFailure(message: String): Nothing =
        throw MenuSnapshotValueException(MenuSnapshotFailureReason.INVALID_STATE_SIZE, message)

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
