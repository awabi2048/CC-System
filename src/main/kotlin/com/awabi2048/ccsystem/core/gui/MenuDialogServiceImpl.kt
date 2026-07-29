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
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level

internal class MenuDialogServiceImpl(
    private val plugin: JavaPlugin,
    private val runtime: MenuRuntimeService,
    private val presentations: MenuPresentationTracker,
) : MenuDialogService {
    override fun show(player: Player, request: MenuDialogRequest) {
        val flowId = dialogFlowSequence.incrementAndGet()
        val suspended = runtime.suspendForExternal(player)
        plugin.logger.info(
            "[MenuDialogDebug] flow=$flowId stage=requested player=${player.name}/${player.uniqueId} " +
                "owner=${request.owner} id=${request.id} suspended=$suspended " +
                "openHolder=${player.openInventory.topInventory.holder?.javaClass?.name ?: "none"}",
        )
        showAfterInventoryClose(player, request, flowId)
    }

    private fun showAfterInventoryClose(player: Player, request: MenuDialogRequest, flowId: Long) {
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
        val confirm = button(request, request.confirm, flowId, "confirm")
        val cancel = button(request, request.cancel, flowId, "cancel")
        val additionalActions = request.additionalActions.map {
            button(request, it, flowId, "additional")
        }
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(request.title)
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
        plugin.logger.info(
            "[MenuDialogDebug] flow=$flowId stage=shown player=${player.name}/${player.uniqueId} " +
                "owner=${request.owner} id=${request.id}",
        )
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
        flowId: Long,
        actionName: String,
    ): ActionButton {
        val action = DialogAction.customClick(
            { response, audience ->
                val target = audience as? Player ?: return@customClick
                plugin.logger.info(
                    "[MenuDialogDebug] flow=$flowId stage=response player=${target.name}/${target.uniqueId} " +
                        "owner=${request.owner} id=${request.id} action=$actionName",
                )
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

    private companion object {
        val dialogFlowSequence = AtomicLong()
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
                if (
                    update != MenuUpdate.None &&
                    update != MenuUpdate.Close &&
                    presentations.current(player)?.revision != originRevision
                ) {
                    plugin.logger.warning(
                        "Dialog変更後の古いMenuUpdateを無視しました: " +
                            "owner=${request.owner} id=${request.id} update=${update::class.simpleName}"
                    )
                    return
                }
                when (update) {
                    MenuUpdate.None -> runtime.completeExternal(player)
                    MenuUpdate.Close -> runtime.close(player)
                    MenuUpdate.Refresh -> show(player, request)
                    MenuUpdate.Resume -> runtime.resumeFromExternal(player)
                    MenuUpdate.Back -> runtime.back(player)
                    is MenuUpdate.Replace -> runtime.replace(player, update.route)
                    is MenuUpdate.Navigate -> runtime.navigate(player, update.route)
                }
            }
        }
    }
}
