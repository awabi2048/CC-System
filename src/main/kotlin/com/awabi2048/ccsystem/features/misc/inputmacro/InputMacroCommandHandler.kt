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
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * input_macro サブコマンドの実行モード。
 *
 * 実行主体は最後の引数で指定され、大文字・小文字は区別しない。
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
 */
class InputMacroCommandHandler {
    private val owner = "cc-system"
    private val dialogId = "input-macro"

    /**
     * コマンド実行の入口。args は input_macro サブコマンドを除いた引数列で、末尾が実行モード。
     */
    fun execute(player: Player, args: List<String>) {
        val mode = InputMacroMode.from(args.lastOrNull())
        if (mode == null) {
            player.sendMessage(LanguageManager.getMessage(player, "input_macro.invalid_mode"))
            player.sendMessage(LanguageManager.getMessage(player, "input_macro.usage"))
            return
        }
        // 権限確認は Dialog 表示前に行う。
        if (!hasModePermission(player, mode)) {
            player.sendMessage(LanguageManager.getMessage(player, "input_macro.no_permission"))
            return
        }
        when (val parsed = InputMacroTemplateParser.parse(args.dropLast(1))) {
            is InputMacroTemplateParseResult.Failure -> {
                player.sendMessage(LanguageManager.getMessage(player, "input_macro.invalid_template"))
                player.sendMessage(LanguageManager.getMessage(player, "input_macro.usage"))
            }
            is InputMacroTemplateParseResult.Success -> showDialog(player, mode, parsed.command)
        }
    }

    /**
     * モード別権限を確認する。既存の管理権限判定（admin / cc-system.* / OP）との整合を維持する。
     */
    private fun hasModePermission(player: Player, mode: InputMacroMode): Boolean {
        return player.hasPermission(mode.permission) ||
            player.hasPermission("cc-system.admin") ||
            player.hasPermission("cc-system.*") ||
            player.isOp
    }

    private fun showDialog(player: Player, mode: InputMacroMode, command: String) {
        CCSystem.getAPI().getMenuDialogService().show(
            player,
            MenuDialogRequest(
                owner = owner,
                id = dialogId,
                title = LanguageManager.getMessageWithoutPrefix(player, "input_macro.dialog.title"),
                body = listOf(
                    LanguageManager.getMessageWithoutPrefix(
                        player,
                        "input_macro.dialog.body",
                        "mode" to mode.label,
                    )
                ),
                inputs = listOf(
                    MenuDialogInput.Text(
                        id = "player_input",
                        label = LanguageManager.getMessageWithoutPrefix(player, "input_macro.dialog.input_label"),
                        initial = "",
                        maxLength = 128,
                    )
                ),
                confirm = MenuDialogButton(
                    LanguageManager.getMessageWithoutPrefix(player, "input_macro.dialog.confirm"),
                    MenuDialogHandler { target, response -> onConfirm(target, mode, command, response) },
                ),
                cancel = MenuDialogButton(
                    LanguageManager.getMessageWithoutPrefix(player, "input_macro.dialog.cancel"),
                    MenuDialogHandler { _, _ -> MenuActionResult.Success(MenuUpdate.Close) },
                ),
                // ESCで閉じるとサーバー側で終了を検知できないため、確定・キャンセル経路を強制する。
                canCloseWithEscape = false,
            )
        )
    }

    private fun onConfirm(
        target: Player,
        mode: InputMacroMode,
        command: String,
        response: MenuDialogResponse,
    ): MenuActionResult {
        val input = response.textValue("player_input")
        if (input.isBlank()) {
            target.sendMessage(LanguageManager.getMessage(target, "input_macro.empty_input"))
            return MenuActionResult.Success(MenuUpdate.Refresh)
        }
        if (InputMacroControl.containsForbidden(input)) {
            target.sendMessage(LanguageManager.getMessage(target, "input_macro.forbidden_input"))
            return MenuActionResult.Success(MenuUpdate.Refresh)
        }
        // console モードは実行直前に権限を再確認する。
        if (mode == InputMacroMode.CONSOLE && !hasModePermission(target, mode)) {
            target.sendMessage(LanguageManager.getMessage(target, "input_macro.no_permission"))
            return MenuActionResult.Success(MenuUpdate.Close)
        }
        val expanded = InputMacroExpander.expand(command, input, target.name, target.uniqueId.toString())
        val executed = dispatch(mode, target, expanded)
        target.sendMessage(
            LanguageManager.getMessage(target, if (executed) "input_macro.execute_success" else "input_macro.execute_failure")
        )
        return MenuActionResult.Success(MenuUpdate.Close)
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
