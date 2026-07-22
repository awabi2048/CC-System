package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuSound
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy

internal object MenuSoundPolicyResolver {
    private val defaults = mapOf(
        MenuClickType.DEFAULT to MenuSound("UI_BUTTON_CLICK", pitch = 2.0f),
        MenuClickType.CONFIRM to MenuSound("UI_BUTTON_CLICK", pitch = 1.6f),
        MenuClickType.CANCEL to MenuSound("UI_BUTTON_CLICK", pitch = 1.2f),
        MenuClickType.NAVIGATION to MenuSound("UI_BUTTON_CLICK", pitch = 2.0f),
        MenuClickType.INFO to MenuSound("UI_BUTTON_CLICK", pitch = 1.5f),
    )

    fun resolve(
        policy: MenuSoundPolicy,
        fallback: MenuSoundPolicy,
        clickType: MenuClickType,
    ): MenuSound? = when (policy) {
            MenuSoundPolicy.Default -> when (fallback) {
                MenuSoundPolicy.Default -> defaults.getValue(clickType)
                MenuSoundPolicy.Silent -> null
                is MenuSoundPolicy.Custom -> fallback.sound
            }
            MenuSoundPolicy.Silent -> null
            is MenuSoundPolicy.Custom -> policy.sound
        }

    fun successPolicy(element: MenuActionSoundPolicy?, menu: MenuActionSoundPolicy): MenuSoundPolicy =
        element?.success ?: menu.success

    fun rejectedPolicy(element: MenuActionSoundPolicy?, menu: MenuActionSoundPolicy): MenuSoundPolicy =
        element?.rejected ?: menu.rejected
}
