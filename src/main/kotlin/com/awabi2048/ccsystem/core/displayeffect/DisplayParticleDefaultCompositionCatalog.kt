package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleCollisionMode
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticleMotionPresetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayParticlePresetId

internal data class DisplayParticleDefaultComposition(
    val motionPresetId: DisplayParticleMotionPresetId,
    val collisionMode: DisplayParticleCollisionMode
)

/** 簡易コマンド用の既定組合せです。外観定義自体には動作を持たせません。 */
internal object DisplayParticleDefaultCompositionCatalog {
    private val compositions = mapOf(
        "cc:ember" to DisplayParticleDefaultComposition(DisplayParticleMotionPresetId("cc:buoyant"), DisplayParticleCollisionMode.NONE),
        "cc:ash" to DisplayParticleDefaultComposition(DisplayParticleMotionPresetId("cc:drift"), DisplayParticleCollisionMode.NONE),
        "cc:spark" to DisplayParticleDefaultComposition(DisplayParticleMotionPresetId("cc:burst"), DisplayParticleCollisionMode.REMOVE),
        "cc:verdant" to DisplayParticleDefaultComposition(DisplayParticleMotionPresetId("cc:orbit"), DisplayParticleCollisionMode.NONE)
    )

    fun require(id: DisplayParticlePresetId): DisplayParticleDefaultComposition =
        requireNotNull(compositions[id.value]) { "既定のパーティクル組合せがありません: ${id.value}" }
}
