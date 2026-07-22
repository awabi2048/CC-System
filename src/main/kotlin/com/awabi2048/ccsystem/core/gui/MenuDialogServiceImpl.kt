package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuDialogButton
import com.awabi2048.ccsystem.api.gui.MenuDialogInput
import com.awabi2048.ccsystem.api.gui.MenuDialogRequest
import com.awabi2048.ccsystem.api.gui.MenuDialogResponse
import com.awabi2048.ccsystem.api.gui.MenuDialogService
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player

internal class MenuDialogServiceImpl(
    private val sounds: MenuSoundService,
    private val runtime: MenuRuntimeService,
) : MenuDialogService {
    override fun show(player: Player, request: MenuDialogRequest) {
        val inputs = request.inputs.map { input ->
            when (input) {
                is MenuDialogInput.Text -> DialogInput.text(input.id, input.label)
                    .initial(input.initial)
                    .width(input.width)
                    .maxLength(input.maxLength)
                    .build()
                is MenuDialogInput.BooleanInput -> DialogInput.bool(input.id, input.label)
                    .initial(input.initial)
                    .build()
            }
        }
        val confirm = button(player, request, request.confirm, MenuClickType.CONFIRM)
        val cancel = button(player, request, request.cancel, MenuClickType.CANCEL)
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(request.title)
                        .body(request.body.map { DialogBody.plainMessage(it) })
                        .inputs(inputs)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build()
                )
                .type(DialogType.confirmation(confirm, cancel))
        }
        player.showDialog(dialog)
    }

    private fun button(
        player: Player,
        request: MenuDialogRequest,
        button: MenuDialogButton,
        clickType: MenuClickType,
    ): ActionButton {
        val action = DialogAction.customClick(
            { response, audience ->
                val target = audience as? Player ?: return@customClick
                val values = MenuDialogResponse(
                    text = request.inputs.filterIsInstance<MenuDialogInput.Text>()
                        .associate { it.id to response.getText(it.id).orEmpty() },
                    booleans = request.inputs.filterIsInstance<MenuDialogInput.BooleanInput>()
                        .associate { it.id to (response.getBoolean(it.id) ?: false) },
                )
                val result = runCatching { button.handler.handle(target, values) }
                    .getOrElse { MenuActionResult.Rejected() }
                applyResult(target, request, button, clickType, result)
            },
            ClickCallback.Options.builder().uses(1).build(),
        )
        return ActionButton.builder(button.label).action(action).build()
    }

    private fun applyResult(
        player: Player,
        request: MenuDialogRequest,
        button: MenuDialogButton,
        clickType: MenuClickType,
        result: MenuActionResult,
    ) {
        when (result) {
            MenuActionResult.Ignored -> return
            is MenuActionResult.Rejected -> {
                play(player, result.sound, request.sounds.rejected, clickType)
                result.message?.let(player::sendMessage)
            }
            is MenuActionResult.Success -> {
                play(player, result.sound, button.sound.takeUnless { it == MenuSoundPolicy.Default }
                    ?: request.sounds.success, clickType)
                when (val update = result.update) {
                    MenuUpdate.None, MenuUpdate.Close -> Unit
                    MenuUpdate.Refresh -> show(player, request)
                    MenuUpdate.Back -> runtime.back(player)
                    is MenuUpdate.Replace -> runtime.open(player, update.route)
                    is MenuUpdate.Navigate -> runtime.open(player, update.route)
                }
            }
        }
    }

    private fun play(player: Player, policy: MenuSoundPolicy, fallback: MenuSoundPolicy, type: MenuClickType) {
        MenuSoundPolicyResolver.resolve(policy, fallback, type)?.let { sounds.play(player, it) }
    }
}
