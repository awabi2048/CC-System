package com.awabi2048.ccsystem.core.displayeffect

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectAppearance
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectNodeDefinition
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectFrame
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectSimulation
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectState
import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStepResult

/**
 * backend固有のEntity UUIDやパケットIDを公開しないための論理ハンドルです。
 * 実際のPaper backendを追加する際も、この値を外部APIへ返しません。
 */
@JvmInline
internal value class DisplayEffectHandle(val token: Long)

/** Runtime終了理由を診断とbackend cleanupへ渡します。 */
internal enum class DisplayEffectDisposalReason {
    EXPIRED,
    CANCELLED,
    BACKEND_INVALIDATED,
    WORLD_UNAVAILABLE,
    SHUTDOWN,
    FAILED
}

/**
 * Display Effectの表示backend契約です。
 * Entity生成・更新・破棄はbackendが担当しますが、寿命と物理状態はRuntimeが所有します。
 */
internal interface DisplayEffectBackend {
    fun create(
        appearance: DisplayEffectAppearance,
        initialFrame: DisplayEffectFrame
    ): DisplayEffectHandle

    fun apply(
        handle: DisplayEffectHandle,
        frame: DisplayEffectFrame
    )

    fun isAlive(handle: DisplayEffectHandle): Boolean

    /** disposeは同じhandleに複数回呼ばれても安全でなければなりません。 */
    fun dispose(
        handle: DisplayEffectHandle,
        reason: DisplayEffectDisposalReason
    )
}

internal enum class DisplayEffectRuntimeStatus {
    NEW,
    ACTIVE,
    STOPPED,
    FAILED
}

internal sealed interface DisplayEffectRuntimeResult {
    data object Started : DisplayEffectRuntimeResult
    data object Advanced : DisplayEffectRuntimeResult
    data object Ignored : DisplayEffectRuntimeResult
    data class Stopped(val reason: DisplayEffectDisposalReason) : DisplayEffectRuntimeResult
    data class Failed(val error: Throwable) : DisplayEffectRuntimeResult
}

/**
 * 1つのDisplay Effectをbackendへ反映する内部Runtimeです。
 * 各粒子にSchedulerを作らず、上位の共有Schedulerからtickされる前提で設計します。
 */
internal class DisplayEffectRuntime(
    private val definition: DisplayEffectNodeDefinition,
    private val backend: DisplayEffectBackend
) {
    private val simulation = DisplayEffectSimulation(definition.physics)
    private var state: DisplayEffectState = simulation.initialState()
    private var handle: DisplayEffectHandle? = null

    var status: DisplayEffectRuntimeStatus = DisplayEffectRuntimeStatus.NEW
        private set

    fun start(): DisplayEffectRuntimeResult {
        if (status != DisplayEffectRuntimeStatus.NEW) {
            return DisplayEffectRuntimeResult.Ignored
        }
        return try {
            handle = backend.create(definition.appearance, simulation.frame(state))
            status = DisplayEffectRuntimeStatus.ACTIVE
            DisplayEffectRuntimeResult.Started
        } catch (failure: Throwable) {
            status = DisplayEffectRuntimeStatus.FAILED
            DisplayEffectRuntimeResult.Failed(failure)
        }
    }

    fun tick(): DisplayEffectRuntimeResult {
        if (status != DisplayEffectRuntimeStatus.ACTIVE) {
            return DisplayEffectRuntimeResult.Ignored
        }
        val currentHandle = handle
            ?: return fail(IllegalStateException("Display Effect Runtimeにbackend handleがありません"))

        val alive = runCatching { backend.isAlive(currentHandle) }
            .getOrElse { failure -> return fail(failure) }
        if (!alive) {
            dispose(DisplayEffectDisposalReason.BACKEND_INVALIDATED)
            return DisplayEffectRuntimeResult.Stopped(DisplayEffectDisposalReason.BACKEND_INVALIDATED)
        }

        val result = runCatching { simulation.step(state) }
            .getOrElse { failure -> return fail(failure) }
        return when (result) {
            DisplayEffectStepResult.Completed -> {
                dispose(DisplayEffectDisposalReason.EXPIRED)
                DisplayEffectRuntimeResult.Stopped(DisplayEffectDisposalReason.EXPIRED)
            }

            is DisplayEffectStepResult.Advanced -> {
                state = result.state
                runCatching { backend.apply(currentHandle, result.frame) }
                    .fold(
                        onSuccess = { DisplayEffectRuntimeResult.Advanced },
                        onFailure = { failure -> fail(failure) }
                    )
            }
        }
    }

    fun stop(reason: DisplayEffectDisposalReason): DisplayEffectRuntimeResult {
        if (status != DisplayEffectRuntimeStatus.ACTIVE) {
            return DisplayEffectRuntimeResult.Ignored
        }
        dispose(reason)
        return DisplayEffectRuntimeResult.Stopped(reason)
    }

    private fun fail(failure: Throwable): DisplayEffectRuntimeResult {
        dispose(DisplayEffectDisposalReason.FAILED)
        status = DisplayEffectRuntimeStatus.FAILED
        return DisplayEffectRuntimeResult.Failed(failure)
    }

    private fun dispose(reason: DisplayEffectDisposalReason) {
        val currentHandle = handle ?: run {
            if (reason != DisplayEffectDisposalReason.FAILED) {
                status = DisplayEffectRuntimeStatus.STOPPED
            }
            return
        }
        // 先にhandleを無効化して、backendのdispose失敗時も二重破棄を防ぎます。
        handle = null
        status = if (reason == DisplayEffectDisposalReason.FAILED) {
            DisplayEffectRuntimeStatus.FAILED
        } else {
            DisplayEffectRuntimeStatus.STOPPED
        }
        runCatching { backend.dispose(currentHandle, reason) }
    }
}
