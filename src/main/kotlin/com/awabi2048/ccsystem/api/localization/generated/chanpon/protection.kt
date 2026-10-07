package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** ちゃんぽんの建築保護で拒否理由を通知するための型付きローカライズキーです。 */
object ChanponProtectionKeys {
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_MIGRATION = LocalizationKey.text("chanpon.protection.build_denied.migration")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_COMPLETED = LocalizationKey.text("chanpon.protection.build_denied.completed")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_SUBMITTED = LocalizationKey.text("chanpon.protection.build_denied.submitted")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_AUTO_ROLLBACK = LocalizationKey.text("chanpon.protection.build_denied.auto_rollback")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_GLOBAL = LocalizationKey.text("chanpon.protection.build_denied.global")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_ARCHIVE = LocalizationKey.text("chanpon.protection.build_denied.archive")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_OWNER_ONLY = LocalizationKey.text("chanpon.protection.build_denied.owner_only")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_MEMBERS_ONLY = LocalizationKey.text("chanpon.protection.build_denied.members_only")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_SPAWN = LocalizationKey.text("chanpon.protection.build_denied.spawn")
    @JvmField val CHANPON_PROTECTION_BUILD_DENIED_EXTERNAL = LocalizationKey.text("chanpon.protection.build_denied.external")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        CHANPON_PROTECTION_BUILD_DENIED_MIGRATION,
        CHANPON_PROTECTION_BUILD_DENIED_COMPLETED,
        CHANPON_PROTECTION_BUILD_DENIED_SUBMITTED,
        CHANPON_PROTECTION_BUILD_DENIED_AUTO_ROLLBACK,
        CHANPON_PROTECTION_BUILD_DENIED_GLOBAL,
        CHANPON_PROTECTION_BUILD_DENIED_ARCHIVE,
        CHANPON_PROTECTION_BUILD_DENIED_OWNER_ONLY,
        CHANPON_PROTECTION_BUILD_DENIED_MEMBERS_ONLY,
        CHANPON_PROTECTION_BUILD_DENIED_SPAWN,
        CHANPON_PROTECTION_BUILD_DENIED_EXTERNAL,
    )
}
