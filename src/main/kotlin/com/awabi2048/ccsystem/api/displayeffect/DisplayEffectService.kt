package com.awabi2048.ccsystem.api.displayeffect

import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Display Entityを用いたエフェクトをCC-Systemのtick管理へ登録する公開APIです。
 *
 * バニラParticleを置き換えるAPIではなく、PaperのBlockDisplay／ItemDisplayで
 * 初期案の表現を行うための独立した経路です。ownerは生成Entityの所有元として
 * 記録され、owner Pluginの無効化時に自動停止されます。呼び出しはPaperのメイン
 * スレッドで行ってください。
 */
interface DisplayEffectService {
    fun start(
        owner: Plugin,
        anchor: Location,
        definition: DisplayEffectNodeDefinition
    ): DisplayEffectStartResult

    fun start(
        owner: Plugin,
        anchor: Location,
        definition: DisplayEffectNodeDefinition,
        assetResolver: DisplayEffectAssetResolver
    ): DisplayEffectStartResult

    /** CC-System 組み込みのボクセル粒子パターン一覧を返します。 */
    fun listVoxelParticlePatterns(): List<VoxelParticlePatternInfo>

    /**
     * 複数の微小 BlockDisplay を1つの論理粒子群として原子的に生成します。
     * 生成途中で失敗した場合は、同じ呼び出しで生成した Entity をすべて回収します。
     */
    fun emitVoxelParticles(
        owner: Plugin,
        anchor: Location,
        request: VoxelParticleEmissionRequest
    ): DisplayEffectStartResult
}

/**
 * custom modelやitem dataを解決済みPaperオブジェクトへ変換する差し替え境界です。
 * resolverはメインスレッドで同期的に呼び出され、返却された可変オブジェクトは
 * CC-System側でcloneしてEntityへ設定します。I/Oや長時間処理は実装しないでください。
 */
interface DisplayEffectAssetResolver {
    fun resolveBlock(assetId: DisplayEffectAssetId): BlockData

    fun resolveItem(assetId: DisplayEffectAssetId): ItemStack
}

/** Resolverが指定されたアセットを生成できないことを明示する例外です。 */
class DisplayEffectAssetResolutionException(
    val assetId: DisplayEffectAssetId,
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

sealed interface DisplayEffectStartResult {
    data class Started(val instance: DisplayEffectInstance) : DisplayEffectStartResult

    data class Rejected(
        val reason: DisplayEffectStartRejection,
        val message: String
    ) : DisplayEffectStartResult
}

enum class DisplayEffectStartRejection {
    NOT_MAIN_THREAD,
    OWNER_DISABLED,
    WORLD_UNAVAILABLE,
    ASSET_UNAVAILABLE,
    INVALID_DEFINITION,
    CAPACITY_EXCEEDED,
    NO_VIEWERS,
    UNKNOWN_PATTERN,
    BACKEND_FAILURE,
    SHUTDOWN
}

interface DisplayEffectInstance {
    val id: UUID
    val status: DisplayEffectInstanceStatus
    val terminalReason: DisplayEffectTerminationReason?

    /** メインスレッド上でEntityを破棄し、以後のtick更新を停止します。 */
    fun stop(): DisplayEffectStopResult
}

enum class DisplayEffectInstanceStatus {
    ACTIVE,
    STOPPED,
    FAILED
}

enum class DisplayEffectTerminationReason {
    EXPIRED,
    CANCELLED,
    BACKEND_INVALIDATED,
    WORLD_UNAVAILABLE,
    OWNER_DISABLED,
    SHUTDOWN,
    FAILED
}

sealed interface DisplayEffectStopResult {
    data object Stopped : DisplayEffectStopResult
    data object AlreadyStopped : DisplayEffectStopResult
    data object NotMainThread : DisplayEffectStopResult
    data object ServiceUnavailable : DisplayEffectStopResult
}
