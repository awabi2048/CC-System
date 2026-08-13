package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpResourceCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "resource"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "broadcast_success", value = EmbeddedLocalizedValue.Text("§a資源ワールド【%world_name%】の生成が完了しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "console_success", value = EmbeddedLocalizedValue.Text("§a資源ワールド【%world_name%】の生成に成功しました。(ボーダーサイズ: %border_size%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "pregen_priority_complete", value = EmbeddedLocalizedValue.Text("§a資源ワールド【%world_name%】の仮生成が完了し、ワープ可能になりました！"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "pregen_all_complete", value = EmbeddedLocalizedValue.Text("資源ワールド【%world_name%】の全エリア生成が完了しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.generate", value = EmbeddedLocalizedValue.Text("§c使用法: /resource generate <type>:<variation> [border_size] [difficulty]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.teleport", value = EmbeddedLocalizedValue.Text("§c使用法: /resource teleport <type>:<variation> [player]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.pause_pregen", value = EmbeddedLocalizedValue.Text("§c使用法: /resource pause_pregen <type>:<variation>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.close", value = EmbeddedLocalizedValue.Text("§c使用法: /resource close <type>:<variation>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.invalid_format", value = EmbeddedLocalizedValue.Text("§c形式が正しくありません (例: normal:a)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.generation_start", value = EmbeddedLocalizedValue.Text("§e資源ワールド %type%:%variation% の生成を開始します..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.generation_success", value = EmbeddedLocalizedValue.Text("§a資源ワールド %type%:%variation% の生成に成功しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.generation_failed", value = EmbeddedLocalizedValue.Text("§c資源ワールド %type%:%variation% の生成に失敗しました。詳細はコンソールを確認してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.delete_incomplete_abort", value = EmbeddedLocalizedValue.Text("既存の資源ワールド削除が完了しなかったため、新規生成を中止しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.delete_incomplete_abort_broadcast", value = EmbeddedLocalizedValue.Text("§c[CC-System] 既存の資源ワールド削除が完了しなかったため、新規生成を中止しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.world_create_failed", value = EmbeddedLocalizedValue.Text("§c[CC-System] ワールド %world_name% の生成に失敗しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.evacuated_for_regeneration", value = EmbeddedLocalizedValue.Text("§e[CC-System] 資源ワールドが再生成されるため、避難しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.world_not_found", value = EmbeddedLocalizedValue.Text("§c[CC-System] 指定された資源ワールドが存在しません。生成してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.world_not_ready", value = EmbeddedLocalizedValue.Text("§c[CC-System] 資源ワールドは現在準備中です。優先エリアの生成をお待ちください (%progress%%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.teleport_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] 資源ワールド (%type%:%variation%) に移動しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.returned_on_close", value = EmbeddedLocalizedValue.Text("§e[CC-System] 資源ワールドが閉鎖されたため、帰還しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.returned_from_world", value = EmbeddedLocalizedValue.Text("§a資源ワールドから帰還しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.normal.name", value = EmbeddedLocalizedValue.Text("通常資源ワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.nether.name", value = EmbeddedLocalizedValue.Text("ネザー資源ワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.end.name", value = EmbeddedLocalizedValue.Text("エンド資源ワールド"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.reload_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] 設定を再読み込みしました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.pause_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] 資源ワールド (%type%:%variation%) の事前読み込みを中断しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.pause_failed", value = EmbeddedLocalizedValue.Text("§c[CC-System] 資源ワールド (%type%:%variation%) の事前読み込みは実行されていません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.close_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] 資源ワールド (%type%:%variation%) を閉鎖しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.close_failed", value = EmbeddedLocalizedValue.Text("§c[CC-System] 資源ワールド (%type%:%variation%) が存在しません。"), domain = DOMAIN),
    )

}
