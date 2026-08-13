package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpContentTutorialRankCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "content/tutorial_rank"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "tutorial_rank.newbie.name", value = EmbeddedLocalizedValue.Text("Newbie"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.newbie.description", value = EmbeddedLocalizedValue.Text("§6くらくろ §7へようこそ！"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.visitor.name", value = EmbeddedLocalizedValue.Text("Visitor"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.visitor.description", value = EmbeddedLocalizedValue.Text("何からはじめようかな？"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.pioneer.name", value = EmbeddedLocalizedValue.Text("Pioneer"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.pioneer.description", value = EmbeddedLocalizedValue.Text("冒険しよう"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.adventurer.name", value = EmbeddedLocalizedValue.Text("Adventurer"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.adventurer.description", value = EmbeddedLocalizedValue.Text("ネザーとエンドへの道を切り開こう"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.attainer.name", value = EmbeddedLocalizedValue.Text("Attainer"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.attainer.description", value = EmbeddedLocalizedValue.Text("チュートリアル達成者"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.attainer.guild_guide.name", value = EmbeddedLocalizedValue.Text("§e§l職業について"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.attainer.guild_guide.completed", value = EmbeddedLocalizedValue.Text("チュートリアルを完了しました。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.attainer.guild_guide.description", value = EmbeddedLocalizedValue.Text("職業は職業ギルドで選択できます。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.attainer.guild_guide.location", value = EmbeddedLocalizedValue.Text("ギルドの案内場所を探してみましょう。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.attainer.reached_messages", value = EmbeddedLocalizedValue.TextList(listOf("§d§lAttainerに到達しました！", "§7チュートリアルランクは完了です。職業を選ぶ準備ができました。", "§7職業メニューは別導線から開いてください。")), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.name", value = EmbeddedLocalizedValue.Text("進行状況"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.description", value = EmbeddedLocalizedValue.Text("チュートリアル全体の進行状況です。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.current_rank", value = EmbeddedLocalizedValue.Text("現在のランク"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.next_rank", value = EmbeddedLocalizedValue.Text("次のランク"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.rank.newbie", value = EmbeddedLocalizedValue.Text("新規"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.rank.visitor", value = EmbeddedLocalizedValue.Text("訪問者"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.rank.pioneer", value = EmbeddedLocalizedValue.Text("開拓者"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.rank.adventurer", value = EmbeddedLocalizedValue.Text("冒険者"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.progress.rank.attainer", value = EmbeddedLocalizedValue.Text("達成者"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.nether_locked", value = EmbeddedLocalizedValue.Text("§cネザーへ行くには、先にPioneerへ到達してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.end_locked", value = EmbeddedLocalizedValue.Text("§cエンドへ行くには、先にAdventurerへ到達してください。"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.no_requirement", value = EmbeddedLocalizedValue.Text("このランクの到達条件はありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.system_not_initialized", value = EmbeddedLocalizedValue.Text("タスク定義が初期化されていません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.play_time", value = EmbeddedLocalizedValue.Text("サーバーで遊ぶ"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.active_overworld", value = EmbeddedLocalizedValue.Text("オーバーワールドを探索する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.active_nether_resource", value = EmbeddedLocalizedValue.Text("ネザー資源ワールドを探索する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.myworld_created", value = EmbeddedLocalizedValue.Text("マイワールドを作成する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.diamond_ore", value = EmbeddedLocalizedValue.Text("ダイヤモンド鉱石"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.nether_portal", value = EmbeddedLocalizedValue.Text("ネザーポータルを作成する"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.ender_eye", value = EmbeddedLocalizedValue.Text("エンダーアイをクラフト"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.end_portal", value = EmbeddedLocalizedValue.Text("エンドポータルを開通させる"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.label.exp", value = EmbeddedLocalizedValue.Text("経験値を集める"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.suffix.defeat", value = EmbeddedLocalizedValue.Text("を討伐"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.suffix.collect", value = EmbeddedLocalizedValue.Text("を集める"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.unit.minute", value = EmbeddedLocalizedValue.Text("分"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.unit.item", value = EmbeddedLocalizedValue.Text("個"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.unit.entity", value = EmbeddedLocalizedValue.Text("体"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.unit.experience", value = EmbeddedLocalizedValue.Text("XP"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.status.completed", value = EmbeddedLocalizedValue.Text("達成済み"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.status.incomplete", value = EmbeddedLocalizedValue.Text("未達成"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "tutorial_rank.task.status.future", value = EmbeddedLocalizedValue.Text("未到達"), domain = DOMAIN),
    )

}
