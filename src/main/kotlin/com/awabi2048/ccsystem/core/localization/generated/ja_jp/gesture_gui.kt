package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** ジェスチャーGUIの日本語埋込カタログです。 */
internal object JaJpGestureGuiCatalog {
    const val LOCALE = "ja_jp"
    const val DOMAIN = "gesture_gui"

    fun entries(): List<EmbeddedLocalizationEntry> = listOf(
        entry("gesture_gui.demo.title", "ジェスチャーGUI"),
        entry("gesture_gui.demo.description", "視線を合わせ、割り当てられた操作を入力してください。"),
        entry("gesture_gui.demo.primary", "左クリック"),
        entry("gesture_gui.demo.secondary", "右クリック"),
        entry("gesture_gui.demo.shift_primary", "Shift＋左クリック"),
        entry("gesture_gui.demo.shift_secondary", "Shift＋右クリック"),
        entry("gesture_gui.demo.swap_hand", "Fキー"),
        entry("gesture_gui.demo.opened", "§aジェスチャーGUIを{screens}画面で開きました。"),
        entry("gesture_gui.demo.closed", "§eジェスチャーGUIを閉じました。"),
        entry("gesture_gui.demo.action", "§bジェスチャーを受け付けました: {gesture}"),
        entry("gesture_gui.demo.usage", "§e使用法: /cc gesture-gui demo [1|2|3|close]"),
        entry("gesture_gui.demo.dialog_close", "閉じる"),
        entry("gesture_gui.demo.status.title", "ステータスレイアウト"),
        entry("gesture_gui.demo.status.description", "アイコンと情報行を分離した状態表示の例です。"),
        entry("gesture_gui.demo.status.health", "体力: 良好"),
        entry("gesture_gui.demo.status.energy", "エネルギー: 充填済み"),
        entry("gesture_gui.demo.status.ready", "準備状態: 完了"),
        entry("gesture_gui.demo.choice.title", "選択カードレイアウト"),
        entry("gesture_gui.demo.choice.description", "視線を合わせて左クリックで役割を選択します。"),
        entry("gesture_gui.demo.choice.builder", "建築家"),
        entry("gesture_gui.demo.choice.explorer", "探検家"),
        entry("gesture_gui.demo.choice.trader", "商人"),
        entry("gesture_gui.demo.choice.guardian", "守護者"),
        entry("gesture_gui.exit_guidance", "Shift＋ジャンプで終了"),
        entry("gesture_gui.debug.subtitle", "§7位置:{inside} 錐角:{cone} hit:{hit} 距離:{dist}/{range} {motion}{dirty}{dummy} 変位:{displacement}"),
        entry("gesture_gui.debug.resummon", "§e[Gaze] 再召喚しました 変位:{displacement}m（セクタ外のため）"),
        entry("gesture_gui.debug.resummon_failed", "§c[Gaze] 再召喚に失敗しました: {detail}"),
        entry("gesture_gui.debug.dummy_started", "§e[Gaze] ダミー追従を開始しました 変位:{displacement}m（セクタ外のため）"),
        entry("gesture_gui.debug.dummy_restored", "§e[Gaze] 本体へ復帰しました（再召喚見送り 変位:{displacement}m）"),
        entry("gesture_gui.debug.main_restored_for_update", "§e[Gaze] 内容更新のため本体へ復帰しました（見た目が凍結位置へ戻ります）"),
        entry("gesture_gui.debug.unpinned", "§e[Gaze] 固定解除のため現在位置へ再配置しました"),
    )

    private fun entry(key: String, value: String) =
        EmbeddedLocalizationEntry(key, EmbeddedLocalizedValue.Text(value), DOMAIN)
}
