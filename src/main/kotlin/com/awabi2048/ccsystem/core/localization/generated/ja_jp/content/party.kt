package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */
internal object JaJpContentPartyCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "content/party"

    fun entries(): List<EmbeddedLocalizationEntry> = buildList {
        addAll(chunk1())
    }

    private fun chunk1(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "party.name", value = EmbeddedLocalizedValue.Text("パーティー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.invite_expired", value = EmbeddedLocalizedValue.Text("パーティー招待の有効期限が切れています"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.joined", value = EmbeddedLocalizedValue.Text("パーティーに参加しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.left", value = EmbeddedLocalizedValue.Text("パーティーから脱退しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.kicked", value = EmbeddedLocalizedValue.Text("パーティーから追放されました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.disbanded", value = EmbeddedLocalizedValue.Text("パーティーが解散されました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.leader_transferred", value = EmbeddedLocalizedValue.Text("パーティーリーダーを移譲しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat_prefix", value = EmbeddedLocalizedValue.Text("パーティー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.feature_disabled", value = EmbeddedLocalizedValue.Text("パーティー機能は現在無効です"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.created", value = EmbeddedLocalizedValue.Text("パーティ「{party}」を作成しました（ID: {id}）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.invited", value = EmbeddedLocalizedValue.Text("{inviter}からパーティー「{party}」への招待（クリックで参加）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.member_joined", value = EmbeddedLocalizedValue.Text("{player} がパーティに参加しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.member_left", value = EmbeddedLocalizedValue.Text("{player} がパーティを脱退しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.member_kicked", value = EmbeddedLocalizedValue.Text("{player} をパーティから追放しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.invite_sent", value = EmbeddedLocalizedValue.Text("{player} に招待を送りました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.info", value = EmbeddedLocalizedValue.Text("パーティ「{party}」 ID={id} リーダー={leader} メンバー={members} ({count}/{capacity}) 募集中={recruiting}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.recruiting_updated", value = EmbeddedLocalizedValue.Text("パーティ募集状態を {enabled} に変更しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat", value = EmbeddedLocalizedValue.Text("「{sender}」 {message}"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.not_in_party", value = EmbeddedLocalizedValue.Text("パーティに所属していません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.player_not_online", value = EmbeddedLocalizedValue.Text("対象プレイヤーはオンラインではありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.usage", value = EmbeddedLocalizedValue.Text("/party menu|list|create|info|invite|accept|join|leave|kick|leader|disband|recruit|chat|chat-toggle"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.default_name", value = EmbeddedLocalizedValue.Text("{player}のパーティー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.recruiting_notice", value = EmbeddedLocalizedValue.Text("パーティー「{party}」が募集中（{count}/{capacity}、クリックで参加）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.list.header", value = EmbeddedLocalizedValue.Text("募集中のパーティー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.list.empty", value = EmbeddedLocalizedValue.Text("募集中のパーティーはありません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.list.entry", value = EmbeddedLocalizedValue.Text("・{party}（{count}/{capacity}、クリックで参加）"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.interaction.busy", value = EmbeddedLocalizedValue.Text("ほかの左クリック操作を受け付けているため、招待を開始できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.interaction.invite_waiting", value = EmbeddedLocalizedValue.Text("招待するプレイヤーを左クリック"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat_channel.enabled", value = EmbeddedLocalizedValue.Text("パーティーチャットに切り替えました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.chat_channel.disabled", value = EmbeddedLocalizedValue.Text("通常チャットに戻しました"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.title", value = EmbeddedLocalizedValue.Text("パーティー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.state", value = EmbeddedLocalizedValue.Text("状態"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.leader_only", value = EmbeddedLocalizedValue.Text("パーティーリーダーのみ変更できます"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.value.none", value = EmbeddedLocalizedValue.Text("なし"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.name", value = EmbeddedLocalizedValue.Text("名前・説明"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.party_name", value = EmbeddedLocalizedValue.Text("パーティー名"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.description", value = EmbeddedLocalizedValue.Text("説明"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.settings.action", value = EmbeddedLocalizedValue.Text("設定を開く"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.name", value = EmbeddedLocalizedValue.Text("メンバー募集"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.enabled", value = EmbeddedLocalizedValue.Text("募集中"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.disabled", value = EmbeddedLocalizedValue.Text("募集停止中"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.recruiting.action", value = EmbeddedLocalizedValue.Text("募集状態を切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.disband.name", value = EmbeddedLocalizedValue.Text("パーティーを解散"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.disband.warning", value = EmbeddedLocalizedValue.Text("この操作は取り消せません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.disband.action", value = EmbeddedLocalizedValue.Text("解散確認を開く"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.name", value = EmbeddedLocalizedValue.Text("パーティーチャット"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.enabled", value = EmbeddedLocalizedValue.Text("パーティーチャット"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.disabled", value = EmbeddedLocalizedValue.Text("通常チャット"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.chat.action", value = EmbeddedLocalizedValue.Text("チャットを切り替え"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.name", value = EmbeddedLocalizedValue.Text("パーティー情報"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.party_name", value = EmbeddedLocalizedValue.Text("パーティー名"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.description", value = EmbeddedLocalizedValue.Text("説明"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.leader_label", value = EmbeddedLocalizedValue.Text("リーダー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.member_label", value = EmbeddedLocalizedValue.Text("メンバー"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.info.capacity_label", value = EmbeddedLocalizedValue.Text("人数"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.name", value = EmbeddedLocalizedValue.Text("プレイヤーを招待"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.full", value = EmbeddedLocalizedValue.Text("パーティーが満員です"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.select", value = EmbeddedLocalizedValue.Text("対象プレイヤーを選択"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.menu.invite.input", value = EmbeddedLocalizedValue.Text("プレイヤー名を入力"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.body", value = EmbeddedLocalizedValue.Text("内容を入力"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.submit", value = EmbeddedLocalizedValue.Text("決定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.cancel", value = EmbeddedLocalizedValue.Text("キャンセル"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.settings.title", value = EmbeddedLocalizedValue.Text("パーティー設定"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.settings.name", value = EmbeddedLocalizedValue.Text("パーティー名"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.settings.description", value = EmbeddedLocalizedValue.Text("説明"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.invite.title", value = EmbeddedLocalizedValue.Text("プレイヤーを招待"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.invite.target", value = EmbeddedLocalizedValue.Text("プレイヤー名"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.disband.title", value = EmbeddedLocalizedValue.Text("パーティーを解散"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.dialog.disband.body", value = EmbeddedLocalizedValue.Text("パーティーを解散します。この操作は取り消せません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "party.error.invalid_action", value = EmbeddedLocalizedValue.Text("パーティ操作の引数または状態が不正です"), domain = DOMAIN),
    )

}
