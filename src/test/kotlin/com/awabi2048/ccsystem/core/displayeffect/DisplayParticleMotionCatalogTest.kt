package com.awabi2048.ccsystem.core.displayeffect

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisplayParticleMotionCatalogTest {
    @Test
    fun `初期移動プリセット8種を重複なく登録する`() {
        val presets = DisplayParticleMotionCatalog.list()
        assertEquals(8, presets.size)
        assertEquals(DisplayParticleMotionKind.entries.toSet(), presets.map { it.kind }.toSet() + DisplayParticleMotionKind.BURST)
        assertEquals(presets.size, presets.map { it.id }.distinct().size)
        assertTrue(DisplayParticleMotionCatalog.require("cc:burst").burstInitializer)
    }

    @Test
    fun `全表示プリセットが登録済み移動プリセットを参照する`() {
        DisplayParticleCatalog.list().forEach { preset ->
            DisplayParticleMotionCatalog.require(preset.motionPresetId)
        }
    }
}
