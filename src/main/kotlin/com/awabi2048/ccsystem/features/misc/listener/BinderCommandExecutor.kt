package com.awabi2048.ccsystem.features.misc.listener

import org.bukkit.entity.Player

object BinderCommandExecutor {

    /**
     * バインドされたコマンドを実行する。
     *
     * プレースホルダー:
     * - %player_name% / %player_uuid% : 常に[player](攻撃者/操作者)
     * - %target_name% / %target_uuid% : [target](被攻撃者) が解決されている場合のみ置換。
     *   Shift+F バインダーなど対象プレイヤーが存在しない呼び出しでは [target] に null を渡すこと。
     */
    fun execute(player: Player, commands: List<String>, target: Player? = null): Boolean {
        var executed = false
        for (command in commands) {
            if (command.isBlank()) continue

            player.performCommand(renderCommand(command, player, target))
            executed = true
        }
        return executed
    }

    internal fun renderCommand(command: String, player: Player, target: Player?): String {
        var rendered = command
            .replace("%player_name%", player.name)
            .replace("%player_uuid%", player.uniqueId.toString())
        if (target != null) {
            rendered = rendered
                .replace("%target_name%", target.name)
                .replace("%target_uuid%", target.uniqueId.toString())
        }
        return rendered
    }
}
