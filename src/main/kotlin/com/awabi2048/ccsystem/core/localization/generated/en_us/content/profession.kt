package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object EnUsContentProfessionCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "content/profession"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "profession.level_up", value = EmbeddedLocalizedValue.Text("§a{profession} leveled up! §eLv.{old} -> Lv.{new} §7(+{gained})"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.new_unlock", value = EmbeddedLocalizedValue.Text("New unlockable skills available ({count}): {skills} [Click to open skill tree]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.bossbar", value = EmbeddedLocalizedValue.Text("§6[{profession}] §7Lv. §e§l{level} §7(§a{current}§7/{required}§7)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.bossbar_with_gain", value = EmbeddedLocalizedValue.Text("§6[{profession}] §7Lv. §e§l{level} §7(§a{current}§7/{required} +§a{gained}§7)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.tunnel_mining.name", value = EmbeddedLocalizedValue.Text("Tunnel Mining"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.precision_mining.name", value = EmbeddedLocalizedValue.Text("Precision Mining"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.felling.name", value = EmbeddedLocalizedValue.Text("Felling"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.wood_utilization.name", value = EmbeddedLocalizedValue.Text("Wood Utilization"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.cultivation.name", value = EmbeddedLocalizedValue.Text("Cultivation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.wild_gathering.name", value = EmbeddedLocalizedValue.Text("Wild Gathering"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.rod_handling.name", value = EmbeddedLocalizedValue.Text("Rod Handling"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.fishing_ground_knowledge.name", value = EmbeddedLocalizedValue.Text("Fishing Ground Knowledge"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.fermentation.name", value = EmbeddedLocalizedValue.Text("Fermentation"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.distillation_aging.name", value = EmbeddedLocalizedValue.Text("Distillation and Aging"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.bulk_cooking.name", value = EmbeddedLocalizedValue.Text("Bulk Cooking"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.precision_cooking.name", value = EmbeddedLocalizedValue.Text("Precision Cooking"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.executed", value = EmbeddedLocalizedValue.Text("§dCompleted a {profession} cycle. Total cycles: {level}."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.name", value = EmbeddedLocalizedValue.Text("§d§l{profession} Remembrance"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.description", value = EmbeddedLocalizedValue.Text("Proof of mastering {profession}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.cycle", value = EmbeddedLocalizedValue.Text("Cycle"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.owner", value = EmbeddedLocalizedValue.Text("Owner"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.specialization", value = EmbeddedLocalizedValue.Text("Specialization"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.representative_statistic", value = EmbeddedLocalizedValue.Text("Representative record"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.lumberjack.name", value = EmbeddedLocalizedValue.Text("Lumberjack"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.lumberjack.description", value = EmbeddedLocalizedValue.Text("Gain profession experience by chopping wood"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.brewer.name", value = EmbeddedLocalizedValue.Text("Brewer"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.brewer.description", value = EmbeddedLocalizedValue.Text("Gain profession experience by brewing drinks"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.miner.name", value = EmbeddedLocalizedValue.Text("Miner"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.miner.description", value = EmbeddedLocalizedValue.Text("Gain profession experience by mining ores"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.cook.name", value = EmbeddedLocalizedValue.Text("Cook"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.cook.description", value = EmbeddedLocalizedValue.Text("Gain profession experience by cooking meals"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.swordsman.name", value = EmbeddedLocalizedValue.Text("Swordsman"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.swordsman.description", value = EmbeddedLocalizedValue.Text("Gain profession experience by defeating monsters"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.warrior.name", value = EmbeddedLocalizedValue.Text("Warrior"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.warrior.description", value = EmbeddedLocalizedValue.Text("Gain profession experience through combat"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.farmer.name", value = EmbeddedLocalizedValue.Text("Farmer"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.farmer.description", value = EmbeddedLocalizedValue.Text("Gain profession experience by harvesting crops"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.gardener.name", value = EmbeddedLocalizedValue.Text("Gardener"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.gardener.description", value = EmbeddedLocalizedValue.Text("Gain profession experience through gardening"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.carpenter.name", value = EmbeddedLocalizedValue.Text("Carpenter"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.carpenter.description", value = EmbeddedLocalizedValue.Text("A profession that gains experience by building structures"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.fisher.name", value = EmbeddedLocalizedValue.Text("Fisher"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.fisher.description", value = EmbeddedLocalizedValue.Text("Read fishing conditions, catch fish, and gain profession experience"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.display", value = EmbeddedLocalizedValue.Text("Toggle mode"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.no_targets", value = EmbeddedLocalizedValue.Text("No skills can be toggled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.targets", value = EmbeddedLocalizedValue.Text("Toggleable skills:"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.enabled", value = EmbeddedLocalizedValue.Text("Enabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.disabled", value = EmbeddedLocalizedValue.Text("Disabled"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.mode", value = EmbeddedLocalizedValue.Text("Toggle mode"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.left_click", value = EmbeddedLocalizedValue.Text("Left-Click"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.right_click", value = EmbeddedLocalizedValue.Text("Right-Click"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.select_action", value = EmbeddedLocalizedValue.Text("Change selected skill"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.mode_action", value = EmbeddedLocalizedValue.Text("Change toggle mode"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "release.profession_unavailable", value = EmbeddedLocalizedValue.Text("This profession is not currently available."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "release.profession_unavailable_lore", value = EmbeddedLocalizedValue.Text("It is still being prepared for release."), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "release.skill_unavailable", value = EmbeddedLocalizedValue.Text("Profession skills are not currently available."), domain = DOMAIN),
    )

}
