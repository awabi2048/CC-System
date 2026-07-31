package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuUpdate

internal object MenuStaleUpdatePolicy {
    fun shouldApply(update: MenuUpdate, originRevision: Long?, currentRevision: Long?): Boolean =
        update == MenuUpdate.None || originRevision == currentRevision
}
