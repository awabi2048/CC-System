package com.awabi2048.ccsystem.api.gui

/**
 * メニューを開いている間、他のプレイヤーの操作やワールド状態の変化を表示へ反映するための定期再描画設定。
 *
 * 全メニューへ一律適用せず、対象画面の [InventoryMenuDefinition] にだけ明示的に付与する（デフォルト無効）。
 * 再描画は CC-System 内のスキャンタスクが、現在その画面を開いておりインベントリを表示中の
 * （Dialog・外部入力を表示していない）プレイヤーだけを対象に実行する。
 *
 * 画面を閉じたプレイヤーは現在ルートの変化で自動的にスキップされるため、タスクの個別管理は不要。
 */
data class MenuAutoRefreshPolicy(
    val intervalTicks: Long,
) {
    init {
        require(intervalTicks >= 20L) { "MenuAutoRefreshPolicy interval must be at least 20 ticks (1 second)" }
    }

    companion object {
        /** 1秒ごと。メンバー管理・環境設定など軽量な画面向け。 */
        @JvmField
        val EVERY_SECOND: MenuAutoRefreshPolicy = MenuAutoRefreshPolicy(20L)

        /** 2秒ごと。やや重いレンダラーを含む画面向け。 */
        @JvmField
        val EVERY_TWO_SECONDS: MenuAutoRefreshPolicy = MenuAutoRefreshPolicy(40L)
    }
}