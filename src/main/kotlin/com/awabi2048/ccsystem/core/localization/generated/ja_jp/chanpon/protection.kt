package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** ちゃんぽん建築保護の拒否理由通知（日本語）の埋込カタログです。 */
internal object JaJpChanponProtectionCatalog {
    const val LOCALE: String = "ja_jp"
    const val DOMAIN: String = "chanpon/protection"

    fun entries(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.migration", value = EmbeddedLocalizedValue.Text("設定移行中のため、現在このワールドでは建築できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.completed", value = EmbeddedLocalizedValue.Text("このワールドは完成済みのため、建築できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.submitted", value = EmbeddedLocalizedValue.Text("このワールドは提出済みのため、建築できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.auto_rollback", value = EmbeddedLocalizedValue.Text("自動ロールバック対象の本番ワールドのため、建築できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.global", value = EmbeddedLocalizedValue.Text("現在は全体の建築が停止されています"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.archive", value = EmbeddedLocalizedValue.Text("このワールドはアーカイブ済みのため、建築できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.owner_only", value = EmbeddedLocalizedValue.Text("このワールドはオーナー限定のため、建築できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.members_only", value = EmbeddedLocalizedValue.Text("このワールドはメンバー限定のため、建築できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.spawn", value = EmbeddedLocalizedValue.Text("スポーン地点にはブロックを設置できません"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.external", value = EmbeddedLocalizedValue.Text("このブロックは保護されています。これがエラーだと思われる場合は、スタッフにお問い合わせください。"), domain = DOMAIN),
    )
}
