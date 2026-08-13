package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAssetId
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectVector3
import com.awabi2048.ccsystem.api.displayeffect.VoxelParticlePatternId
import com.awabi2048.ccsystem.api.displayeffect.VoxelParticlePatternInfo

/** 1つの微小立方体を、粒子中心からの中心座標と辺長で定義します。 */
internal data class VoxelParticleVoxel(
    val centerOffset: DisplayEffectVector3,
    val size: Double,
    val blockAssetId: DisplayEffectAssetId
) {
    init {
        require(size.isFinite() && size in 0.02..0.5) { "ボクセル辺長は 0.02..0.5 block で指定してください: $size" }
    }
}

/** 複数の BlockDisplay を共有物理状態で動かす不変パターンです。 */
internal data class VoxelParticlePattern(
    val id: VoxelParticlePatternId,
    val voxels: List<VoxelParticleVoxel>,
    val lifetimeTicks: Int,
    val gravityPerTick: DisplayEffectVector3,
    val velocityRetentionPerTick: Double
) {
    init {
        require(voxels.isNotEmpty() && voxels.size <= 16) { "ボクセル粒子は 1..16 voxel で構成してください" }
        require(lifetimeTicks in 1..100) { "ボクセル粒子の寿命は 1..100 tick で指定してください" }
        require(velocityRetentionPerTick in 0.0..1.0) { "ボクセル粒子の drag が不正です" }
    }

    fun info(): VoxelParticlePatternInfo = VoxelParticlePatternInfo(id, voxels.size, lifetimeTicks)
}

/**
 * 初期カタログはバニラの単色コンクリートだけで構成します。
 * パターンの意味、BlockData、Entity コストを一箇所に置き、コマンド側の暗黙変換を防ぎます。
 */
internal object VoxelParticleCatalog {
    private fun voxel(x: Double, y: Double, z: Double, size: Double, material: String) = VoxelParticleVoxel(
        DisplayEffectVector3(x, y, z),
        size,
        DisplayEffectAssetId("minecraft:$material")
    )

    private val patterns = listOf(
        VoxelParticlePattern(
            VoxelParticlePatternId("minecraft:flame"),
            listOf(
                voxel(0.0, 0.00, 0.0, 0.12, "yellow_concrete"),
                voxel(0.0, 0.10, 0.0, 0.10, "orange_concrete"),
                voxel(-0.06, -0.05, 0.0, 0.08, "red_concrete"),
                voxel(0.06, -0.05, 0.0, 0.08, "orange_concrete")
            ),
            lifetimeTicks = 16,
            gravityPerTick = DisplayEffectVector3(0.0, 0.004, 0.0),
            velocityRetentionPerTick = 0.92
        ),
        VoxelParticlePattern(
            VoxelParticlePatternId("minecraft:smoke"),
            listOf(
                voxel(0.0, 0.0, 0.0, 0.13, "gray_concrete"),
                voxel(0.09, 0.05, 0.0, 0.10, "light_gray_concrete"),
                voxel(-0.07, 0.08, 0.04, 0.09, "gray_concrete"),
                voxel(0.0, 0.15, -0.03, 0.08, "white_concrete")
            ),
            lifetimeTicks = 24,
            gravityPerTick = DisplayEffectVector3(0.0, 0.002, 0.0),
            velocityRetentionPerTick = 0.94
        ),
        VoxelParticlePattern(
            VoxelParticlePatternId("minecraft:crit"),
            listOf(
                voxel(0.0, 0.0, 0.0, 0.08, "white_concrete"),
                voxel(0.10, 0.0, 0.0, 0.05, "light_gray_concrete"),
                voxel(-0.10, 0.0, 0.0, 0.05, "light_gray_concrete"),
                voxel(0.0, 0.10, 0.0, 0.05, "white_concrete"),
                voxel(0.0, -0.10, 0.0, 0.05, "white_concrete")
            ),
            lifetimeTicks = 12,
            gravityPerTick = DisplayEffectVector3(0.0, -0.01, 0.0),
            velocityRetentionPerTick = 0.90
        ),
        VoxelParticlePattern(
            VoxelParticlePatternId("minecraft:happy_villager"),
            listOf(
                voxel(0.0, 0.0, 0.0, 0.09, "lime_concrete"),
                voxel(0.08, 0.08, 0.0, 0.06, "green_concrete"),
                voxel(-0.08, 0.08, 0.0, 0.06, "lime_concrete"),
                voxel(0.0, 0.14, 0.04, 0.05, "white_concrete")
            ),
            lifetimeTicks = 20,
            gravityPerTick = DisplayEffectVector3(0.0, 0.003, 0.0),
            velocityRetentionPerTick = 0.93
        )
    )

    private val byId = patterns.associateBy { it.id }

    fun list(): List<VoxelParticlePattern> = patterns

    fun find(id: VoxelParticlePatternId): VoxelParticlePattern? = byId[id]
}
