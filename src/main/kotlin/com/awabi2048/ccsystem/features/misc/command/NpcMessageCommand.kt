package com.awabi2048.ccsystem.features.misc.command

import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.config.MessageManager
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * NPCメッセージ表示コマンド
 * 使用法: /npc_message <message_id> [player]
 */
class NpcMessageCommand : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!hasPermission(sender)) {
            sender.sendMessage(LanguageManager.getMessage(null, "no_permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§c使用法: /npc_message <message_id> [player]")
            return true
        }

        val messageId = args[0]
        var targetPlayer: Player? = if (sender is Player) sender else null

        if (args.size >= 2) {
            val targetName = args[1]
            targetPlayer = Bukkit.getPlayer(targetName)
            if (targetPlayer == null) {
                sender.sendMessage("§cプレイヤーが見つかりません: $targetName")
                return true
            }
        }

        if (targetPlayer == null) {
            sender.sendMessage("§cプレイヤーを指定するか、プレイヤーが実行してください。")
            return true
        }

        // LanguageManagerからメッセージテキストを取得（custom_messages）
        val texts = LanguageManager.getCustomMessageTexts(targetPlayer, messageId)
        if (texts.isEmpty()) {
            sender.sendMessage("§cメッセージが未定義か、空の状態です: $messageId")
            return true
        }

        val style = MessageManager.getStyle(targetPlayer, messageId)
        val messagesToSend = mutableListOf<String>()

        when (style) {
            "random" -> messagesToSend.add(texts.random())
            "order" -> {
                val index = MessageManager.getOrderIndex(targetPlayer, messageId, texts.size)
                messagesToSend.add(texts[index])
            }
            "batch" -> messagesToSend.addAll(texts)
            else -> messagesToSend.addAll(texts)
        }

        for (msg in messagesToSend) {
            targetPlayer.sendMessage(
                LegacyComponentSerializer.legacyAmpersand().deserialize(msg)
            )
        }

        if (sender != targetPlayer) {
            sender.sendMessage("§aプレイヤー ${targetPlayer.name} にメッセージを表示しました。")
        }

        return true
    }

    private fun hasPermission(sender: CommandSender): Boolean {
        return sender.hasPermission("ccsystem.npc_message.use") ||
               sender.hasPermission("ccsystem.*") ||
               sender.isOp
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (!hasPermission(sender)) return emptyList()

        if (args.size == 1) {
            // メッセージIDの候補
            val player = if (sender is Player) sender else null
            return MessageManager.getMessageIds(player)
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toList()
        }

        if (args.size == 2) {
            // プレイヤー名の候補
            return null // Bukkitのデフォルトプレイヤーリストを使用
        }

        return emptyList()
    }
}