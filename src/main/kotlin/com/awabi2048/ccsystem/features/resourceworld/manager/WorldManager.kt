package com.awabi2048.ccsystem.features.resourceworld.manager

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.features.resourceworld.manager.MacroManager
import com.awabi2048.ccsystem.features.resourceworld.manager.PregenerationStateManager
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

/**
 * 資源ワールドの管理を行うマネージャー
 */
object WorldManager {
    private val logger: Logger = CCSystem.instance.logger
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    
    // テレポート準備ができたワールド名のセット
    private val readyWorlds = mutableSetOf<String>()

    // 現在の生成進捗 (ワールド名 -> 進捗率%)
    private val pregenProgress = mutableMapOf<String, Int>()

    // 優先エリアの生成進捗 (ワールド名 -> 進捗率%)
    private val priorityPregenProgress = mutableMapOf<String, Int>()

    data class PregenTaskInfo(
        val runnable: BukkitRunnable,
        val startTime: Long,
        val borderSize: Int,
        val totalChunks: Int,
        val priorityChunksCount: Int,
        var currentIndex: Int
    )

    // 事前読み込みタスクの追跡 (ワールド名 -> TaskInfo) - 管理機能用
    private val pregenTaskInfos = mutableMapOf<String, PregenTaskInfo>()

    // 実行中の事前生成タスク (ワールド名 -> BukkitRunnable) - 中断・再開用
    private val pregenTasks = mutableMapOf<String, BukkitRunnable>()

    // 優先エリア完了時間 (ワールド名 -> 完了時刻)
    private val priorityCompleteTime = mutableMapOf<String, Long>()

    // 全エリア完了時刻 (ワールド名 -> 完了時刻)
    private val allCompleteTime = mutableMapOf<String, Long>()

    /**
     * 資源ワールドを生成する
     */
    fun generateResourceWorld(type: String, variation: String, customBorderSize: Int? = null, customDifficulty: Difficulty? = null): Boolean {
        val resourceConfig = ConfigManager.getResourceConfig(type) ?: run {
            logger.warning("リソースタイプ $type の設定が見つかりません。")
            return false
        }

        if (!resourceConfig.variations.contains(variation.lowercase())) {
            logger.warning("バリエーション $variation はリソースタイプ $type に定義されていません。")
            return false
        }

        // 既存のワールド名を取得（マクロ用）
        val existingWorldPrefix = "${resourceConfig.baseName}.${variation}."
        val existingWorld = Bukkit.getWorlds().find { it.name.startsWith(existingWorldPrefix) }
        val existingWorldName = existingWorld?.name ?: findExistingResourceWorldFolderName(existingWorldPrefix)

        // 1. 既存の資源ワールドを削除（削除前マクロを実行）
        if (existingWorldName != null) {
            MacroManager.executeBeforeDelete(existingWorldName)
            object : BukkitRunnable() {
                override fun run() {
                    deleteThenCreateResourceWorld(resourceConfig, type, variation, customBorderSize, customDifficulty)
                }
            }.runTaskLater(CCSystem.instance, ConfigManager.getMacroBeforeDeleteWaitAfterTicks())
        } else {
            createResourceWorld(resourceConfig, type, variation, customBorderSize, customDifficulty)
        }
        
        return true
    }

    private fun deleteThenCreateResourceWorld(
        resourceConfig: ConfigManager.ResourceConfig,
        type: String,
        variation: String,
        customBorderSize: Int?,
        customDifficulty: Difficulty?
    ) {
        deleteResourceWorld(type, variation) { deleted ->
            if (!deleted) {
                val errorMsg = LanguageManager.getRawString(null, "resource.delete_incomplete_abort")
                logger.severe(errorMsg)
                broadcastLegacy(LanguageManager.getRawString(null, "resource.delete_incomplete_abort_broadcast"))
                return@deleteResourceWorld
            }

            createResourceWorld(resourceConfig, type, variation, customBorderSize, customDifficulty)
        }
    }

    private fun findExistingResourceWorldFolderName(prefix: String): String? {
        return Bukkit.getWorldContainer()
            .listFiles()
            ?.filter { it.isDirectory && it.name.startsWith(prefix) }
            ?.maxByOrNull { it.name }
            ?.name
    }

