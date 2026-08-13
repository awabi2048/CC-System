package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionMode
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionProperties
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionPresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisplayParticleMotionCatalogTest {
    @Test
    fun `初期移動プリセット8種を重複なく登録する`() {
        val presets = DisplayParticleMotionCatalog.list()
        assertEquals(8, presets.size)
        assertEquals(DisplayParticleMotionKind.entries.toSet(), presets.map { it.kind }.toSet())
        assertEquals(presets.size, presets.map { it.id }.distinct().size)
        assertTrue(DisplayParticleMotionCatalog.require("cc:burst").burstInitializer)
    }

    @Test
    fun `外観プリセットは移動プリセットを保持しない`() {
        assertTrue(DisplayParticleCatalog.list().isNotEmpty())
        assertTrue(DisplayParticlePreset::class.java.declaredFields.none { it.name == "motionPresetId" })
    }

    @Test
    fun `上昇と漂流の揺らぎ強度を3倍値で固定する`() {
        assertEquals(0.0045, DisplayParticleMotionCatalog.require("cc:buoyant").turbulenceStrength)
        assertEquals(0.0105, DisplayParticleMotionCatalog.require("cc:drift").turbulenceStrength)
    }

    @Test
    fun `動作と衝突に無効なプロパティを拒否する`() {
        assertThrows(IllegalArgumentException::class.java) {
            DisplayParticleMotionCatalog.resolve(
                DisplayParticleMotionPresetId("cc:static"),
                DisplayParticleMotionProperties(turbulenceStrength = 0.01),
                DisplayParticleCollisionMode.NONE,
                DisplayParticleCollisionProperties()
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            DisplayParticleMotionCatalog.resolve(
                DisplayParticleMotionPresetId("cc:inertial"),
                DisplayParticleMotionProperties(),
                DisplayParticleCollisionMode.NONE,
                DisplayParticleCollisionProperties(restitution = 0.5)
            )
        }
    }
}
