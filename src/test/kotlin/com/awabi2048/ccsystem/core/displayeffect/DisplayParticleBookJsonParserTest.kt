package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionMode
import org.junit.jupiter.api.Assertions.assertEquals
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
        val parsed = DisplayParticleBookJsonParser.parsePages(validPages())

        assertEquals(listOf(3, 1), parsed.preset.textures.map { it.weight })
        assertEquals(4, parsed.request.count)
    }

    @Test
    fun `同一ページの複数項目と項目の重複を拒否する`() {
        assertThrows(IllegalArgumentException::class.java) {
            DisplayParticleBookJsonParser.parsePages(
                validPages().toMutableList().also {
                    it[0] = it[0] + "," + it[1]
                    it.removeAt(1)
                }
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            DisplayParticleBookJsonParser.parsePages(validPages().toMutableList().also { it[1] = it[0] })
        }
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

    private fun validPages() = listOf(
        "\"textures\":[{\"block\":\"minecraft:orange_concrete\",\"weight\":3},{\"block\":\"minecraft:yellow_concrete\",\"weight\":1}]",
        "\"scale\":{\"initial\":0.05,\"peak\":0.12,\"peak_progress\":0.3,\"scale_in_ticks\":3,\"variation\":0.2}",
        "\"rotation\":{\"random_initial\":true,\"angular_velocity\":[0.08,0.13,0.05],\"variation\":0.3}",
        "\"lifetime\":{\"ticks\":24,\"variation\":2,\"fade_out_ticks\":7,\"fade_variation\":1,\"spawn_delay\":2}",
        "\"motion\":{\"preset\":\"burst\",\"initial_velocity\":[0,0.03,0],\"radial_speed\":0.15}",
        "\"collision\":{\"mode\":\"remove\"}",
        "\"emission\":{\"offset\":[0,1,0],\"delta\":[0.1,0.1,0.1],\"speed\":0.02,\"count\":4,\"visibility\":\"force\"}"
    )
}
