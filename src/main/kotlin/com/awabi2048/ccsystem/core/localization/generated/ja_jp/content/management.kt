package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpContentManagementCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "content/management"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "content_management.no_permission", value = EmbeddedLocalizedValue.Text("&c権限がありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.usage", value = EmbeddedLocalizedValue.Text("&e使用法: /ccc <status|enable|disable> [feature]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.status_header", value = EmbeddedLocalizedValue.Text("&6=== CC-Content コンテンツ状態 ==="), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.status_line", value = EmbeddedLocalizedValue.Text("&7- &f{feature}: {status}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.enabled", value = EmbeddedLocalizedValue.Text("&a有効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.disabled", value = EmbeddedLocalizedValue.Text("&c無効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.warning", value = EmbeddedLocalizedValue.Text("&e有効（警告あり）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.state.failure", value = EmbeddedLocalizedValue.Text("&c有効（初期化失敗）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.arena", value = EmbeddedLocalizedValue.Text("アリーナ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.rank", value = EmbeddedLocalizedValue.Text("職業・ランク"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.brewery", value = EmbeddedLocalizedValue.Text("醸造"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.cooking", value = EmbeddedLocalizedValue.Text("料理"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.fishing", value = EmbeddedLocalizedValue.Text("釣り"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.resource_collection", value = EmbeddedLocalizedValue.Text("資源収集"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.seasonal", value = EmbeddedLocalizedValue.Text("季節イベント"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.sukima_dungeon", value = EmbeddedLocalizedValue.Text("スキマダンジョン"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.party", value = EmbeddedLocalizedValue.Text("パーティー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature.minigame", value = EmbeddedLocalizedValue.Text("ミニゲーム"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.unknown_feature", value = EmbeddedLocalizedValue.Text("&c不明なコンテンツです: {feature}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.feature_unavailable", value = EmbeddedLocalizedValue.Text("&c{feature}は現在利用できません。/ccc status で状態を確認してください"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.already_set", value = EmbeddedLocalizedValue.Text("&e{feature} は既に{status}です"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.enable_dependency_required", value = EmbeddedLocalizedValue.Text("&c{feature}を有効化する前に、次のコンテンツを有効化してください: {dependencies}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.enable_dependency_unavailable", value = EmbeddedLocalizedValue.Text("&c{feature}を有効化できません。次の依存コンテンツの初期化状態を解消してください: {dependencies}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.disable_dependent_required", value = EmbeddedLocalizedValue.Text("&c{feature}を無効化する前に、次のコンテンツを無効化してください: {dependents}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.change_started", value = EmbeddedLocalizedValue.Text("&6{feature}を{status}に変更し、CC-Contentを再初期化します..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.change_success", value = EmbeddedLocalizedValue.Text("&a{feature}を{status}に変更しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.save_failed", value = EmbeddedLocalizedValue.Text("&cコンテンツ設定の保存に失敗しました: {reason}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.usage", value = EmbeddedLocalizedValue.Text("&e使用法: /ccc debug <clear_block_placement_data|placement_recording|clear_fishing_grounds|complete_oage_daily> ..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.unknown_mode", value = EmbeddedLocalizedValue.Text("&c不明なデバッグ項目です: {mode}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.usage", value = EmbeddedLocalizedValue.Text("&e使用法: /ccc debug placement_recording <enable|disable|status>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.unavailable", value = EmbeddedLocalizedValue.Text("&cブロック配置記録の制御機能が利用できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.status", value = EmbeddedLocalizedValue.Text("&7ブロック配置記録: {status}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.changed", value = EmbeddedLocalizedValue.Text("&aブロック配置記録を{status}にしました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.state.enabled", value = EmbeddedLocalizedValue.Text("&a有効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.placement_recording.state.disabled", value = EmbeddedLocalizedValue.Text("&c無効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.fishing_ground.usage", value = EmbeddedLocalizedValue.Text("&e使用法: /ccc debug clear_fishing_grounds"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.fishing_ground.unavailable", value = EmbeddedLocalizedValue.Text("&c天然の好漁場を管理する機能が利用できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.debug.fishing_ground.cleared", value = EmbeddedLocalizedValue.Text("&a現在の天然の好漁場をすべて削除しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.no_permission", value = EmbeddedLocalizedValue.Text("&cDisplayパーティクルを表示する権限がありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.usage", value = EmbeddedLocalizedValue.Text("&e使用法: /ccc particle <pattern> [<x> <y> <z>] [<dx> <dy> <dz> <speed> <count> [normal|force]]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.invalid_argument", value = EmbeddedLocalizedValue.Text("&cパーティクル引数が不正です: {detail}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.unknown_pattern", value = EmbeddedLocalizedValue.Text("&c未登録のDisplayパーティクル・プリセットです: {pattern}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.no_viewers", value = EmbeddedLocalizedValue.Text("&e表示範囲内にプレイヤーがいないため生成を省略しました。必要なら force を指定してください"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.started", value = EmbeddedLocalizedValue.Text("&aDisplayパーティクルを表示しました: {pattern} x{count}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "content_management.particle.rejected", value = EmbeddedLocalizedValue.Text("&cDisplayパーティクルを表示できませんでした: {detail}"), domain = DOMAIN),
    )

}