    private fun createResourceWorld(
        resourceConfig: ConfigManager.ResourceConfig,
        type: String,
        variation: String,
        customBorderSize: Int?,
        customDifficulty: Difficulty?
    ): Boolean {
        val dateStr = LocalDateTime.now().format(dateFormatter)
        val worldName = "${resourceConfig.baseName}.$variation.$dateStr"

        val creator = WorldCreator(worldName)
        when (type.lowercase()) {
            "nether" -> creator.environment(World.Environment.NETHER)
            "end" -> creator.environment(World.Environment.THE_END)
            else -> creator.environment(World.Environment.NORMAL)
        }

        logger.info("資源ワールド $worldName を生成しています...")
        val world = creator.createWorld() ?: run {
            val errorMsg = LanguageManager.getRawString(null, "resource.world_create_failed", "world_name" to worldName)
            logger.severe(errorMsg)
            broadcastLegacy(errorMsg)
            return false
        }

        val borderSize = customBorderSize ?: resourceConfig.defaultBorder
        val border = world.worldBorder
        border.setCenter(0.5, 0.5)
        border.size = borderSize.toDouble()

        val spawnLoc: Location = calculateSpawnLocation(world)
        world.setSpawnLocation(spawnLoc)

        val difficulty = customDifficulty ?: ConfigManager.getDefaultDifficulty()
        world.difficulty = difficulty
        logger.info("ワールド $worldName の難易度を ${difficulty.name} に設定しました。")

        val broadcastMsg = LanguageManager.getRawString(null, "broadcast_success")
            .replace("%world_name%", worldName)
            .replace("%border_size%", borderSize.toString())
        val consoleMsg = LanguageManager.getRawString(null, "console_success")
            .replace("%world_name%", worldName)
            .replace("%border_size%", borderSize.toString())

        broadcastLegacy(broadcastMsg)
        logger.info(consoleMsg)

        createScaffold(world, spawnLoc)
        MacroManager.executeAfterGeneration(worldName, borderSize)
        startPregeneration(world, borderSize)
        return true
    }

