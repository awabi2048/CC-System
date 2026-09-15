package com.awabi2048.ccsystem.api.worldedit

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.math.transform.Transform
import org.bukkit.entity.Player
import kotlin.math.cos
import kotlin.math.sin

/**
 * WorldEdit選択範囲と台帳座標を結ぶ支援です。
 *
 * WorldEdit型に触るため、[WorldEditAvailability.isAvailable] が真の場合だけ
 * 呼び出してください。純粋な分類・解釈は [WorldEditCommands] と
 * [WorldEditMoveCommandParser]（いずれもWorldEdit不在時に安全）へ委ねます。
 * 公開シグネチャはBukkit型と [WorldEditBlockPoint] 等だけにし、WorldEdit型を
 * consumer 側へ漏らしません。`cut/copy/paste` はBukkitの通常ブロックイベントを
 * 通らないため、コマンド先読みで確保した [WorldEditCapture] を遅延確定します。
 */
object WorldEditSelectionSupport {
    /**
     * プレイヤーの選択範囲に完全に含まれる台帳項目を相対化して確保します。
     *
     * 選択範囲が未確定の場合は null を返します。該当項目がない場合も
     * 空リストの capture を返すため、保留の破棄は呼出側で判断します。
     */
    fun <T> capture(
        player: Player,
        entries: List<T>,
        pointOf: (T) -> WorldEditBlockPoint,
    ): WorldEditCapture<T>? {
        val actor = BukkitAdapter.adapt(player)
        val session = WorldEdit.getInstance().sessionManager.get(actor)
        val selection = try {
            session.getSelection()
        } catch (_: IncompleteRegionException) {
            return null
        }
        val sourceWorldKey = player.world.key.toString()
        val originVector = runCatching { session.getPlacementPosition(actor) }
            .getOrElse { BukkitAdapter.asBlockVector(player.location) }
        val origin = WorldEditOrigin(sourceWorldKey, originVector.x(), originVector.y(), originVector.z())

        val relatives = entries
            .filter { pointOf(it).worldKey == sourceWorldKey }
            .filter { selection.contains(BlockVector3.at(pointOf(it).x, pointOf(it).y, pointOf(it).z)) }
            .map {
                val point = pointOf(it)
                WorldEditRelativeEntry(
                    entry = it,
                    relativeX = point.x - origin.x,
                    relativeY = point.y - origin.y,
                    relativeZ = point.z - origin.z,
                )
            }
        return WorldEditCapture(origin, relatives)
    }

    /**
     * クリップボードの基準点が確保時と一致するかを返します。
     *
     * `//cut` 後の移動元削除や、別操作で上書きされた貼付けの誤適用を防ぎます。
     * 取得失敗時は不一致として false を返します。
     */
    fun isClipboardOriginMatching(player: Player, origin: WorldEditOrigin): Boolean {
        val actor = BukkitAdapter.adapt(player)
        val session = WorldEdit.getInstance().sessionManager.get(actor)
        val clipboard = runCatching { session.getClipboard().clipboard }.getOrNull() ?: return false
        return clipboard.origin == BlockVector3.at(origin.x, origin.y, origin.z)
    }

    /**
     * 保留中の貼付け先絶対座標を解決します。
     *
     * クリップボードの基準点が確保時と異なる場合や取得失敗時は null を返し、
     * 台帳を変更しません。`//rotate`・`//flip` 後の貼付けにも追従するため、
     * クリップボードの transform を相対座標へ適用します。
     */
    fun <T> resolvePastePoints(
        player: Player,
        capture: WorldEditCapture<T>,
    ): List<WorldEditPastedPoint<T>>? {
        val actor = BukkitAdapter.adapt(player)
        val session = WorldEdit.getInstance().sessionManager.get(actor)
        val clipboard = runCatching { session.getClipboard() }.getOrNull() ?: return null
        if (clipboard.clipboard.origin != BlockVector3.at(capture.origin.x, capture.origin.y, capture.origin.z)) {
            return null
        }
        val transform = clipboard.transform
        val pasteVector = runCatching { session.getPlacementPosition(actor) }
            .getOrElse { BukkitAdapter.asBlockVector(player.location) }
        val targetWorldKey = player.world.key.toString()
        return capture.entries.map { relative ->
            val transformed = transformRelative(relative.relativeX, relative.relativeY, relative.relativeZ, transform)
            WorldEditPastedPoint(
                entry = relative.entry,
                worldKey = targetWorldKey,
                x = pasteVector.x() + transformed.x(),
                y = pasteVector.y() + transformed.y(),
                z = pasteVector.z() + transformed.z(),
            )
        }
    }

    /**
     * `//move` の確定移動量を解決します。
     *
     * 明示ベクトル（`x,y,z`・`^x,^y,^z`）を先に試し、失敗時はWorldEdit自身の
     * 方向解決へ委譲して `forward`・`me`・斜め方向等に対応します。最後に
     * 移動回数を乗じます。解決不能時は null を返します。
     */
    fun resolveMoveOffset(command: WorldEditMoveCommand, player: Player): WorldEditMoveOffset? {
        val offset = parseExplicitMoveVector(command.offsetToken, player)
            ?: runCatching {
                WorldEdit.getInstance()
                    .getDiagonalDirection(BukkitAdapter.adapt(player), command.offsetToken)
            }.getOrNull()
            ?: return null
        val multiplied = offset.multiply(command.multiplier)
        return WorldEditMoveOffset(multiplied.x(), multiplied.y(), multiplied.z())
    }

    private fun transformRelative(x: Int, y: Int, z: Int, transform: Transform): BlockVector3 {
        if (transform.isIdentity) return BlockVector3.at(x, y, z)
        return transform.apply(Vector3.at(x.toDouble(), y.toDouble(), z.toDouble()))
            .round()
            .toBlockPoint()
    }

    private fun parseExplicitMoveVector(token: String, player: Player): BlockVector3? {
        val isLocalVector = token.startsWith('^')
        val raw = if (isLocalVector) token.drop(1) else token
        val components = raw.split(',')
            .map { it.removePrefix("^").toIntOrNull() }
        if (components.size != 3 || components.any { it == null }) return null

        val vector = BlockVector3.at(
            components[0]!!,
            components[1]!!,
            components[2]!!,
        )
        if (!isLocalVector) return vector

        // ^x,^y,^z は WorldEdit のローカル座標です。WorldEdit の OffsetConverter と
        // 同じ基底ベクトルで変換し、コマンド側と移動先の座標を一致させます。
        val location = BukkitAdapter.adapt(player.location)
        val yaw = Math.toRadians(location.yaw.toDouble() + 90.0)
        val pitch = Math.toRadians(-location.pitch.toDouble() + 90.0)
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)
        val forward = location.direction
        val up = Vector3.at(
            cosYaw * cosPitch,
            sinPitch,
            sinYaw * cosPitch,
        )
        val right = forward.cross(up).multiply(-1.0)
        val transform = AffineTransform(
            forward.x(), up.x(), right.x(), 0.0,
            forward.y(), up.y(), right.y(), 0.0,
            forward.z(), up.z(), right.z(), 0.0,
        )
        return transform.apply(vector.toVector3()).round().toBlockPoint()
    }
}
