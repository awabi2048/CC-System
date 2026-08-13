package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DisplayParticleBookJsonParserTest {
    @Test
    fun `重み付きテクスチャを含む本JSONを一時定義へ変換する`() {
        val parsed = DisplayParticleBookJsonParser.parse(validJson())

        assertEquals(listOf(3, 1), parsed.preset.textures.map { it.weight })
        assertEquals("cc:burst", parsed.request.motionPresetId.value)
        assertEquals(DisplayParticleCollisionMode.REMOVE, parsed.request.collisionMode)
        assertEquals(4, parsed.request.count)
        assertEquals(0.15, parsed.request.motionProperties.radialSpeed)
    }

    @Test
    fun `未知項目と動作に無効なプロパティを拒否する`() {
        assertThrows(IllegalArgumentException::class.java) {
            DisplayParticleBookJsonParser.parse(validJson().replace("\"textures\":", "\"unknown\":1,\"textures\":"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            DisplayParticleBookJsonParser.parse(
                validJson().replace("\"preset\":\"burst\"", "\"preset\":\"static\"")
            )
        }
    }

    @Test
    fun `ページごとの項目を空白なしの単一JSONとして解析する`() {
        val parsed = DisplayParticleBookJsonParser.parsePages(DisplayParticleBookSample.pages)

        assertEquals(listOf(3, 1), parsed.preset.textures.map { it.weight })
        assertEquals(4, parsed.request.count)
    }

    @Test
    fun `初期サンプルは本の物理ページ制限内で7項目を保持する`() {
        assertEquals(7, DisplayParticleBookSample.pages.size)
        assertTrue(DisplayParticleBookSample.pages.all { it.length <= 1_024 })
        assertTrue(DisplayParticleBookSample.pages.all { page ->
            com.google.gson.JsonParser.parseString("{$page}").asJsonObject.size() == 1
        })
    }

    @Test
    fun `項目の重複を削除が必要な修正として返す`() {
        val prepared = DisplayParticleBookJsonParser.preparePages(
            DisplayParticleBookSample.pages.toMutableList().also { it[1] = it[0] }
        )

        assertTrue("textures(duplicate)" in prepared.removedPaths)
        assertTrue("scale" in prepared.addedPaths)
    }

    @Test
    fun `ページ項目の外側にある文字列を破棄する`() {
        val decorated = DisplayParticleBookSample.pages.mapIndexed { index, page ->
            "ページ${index + 1} \"memo\":{\"ignored\":true} $page この外側は無視"
        }

        val parsed = DisplayParticleBookJsonParser.parsePages(decorated)

        assertEquals(4, parsed.request.count)
    }

    @Test
    fun `文字列値が不正な場合は使用可能な候補を返す`() {
        val pages = DisplayParticleBookSample.pages.toMutableList().also {
            it[4] = it[4].replace("\"burst\"", "\"unknown\"")
        }

        val failure = assertThrows(DisplayParticleBookStringChoiceException::class.java) {
            DisplayParticleBookJsonParser.parsePages(pages)
        }

        assertEquals("motion.preset", failure.field)
        assertTrue(failure.choices.any { it.value == "burst" })
    }

    @Test
    fun `不足項目をサンプル値で補完する`() {
        val incomplete = DisplayParticleBookSample.pages.dropLast(1).toMutableList().also {
            it[1] = "\"scale\":{\"initial\":0.05}"
        }

        val prepared = DisplayParticleBookJsonParser.preparePages(incomplete)

        assertTrue("emission" in prepared.addedPaths)
        assertTrue("scale.peak" in prepared.addedPaths)
        assertTrue(prepared.removedPaths.isEmpty())
        assertEquals(7, prepared.pages.size)
    }

    @Test
    fun `削除を伴う修正を保留情報として返す`() {
        val decorated = DisplayParticleBookSample.pages.toMutableList().also {
            it[1] = "前置き ${it[1]} 後置き"
            it[2] = it[2].replace("}", ",\"unknown\":1}")
        }

        val prepared = DisplayParticleBookJsonParser.preparePages(decorated)

        assertTrue(prepared.removedPaths.any { it.endsWith(".outside") })
        assertTrue("rotation.unknown" in prepared.removedPaths)
        assertEquals(7, prepared.pages.size)
    }

    private fun validJson() = """
        {
          "textures":[
            {"block":"minecraft:orange_concrete","weight":3},
            {"block":"minecraft:yellow_concrete","weight":1}
          ],
          "scale":{"initial":0.05,"peak":0.12,"peak_progress":0.3,"scale_in_ticks":3,"variation":0.2},
          "rotation":{"random_initial":true,"angular_velocity":[0.08,0.13,0.05],"variation":0.3},
          "lifetime":{"ticks":24,"variation":2,"fade_out_ticks":7,"fade_variation":1,"spawn_delay":2},
          "motion":{"preset":"burst","initial_velocity":[0,0.03,0],"radial_speed":0.15},
          "collision":{"mode":"remove"},
          "emission":{"offset":[0,1,0],"delta":[0.1,0.1,0.1],"speed":0.02,"count":4,"visibility":"force"}
        }
    """.trimIndent()

}
