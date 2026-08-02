package com.awabi2048.ccsystem.api.gui

import java.util.UUID
import java.util.AbstractList
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuImmutableSnapshotTest {
    @Test
    fun `route contract and interaction detach mutable source maps in stable key order`() {
        val routeSource = linkedMapOf("z" to "last", "a" to "first")
        val contractSource = linkedMapOf("operation" to "toggle")
        val argumentsSource = linkedMapOf("world" to "one")
        val routePayloadSource = linkedMapOf("page" to "2")
        val attributesSource = linkedMapOf<String, Any>("handle" to Any())

        val route = MenuRoute("test", "menu", routeSource)
        val runtimeRoute = MenuRuntimeRouteSnapshot("test", "menu", routeSource)
        val contract = MenuReversibleContract("test:state", contractSource)
        val interaction = MenuReversibleInteractionContext(
            4,
            ClickType.LEFT,
            "toggle",
            null,
            contract,
            9L,
            argumentsSource,
            attributesSource,
            routePayloadSource,
        )

        routeSource["a"] = "changed"
        contractSource["operation"] = "changed"
        argumentsSource["world"] = "changed"
        routePayloadSource["page"] = "changed"
        attributesSource["later"] = "not captured"

        assertEquals(listOf("a", "z"), route.payload.keys.toList())
        assertEquals("first", route.payload["a"])
        assertEquals("first", runtimeRoute.payload["a"])
        assertEquals("toggle", contract.arguments["operation"])
        assertEquals(mapOf("world" to "one"), interaction.arguments)
        assertEquals(mapOf("page" to "2"), interaction.routePayload)
        assertEquals(setOf("handle"), interaction.attributes.keys)
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (route.payload as MutableMap<String, String>)["x"] = "y"
        }
    }

    @Test
    fun `codec deep copies nested collections and emits deterministic JSON evidence`() {
        val nested = mutableListOf<Any?>("before", mutableMapOf("count" to 1))
        val source = linkedMapOf<String, Any?>(
            "route" to MenuRoute("test", "detail", mapOf("world" to "alpha")),
            "ids" to linkedSetOf(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000001")),
            "nested" to nested,
        )

        val snapshot = MenuSnapshotCodec.snapshot(source)
        nested[0] = "after"
        @Suppress("UNCHECKED_CAST")
        (nested[1] as MutableMap<String, Int>)["count"] = 2

        @Suppress("UNCHECKED_CAST")
        val copied = snapshot.value as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val copiedNested = copied.getValue("nested") as List<Any?>
        assertEquals("before", copiedNested[0])
        assertEquals(mapOf("count" to 1), copiedNested[1])
        assertNotSame(source, copied)
        assertEquals(snapshot, MenuSnapshotCodec.snapshot(sourceForSameEvidence()))
        assertEquals(snapshot.hashCode(), MenuSnapshotCodec.snapshot(sourceForSameEvidence()).hashCode())
        assertEquals(snapshot.jsonEvidenceText(), MenuSnapshotCodec.snapshot(sourceForSameEvidence()).jsonEvidenceText())
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (copiedNested as MutableList<Any?>).add("forbidden")
        }
    }

    @Test
    fun `codec rejects unknown mutable values non string keys and collection cycles`() {
        assertThrows(MenuSnapshotValueException::class.java) { MenuSnapshotCodec.snapshot(StringBuilder("mutable")) }
        assertThrows(MenuSnapshotValueException::class.java) { MenuSnapshotCodec.snapshot(mapOf(1 to "invalid")) }

        val cyclic = mutableListOf<Any?>()
        cyclic.add(cyclic)
        assertThrows(MenuSnapshotValueException::class.java) { MenuSnapshotCodec.snapshot(cyclic) }
    }

    @Test
    fun `provider state is detached at capture and rejects unknown values`() {
        val source = mutableListOf("before")
        val state = object : MenuReversibleOpaqueState {
            override fun snapshot(): Any = mapOf("values" to source)
        }
        val captured = MenuReversibleStateSnapshot.capture(state)
        source[0] = "after"

        @Suppress("UNCHECKED_CAST")
        val value = captured.value as Map<String, Any?>
        assertEquals(listOf("before"), value["values"])
        assertThrows(MenuSnapshotValueException::class.java) {
            MenuReversibleStateSnapshot.capture(object : MenuReversibleOpaqueState {
                override fun snapshot(): Any = Any()
            })
        }
    }

    @Test
    fun `codec enforces depth node collection string and total encoded limits`() {
        var deep: Any? = "leaf"
        repeat(MenuSnapshotCodec.MAX_DEPTH + 1) { deep = listOf(deep) }
        assertEquals(MenuSnapshotFailureReason.INVALID_STATE_DEPTH, failureOf { MenuSnapshotCodec.snapshot(deep) }.reason)

        val oversizedCollection = List(MenuSnapshotCodec.MAX_COLLECTION_SIZE + 1) { it }
        assertEquals(MenuSnapshotFailureReason.INVALID_STATE_SIZE, failureOf { MenuSnapshotCodec.snapshot(oversizedCollection) }.reason)

        val unicode = "界".repeat(MenuSnapshotCodec.MAX_STRING_UTF8_BYTES / 3 + 1)
        assertTrue(unicode.length < MenuSnapshotCodec.MAX_STRING_UTF8_BYTES)
        assertEquals(MenuSnapshotFailureReason.INVALID_STATE_SIZE, failureOf { MenuSnapshotCodec.snapshot(unicode) }.reason)

        val individuallyValid = "a".repeat(MenuSnapshotCodec.MAX_STRING_UTF8_BYTES)
        assertEquals(MenuSnapshotFailureReason.INVALID_STATE_SIZE, failureOf {
            MenuSnapshotCodec.snapshot(List(5) { individuallyValid })
        }.reason)

        val manyNodes = List(MenuSnapshotCodec.MAX_COLLECTION_SIZE) {
            List(4) { listOf(it) }
        }
        assertEquals(MenuSnapshotFailureReason.INVALID_STATE_SIZE, failureOf { MenuSnapshotCodec.snapshot(manyNodes) }.reason)

        val oversizedKey = "鍵".repeat(MenuSnapshotCodec.MAX_STRING_UTF8_BYTES / 3 + 1)
        assertEquals(MenuSnapshotFailureReason.INVALID_STATE_SIZE, failureOf {
            MenuSnapshotCodec.snapshot(mapOf(oversizedKey to "value"))
        }.reason)
    }

    @Test
    fun `total budget rejects while reading only the bounded prefix`() {
        val reads = AtomicInteger()
        val chunk = "\\\"\n界".repeat(12_000)
        val lazyLarge = object : AbstractList<String>() {
            override val size: Int = 1_000
            override fun get(index: Int): String {
                reads.incrementAndGet()
                return chunk
            }
        }
        val failure = failureOf { MenuSnapshotCodec.snapshot(lazyLarge) }
        assertEquals(MenuSnapshotFailureReason.INVALID_STATE_SIZE, failure.reason)
        assertTrue(reads.get() < 10, "総量超過後もcollectionを走査しています: ${reads.get()}")
    }

    private fun failureOf(block: () -> Unit): MenuSnapshotValueException =
        assertThrows(MenuSnapshotValueException::class.java, block)

    private fun sourceForSameEvidence(): Map<String, Any?> = linkedMapOf(
        "nested" to listOf("before", mapOf("count" to 1)),
        "ids" to linkedSetOf(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002")),
        "route" to MenuRoute("test", "detail", mapOf("world" to "alpha")),
    )
}
