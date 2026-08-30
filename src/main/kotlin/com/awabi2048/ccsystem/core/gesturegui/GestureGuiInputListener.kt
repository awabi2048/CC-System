package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.gesturegui.GestureGuiGesture
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import io.papermc.paper.event.player.PlayerArmSwingEvent
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Bukkitの複数イベントをジェスチャーへ正規化します。
 * 同じパケット由来の入力はtick単位で一度だけActionへ渡します。
 */
class GestureGuiInputListener(private val service: GestureGuiServiceImpl) : Listener {
    private val inputDeduplicator = GestureGuiInputDeduplicator()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onAnimation(event: PlayerArmSwingEvent) {
        // 攻撃の正規入口はメインハンドだけです。オフハンドの腕振り（アイテム使用など）を
        // PRIMARYへ変換すると、Inventory GUIとは無関係なGesture画面まで誤作動します。
        if (event.hand != EquipmentSlot.HAND) return
        // Interactionが入力の入口になっているセッションでは、ARM_SWINGを
        // そのままActionへ変換すると、同じ左クリックのPrePlayerAttackEntityEvent
        // と二重に判定されます。Interactionを持たない旧クライアント／画面だけを
        // フォールバックとして扱い、Paper 26.1.2の通常経路は攻撃イベントへ一本化します。
        if (service.isParticipating(event.player.uniqueId)) return
        if (dispatch(event.player, primary(event.player))) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onWorldInteract(event: PlayerInteractEvent) {
        if (event.action.isRightClick && !GestureGuiRightClickHandPolicy.accepts(event.hand)) {
            // PlayerInteractEventはメイン／オフハンドごとに発火します。右クリックを
            // 両方Actionへ渡すと、同じ入力が二重実行されるため、Gesture GUIの正規入口は
            // メインハンドだけに限定します。Kantanの完全遮断中だけは、無視したオフハンド
            // からブロック・アイテム使用が漏れないようイベント自体を拒否します。
            if (
                service.isWorldClickSuppressed(event.player.uniqueId) ||
                service.isSecondaryInputDisabled(event.player.uniqueId)
            ) {
                suppressWorldRightClick(event)
            }
            return
        }
        if (service.isWorldClickSuppressed(event.player.uniqueId)) {
            if (event.action.isRightClick) {
                // KantanのGesture GUIでは、画面外も含めて右クリックを通常ワールドへ
                // 到達させません。Inventory GUIの右クリックとは別イベント経路です。
                suppressWorldRightClick(event)
            } else if (event.action.isLeftClick) {
                // 画面内であれば通常どおりActionを解決し、未割当領域でも外部ブロックへ
                // は漏らさないため、結果に関係なくイベントを吸収します。
                dispatch(event.player, primary(event.player))
            }
            event.isCancelled = true
            return
        }
        val gesture = when {
            event.action.isLeftClick -> primary(event.player)
            event.action.isRightClick -> {
                if (service.isSecondaryInputDisabled(event.player.uniqueId)) {
                    // Inventory GUIとは別に、Gesture GUI操作中のワールド右クリックだけを拒否します。
                    suppressWorldRightClick(event)
                    return
                }
                secondary(event.player)
            }
            else -> return
        }
        if (dispatch(event.player, gesture)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onEntityInteract(event: PlayerInteractEntityEvent) {
        val catcher = service.ownsCatcher(event.rightClicked, event.player.uniqueId)
        if (!GestureGuiRightClickHandPolicy.accepts(event.hand)) {
            // Interactionへのオフハンド右クリックは、正規の右クリック判定へ渡しません。
            // 所有catcher／完全遮断セッションだけは、背後のエンティティ操作を防ぐため
            // イベントをキャンセルします。
            if (catcher || service.isWorldClickSuppressed(event.player.uniqueId)) {
                event.isCancelled = true
            }
            return
        }
        if (service.isWorldClickSuppressed(event.player.uniqueId)) {
            // 右クリックActionは原則廃止し、Interaction以外のエンティティも含めて
            // 外部プラグインへ入力を渡さない完全吸収とします。
            event.isCancelled = true
            return
        }
        if (service.isSecondaryInputDisabled(event.player.uniqueId)) {
            if (catcher) event.isCancelled = true
            return
        }
        // catcherは背後のブロックを遮る入口であり、正規の要素判定は同じ視線rayで行います。
        val handled = dispatch(event.player, secondary(event.player))
        if (catcher || handled) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onEntityAttack(event: PrePlayerAttackEntityEvent) {
        val player = event.player
        val catcher = service.ownsCatcher(event.attacked, player.uniqueId)
        if (service.isWorldClickSuppressed(player.uniqueId)) {
            // 画面内ならActionを解決し、対象が別エンティティでも攻撃自体は吸収します。
            dispatch(player, primary(player))
            event.isCancelled = true
            return
        }
        if (!catcher) return
        val handled = dispatch(player, primary(player))
        if (catcher || handled) event.isCancelled = true
    }

    /** 古いPaper経路でInteractionがPreイベントへ到達しない場合の防御的フォールバックです。 */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onLegacyEntityAttack(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val catcher = service.ownsCatcher(event.entity, player.uniqueId)
        if (service.isWorldClickSuppressed(player.uniqueId)) {
            dispatch(player, primary(player))
            event.isCancelled = true
            return
        }
        if (!catcher) return
        val handled = dispatch(player, primary(player))
        if (catcher || handled) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onSwapHand(event: PlayerSwapHandItemsEvent) {
        // FキーはGUIを開いているだけでは奪いません。視線が操作可能な画面へ
        // 実際に向いている場合だけAction解決とイベント消費を行います。
        // PUBLIC画面ではFが第三者の最初の操作にもなり得るため、Action解決中に
        // claimを取得し、後続のFreecam listenerは同じイベントを所有として認識できます。
        if (!service.isLookingAtScreen(event.player)) return
        if (dispatch(event.player, GestureGuiGesture.SWAP_HAND)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onJump(event: PlayerJumpEvent) {
        if (!event.player.isSneaking || !service.leaveOrClose(event.player.uniqueId)) return
        event.isCancelled = true
    }

    private fun primary(player: Player) =
        if (player.isSneaking) GestureGuiGesture.SHIFT_PRIMARY else GestureGuiGesture.PRIMARY

    private fun secondary(player: Player) =
        if (player.isSneaking) GestureGuiGesture.SHIFT_SECONDARY else GestureGuiGesture.SECONDARY

    private fun suppressWorldRightClick(event: PlayerInteractEvent) {
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
        event.isCancelled = true
    }

    private fun dispatch(player: Player, gesture: GestureGuiGesture): Boolean {
        val key = GestureGuiInputKey(player.uniqueId, gesture)
        val tick = Bukkit.getCurrentTick()
        // 既に同tickの正規イベントを処理済みなら、参加状態に関係なく後続イベントも
        // 消費します。右クリック無効化などで最初のイベント時点にactorを作らない
        // ケースでも、後続の通常ワールド処理へ漏らさないためです。
        if (inputDeduplicator.isHandled(key, tick)) return true
        // ARM_SWINGが先に届いても、まだrayが画面要素へ交差していない場合があります。
        // イベントを消費しただけの再試行可能な結果まで重複済みとして記録すると、
        // 同じtickのInteractイベントが再判定できず、クリック音もActionも消えます。
        val result = service.dispatchGesture(player, gesture)
        inputDeduplicator.record(key, tick, result.deduplicate)
        return result.consumed
    }

}

internal data class GestureGuiInputKey(val playerId: UUID, val gesture: GestureGuiGesture)

/** 右クリックの正規入力として受理する手を明示します。nullもメインハンド扱いしません。 */
internal object GestureGuiRightClickHandPolicy {
    fun accepts(hand: EquipmentSlot?): Boolean = hand == EquipmentSlot.HAND
}

/**
 * ARM_SWINGとInteractが同じtickに届く入力を、処理成功時だけ一度にまとめます。
 * 未処理の最初のイベントを記録しないことが、後続イベントの再試行を許可する契約です。
 */
internal class GestureGuiInputDeduplicator {
    private val handledTick = mutableMapOf<GestureGuiInputKey, Int>()

    fun isHandled(key: GestureGuiInputKey, tick: Int): Boolean = handledTick[key] == tick

    fun record(key: GestureGuiInputKey, tick: Int, deduplicate: Boolean) {
        if (!deduplicate) return
        handledTick[key] = tick
        if (handledTick.size > 256) handledTick.entries.removeIf { it.value < tick - 1 }
    }
}
