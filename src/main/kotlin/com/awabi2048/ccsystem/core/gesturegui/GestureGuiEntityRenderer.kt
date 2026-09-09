package com.awabi2048.ccsystem.core.gesturegui

import com.awabi2048.ccsystem.api.entity.SystemEntityRegistry
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * クリック吸収用 catcher（Interaction）の生成・移動・破棄だけを担います。
 *
 * 描画物（BlockDisplay / TextDisplay / ItemDisplay・hover）は Bukkit Entity として
 * 生成しません。描画は [GestureGuiProtocolLibBackend] による client-only virtual entity
 * へ完全移行し、server world 上の GUI 描画 Entity 数は 0 です。
 * catcher は操作 player につき原則 1 個、視線が GUI 内の間のみ有効で、
 * GUI element の hit-test には使用しません（数値 geometry が正本です）。
 */
internal class GestureGuiEntityRenderer(
    private val plugin: Plugin,
    private val systemEntityRegistry: SystemEntityRegistry,
) {
    private val sessionKey = NamespacedKey(plugin, "gesture_gui_session")
    private val actorKey = NamespacedKey(plugin, "gesture_gui_actor")
    private val revisionKey = NamespacedKey(plugin, "gesture_gui_revision")

    internal data class CatcherHandle(val actorId: UUID, val entity: Interaction)

    fun spawnCatcher(
        player: Player,
        sessionId: UUID,
        revision: Long,
        location: Location,
        responsive: Boolean,
    ): CatcherHandle {
        val entity = player.world.spawn(location, Interaction::class.java) {
            it.isPersistent = false
            // 後から参加したプレイヤーにも送信されないよう、生成時点から個人表示に固定します。
            it.isVisibleByDefault = false
            // InteractionのLocationは底面基準です。サービス側で目位置から
            // 半分だけ下げて配置することで、ヒットボックスの中央を視点へ一致させます。
            it.interactionWidth = GESTURE_CATCHER_SIZE
            it.interactionHeight = GESTURE_CATCHER_SIZE
            // Interactionの応答設定は腕振り等のクライアント応答を制御します。
            // 実際の操作可否はGestureGuiService/Listenerで別途判定します。
            it.isResponsive = responsive
            mark(it, sessionId, revision)
            it.persistentDataContainer.set(actorKey, PersistentDataType.STRING, player.uniqueId.toString())
        }
        // Interactionは操作者固有です。他者の照準や操作を横取りさせません。
        player.showEntity(plugin, entity)
        return CatcherHandle(player.uniqueId, entity)
    }

    fun moveCatcher(handle: CatcherHandle, location: Location) {
        if (handle.entity.world == location.world) handle.entity.teleport(location)
    }

    fun removeCatcher(handle: CatcherHandle) = handle.entity.remove()

    fun ownsCatcher(entity: Entity, playerId: UUID? = null): Boolean {
        val owner = entity.persistentDataContainer.get(actorKey, PersistentDataType.STRING)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return false
        return playerId == null || owner == playerId
    }

    /**
     * セッションタグを正本として、ハンドルから漏れた catcher を回収します。
     *
     * 描画物は virtual 化されて server 上に存在しないため、対象は catcher のみです。
     * タグは本プラグインが生成した Entity にだけ付くため、通常の Entity を巻き込みません。
     */
    fun removeSessionEntities(sessionId: UUID) {
        val expected = sessionId.toString()
        Bukkit.getWorlds().forEach { world ->
            world.entities.toList()
                .filter { entity ->
                    entity.persistentDataContainer.get(sessionKey, PersistentDataType.STRING) == expected
                }
                .forEach(Entity::remove)
        }
    }

    private fun mark(entity: Entity, sessionId: UUID, revision: Long) {
        systemEntityRegistry.mark(entity, plugin)
        entity.persistentDataContainer.set(sessionKey, PersistentDataType.STRING, sessionId.toString())
        entity.persistentDataContainer.set(revisionKey, PersistentDataType.LONG, revision)
    }
}

/**
 * プレイヤー入力を吸収するInteractionの一辺です。
 * 生成側と配置側で別のリテラルを持つと、中央合わせがずれるため共有します。
 */
internal const val GESTURE_CATCHER_SIZE: Float = 0.18f
