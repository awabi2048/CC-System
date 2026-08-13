package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpChanponUtilitiesCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "chanpon/utilities"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon_utilities.common.prefix", value = EmbeddedLocalizedValue.Text("&7[&bChanpon-Utilities&7] "), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.usage", value = EmbeddedLocalizedValue.Text("&c使用方法: /cu status|enable <module>|disable <module>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.no_permission", value = EmbeddedLocalizedValue.Text("&cこのコマンドを実行する権限がありません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.unknown_module", value = EmbeddedLocalizedValue.Text("&c不明なモジュールです: {id}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.feature_disabled", value = EmbeddedLocalizedValue.Text("&eモジュール {id} は現在無効です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.changed", value = EmbeddedLocalizedValue.Text("&aモジュール {id} を {state} に変更しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.unchanged", value = EmbeddedLocalizedValue.Text("&7モジュール {id} はすでに {state} です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.failed", value = EmbeddedLocalizedValue.Text("&cモジュール {id} の変更に失敗しました。設定を元に戻しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.status.header", value = EmbeddedLocalizedValue.Text("&bChanpon-Utilities モジュール状態"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.status.entry", value = EmbeddedLocalizedValue.Text("&7- &f{id}&7: {state}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.state.enabled", value = EmbeddedLocalizedValue.Text("&a有効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.state.disabled", value = EmbeddedLocalizedValue.Text("&c無効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.management.state.unavailable", value = EmbeddedLocalizedValue.Text("&e有効（依存プラグインなし）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.unlink.success", value = EmbeddedLocalizedValue.Text("&a書見台とのリンクを解除しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.unlink.already", value = EmbeddedLocalizedValue.Text("&eこの本棚はリンクされていません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.started", value = EmbeddedLocalizedValue.TextList(listOf("&bFreeCamを開始しました！", "&7Shiftキーを2回押すか、/freecam stopコマンドで終了できます")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.previous_camera_too_far", value = EmbeddedLocalizedValue.TextList(listOf("&7最後に使用した位置から離れたため、現在位置からFreeCamを開始しました")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.recovered", value = EmbeddedLocalizedValue.Text("&e未終了のFreeCam状態から身体の位置へ復旧しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.unavailable", value = EmbeddedLocalizedValue.Text("&c現在FreeCamを開始できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.not_active", value = EmbeddedLocalizedValue.Text("&cFreeCam中ではありません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.usage", value = EmbeddedLocalizedValue.Text("&c使用法: /freecam [start|stop|status|toggle_shortcut|reset_distance]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.shortcut.enabled", value = EmbeddedLocalizedValue.Text("&aFreeCamのショートカットを有効にしました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.shortcut.disabled", value = EmbeddedLocalizedValue.Text("&eFreeCamのショートカットを無効にしました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.reset_distance.usage", value = EmbeddedLocalizedValue.Text("&c使用法: /freecam reset_distance <0～30000000 | reset>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.reset_distance.changed", value = EmbeddedLocalizedValue.Text("&aカメラ位置のリセット距離を {distance} ブロックに設定しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.reset_distance.reset", value = EmbeddedLocalizedValue.Text("&aカメラ位置のリセット距離を既定値の {distance} ブロックに戻しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.status.active", value = EmbeddedLocalizedValue.Text("&aFreeCam中です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.status.inactive", value = EmbeddedLocalizedValue.Text("&7FreeCam中ではありません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.body.damaged", value = EmbeddedLocalizedValue.Text("&c身体が攻撃されたためFreeCamを終了しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.item.name", value = EmbeddedLocalizedValue.Text("&bFreeCam起動アイテム"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.freecam.item.lore", value = EmbeddedLocalizedValue.Text("&7使用するとFreeCamを開始します。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.safety.enabled", value = EmbeddedLocalizedValue.Text("&a観光用の安全機能をオンにしました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.safety.disabled", value = EmbeddedLocalizedValue.Text("&e観光用の安全機能をオフにしました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon_utilities.safety.blocked", value = EmbeddedLocalizedValue.Text("&7現在、観光用の安全機能がオンになっています。&e/anzen&7でオフにできます。"), domain = DOMAIN),
    )

}
