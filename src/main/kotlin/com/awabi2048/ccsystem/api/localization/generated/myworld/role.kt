package com.awabi2048.ccsystem.api.localization.generated

import com.awabi2048.ccsystem.api.localization.LocalizationKey

/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */
object MyworldRoleKeys {
    @JvmField val ROLE_OWNER: LocalizationKey<String> = LocalizationKey.text("role.owner")
    @JvmField val ROLE_MODERATOR: LocalizationKey<String> = LocalizationKey.text("role.moderator")
    @JvmField val ROLE_MEMBER: LocalizationKey<String> = LocalizationKey.text("role.member")

    internal fun all(): List<LocalizationKey<*>> = listOf(
        ROLE_OWNER,
        ROLE_MODERATOR,
        ROLE_MEMBER,
    )
}
