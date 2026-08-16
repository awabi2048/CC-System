package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsResourceCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "resource"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "broadcast_success", value = EmbeddedLocalizedValue.Text("§aResource world 【%world_name%】 generation has completed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "console_success", value = EmbeddedLocalizedValue.Text("§aResource world 【%world_name%】 has been successfully generated. (Border size: %border_size%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "pregen_priority_complete", value = EmbeddedLocalizedValue.Text("§aResource world 【%world_name%】 priority area pre-generation is complete and you can warp now!"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "pregen_all_complete", value = EmbeddedLocalizedValue.Text("Resource world 【%world_name%】 full area generation has completed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.generate", value = EmbeddedLocalizedValue.Text("§cUsage: /resource generate <type>:<variation> [border_size] [difficulty]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.teleport", value = EmbeddedLocalizedValue.Text("§cUsage: /resource teleport <type>:<variation> [player]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.pause_pregen", value = EmbeddedLocalizedValue.Text("§cUsage: /resource pause_pregen <type>:<variation>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.usage.close", value = EmbeddedLocalizedValue.Text("§cUsage: /resource close <type>:<variation>"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.invalid_format", value = EmbeddedLocalizedValue.Text("§cInvalid format (example: normal:a)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.generation_start", value = EmbeddedLocalizedValue.Text("§eStarting generation of resource world %type%:%variation%..."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.generation_success", value = EmbeddedLocalizedValue.Text("§aResource world %type%:%variation% has been successfully generated."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.generation_failed", value = EmbeddedLocalizedValue.Text("§cFailed to generate resource world %type%:%variation%. Check console for details."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.delete_incomplete_abort", value = EmbeddedLocalizedValue.Text("Generation was aborted because the existing resource world could not be fully deleted."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.delete_incomplete_abort_broadcast", value = EmbeddedLocalizedValue.Text("§c[CC-System] Generation was aborted because the existing resource world could not be fully deleted."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.world_create_failed", value = EmbeddedLocalizedValue.Text("§c[CC-System] Failed to create world %world_name%."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.evacuated_for_regeneration", value = EmbeddedLocalizedValue.Text("§e[CC-System] You were evacuated because the resource world is being regenerated."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.world_not_found", value = EmbeddedLocalizedValue.Text("§c[CC-System] The specified resource world does not exist. Please generate it first."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.world_not_ready", value = EmbeddedLocalizedValue.Text("§c[CC-System] The resource world is still being prepared. Please wait for priority generation to finish (%progress%%)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.teleport_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] Moved to resource world (%type%:%variation%)."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.returned_on_close", value = EmbeddedLocalizedValue.Text("§e[CC-System] You were returned because the resource world was closed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.returned_from_world", value = EmbeddedLocalizedValue.Text("§aReturned from the resource world."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.normal.name", value = EmbeddedLocalizedValue.Text("NormalResourceWorld"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.nether.name", value = EmbeddedLocalizedValue.Text("NetherResourceWorld"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.end.name", value = EmbeddedLocalizedValue.Text("EndResourceWorld"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.reload_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] Configuration has been reloaded."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.pause_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] Pre-generation of resource world (%type%:%variation%) has been paused."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.pause_failed", value = EmbeddedLocalizedValue.Text("§c[CC-System] Resource world (%type%:%variation%) pre-generation is not running."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.close_success", value = EmbeddedLocalizedValue.Text("§a[CC-System] Resource world (%type%:%variation%) has been closed."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "resource.close_failed", value = EmbeddedLocalizedValue.Text("§c[CC-System] Resource world (%type%:%variation%) does not exist."), domain = DOMAIN),
    )

}
