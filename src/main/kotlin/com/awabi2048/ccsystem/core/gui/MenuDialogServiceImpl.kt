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
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
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

    private fun showAfterInventoryClose(
        player: Player,
        request: MenuDialogRequest,
        validationMessage: Component? = null,
    ) {
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
                        // Paperの公開APIにはDialogをEscで閉じた通知がないため、
                        // Kantanの外部入力を伴うDialogだけはEsc終了を許可しません。
                        // ここで閉じられるとボタンcallbackを経由できず、
                        // suspendForExternalの復帰処理を実行できないためです。
                        // 他プラグインのDialogは従来どおりrequestの指定を尊重します。
                        .canCloseWithEscape(
                            request.canCloseWithEscape && request.owner !in KANTAN_EXTERNAL_OWNERS,
                        )
                        // 警告はrequest.bodyへ書き戻さず、この表示だけへ追加します。
                        // 同じDialogを連続再送しても説明文が累積しないようにします。
                        .body((request.body + listOfNotNull(validationMessage)).map { DialogBody.plainMessage(it) })
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
        try {
            player.showDialog(dialog)
            presentations.markOpened(
                player,
                com.awabi2048.ccsystem.api.gui.MenuSurface.DIALOG,
                request.owner,
                request.id,
            )
        } catch (failure: Throwable) {
            // Dialog生成に失敗した場合、外部画面へ切り替える前のメニュー入力を
            // 保留したままにしない。再開できる実装なら同一ルートを復元します。
            presentations.markClosed(player)
            runtime.resumeFromExternal(player)
            throw failure
        }
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
                applyResult(target, request, values, result, originRevision)
            },
            ClickCallback.Options.builder().uses(1).build(),
        )
        return ActionButton.builder(button.label).action(action).build()
    }

    private fun applyResult(
        player: Player,
        request: MenuDialogRequest,
        response: MenuDialogResponse,
        result: MenuActionResult,
        originRevision: Long?,
    ) {
        when (result) {
            MenuActionResult.Ignored -> {
                // DialogはafterAction=CLOSEのため、Ignoredでもクライアント側の
                // 表示は閉じます。古い入力を無視しただけで外部サスペンドを残すと、
                // 次回のインベントリ／チャット入力が永久に取り込まれるため、同じ
                // Dialogがまだ現行表示の場合だけ安全にRuntimeを再開します。
                resumeIfCurrentDialog(player, request, originRevision)
                return
            }
            is MenuActionResult.Rejected -> {
                // Rejectedは入力値を引き継いで同じDialogを再表示する経路ですが、
                // 応答が遅れて届いた場合に別タブ／別Dialogを旧画面で上書きしては
                // いけません。SuccessだけでなくRejectedにも同じ世代境界を適用します。
                if (!isCurrentDialog(player, request, originRevision)) return
                val message = result.message ?: Component.text("入力値を確認してください。", NamedTextColor.RED)
                // PaperのDialogには入力欄単位のエラーAPIがないため、クライアントが
                // 入力欄を閉じたままになるよりも、入力内容を初期値へ戻した同一画面を
                // 再表示します。全入力種別の値を引き継ぎ、修正→再送信を可能にします。
                showAfterInventoryClose(
                    player,
                    request.copy(
                        inputs = request.inputs.map { input ->
                            when (input) {
                                is MenuDialogInput.Text -> input.copy(initial = response.textValue(input.id))
                                is MenuDialogInput.BooleanInput ->
                                    input.copy(initial = response.booleanValue(input.id))
                                is MenuDialogInput.SingleOption -> {
                                    val selected = response.selectedValue(input.id)
                                    // 選択なしを許すDialogでは空文字を維持し、既存の初期選択を
                                    // 消さないようにします。選択済みならその一件だけを初期値にします。
                                    input.copy(
                                        options = if (selected.isEmpty()) {
                                            input.options
                                        } else {
                                            input.options.map { option -> option.copy(initial = option.id == selected) }
                                        },
                                    )
                                }
                            }
                        },
                    ),
                    validationMessage = message,
                )
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
                    MenuUpdate.None ->
                        // ハンドラが別のDialogを同期的に開いた場合は、その新しい表示を
                        // 壊さないよう、現行IDが同じときだけ元メニューへ戻します。
                        resumeIfCurrentDialog(player, request, originRevision)
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

    /**
     * Dialogが閉じたのに後続表示へ遷移しなかった場合の共通復帰処理です。
     * Presentationのowner/idを照合することで、遅れて届いた古い応答が新しい
     * Dialogや別プラグインの外部入力セッションを解放することを防ぎます。
     */
    private fun resumeIfCurrentDialog(player: Player, request: MenuDialogRequest, originRevision: Long?) {
        if (!isCurrentDialog(player, request, originRevision)) return
        presentations.markClosed(player)
        runtime.resumeFromExternal(player)
    }

    private fun isCurrentDialog(
        player: Player,
        request: MenuDialogRequest,
        originRevision: Long?,
    ): Boolean {
        val current = presentations.current(player) ?: return false
        return originRevision != null &&
            current.surface == com.awabi2048.ccsystem.api.gui.MenuSurface.DIALOG &&
            current.owner == request.owner &&
            current.id == request.id &&
            current.revision == originRevision
    }

    private companion object {
        /** 入力Dialogを閉じた経路が必ずCancel/ボタンcallbackを通る所有者です。 */
        private val KANTAN_EXTERNAL_OWNERS = setOf("kantan", "kantan-commander")
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
