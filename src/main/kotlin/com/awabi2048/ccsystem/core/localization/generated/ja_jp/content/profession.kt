package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpContentProfessionCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "content/profession"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "profession.lumberjack.name", value = EmbeddedLocalizedValue.Text("木こり"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.lumberjack.description", value = EmbeddedLocalizedValue.Text("木を伐採して経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.brewer.name", value = EmbeddedLocalizedValue.Text("醸造家"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.brewer.description", value = EmbeddedLocalizedValue.Text("ポーション醸造で経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.miner.name", value = EmbeddedLocalizedValue.Text("鉱夫"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.miner.description", value = EmbeddedLocalizedValue.Text("鉱石採掘で経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.cook.name", value = EmbeddedLocalizedValue.Text("料理人"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.cook.description", value = EmbeddedLocalizedValue.Text("料理を作って経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.swordsman.name", value = EmbeddedLocalizedValue.Text("剣士"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.swordsman.description", value = EmbeddedLocalizedValue.Text("モンスター討伐で経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.warrior.name", value = EmbeddedLocalizedValue.Text("戦士"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.warrior.description", value = EmbeddedLocalizedValue.Text("近接戦闘で経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.farmer.name", value = EmbeddedLocalizedValue.Text("農家"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.farmer.description", value = EmbeddedLocalizedValue.Text("作物の収穫で経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.gardener.name", value = EmbeddedLocalizedValue.Text("庭師"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.gardener.description", value = EmbeddedLocalizedValue.Text("装飾的なブロック設置で経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.carpenter.name", value = EmbeddedLocalizedValue.Text("大工"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.carpenter.description", value = EmbeddedLocalizedValue.Text("建築で経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.fisher.name", value = EmbeddedLocalizedValue.Text("釣り人"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.fisher.description", value = EmbeddedLocalizedValue.Text("釣り場の条件を読み、釣果を釣り上げて経験値を得る職業"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.level_up", value = EmbeddedLocalizedValue.Text("§a{profession}がレベルアップしました！ §eLv.{old} -> Lv.{new} §7(+{gained})"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.new_unlock", value = EmbeddedLocalizedValue.Text("新しく習得可能なスキルがあります（{count}個）: {skills} [クリックしてスキルツリーを開く]"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.bossbar", value = EmbeddedLocalizedValue.Text("§6【{profession}】 §7Lv. §e§l{level} §7(§a{current}§7/{required}§7)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.bossbar_with_gain", value = EmbeddedLocalizedValue.Text("§6【{profession}】 §7Lv. §e§l{level} §7(§a{current}§7/{required} +§a{gained}§7)"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.tunnel_mining.name", value = EmbeddedLocalizedValue.Text("坑道採掘"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.precision_mining.name", value = EmbeddedLocalizedValue.Text("精密採掘"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.felling.name", value = EmbeddedLocalizedValue.Text("伐採"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.wood_utilization.name", value = EmbeddedLocalizedValue.Text("木材活用"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.cultivation.name", value = EmbeddedLocalizedValue.Text("栽培"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.wild_gathering.name", value = EmbeddedLocalizedValue.Text("野外採集"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.rod_handling.name", value = EmbeddedLocalizedValue.Text("竿さばき"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.fishing_ground_knowledge.name", value = EmbeddedLocalizedValue.Text("釣り場知識"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.fermentation.name", value = EmbeddedLocalizedValue.Text("発酵調合"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.distillation_aging.name", value = EmbeddedLocalizedValue.Text("蒸留熟成"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.bulk_cooking.name", value = EmbeddedLocalizedValue.Text("まとめ調理"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.specialization.precision_cooking.name", value = EmbeddedLocalizedValue.Text("精密調理"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.executed", value = EmbeddedLocalizedValue.Text("§d{profession}の周回を完了しました。周回数は{level}です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.name", value = EmbeddedLocalizedValue.Text("§d§l{profession}の思念"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.description", value = EmbeddedLocalizedValue.Text("{profession}を極めた証"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.cycle", value = EmbeddedLocalizedValue.Text("周回数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.owner", value = EmbeddedLocalizedValue.Text("保有者"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.specialization", value = EmbeddedLocalizedValue.Text("分岐"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "profession.prestige.token.representative_statistic", value = EmbeddedLocalizedValue.Text("代表実績"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.display", value = EmbeddedLocalizedValue.Text("モード切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.no_targets", value = EmbeddedLocalizedValue.Text("切り替え対象のスキルがありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.targets", value = EmbeddedLocalizedValue.Text("切り替え対象スキル:"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.enabled", value = EmbeddedLocalizedValue.Text("有効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.disabled", value = EmbeddedLocalizedValue.Text("無効"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.mode", value = EmbeddedLocalizedValue.Text("切り替え様式"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.left_click", value = EmbeddedLocalizedValue.Text("左クリック"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.right_click", value = EmbeddedLocalizedValue.Text("右クリック"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.select_action", value = EmbeddedLocalizedValue.Text("スキル選択を切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "active_skill.selector.mode_action", value = EmbeddedLocalizedValue.Text("様式を変更"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "release.profession_unavailable", value = EmbeddedLocalizedValue.Text("この職業は現在利用できません。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "release.profession_unavailable_lore", value = EmbeddedLocalizedValue.Text("公開準備中です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "release.skill_unavailable", value = EmbeddedLocalizedValue.Text("職業スキルは現在利用できません。"), domain = DOMAIN),
    )

}
