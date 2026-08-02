package com.awabi2048.ccsystem.features.misc.inputmacro

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuDialogButton
import com.awabi2048.ccsystem.api.gui.MenuDialogHandler
import com.awabi2048.ccsystem.api.gui.MenuDialogInput
import com.awabi2048.ccsystem.api.gui.MenuDialogRequest
import com.awabi2048.ccsystem.api.gui.MenuDialogResponse
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import com.awabi2048.ccsystem.core.config.LanguageManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * input_macro サブコマンドの実行モード。
 *
 * 実行主体は最後の引数で指定され、大文字・小文字は区別しない。省略時はコンソール実行が既定。
 * 仕様: `.docs/planning/active-review/2026-08-02-cc-system-input-macro-dialog-spec.md` 第6章
 */
enum class InputMacroMode(val label: String, val permission: String) {
    CONSOLE("console", "cc-system.input-macro.console"),
    PLAYER("player", "cc-system.input-macro.player"),
    ;

    companion object {
        fun from(raw: String?): InputMacroMode? =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) }
    }
}

/**
 * input_macro 機能の Dialog 構築・確定/キャンセル・コマンド実行を担う。
 *
 * コマンドの認識と送信者検証は CCSystemCommand 側で行い、本クラスは input_macro の責務だけを扱う。
 * 呼び出し主体はコンソールを基本とし、追加で管理権限（cc-system.admin / cc-system.* / OP）をもつ
 * プレイヤーからも実行できる。
 */
class InputMacroCommandHandler {
    private val owner = "cc-system"
    private val dialogId = "input-macro"

    /**
     * コマンド実行の入口。args は input_macro サブコマンドを除いた引数列で、
     * 先頭が対象プレイヤー名、末尾が実行モード（省略可）。
     */
    fun execute(sender: CommandSender, args: List<String>) {
        val senderPlayer = sender as? Player
        when (val parsed = InputMacroArgumentParser.parse(args)) {
            is InputMacroArgumentResult.Failure -> {
                sender.sendMessage(LanguageManager.getMessage(senderPlayer, "input_macro.usage"))
            }
            is InputMacroArgumentResult.Success -> {
                val target = Bukkit.getPlayerExact(parsed.playerName)
                if (target == null) {
                    sender.sendMessage(
                        LanguageManager.getMessage(
                            senderPlayer,
                            "player_not_found",
                            "player" to parsed.playerName,
                        )
                    )
                    return
                }
                // 権限確認は Dialog 表示前に行う。
                if (!hasModePermission(sender, parsed.mode)) {
                    sender.sendMessage(LanguageManager.getMessage(senderPlayer, "input_macro.no_permission"))
                    return
                }
                when (val template = InputMacroTemplateParser.parse(parsed.templateParts)) {
                    is InputMacroTemplateParseResult.Failure -> {
                        sender.sendMessage(LanguageManager.getMessage(senderPlayer, "input_macro.invalid_template"))
                        sender.sendMessage(LanguageManager.getMessage(senderPlayer, "input_macro.usage"))
                    }
                    is InputMacroTemplateParseResult.Success ->
                        showDialog(sender, target, parsed.mode, template.command)
                }
            }
        }
    }

    /**
     * モード別権限を確認する。既存の管理権限判定（admin / cc-system.* / OP）との整合を維持する。
     * コンソール送信者は常に許可される。
     */
    private fun hasModePermission(sender: CommandSender, mode: InputMacroMode): Boolean {
        return sender.hasPermission(mode.permission) ||
            sender.hasPermission("cc-system.admin") ||
            sender.hasPermission("cc-system.*") ||
            sender.isOp
    }

    private fun showDialog(
        sender: CommandSender,
        target: Player,
        mode: InputMacroMode,
        command: String,
        warningTitle: Component? = null,
    ) {
        // Dialog 表示用には %player_input% を未解決のまま、実行コマンドの内容を案内する。
        val commandPreview = InputMacroExpander.expandForDialog(command, target.name, target.uniqueId.toString())
        val title = warningTitle ?: Component.empty()
        CCSystem.getAPI().getMenuDialogService().show(
            target,
            MenuDialogRequest(
                owner = owner,
                id = dialogId,
                title = title,
                body = emptyList(),
                inputs = listOf(
                    MenuDialogInput.Text(
                        id = "player_input",
                        label = LanguageManager.getMessageWithoutPrefix(
                            target,
                            "input_macro.dialog.input_label",
                            "command" to commandPreview,
                        ),
                        initial = "",
                        maxLength = 128,
                    )
                ),
                confirm = MenuDialogButton(
                    LanguageManager.getMessageWithoutPrefix(target, "input_macro.dialog.confirm"),
                    MenuDialogHandler { clicked, response -> onConfirm(sender, clicked, mode, command, response) },
                ),
                cancel = MenuDialogButton(
                    LanguageManager.getMessageWithoutPrefix(target, "input_macro.dialog.cancel"),
                    MenuDialogHandler { _, _ -> MenuActionResult.Success(MenuUpdate.Close) },
                ),
                // ESCで閉じるとサーバー側で終了を検知できないため、確定・キャンセル経路を強制する。
                canCloseWithEscape = false,
            )
        )
    }

    private fun onConfirm(
        sender: CommandSender,
        target: Player,
        mode: InputMacroMode,
        command: String,
        response: MenuDialogResponse,
    ): MenuActionResult {
        val input = response.textValue("player_input")
        if (input.isBlank()) {
            showWarning(sender, target, mode, command, "input_macro.empty_input")
            return MenuActionResult.Success(MenuUpdate.None)
        }
        if (InputMacroControl.containsForbidden(input)) {
            showWarning(sender, target, mode, command, "input_macro.forbidden_input")
            return MenuActionResult.Success(MenuUpdate.None)
        }
        // console モードは実行直前に呼び出し主体の権限を再確認する。
        if (mode == InputMacroMode.CONSOLE && !hasModePermission(sender, mode)) {
            target.sendMessage(LanguageManager.getMessage(target, "input_macro.no_permission"))
            return MenuActionResult.Success(MenuUpdate.Close)
        }
        val expanded = InputMacroExpander.expand(command, input, target.name, target.uniqueId.toString())
        dispatch(mode, target, expanded)
        return MenuActionResult.Success(MenuUpdate.Close)
    }

    /**
     * エラー時の警告タイトル付き Dialog を再表示する。
     */
    private fun showWarning(
        sender: CommandSender,
        target: Player,
        mode: InputMacroMode,
        command: String,
        messageKey: String,
    ) {
        val warning = LanguageManager.getMessageWithoutPrefix(target, messageKey)
        showDialog(sender, target, mode, command, warning)
    }

    /**
     * 展開済みコマンドを、指定された権限主体で1回だけ実行する。
     * player モードはプレイヤー権限を、console モードはコンソール送信者を使う。
     */
    private fun dispatch(mode: InputMacroMode, player: Player, command: String): Boolean {
        return when (mode) {
            InputMacroMode.CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            InputMacroMode.PLAYER -> player.performCommand(command)
        }
    }
}
