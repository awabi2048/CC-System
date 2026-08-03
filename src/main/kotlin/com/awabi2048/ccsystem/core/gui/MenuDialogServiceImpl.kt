package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuDialogButton
import com.awabi2048.ccsystem.api.gui.MenuDialogInput
import com.awabi2048.ccsystem.api.gui.MenuDialogRequest
import com.awabi2048.ccsystem.api.gui.MenuDialogResponse
import com.awabi2048.ccsystem.api.gui.MenuDialogService
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

internal class MenuDialogServiceImpl(
    private val plugin: JavaPlugin,
    private val runtime: MenuRuntimeService,
    private val presentations: MenuPresentationTracker,
) : MenuDialogService {
    override fun show(player: Player, request: MenuDialogRequest) {
        runtime.suspendForExternal(player)
        showAfterInventoryClose(player, request)
    }

    private fun showAfterInventoryClose(player: Player, request: MenuDialogRequest) {
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
                is MenuDialogInput.SingleOption -> DialogInput.singleOption(
                    input.id,
                    input.label,
                    input.options.map { option ->
                        SingleOptionDialogInput.OptionEntry.create(
                            option.id,
                            option.label,
                            option.initial,
                        )
                    },
                )
                    .width(input.width)
                    .build()
            }
        }
        val confirm = button(request, request.confirm)
        val cancel = button(request, request.cancel)
        val additionalActions = request.additionalActions.map {
            button(request, it)
        }
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(request.title)
                        .canCloseWithEscape(request.canCloseWithEscape)
                        .body(request.body.map { DialogBody.plainMessage(it) })
                        .inputs(inputs)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build()
                )
                .type(
                    if (additionalActions.isEmpty()) {
                        DialogType.confirmation(confirm, cancel)
                    } else {
                        DialogType.multiAction(
                            listOf(confirm) + additionalActions,
                            cancel,
                            request.columns,
                        )
                    },
                )
        }
        player.showDialog(dialog)
        presentations.markOpened(
            player,
            com.awabi2048.ccsystem.api.gui.MenuSurface.DIALOG,
            request.owner,
            request.id,
        )
    }

    private fun button(
        request: MenuDialogRequest,
        button: MenuDialogButton,
    ): ActionButton {
        val action = DialogAction.customClick(
            { response, audience ->
                val target = audience as? Player ?: return@customClick
                val values = MenuDialogResponse(
                    text = request.inputs.filterIsInstance<MenuDialogInput.Text>()
                        .associate { it.id to response.getText(it.id).orEmpty() },
                    booleans = request.inputs.filterIsInstance<MenuDialogInput.BooleanInput>()
                        .associate { it.id to (response.getBoolean(it.id) ?: false) },
                    selections = request.inputs.filterIsInstance<MenuDialogInput.SingleOption>()
                        .associate { it.id to response.getText(it.id).orEmpty() },
                )
                val originRevision = presentations.current(target)?.revision
                val result = runCatching { button.handler.handle(target, values) }
                    .getOrElse { failure ->
                        plugin.logger.log(
                            Level.SEVERE,
                            "Dialog処理に失敗しました: owner=${request.owner} id=${request.id} player=${target.uniqueId}",
                            failure
                        )
                        MenuActionResult.Rejected()
                    }
                applyResult(target, request, result, originRevision)
            },
            ClickCallback.Options.builder().uses(1).build(),
        )
        return ActionButton.builder(button.label).action(action).build()
    }

    private fun applyResult(
        player: Player,
        request: MenuDialogRequest,
        result: MenuActionResult,
        originRevision: Long?,
    ) {
        when (result) {
            MenuActionResult.Ignored -> return
            is MenuActionResult.Rejected -> {
                result.message?.let(player::sendMessage)
            }
            is MenuActionResult.Success -> {
                val update = result.update
                if (!MenuStaleUpdatePolicy.shouldApply(
                        update,
                        originRevision,
                        presentations.current(player)?.revision,
                    )
                ) {
                    return
                }
                when (update) {
                    MenuUpdate.None -> Unit
                    MenuUpdate.Close -> runtime.close(player)
                    MenuUpdate.Cancel -> runtime.cancelConfirmationFlow(player)
                    MenuUpdate.Refresh -> show(player, request)
                    MenuUpdate.Resume -> runtime.finishExternal(player)
                    MenuUpdate.Back -> runtime.back(player)
                    is MenuUpdate.Replace -> runtime.replace(player, update.route)
                    is MenuUpdate.Navigate -> runtime.navigate(player, update.route)
                }
            }
        }
    }
}

/**
 * Dialog/Formからも、確認画面と同じCancel意味論を共有します。
 * 公開Runtime実装を差し替えるテスト環境では、旧来の終了処理を安全な代替として使います。
 */
internal fun MenuRuntimeService.cancelConfirmationFlow(player: Player) {
    if (this is MenuRuntimeServiceImpl) {
        cancelConfirmation(player)
    } else {
        close(player)
        clear(player)
    }
}
