package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiMenuCapabilityInvocationSpec
import com.awabi2048.ccsystem.api.gui.MenuInteraction

/** 型付きCapability宣言をRuntimeの正規Interactionへ変換します。 */
internal object GuiMenuCapabilityInteractionFactory {
    fun create(spec: GuiMenuCapabilityInvocationSpec): MenuInteraction.Capability =
        MenuInteraction.Capability(
            capabilityId = spec.capability.capabilityId,
            arguments = spec.arguments,
            attributes = spec.attributes,
            acceptedClicks = spec.acceptedClicks,
            safety = spec.safety,
            safetyByClick = spec.safetyByClick,
            reversibleContractByClick = spec.reversibleContractByClick,
        )
}
