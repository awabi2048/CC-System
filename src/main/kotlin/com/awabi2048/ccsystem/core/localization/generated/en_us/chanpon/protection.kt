package com.awabi2048.ccsystem.core.localization.generated

import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue
import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry

/** ちゃんぽん建築保護の拒否理由通知（英語）の埋込カタログです。 */
internal object EnUsChanponProtectionCatalog {
    const val LOCALE: String = "en_us"
    const val DOMAIN: String = "chanpon/protection"

    fun entries(): List<EmbeddedLocalizationEntry> = listOf(
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.migration", value = EmbeddedLocalizedValue.Text("You cannot build here while the configuration is migrating"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.completed", value = EmbeddedLocalizedValue.Text("You cannot build here: this world has been completed"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.submitted", value = EmbeddedLocalizedValue.Text("You cannot build here: this world has been submitted"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.auto_rollback", value = EmbeddedLocalizedValue.Text("You cannot build here: this production world is locked for scheduled rollback"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.global", value = EmbeddedLocalizedValue.Text("Building is currently disabled on this server"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.archive", value = EmbeddedLocalizedValue.Text("You cannot build here: this world is archived"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.owner_only", value = EmbeddedLocalizedValue.Text("You cannot build here: this world is restricted to its owner"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.members_only", value = EmbeddedLocalizedValue.Text("You cannot build here: this world is restricted to members"), domain = DOMAIN),
        EmbeddedLocalizationEntry(key = "chanpon.protection.build_denied.spawn", value = EmbeddedLocalizedValue.Text("You cannot place blocks at the spawn point"), domain = DOMAIN),
    )
}
