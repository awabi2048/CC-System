package com.awabi2048.ccsystem.api.gesturegui.html

import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiTheme
import com.awabi2048.ccsystem.api.gesturegui.theme.GestureGuiThemeTokens
import java.lang.reflect.Proxy
import net.kyori.adventure.text.Component
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * テーマ環境の解決順テストです。サーバーなしで実行できます。
 *
 * `BlockData` は Proxy、`ItemStack` はコンストラクタを経由しない空インスタンスで
 * 代用します。`Material.createBlockData`・`ItemStack.of` は RegistryAccess を要求し
 * サーバーなしでは評価できないため、生素材フォールバック（`rawBlock` / `rawItem` の
 * 既定実装）は検証対象外とし、トークン解決・ID登録・class 意図・フォールバック経路の
 * 順序だけを検証します。
 */
class GestureGuiThemedEnvironmentTest {
    private fun blockData(): BlockData = Proxy.newProxyInstance(
        BlockData::class.java.classLoader,
        arrayOf(BlockData::class.java),
    ) { _, _, _ -> null } as BlockData

    /** コンストラクタを通らない ItemStack です。同一性の検証専用で、メソッドは呼べません。 */
    private fun dummyItemStack(): ItemStack {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as sun.misc.Unsafe
        @Suppress("UNCHECKED_CAST")
        return unsafe.allocateInstance(ItemStack::class.java) as ItemStack
    }

    private val surface = blockData()
    private val accent = blockData()
    private val accentDeep = blockData()
    private val danger = blockData()
    private val back = blockData()
    private val on = blockData()
    private val off = blockData()
    private val outlineSelected = blockData()
    private val outlineSet = blockData()
    private val navPrev = dummyItemStack()

    private val theme = GestureGuiTheme(
        surface = surface,
        accent = accent,
        accentDeep = accentDeep,
        danger = danger,
        back = back,
        on = on,
        off = off,
        outlineSelected = outlineSelected,
        outlineSet = outlineSet,
        items = mapOf(GestureGuiThemeTokens.NAV_PREV to navPrev),
    )

    private val environment = GestureGuiThemedEnvironment(theme)

    @Test
    fun `background resolves block tokens`() {
        assertSame(surface, environment.buttonBackground("surface", emptySet(), null))
        assertSame(accentDeep, environment.buttonBackground("accent-deep", emptySet(), null))
        assertSame(accentDeep, environment.buttonBackground("accent_deep", emptySet(), null))
        assertSame(danger, environment.containerBackground("danger", emptySet(), null))
    }

    @Test
    fun `button background defaults to surface when unspecified`() {
        assertSame(surface, environment.buttonBackground(null, emptySet(), null))
    }

    @Test
    fun `unknown explicit background is an error`() {
        assertThrows(IllegalStateException::class.java) {
            environment.buttonBackground("not-a-material", emptySet(), null)
        }
        assertThrows(IllegalStateException::class.java) {
            environment.blockData("not-a-material", emptySet(), null)
        }
    }

    @Test
    fun `id registered block wins over css value`() {
        val override = blockData()
        val env = GestureGuiThemedEnvironment(theme, blockById = { id -> if (id == "card-1") override else null })
        assertSame(override, env.buttonBackground("danger", emptySet(), "card-1"))
        assertSame(override, env.containerBackground("danger", emptySet(), "card-1"))
    }

    @Test
    fun `outline resolves state classes with selected precedence`() {
        assertSame(outlineSelected, environment.outlineBlock(setOf("selected"), null))
        assertSame(outlineSet, environment.outlineBlock(setOf("set"), null))
        assertSame(outlineSelected, environment.outlineBlock(setOf("set", "selected"), null))
        assertNull(environment.outlineBlock(setOf("card"), null))
        assertNull(environment.outlineBlock(emptySet(), null))
    }

    @Test
    fun `outline id registration wins over state class`() {
        val override = blockData()
        val env = GestureGuiThemedEnvironment(theme, outlineById = { id -> if (id == "card-1") override else null })
        assertSame(override, env.outlineBlock(setOf("selected"), "card-1"))
    }

    @Test
    fun `text size resolves role classes and falls back to body`() {
        assertEquals(0.006, environment.textSize("p", setOf("title"), null))
        assertEquals(0.005, environment.textSize("span", setOf("label"), null))
        assertEquals(0.0045, environment.textSize("p", setOf("caption"), null))
        assertEquals(0.0055, environment.textSize("p", emptySet(), null))
        assertEquals(0.0055, environment.textSize("p", setOf("unrelated"), null))
    }

    @Test
    fun `text size id override wins over role class`() {
        val env = GestureGuiThemedEnvironment(theme, textSizeById = { id -> if (id == "big") 0.02 else null })
        assertEquals(0.02, env.textSize("p", setOf("caption"), "big"))
    }

    @Test
    fun `item resolves theme glyph then raw fallback then error`() {
        assertSame(navPrev, environment.itemStack("nav-prev", emptySet(), null))

        val fallback = dummyItemStack()
        val env = object : GestureGuiThemedEnvironment(theme) {
            override fun rawItem(reference: String): ItemStack? =
                if (reference == "external-thing") fallback else null
        }
        assertSame(fallback, env.itemStack("external-thing", emptySet(), null))
        assertThrows(IllegalStateException::class.java) {
            env.itemStack("not-an-item", emptySet(), null)
        }
    }

    @Test
    fun `extra blocks and text sizes extend the vocabulary`() {
        val custom = blockData()
        val themed = theme.copy(
            extraBlocks = mapOf("panel-cta" to custom),
            extraTextSizes = mapOf("display" to 0.01),
        )
        val env = GestureGuiThemedEnvironment(themed)
        assertSame(custom, env.containerBackground("panel-cta", emptySet(), null))
        assertSame(custom, env.containerBackground("panel_cta", emptySet(), null))
        assertEquals(0.01, env.textSize("p", setOf("display"), null))
    }

    @Test
    fun `behavior lookups stay id keyed`() {
        val registered = Component.text("registered")
        val env = GestureGuiThemedEnvironment(
            theme,
            textById = { id -> if (id == "label-1") registered else null },
        )
        assertSame(registered, env.textComponent("ignored", "p", emptySet(), "label-1"))
        assertEquals(Component.text("raw"), env.textComponent("raw", "p", emptySet(), "other"))
    }
}