    /**
     * スポーン地点に足場を生成する
     */
    private fun createScaffold(world: World, location: Location) {
        val material = ConfigManager.getScaffoldMaterial()
        val radius = ConfigManager.getScaffoldRadius()
        val centerX = location.blockX
        val centerY = location.blockY - 1
        val centerZ = location.blockZ

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                world.getBlockAt(centerX + x, centerY, centerZ + z).type = material
            }
        }
        logger.info("ワールド ${world.name} のスポーン地点に半径 $radius の足場を生成しました (${material.name})")
    }

    private fun calculateSpawnLocation(world: World): Location {
        return when (world.environment) {
            World.Environment.NETHER -> {
                val searchRadius = ConfigManager.getNetherSpawnSearchRadius()
                val maxAttempts = ConfigManager.getNetherSpawnSearchAttempts()
                val safeBlocks = ConfigManager.getNetherSpawnSafeBlocks()
                val random = java.util.Random()

                // デフォルト値（見つからない場合のフォールバック）
                var bestLoc = Location(world, 0.5, 64.0, 0.5)
                var found = false

                for (i in 1..maxAttempts) {
                    val rx = random.nextInt(searchRadius * 2 + 1) - searchRadius
                    val rz = random.nextInt(searchRadius * 2 + 1) - searchRadius

                    // ネザーはY層を120から1まで探索
                    var foundY = 64.0
                    for (y in 120 downTo 1) {
                        val block = world.getBlockAt(rx, y, rz)
                        if (safeBlocks.contains(block.type)) {
                            foundY = (y + 1).toDouble()
                            break
                        }
                    }

                    val groundBlock = world.getBlockAt(rx, foundY.toInt() - 1, rz)

                    // 安全なブロックかチェック
                    if (safeBlocks.contains(groundBlock.type)) {
                        val y = foundY.toInt() - 1
                        val blockAbove1 = world.getBlockAt(rx, y + 1, rz)
                        val blockAbove2 = world.getBlockAt(rx, y + 2, rz)

                        // 窒息しないかチェック（頭上に2ブロックの空間が必要）
                        if (!blockAbove1.type.isSolid && !blockAbove2.type.isSolid) {
                            // 溶岩の上ではないかチェック
                            val material1 = blockAbove1.type
                            val material2 = blockAbove2.type
                            if (material1 != Material.LAVA && material2 != Material.LAVA) {
                                bestLoc = Location(world, rx + 0.5, (y + 1).toDouble(), rz + 0.5)
                                found = true
                                logger.info("ネザーの適切なスポーン位置を発見: ($rx, ${y + 1}, $rz) (試行回数: $i)")
                                break
                            }
                        }
                    }
                }

                if (!found) {
                    logger.warning("ネザーで適切なスポーン位置が見つかりませんでした。デフォルト位置を使用します: (0, ${bestLoc.y.toInt()}, 0)")
                }

                bestLoc
            }
            World.Environment.THE_END -> {
                var bestLoc = Location(world, 0.5, (world.getHighestBlockAt(0, 0).y + 1).toDouble(), 0.5)
                val random = java.util.Random()
                for (i in 1..100) {
                    val rx = random.nextInt(65) - 32
                    val rz = random.nextInt(65) - 32
                    val topBlock = world.getHighestBlockAt(rx, rz)
                    if (topBlock.type == Material.END_STONE) {
                        bestLoc = topBlock.location.add(0.5, 1.0, 0.5)
                        break
                    }
                }
                bestLoc
            }
            else -> {
                // 適切な地表を探す
                val searchRadius = ConfigManager.getSpawnSearchRadius()
                val maxAttempts = ConfigManager.getSpawnSearchAttempts()
                val safeBlocks = ConfigManager.getSpawnSafeBlocks()
                val random = java.util.Random()

                // デフォルト値（見つからない場合のフォールバック）
                var bestLoc = Location(world, 0.5, (world.getHighestBlockAt(0, 0).y + 1).toDouble(), 0.5)
                var found = false

                for (i in 1..maxAttempts) {
                    val rx = random.nextInt(searchRadius * 2 + 1) - searchRadius
                    val rz = random.nextInt(searchRadius * 2 + 1) - searchRadius
                    val groundBlock = world.getHighestBlockAt(rx, rz)

                    // 安全なブロックかチェック
                    if (safeBlocks.contains(groundBlock.type)) {
                        val y = groundBlock.y
                        val blockAbove1 = world.getBlockAt(rx, y + 1, rz)
                        val blockAbove2 = world.getBlockAt(rx, y + 2, rz)

                        // 窒息しないかチェック（頭上に2ブロックの空間が必要）
                        if (!blockAbove1.type.isSolid && !blockAbove2.type.isSolid) {
                            // 水や溶岩の上ではないかチェック
                            val material1 = blockAbove1.type
                            val material2 = blockAbove2.type
                            if (material1 != Material.WATER && material1 != Material.LAVA &&
                                material2 != Material.WATER && material2 != Material.LAVA) {
                                bestLoc = Location(world, rx + 0.5, (y + 1).toDouble(), rz + 0.5)
                                found = true
                                logger.info("適切なスポーン位置を発見: ($rx, ${y + 1}, $rz) (試行回数: $i)")
                                break
                            }
                        }
                    }
                }

                if (!found) {
                    logger.warning("適切なスポーン位置が見つかりませんでした。デフォルト位置を使用します: (0, ${bestLoc.y.toInt()}, 0)")
                }

                bestLoc
            }
        }
    }

    /**
     * チャンクの事前生成を開始する
     */
    private fun startPregeneration(world: World, borderSize: Int) {
        startPregeneration(world, borderSize, 0, 0L, false, false)
    }

    /**
     * チャンクの事前生成を開始する（中断からの再開対応）
     */
    private fun startPregeneration(world: World, borderSize: Int, startIndex: Int, elapsedMillis: Long, priorityCompleted: Boolean, allCompleted: Boolean) {
        if (allCompleted) {
            logger.info("ワールド ${world.name} の事前生成は既に完了しています。")
            return
        }

        val priorityDiameter = ConfigManager.getPregenPriorityDiameter()
        val delay = ConfigManager.getPregenDelayTicks()
        val batchSize = ConfigManager.getPregenBatchSize()

        // 生成すべき全チャンクの座標リストを作成
        val chunks = mutableListOf<ChunkCoords>()
        val radiusChunks = (borderSize / 2 / 16) + 1

        for (x in -radiusChunks..radiusChunks) {
            for (z in -radiusChunks..radiusChunks) {
                chunks.add(ChunkCoords(x, z))
            }
        }

        // 優先ゾーン（スポーン周辺）をリストの先頭に持ってくる
        val priorityRadius = (priorityDiameter / 2 / 16) + 1
        val priorityChunks = chunks.filter { Math.abs(it.x) <= priorityRadius && Math.abs(it.z) <= priorityRadius }
        val remainingChunks = chunks.filter { !priorityChunks.contains(it) }

        val sortedChunks = priorityChunks + remainingChunks
        val totalChunks = sortedChunks.size
        val startTime = System.currentTimeMillis() - elapsedMillis
        val initialPercent = (startIndex * 100) / totalChunks.coerceAtLeast(1)
        val initialPriorityPercent = ((startIndex.coerceAtMost(priorityChunks.size)) * 100) / priorityChunks.size.coerceAtLeast(1)
        pregenProgress[world.name] = initialPercent
        priorityPregenProgress[world.name] = if (priorityCompleted) 100 else initialPriorityPercent
        if (priorityCompleted) {
            readyWorlds.add(world.name)
        }

        // 状態の初期化・更新
        val state = PregenerationStateManager.PregenState(
            worldName = world.name,
            borderSize = borderSize,
            currentIndex = startIndex,
            elapsedMillis = elapsedMillis,
            priorityCompleted = priorityCompleted,
            allCompleted = false
        )
        PregenerationStateManager.setState(state)

        val task = object : BukkitRunnable() {
            var index = startIndex
            var lastReportedPercent = -1
            var lastSavedPercent = -1

            override fun run() {
                val endIdx = Math.min(index + batchSize, totalChunks)

                for (i in index until endIdx) {
                    val coords = sortedChunks[i]
                    world.getChunkAtAsync(coords.x, coords.z)
                }

                index = endIdx
                pregenTaskInfos[world.name]?.currentIndex = index

                // 進捗報告
                val percent = (index * 100) / totalChunks
                pregenProgress[world.name] = percent
                val priorityPercent = ((index.coerceAtMost(priorityChunks.size)) * 100) / priorityChunks.size.coerceAtLeast(1)
                priorityPregenProgress[world.name] = priorityPercent

                if (percent / 10 > lastReportedPercent / 10) {
                    logger.info("資源ワールド ${world.name} チャンク生成中... $percent%")
                    lastReportedPercent = percent
                }

                // 定期的に状態を保存（10%ごと）
                if (percent / 10 > lastSavedPercent / 10) {
                    PregenerationStateManager.updateState(world.name) {
                        it.currentIndex = index
                        it.elapsedMillis = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
                    }
                    PregenerationStateManager.save()
                    lastSavedPercent = percent
                }

                // 優先ゾーン完了判定
                if (index >= priorityChunks.size && !readyWorlds.contains(world.name)) {
                    readyWorlds.add(world.name)
                    priorityCompleteTime[world.name] = System.currentTimeMillis()
                    val msg = LanguageManager.getRawString(null, "pregen_priority_complete").replace("%world_name%", world.name)
                    broadcastLegacy(msg)

                    val consoleMsg = LanguageManager.getRawString(null, "pregen_priority_complete").replace("%world_name%", world.name)
                    logger.info(consoleMsg)

                    // 優先エリア生成完了後マクロの実行
                    MacroManager.executeAfterPriorityPregen(world.name)

                    PregenerationStateManager.updateState(world.name) {
                        it.currentIndex = index
                        it.elapsedMillis = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
                        it.priorityCompleted = true
                    }
                    PregenerationStateManager.save()
                }

                // 全完了判定
                if (index >= totalChunks) {
                    val msg = LanguageManager.getRawString(null, "pregen_all_complete").replace("%world_name%", world.name)
                    logger.info(msg)
                    pregenProgress.remove(world.name)
                    priorityPregenProgress.remove(world.name)
                    allCompleteTime[world.name] = System.currentTimeMillis()
                    pregenTaskInfos.remove(world.name)
                    pregenTasks.remove(world.name)
                    PregenerationStateManager.remove(world.name)

                    // 全エリア生成完了後マクロの実行
                    MacroManager.executeAfterAllPregen(world.name)

                    this.cancel()
                }
            }
        }

        // タスクを開始して情報を保存
        task.runTaskTimer(CCSystem.instance, 0L, delay)
        val taskInfo = PregenTaskInfo(task, startTime, borderSize, totalChunks, priorityChunks.size, startIndex)
        pregenTaskInfos[world.name] = taskInfo
        pregenTasks[world.name] = task
    }

    private fun broadcastLegacy(message: String) {
        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(message))
    }

    data class ChunkCoords(val x: Int, val z: Int)

    fun isWorldReady(worldName: String): Boolean = readyWorlds.contains(worldName)

    fun getPregenProgress(worldName: String): Int = pregenProgress[worldName] ?: 0

    fun getPriorityPregenProgress(worldName: String): Int = priorityPregenProgress[worldName] ?: if (readyWorlds.contains(worldName)) 100 else 0

    /**
     * すべての事前生成タスクをキャンセルする
     */
    fun cancelAllPregenTasks() {
        logger.info("すべての事前生成タスクをキャンセルしています...")
        for ((worldName, taskInfo) in pregenTaskInfos) {
            PregenerationStateManager.updateState(worldName) {
                it.currentIndex = taskInfo.currentIndex
                it.elapsedMillis = (System.currentTimeMillis() - taskInfo.startTime).coerceAtLeast(0L)
            }
            taskInfo.runnable.cancel()
            logger.info("ワールド $worldName の事前生成タスクをキャンセルしました。")
        }
        pregenTaskInfos.clear()
        pregenTasks.clear()

        // 現在の状態を保存
        PregenerationStateManager.save()
        logger.info("事前生成の状態を保存しました。")
    }

    /**
     * 中断されていた事前生成を再開する
     */
    fun resumePregeneration() {
        logger.info("中断されていた事前生成をチェックしています...")

        val states = PregenerationStateManager.getAllStates()
        for (state in states.values) {
            if (state.allCompleted) {
                continue
            }

            val world = Bukkit.getWorld(state.worldName)
            if (world == null) {
                logger.warning("ワールド ${state.worldName} が見つかりません。事前生成をスキップします。")
                PregenerationStateManager.remove(state.worldName)
                continue
            }

            if (state.priorityCompleted) {
                readyWorlds.add(state.worldName)
            }

            logger.info("ワールド ${state.worldName} の事前生成をインデックス ${state.currentIndex} から再開します...")
            startPregeneration(world, state.borderSize, state.currentIndex, state.elapsedMillis, state.priorityCompleted, false)
        }
    }

    /**
     * 指定されたリソースタイプとバリエーションに該当する既存ワールドを削除する
     */
    fun deleteResourceWorld(type: String, variation: String, onComplete: (Boolean) -> Unit = {}) {
        val resourceConfig = ConfigManager.getResourceConfig(type) ?: run {
            onComplete(false)
            return
        }
        val prefix = "${resourceConfig.baseName}.$variation."

        val worldsToRemove = Bukkit.getWorlds().filter { it.name.startsWith(prefix) }
        
        for (world in worldsToRemove) {
            val worldName = world.name
            readyWorlds.remove(worldName)
            pregenProgress.remove(worldName)
            priorityPregenProgress.remove(worldName)

            // 事前生成タスクをキャンセル
            pregenTaskInfos[worldName]?.runnable?.cancel()
            pregenTaskInfos.remove(worldName)
            pregenTasks[worldName]?.cancel()
            pregenTasks.remove(worldName)

            // 状態を削除
            PregenerationStateManager.remove(worldName)
            
            // プレイヤーを避難させる
            val evacuationCmd = ConfigManager.getEvacuationCommand()
            for (player in world.players) {
                player.performCommand(evacuationCmd)
                player.sendMessage(LanguageManager.getMessage(player, "resource.evacuated_for_regeneration"))
            }

            Bukkit.unloadWorld(world, false)
            logger.info("ワールド $worldName をアンロードしました。")
        }

        // ワールドアンロード後、ファイルが完全に解放されるまで少し待機
        // 非同期で削除処理を実行
        object : BukkitRunnable() {
            private var attempts = 0
            private val maxAttempts = 5
            
            override fun run() {
                val worldContainer = Bukkit.getWorldContainer()
                val files = worldContainer.listFiles() ?: run {
                    this.cancel()
                    logger.severe("ワールドコンテナの一覧取得に失敗したため、ワールド削除状態を確認できませんでした。")
                    onComplete(false)
                    return
                }
                var hasRemainingFiles = false
                
                for (file in files) {
                    if (file.isDirectory && file.name.startsWith(prefix)) {
                        if (deleteWorldFolder(file)) {
                            logger.info("ワールドフォルダ ${file.name} を削除しました。")
                        } else {
                            hasRemainingFiles = true
                            logger.warning("ワールドフォルダ ${file.name} の削除に失敗しました。リトライします (${attempts + 1}/${maxAttempts})")
                        }
                    }
                }
                
                attempts++
                if (hasRemainingFiles && attempts < maxAttempts) {
                    // リトライ（1秒後）
                    this.runTaskLater(CCSystem.instance, 20L)
                } else {
                    this.cancel()
                    if (hasRemainingFiles) {
                        logger.severe("ワールドフォルダの削除が完了しませんでした。手動での削除が必要かもしれません。")
                        onComplete(false)
                    } else {
                        onComplete(true)
                    }
                }
            }
        }.runTaskLater(CCSystem.instance, 20L) // 1秒後に最初の削除を試行
    }

    private fun deleteWorldFolder(path: java.io.File): Boolean {
        if (!path.exists()) return true
        
        val files = path.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    if (!deleteWorldFolder(file)) {
                        logger.warning("ディレクトリの削除に失敗しました: ${file.absolutePath}")
                    }
                } else {
                    if (!file.delete()) {
                        logger.warning("ファイルの削除に失敗しました: ${file.absolutePath}")
                    }
                }
            }
        }
        
        return if (path.delete()) {
            true
        } else {
            logger.warning("フォルダの削除に失敗しました: ${path.absolutePath}")
            false
        }
    }

    /**
     * サーバー起動時に既存の資源ワールドをロードする
     */
    fun loadExistingWorlds() {
        logger.info("既存の資源ワールドをスキャンしています...")
        val worldContainer = Bukkit.getWorldContainer()
        val files = worldContainer.listFiles() ?: return
        
        val resourceConfigs = ConfigManager.getAllResourceConfigs()
        val worldsToLoad = mutableMapOf<String, java.io.File>() // "baseName.variation" -> latest folder

        for (file in files) {
            if (!file.isDirectory) continue
            val nameParts = file.name.split(".")
            if (nameParts.size != 3) continue

            val baseName = nameParts[0]
            val variation = nameParts[1]
            val dateStr = nameParts[2]

            // 対応する設定があるか確認
            val configEntry = resourceConfigs.entries.find { it.value.baseName == baseName } ?: continue
            if (!configEntry.value.variations.contains(variation)) continue

            val key = "$baseName.$variation"
            val existing = worldsToLoad[key]
            if (existing == null || file.name > existing.name) {
                // 古い方は削除対象（または単に無視）
                if (existing != null) {
                    logger.info("古い資源ワールド ${existing.name} をスキップします。")
                }
                worldsToLoad[key] = file
            }
        }

        for ((key, folder) in worldsToLoad) {
            val worldName = folder.name
            if (Bukkit.getWorld(worldName) == null) {
                logger.info("既存の資源ワールド $worldName をロードしています...")
                val creator = WorldCreator(worldName)
                
                // 環境設定の復元
                val type = resourceConfigs.entries.find { key.startsWith(it.value.baseName) }?.key ?: "normal"
                when (type.lowercase()) {
                    "nether" -> creator.environment(World.Environment.NETHER)
                    "end" -> creator.environment(World.Environment.THE_END)
                    else -> creator.environment(World.Environment.NORMAL)
                }
                
                if (creator.createWorld() != null) {
                    readyWorlds.add(worldName)
                    pregenProgress[worldName] = 100 // 既存ワールドは100%完了とみなす
                    logger.info("資源ワールド $worldName のロードに成功しました。")
                    priorityPregenProgress[worldName] = 100
                } else {
                    logger.severe("資源ワールド $worldName のロードに失敗しました。")
                }
            } else {
                readyWorlds.add(worldName)
                pregenProgress[worldName] = 100 // 既存ワールドは100%完了とみなす
                priorityPregenProgress[worldName] = 100
            }
        }
    }

    /**
     * プレイヤーを資源ワールドに転送する
     */
    fun teleportToResourceWorld(player: Player, type: String, variation: String): Boolean {
        val resourceConfig = ConfigManager.getResourceConfig(type) ?: return false
        val prefix = "${resourceConfig.baseName}.${variation.lowercase()}."

        val world = Bukkit.getWorlds().find { it.name.startsWith(prefix) } ?: run {
            player.sendMessage(LanguageManager.getMessage(player, "resource.world_not_found"))
            return false
        }

        if (!isWorldReady(world.name)) {
            val progress = getPregenProgress(world.name)
            player.sendMessage(LanguageManager.getMessage(player, "resource.world_not_ready", "progress" to progress.toString()))
            return false
        }

        player.teleport(world.spawnLocation)
        player.sendMessage(LanguageManager.getMessage(player, "resource.teleport_success", "type" to type, "variation" to variation))
        return true
    }

    /**
     * 指定された資源ワールドの事前読み込みを中断する
     */
    fun pausePregeneration(type: String, variation: String): Boolean {
        val resourceConfig = ConfigManager.getResourceConfig(type) ?: return false
        val prefix = "${resourceConfig.baseName}.${variation.lowercase()}."

        val world = Bukkit.getWorlds().find { it.name.startsWith(prefix) } ?: run {
            return false
        }

        val task = pregenTasks[world.name]
        if (task != null) {
            pregenTaskInfos[world.name]?.let { taskInfo ->
                PregenerationStateManager.updateState(world.name) {
                    it.currentIndex = taskInfo.currentIndex
                    it.elapsedMillis = (System.currentTimeMillis() - taskInfo.startTime).coerceAtLeast(0L)
                }
                PregenerationStateManager.save()
            }
            task.cancel()
            pregenTasks.remove(world.name)
            logger.info("資源ワールド ${world.name} の事前読み込みを中断しました。")
            return true
        }

        return false
    }

    /**
     * 指定された資源ワールドを停止する
     */
    fun closeResourceWorld(type: String, variation: String): Boolean {
        val resourceConfig = ConfigManager.getResourceConfig(type) ?: return false
        val prefix = "${resourceConfig.baseName}.${variation.lowercase()}."

        val world = Bukkit.getWorlds().find { it.name.startsWith(prefix) } ?: run {
            return false
        }

        // 事前読み込み中なら中断
        val task = pregenTasks[world.name]
        if (task != null) {
            task.cancel()
            pregenTasks.remove(world.name)
            logger.info("資源ワールド ${world.name} の事前読み込みを中断しました。")
        }

        // ワールド内のプレイヤーを避難させる
        val evacuationCmd = ConfigManager.getEvacuationCommand()
        for (player in world.players) {
            player.performCommand(evacuationCmd)
            player.sendMessage(LanguageManager.getMessage(player, "resource.returned_on_close"))
        }

        readyWorlds.remove(world.name)
        pregenProgress.remove(world.name)
        priorityPregenProgress.remove(world.name)
        priorityCompleteTime.remove(world.name)
        allCompleteTime.remove(world.name)

        logger.info("ワールド ${world.name} を閉鎖しました。")
        return true
    }

    fun getPregenTasks(): Map<String, PregenTaskInfo> = pregenTaskInfos.toMap()
    fun getPriorityCompleteTime(worldName: String): Long? = priorityCompleteTime[worldName]
    fun getAllCompleteTime(worldName: String): Long? = allCompleteTime[worldName]
}
