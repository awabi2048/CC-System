package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiScreenPose
import com.awabi2048.ccsystem.api.gesturegui.GestureGuiView
import java.util.UUID

/**
 * Gesture GUI の論理モデルと packet 実装を分離する描画 backend です。
 *
 * 利用側（KantanCommander 等）へ packet / NMS / ProtocolLib の詳細を露出させず、
 * CC-System 内の virtual visual 差分適用だけがこの境界を通ります。
 * 本命実装は ProtocolLib による client-only virtual entity です。
 */
internal interface GestureGuiRenderBackend {
    /** 論理 visual を viewer へ生成します。既存 ID への再送は行いません。 */
    fun spawn(viewerId: UUID, sessionId: UUID, revision: Long, pose: GestureGuiScreenPose, view: GestureGuiView)

    /** 追加・変更・削除の差分だけを viewer へ反映します。無変化には触れません。 */
    fun update(viewerId: UUID, sessionId: UUID, revision: Long, pose: GestureGuiScreenPose, oldView: GestureGuiView, newView: GestureGuiView)

    /** pose 変化時のみ viewer 側の配置を移動します。寸法確定は含みません。 */
    fun move(viewerId: UUID, sessionId: UUID, pose: GestureGuiScreenPose, view: GestureGuiView)

    /** viewer に存在する virtual entity だけを破棄します。全画面破棄は行いません。 */
    fun destroy(viewerId: UUID, sessionId: UUID)
}
