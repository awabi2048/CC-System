package com.awabi2048.ccsystem.features.resourceworld.manager

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import org.bukkit.Bukkit
import java.util.logging.Logger

/**
 * マクロコマンドを管理するマネージャー
 */
object MacroManager {
    private val logger: Logger = CCSystem.instance.logger

    /**
     * ワールド削除前のマクロを実行
     * プレースホルダー: {world_name}, {display_world_name}, {old_world_name}
     */
    fun executeBeforeDelete(worldName: String) {
        if (!ConfigManager.isMacroBeforeDeleteEnabled()) return

        val commands = ConfigManager.getMacroBeforeDeleteCommands()
        executeCommands(
            commands,
            worldName,
            resolveDisplayWorldName(worldName),
            "before_delete",
            oldWorldName = worldName
        )
    }

    /**
     * ワールド生成完了後のマクロを実行
     * プレースホルダー: {world_name}, {display_world_name}, %border_size%
     */
    fun executeAfterGeneration(worldName: String, borderSize: Int) {
        if (!ConfigManager.isMacroAfterGenerationEnabled()) return

        val commands = ConfigManager.getMacroAfterGenerationCommands()
        executeCommands(commands, worldName, resolveDisplayWorldName(worldName), "after_generation", borderSize)
    }

    /**
     * 優先エリア生成完了後のマクロを実行
     * プレースホルダー: {world_name}, {display_world_name}
     */
    fun executeAfterPriorityPregen(worldName: String) {
        if (!ConfigManager.isMacroAfterPriorityPregenEnabled()) return

        val commands = ConfigManager.getMacroAfterPriorityPregenCommands()
        executeCommands(commands, worldName, resolveDisplayWorldName(worldName), "after_priority_pregen")
    }

    /**
     * 全エリア生成完了後のマクロを実行
     * プレースホルダー: {world_name}, {display_world_name}
     */
    fun executeAfterAllPregen(worldName: String) {
        if (!ConfigManager.isMacroAfterAllPregenEnabled()) return

        val commands = ConfigManager.getMacroAfterAllPregenCommands()
        executeCommands(commands, worldName, resolveDisplayWorldName(worldName), "after_all_pregen")
    }

    /**
     * コマンドを実行する
     */
    private fun executeCommands(
        commands: List<String>,
        worldName: String,
        displayWorldName: String,
        macroType: String,
        borderSize: Int? = null,
        oldWorldName: String? = null
    ) {
        if (commands.isEmpty()) return

        logger.info("マクロ [$macroType] を実行します (${commands.size}個のコマンド)")

        for (command in commands) {
            // プレースホルダーを置換
            var processedCommand = command
                .replace("{world_name}", worldName)
                .replace("{display_world_name}", displayWorldName)
            if (oldWorldName != null) {
                processedCommand = processedCommand.replace("{old_world_name}", oldWorldName)
            }
            if (borderSize != null) {
                processedCommand = processedCommand.replace("%border_size%", borderSize.toString())
            }

            // 先頭の空白とスラッシュを除去
            processedCommand = processedCommand.trim().removePrefix("/")

            try {
                // コンソールからコマンドを実行
                val success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand)
                if (success) {
                    logger.info("マクロコマンドを実行しました: $processedCommand")
                } else {
                    logger.warning("マクロコマンドの実行に失敗しました: $processedCommand")
                }
            } catch (e: Exception) {
                logger.severe("マクロコマンド実行中にエラーが発生しました: $processedCommand")
                e.printStackTrace()
            }
        }
    }

    private fun resolveDisplayWorldName(worldName: String): String {
        val parts = worldName.split(".")
        if (parts.size < 2) {
            return worldName
        }

        val baseName = parts[0]
        val variation = parts[1]
        val type = ConfigManager.getAllResourceConfigs()
            .entries
            .find { it.value.baseName == baseName }
            ?.key
            ?.lowercase()
            ?: return worldName

        val key = "resource.$type.name"
        val baseDisplayName = LanguageManager.getRawString(null, key)
        if (baseDisplayName == key) {
            return worldName
        }

        return "$baseDisplayName$variation"
    }
}
