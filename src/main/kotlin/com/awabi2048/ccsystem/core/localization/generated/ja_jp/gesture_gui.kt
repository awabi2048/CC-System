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
        entry("gesture_gui.exit_guidance", "Shift＋ジャンプで終了"),
    )

    private fun entry(key: String, value: String) =
        EmbeddedLocalizationEntry(key, EmbeddedLocalizedValue.Text(value), DOMAIN)
}
