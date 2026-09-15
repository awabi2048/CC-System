package com.awabi2048.ccsystem.api.worldedit

/**
 * WorldEdit選択範囲と台帳座標を結ぶBukkit型だけの値です。
 *
 * WorldEdit型（`BlockVector3`・`Transform`・`Region` 等）を公開APIのシグネチャへ
 * 漏らさないため、WorldEdit不在時に consumer がクラス解決で失敗しません。
 * WorldEdit型に触る処理は [WorldEditSelectionSupport] の内部に閉じます。
 */

/** ワールドキー付きのブロック座標です。台帳側の1点を表します。 */
data class WorldEditBlockPoint(
    val worldKey: String,
    val x: Int,
    val y: Int,
    val z: Int,
)

/** `//copy`・`//cut` 時の貼付け基準点です。 */
data class WorldEditOrigin(
    val worldKey: String,
    val x: Int,
    val y: Int,
    val z: Int,
)

/** 基準点からの相対差分で保持する台帳項目です。 */
data class WorldEditRelativeEntry<T>(
    val entry: T,
    val relativeX: Int,
    val relativeY: Int,
    val relativeZ: Int,
)

/**
 * `//copy`・`//cut`・`//move` 検出時に確保する保留情報です。
 *
 * `PlayerCommandPreprocessEvent` はコマンド実行前に発生するため、実ブロックの
 * 確定前に基準点＋相対差分だけを保持し、確定は遅延タスクへ委ねます。
 */
data class WorldEditCapture<T>(
    val origin: WorldEditOrigin,
    val entries: List<WorldEditRelativeEntry<T>>,
)

/** `//paste` 解決後の絶対座標です。項目との対応を保ったまま返します。 */
data class WorldEditPastedPoint<T>(
    val entry: T,
    val worldKey: String,
    val x: Int,
    val y: Int,
    val z: Int,
)

/** `//move` の確定移動量です。 */
data class WorldEditMoveOffset(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun isZero(): Boolean = x == 0 && y == 0 && z == 0

    fun moved(point: WorldEditBlockPoint): WorldEditBlockPoint =
        point.copy(x = point.x + x, y = point.y + y, z = point.z + z)
}
