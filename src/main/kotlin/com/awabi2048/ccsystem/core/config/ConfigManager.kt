package com.awabi2048.ccsystem.core.config

import com.awabi2048.ccsystem.CCSystem
import org.bukkit.Difficulty
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * CC-System統合設定マネージャー
 *
 * 設定を機能ごとに分割して読み込みます。
 */
object ConfigManager {
    private const val CORE_CONFIG_PATH = "config/core.yml"
    private const val MISC_CONFIG_PATH = "config/misc.yml"
    private const val RESOURCE_WORLD_CONFIG_PATH = "config/resource_world.yml"
    private const val RENTAL_AREA_CONFIG_PATH = "config/rental_area.yml"
    private const val PUBLIC_SIGN_CONFIG_PATH = "config/public_sign.yml"
    private const val ANNOUNCE_CONFIG_PATH = "config/announce.yml"
    private const val QUEUE_CONFIG_PATH = "config/queue.yml"

    private lateinit var coreConfigFile: File
    private lateinit var coreConfig: YamlConfiguration

    // === 機能トグル ===
    private var featureResourceWorldEnabled: Boolean = true
    private var featureRentalAreaEnabled: Boolean = true
    private var featurePublicSignEnabled: Boolean = true
    private var featureMusicEnabled: Boolean = true
    private var featureDynamicDistanceEnabled: Boolean = false
    private var featureShiftFBinderEnabled: Boolean = true
    private var featureGlobalSoundEventsEnabled: Boolean = true
    private var featureDelayCommandEnabled: Boolean = true
    private var featureNpcMessageEnabled: Boolean = true

    private var announceMenuCommand: String = "menu"
    private var announceUncheckedNotifyIntervalMinutes: Long = 10L

    private var chunkTaskQueuePriorityOrder: List<String> = listOf("SUKIMA_DUNGEON", "ARENA", "RESOURCE")
    private var chunkTaskQueueReadIntervalTicks: Long = 20L
    private var chunkTaskQueueDataFilePath: String = "data/queue/queue_data.yml"

    data class DistanceDelta(
        val view: Int,
        val simulation: Int,
        val send: Int
    )

    data class SpeedFactorRule(
        val maxBps: Double,
        val delta: DistanceDelta
    )

    data class OnlineFactorRule(
        val maxPlayers: Int,
        val delta: DistanceDelta
    )

    data class DynamicDistanceSettings(
        val enabled: Boolean,
        val intervalTicks: Long,
        val applyCooldownTicks: Long,
        val worldBlacklist: List<String>,
        val viewLimit: IntRange,
        val simulationLimit: IntRange,
        val sendLimit: IntRange,
        val baseView: Int,
        val baseSimulation: Int,
        val baseSend: Int,
        val speedRules: List<SpeedFactorRule>,
        val onlineRules: List<OnlineFactorRule>
    )

    // === コア設定 ===
    private var defaultLanguage: String = "ja_jp"
    private var debug: Boolean = false

    // === Misc設定 ===
    private var shiftFBinderCommands: List<String> = listOf("say %player_name%")
    private val worldMusicSettings = mutableMapOf<String, MusicSetting>()

    // 動的描画距離設定
    private var dynamicDistanceIntervalTicks: Long = 20
    private var dynamicDistanceApplyCooldownTicks: Long = 60
    private var dynamicDistanceWorldBlacklist: List<String> = emptyList()
    private var dynamicDistanceViewLimit: IntRange = 2..20
    private var dynamicDistanceSimulationLimit: IntRange = 2..12
    private var dynamicDistanceSendLimit: IntRange = 2..20
    private var dynamicDistanceBaseView: Int = 10
    private var dynamicDistanceBaseSimulation: Int = 6
    private var dynamicDistanceBaseSend: Int = 10
    private var dynamicDistanceSpeedRules: List<SpeedFactorRule> = listOf(
        SpeedFactorRule(4.5, DistanceDelta(1, 1, 1)),
        SpeedFactorRule(8.0, DistanceDelta(2, 1, 2)),
        SpeedFactorRule(999.0, DistanceDelta(3, 0, 3))
    )
    private var dynamicDistanceOnlineRules: List<OnlineFactorRule> = listOf(
        OnlineFactorRule(15, DistanceDelta(0, 0, 0)),
        OnlineFactorRule(35, DistanceDelta(-2, -1, -2)),
        OnlineFactorRule(999, DistanceDelta(-4, -2, -4))
    )

    // === PublicSign設定 ===
    private var publicSignDefaultExpireDays: Int = 7
    private var publicSignContentLines: Int = 3

    // === ResourceWorld設定 ===
    private var evacuationCommand: String = "spawn"
    private var pregenPriorityDiameter: Int = 1000
    private var pregenDelayTicks: Long = 5
    private var pregenBatchSize: Int = 25
    private var defaultDifficulty: Difficulty = Difficulty.NORMAL
    private var scaffoldMaterial: Material = Material.GLASS
    private var scaffoldRadius: Int = 3
    private var actionBarMessage: String = "§bShift長押しでスポーンに戻ります"
    private var particleType: Particle = Particle.CLOUD
    private var particleCount: Int = 5
    private var particleSpeed: Double = 0.01
    private var particleInterval: Long = 2
    private var soundStart: String = "BLOCK_NOTE_BLOCK_BELL"
    private var soundSuccess: String = "ENTITY_EXPERIENCE_ORB_PICKUP"
    private var spawnSearchRadius: Int = 64
    private var spawnSearchAttempts: Int = 200
    private var spawnSafeBlocks: List<Material> = listOf(
        Material.GRASS_BLOCK,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.PODZOL,
        Material.STONE,
        Material.COBBLESTONE,
        Material.SAND,
        Material.SANDSTONE,
        Material.GRAVEL,
        Material.MOSS_BLOCK
    )
    private var netherSpawnSearchRadius: Int = 64
    private var netherSpawnSearchAttempts: Int = 200
    private var netherSpawnSafeBlocks: List<Material> = listOf(
        Material.NETHERRACK,
        Material.SOUL_SAND,
        Material.SOUL_SOIL,
        Material.BASALT,
        Material.BLACKSTONE,
        Material.NETHER_BRICKS,
        Material.CRIMSON_NYLIUM,
        Material.WARPED_NYLIUM
    )

    // マクロ設定
    private val macroBeforeDelete = mutableListOf<String>()
    private val macroAfterGeneration = mutableListOf<String>()
    private val macroAfterPriorityPregen = mutableListOf<String>()
    private val macroAfterAllPregen = mutableListOf<String>()
    private var macroBeforeDeleteEnabled: Boolean = false
    private var macroBeforeDeleteWaitAfterTicks: Long = 20L
    private var macroAfterGenerationEnabled: Boolean = false
    private var macroAfterPriorityPregenEnabled: Boolean = false
    private var macroAfterAllPregenEnabled: Boolean = false

    // リソースタイプ設定
    private val resourceConfigs = mutableMapOf<String, ResourceConfig>()

    data class MusicSetting(
        val sound: String,
        val volume: Float,
        val pitch: Float,
        val duration: Int
    )

    data class ResourceConfig(
        val baseName: String,
        val defaultBorder: Int,
        val variations: List<String>
    )

    fun load() {
        val core = loadYaml(CORE_CONFIG_PATH)
        val misc = loadYaml(MISC_CONFIG_PATH)
        val resourceWorld = loadYaml(RESOURCE_WORLD_CONFIG_PATH)
        loadYaml(RENTAL_AREA_CONFIG_PATH)
        val publicSign = loadYaml(PUBLIC_SIGN_CONFIG_PATH)
        val announce = loadYaml(ANNOUNCE_CONFIG_PATH)
        val queue = loadYaml(QUEUE_CONFIG_PATH)

        loadCoreSettings(core)
        loadMiscSettings(misc)
        loadPublicSignSettings(publicSign)
        loadResourceWorldSettings(resourceWorld)
        loadAnnounceSettings(announce)
        loadQueueSettings(queue)
    }

    fun reload() {
        load()
    }

    fun setFeatureEnabled(featureKey: String, enabled: Boolean): Boolean {
        if (!::coreConfig.isInitialized || !::coreConfigFile.isInitialized) {
            return false
        }
        coreConfig.set("features.$featureKey", enabled)
        return runCatching {
            coreConfig.save(coreConfigFile)
            true
        }.getOrElse {
            CCSystem.instance.logger.warning("features.$featureKey の保存に失敗しました: ${it.message}")
            false
        }
    }

    fun setDebugEnabled(enabled: Boolean): Boolean {
        if (!::coreConfig.isInitialized || !::coreConfigFile.isInitialized) {
            return false
        }
        coreConfig.set("core.debug", enabled)
        return runCatching {
            coreConfig.save(coreConfigFile)
            true
        }.getOrElse {
            CCSystem.instance.logger.warning("core.debug の保存に失敗しました: ${it.message}")
            false
        }
    }

    private fun loadYaml(relativePath: String): YamlConfiguration {
        val file = File(CCSystem.instance.dataFolder, relativePath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        val yaml = YamlConfiguration.loadConfiguration(file)
        if (relativePath == CORE_CONFIG_PATH) {
            coreConfigFile = file
            coreConfig = yaml
        }
        return yaml
    }

    private fun loadCoreSettings(core: YamlConfiguration) {
        val featuresSection = core.getConfigurationSection("features")
        featureResourceWorldEnabled = featuresSection?.getBoolean("resource_world", true) ?: true
        featureRentalAreaEnabled = featuresSection?.getBoolean("rental_area", true) ?: true
        featurePublicSignEnabled = featuresSection?.getBoolean("public_sign", true) ?: true
        featureMusicEnabled = featuresSection?.getBoolean("music", true) ?: true
        featureDynamicDistanceEnabled = featuresSection?.getBoolean("dynamic_distance", false) ?: false
        featureShiftFBinderEnabled = featuresSection?.getBoolean("shift_f_binder", true) ?: true
        featureGlobalSoundEventsEnabled = featuresSection?.getBoolean("disable_global_sound_events", true) ?: true
        featureDelayCommandEnabled = featuresSection?.getBoolean("delay_command", true) ?: true
        featureNpcMessageEnabled = featuresSection?.getBoolean("npc_message", true) ?: true

        val coreSection = core.getConfigurationSection("core")
        val configuredDefaultLanguage = coreSection?.getString("default_language")
        defaultLanguage = normalizeLanguageCode(configuredDefaultLanguage ?: "ja_jp")
        debug = coreSection?.getBoolean("debug") ?: false
    }

    private fun normalizeLanguageCode(raw: String): String {
        return when (raw.trim().lowercase()) {
            "ja" -> "ja_jp"
            "en" -> "en_us"
            else -> raw.trim().lowercase()
        }
    }

    private fun loadMiscSettings(misc: YamlConfiguration) {
        val shiftFBinderSection = misc.getConfigurationSection("shift_f_binder")
        shiftFBinderCommands = shiftFBinderSection?.getStringList("commands") ?: listOf("say %player_name%")

        val musicSection = misc.getConfigurationSection("music")
        val worldsSection = musicSection?.getConfigurationSection("worlds")
        worldMusicSettings.clear()
        if (worldsSection != null) {
            for (worldName in worldsSection.getKeys(false)) {
                val worldSection = worldsSection.getConfigurationSection(worldName)
                if (worldSection != null) {
                    val sound = worldSection.getString("sound") ?: "minecraft:music.game"
                    val volume = worldSection.getDouble("volume").toFloat()
                    val pitch = worldSection.getDouble("pitch").toFloat()
                    val duration = worldSection.getInt("duration", 200)
                    worldMusicSettings[worldName] = MusicSetting(sound, volume, pitch, duration)
                }
            }
        }

        val dynamicDistanceSection = misc.getConfigurationSection("dynamic_distance")
        dynamicDistanceIntervalTicks = (dynamicDistanceSection?.getLong("interval_ticks") ?: 20L).coerceAtLeast(1L)
        dynamicDistanceApplyCooldownTicks =
            (dynamicDistanceSection?.getLong("apply_cooldown_ticks") ?: 60L).coerceAtLeast(0L)
        dynamicDistanceWorldBlacklist = dynamicDistanceSection?.getStringList("world_blacklist") ?: emptyList()

        val limitSection = dynamicDistanceSection?.getConfigurationSection("limits")
        dynamicDistanceViewLimit = parseRangeString(limitSection?.getString("view"), 2..20)
        dynamicDistanceSimulationLimit = parseRangeString(limitSection?.getString("simulation"), 2..12)
        dynamicDistanceSendLimit = parseRangeString(limitSection?.getString("send"), 2..20)

        val baseSection = dynamicDistanceSection?.getConfigurationSection("base")
        dynamicDistanceBaseView = baseSection?.getInt("view") ?: 10
        dynamicDistanceBaseSimulation = baseSection?.getInt("simulation") ?: 6
        dynamicDistanceBaseSend = baseSection?.getInt("send") ?: 10

        dynamicDistanceSpeedRules = parseSpeedRules(misc)
        dynamicDistanceOnlineRules = parseOnlineRules(misc)
    }

    private fun loadPublicSignSettings(publicSign: YamlConfiguration) {
        val publicSignSection = publicSign.getConfigurationSection("public_sign")
        publicSignDefaultExpireDays = publicSignSection?.getInt("default_expire_days") ?: 7
        publicSignContentLines = (publicSignSection?.getInt("content_lines") ?: 3).coerceIn(1, 20)
    }

    private fun loadResourceWorldSettings(resourceWorld: YamlConfiguration) {
        evacuationCommand = resourceWorld.getString("evacuation_command") ?: "spawn"

        val pregenSection = resourceWorld.getConfigurationSection("pregen")
        pregenPriorityDiameter = pregenSection?.getInt("priority_diameter") ?: 1000
        pregenDelayTicks = (pregenSection?.getInt("delay_ticks") ?: 5).toLong()
        pregenBatchSize = pregenSection?.getInt("batch_size") ?: 25

        val difficultyStr = resourceWorld.getString("default_difficulty") ?: "normal"
        defaultDifficulty = try {
            Difficulty.valueOf(difficultyStr.uppercase())
        } catch (_: IllegalArgumentException) {
            Difficulty.NORMAL
        }

        val scaffoldSection = resourceWorld.getConfigurationSection("scaffold")
        scaffoldMaterial = Material.matchMaterial(scaffoldSection?.getString("material") ?: "GLASS") ?: Material.GLASS
        scaffoldRadius = scaffoldSection?.getInt("radius") ?: 3
        actionBarMessage = scaffoldSection?.getString("action_bar_message") ?: "§bShift長押しでスポーンに戻ります"

        val particleSection = scaffoldSection?.getConfigurationSection("particle")
        particleType = Particle.valueOf(particleSection?.getString("type")?.uppercase() ?: "CLOUD")
        particleCount = particleSection?.getInt("count") ?: 5
        particleSpeed = particleSection?.getDouble("speed") ?: 0.01
        particleInterval = (particleSection?.getInt("interval") ?: 2).toLong()

        val soundSection = scaffoldSection?.getConfigurationSection("sound")
        soundStart = soundSection?.getString("start") ?: "BLOCK_NOTE_BLOCK_BELL"
        soundSuccess = soundSection?.getString("success") ?: "ENTITY_EXPERIENCE_ORB_PICKUP"

        val spawnSection = resourceWorld.getConfigurationSection("spawn")
        spawnSearchRadius = spawnSection?.getInt("search_radius") ?: 64
        spawnSearchAttempts = spawnSection?.getInt("search_attempts") ?: 200
        val safeBlocksList = spawnSection?.getStringList("safe_blocks")
        if (safeBlocksList != null && safeBlocksList.isNotEmpty()) {
            spawnSafeBlocks = safeBlocksList
                .mapNotNull { Material.matchMaterial(it.uppercase()) }
                .filter { it.isBlock }
        }

        val netherSpawnSection = resourceWorld.getConfigurationSection("nether_spawn")
        netherSpawnSearchRadius = netherSpawnSection?.getInt("search_radius") ?: 64
        netherSpawnSearchAttempts = netherSpawnSection?.getInt("search_attempts") ?: 200
        val netherSafeBlocksList = netherSpawnSection?.getStringList("safe_blocks")
        if (netherSafeBlocksList != null && netherSafeBlocksList.isNotEmpty()) {
            netherSpawnSafeBlocks = netherSafeBlocksList
                .mapNotNull { Material.matchMaterial(it.uppercase()) }
                .filter { it.isBlock }
        }

        val macroSection = resourceWorld.getConfigurationSection("macros")

        val beforeDeleteSection = macroSection?.getConfigurationSection("before_delete")
        macroBeforeDelete.clear()
        macroBeforeDelete.addAll(beforeDeleteSection?.getStringList("commands") ?: emptyList())
        macroBeforeDeleteWaitAfterTicks = (beforeDeleteSection?.getLong("wait_after_ticks") ?: 20L).coerceAtLeast(0L)
        macroBeforeDeleteEnabled = macroBeforeDelete.isNotEmpty()

        val afterGenSection = macroSection?.getConfigurationSection("after_generation")
        macroAfterGeneration.clear()
        macroAfterGeneration.addAll(afterGenSection?.getStringList("commands") ?: emptyList())
        macroAfterGenerationEnabled = macroAfterGeneration.isNotEmpty()

        val afterPrioritySection = macroSection?.getConfigurationSection("after_priority_pregen")
        macroAfterPriorityPregen.clear()
        macroAfterPriorityPregen.addAll(afterPrioritySection?.getStringList("commands") ?: emptyList())
        macroAfterPriorityPregenEnabled = macroAfterPriorityPregen.isNotEmpty()

        val afterAllSection = macroSection?.getConfigurationSection("after_all_pregen")
        macroAfterAllPregen.clear()
        macroAfterAllPregen.addAll(afterAllSection?.getStringList("commands") ?: emptyList())
        macroAfterAllPregenEnabled = macroAfterAllPregen.isNotEmpty()

        resourceConfigs.clear()
        val resourcesSection = resourceWorld.getConfigurationSection("resources")
        if (resourcesSection != null) {
            for (type in listOf("normal", "nether", "end")) {
                val typeSection = resourcesSection.getConfigurationSection(type)
                if (typeSection != null) {
                    val baseName = typeSection.getString("base_name") ?: "resource_$type"
                    val defaultBorder = typeSection.getInt(
                        "default_border",
                        when (type) {
                            "normal" -> 5000
                            "nether" -> 2500
                            "end" -> 3000
                            else -> 5000
                        }
                    )
                    val variations = typeSection.getStringList("variations")
                    resourceConfigs[type] = ResourceConfig(baseName, defaultBorder, variations)
                }
            }
        }
    }

    private fun loadAnnounceSettings(announce: YamlConfiguration) {
        announceMenuCommand = announce.getString("announce_menu_command") ?: "menu"
        announceUncheckedNotifyIntervalMinutes =
            (announce.getLong("announce_unchecked_notify_interval_minutes", 10L)).coerceAtLeast(1L)
    }

    private fun loadQueueSettings(queue: YamlConfiguration) {
        val queueSection = queue.getConfigurationSection("chunk_task_queue")
        chunkTaskQueuePriorityOrder =
            queueSection?.getStringList("priority_order")?.takeIf { it.isNotEmpty() }
                ?: listOf("SUKIMA_DUNGEON", "ARENA", "RESOURCE")
        chunkTaskQueueReadIntervalTicks = (queueSection?.getLong("read_interval_ticks") ?: 20L).coerceAtLeast(1L)
        chunkTaskQueueDataFilePath =
            queueSection?.getString("queue_data_file")?.trim()?.ifBlank { null } ?: "data/queue/queue_data.yml"
    }

    private fun parseRangeString(raw: String?, defaultRange: IntRange): IntRange {
        if (raw.isNullOrBlank()) return defaultRange
        val regex = Regex("^\\s*(-?\\d+)\\s*\\.\\.\\s*(-?\\d+)\\s*$")
        val matched = regex.find(raw) ?: return defaultRange
        val first = matched.groupValues[1].toIntOrNull() ?: return defaultRange
        val second = matched.groupValues[2].toIntOrNull() ?: return defaultRange
        val min = kotlin.math.min(first, second)
        val max = kotlin.math.max(first, second)
        return min..max
    }

    private fun parseSpeedRules(config: YamlConfiguration): List<SpeedFactorRule> {
        val mapList = config.getMapList("dynamic_distance.factors.horizontal_speed")
        if (mapList.isEmpty()) {
            return listOf(
                SpeedFactorRule(4.5, DistanceDelta(1, 1, 1)),
                SpeedFactorRule(8.0, DistanceDelta(2, 1, 2)),
                SpeedFactorRule(999.0, DistanceDelta(3, 0, 3))
            )
        }

        val parsed = mutableListOf<SpeedFactorRule>()
        for (entry in mapList) {
            val maxBps = toDouble(entry["max_bps"]) ?: continue
            val delta = parseDelta(entry["delta"]) ?: continue
            parsed.add(SpeedFactorRule(maxBps, delta))
        }

        return if (parsed.isEmpty()) {
            listOf(
                SpeedFactorRule(4.5, DistanceDelta(1, 1, 1)),
                SpeedFactorRule(8.0, DistanceDelta(2, 1, 2)),
                SpeedFactorRule(999.0, DistanceDelta(3, 0, 3))
            )
        } else {
            parsed.sortedBy { it.maxBps }
        }
    }

    private fun parseOnlineRules(config: YamlConfiguration): List<OnlineFactorRule> {
        val mapList = config.getMapList("dynamic_distance.factors.online_players")
        if (mapList.isEmpty()) {
            return listOf(
                OnlineFactorRule(15, DistanceDelta(0, 0, 0)),
                OnlineFactorRule(35, DistanceDelta(-2, -1, -2)),
                OnlineFactorRule(999, DistanceDelta(-4, -2, -4))
            )
        }

        val parsed = mutableListOf<OnlineFactorRule>()
        for (entry in mapList) {
            val maxPlayers = toInt(entry["max_players"]) ?: continue
            val delta = parseDelta(entry["delta"]) ?: continue
            parsed.add(OnlineFactorRule(maxPlayers, delta))
        }

        return if (parsed.isEmpty()) {
            listOf(
                OnlineFactorRule(15, DistanceDelta(0, 0, 0)),
                OnlineFactorRule(35, DistanceDelta(-2, -1, -2)),
                OnlineFactorRule(999, DistanceDelta(-4, -2, -4))
            )
        } else {
            parsed.sortedBy { it.maxPlayers }
        }
    }

    private fun parseDelta(value: Any?): DistanceDelta? {
        val map = value as? Map<*, *> ?: return null
        val view = toInt(map["view"]) ?: return null
        val simulation = toInt(map["simulation"]) ?: return null
        val send = toInt(map["send"]) ?: return null
        return DistanceDelta(view, simulation, send)
    }

    private fun toInt(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun toDouble(value: Any?): Double? {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    // === ゲッター ===
    fun getDefaultLanguage(): String = defaultLanguage
    fun getLanguage(): String = defaultLanguage
    fun isDebug(): Boolean = debug

    fun getShiftFBinderCommands(): List<String> = shiftFBinderCommands
    fun isResourceWorldEnabled(): Boolean = featureResourceWorldEnabled
    fun isRentalAreaEnabled(): Boolean = featureRentalAreaEnabled
    fun isPublicSignEnabled(): Boolean = featurePublicSignEnabled
    fun isMusicEnabled(): Boolean = featureMusicEnabled
    fun getMusicSetting(worldName: String): MusicSetting? = worldMusicSettings[worldName]
    fun getAllMusicSettings(): Map<String, MusicSetting> = worldMusicSettings.toMap()
    fun isGlobalSoundEventsAutoDisable(): Boolean = featureGlobalSoundEventsEnabled
    fun isShiftFBinderEnabled(): Boolean = featureShiftFBinderEnabled
    fun isDelayCommandEnabled(): Boolean = featureDelayCommandEnabled
    fun isNpcMessageEnabled(): Boolean = featureNpcMessageEnabled
    fun getAnnounceMenuCommand(): String = announceMenuCommand
    fun getAnnounceUncheckedNotifyIntervalMinutes(): Long = announceUncheckedNotifyIntervalMinutes
    fun isDynamicDistanceEnabled(): Boolean = featureDynamicDistanceEnabled
    fun getPublicSignDefaultExpireDays(): Int = publicSignDefaultExpireDays
    fun getPublicSignContentLines(): Int = publicSignContentLines
    fun getChunkTaskQueuePriorityOrder(): List<String> = chunkTaskQueuePriorityOrder.toList()
    fun getChunkTaskQueueReadIntervalTicks(): Long = chunkTaskQueueReadIntervalTicks
    fun getChunkTaskQueueDataFilePath(): String = chunkTaskQueueDataFilePath

    fun getDynamicDistanceSettings(): DynamicDistanceSettings {
        return DynamicDistanceSettings(
            enabled = featureDynamicDistanceEnabled,
            intervalTicks = dynamicDistanceIntervalTicks,
            applyCooldownTicks = dynamicDistanceApplyCooldownTicks,
            worldBlacklist = dynamicDistanceWorldBlacklist.toList(),
            viewLimit = dynamicDistanceViewLimit,
            simulationLimit = dynamicDistanceSimulationLimit,
            sendLimit = dynamicDistanceSendLimit,
            baseView = dynamicDistanceBaseView,
            baseSimulation = dynamicDistanceBaseSimulation,
            baseSend = dynamicDistanceBaseSend,
            speedRules = dynamicDistanceSpeedRules.toList(),
            onlineRules = dynamicDistanceOnlineRules.toList()
        )
    }

    fun getEvacuationCommand(): String = evacuationCommand
    fun getPregenPriorityDiameter(): Int = pregenPriorityDiameter
    fun getPregenDelayTicks(): Long = pregenDelayTicks
    fun getPregenBatchSize(): Int = pregenBatchSize
    fun getDefaultDifficulty(): Difficulty = defaultDifficulty
    fun getScaffoldMaterial(): Material = scaffoldMaterial
    fun getScaffoldRadius(): Int = scaffoldRadius
    fun getActionBarMessage(): String = actionBarMessage
    fun getParticleType(): Particle = particleType
    fun getParticleCount(): Int = particleCount
    fun getParticleSpeed(): Double = particleSpeed
    fun getParticleInterval(): Long = particleInterval
    fun getSoundStart(): String = soundStart
    fun getSoundSuccess(): String = soundSuccess
    fun getSpawnSearchRadius(): Int = spawnSearchRadius
    fun getSpawnSearchAttempts(): Int = spawnSearchAttempts
    fun getSpawnSafeBlocks(): List<Material> = spawnSafeBlocks.toList()
    fun getNetherSpawnSearchRadius(): Int = netherSpawnSearchRadius
    fun getNetherSpawnSearchAttempts(): Int = netherSpawnSearchAttempts
    fun getNetherSpawnSafeBlocks(): List<Material> = netherSpawnSafeBlocks.toList()
    fun isMacroBeforeDeleteEnabled(): Boolean = macroBeforeDeleteEnabled
    fun getMacroBeforeDeleteCommands(): List<String> = macroBeforeDelete.toList()
    fun getMacroBeforeDeleteWaitAfterTicks(): Long = macroBeforeDeleteWaitAfterTicks
    fun isMacroAfterGenerationEnabled(): Boolean = macroAfterGenerationEnabled
    fun getMacroAfterGenerationCommands(): List<String> = macroAfterGeneration.toList()
    fun isMacroAfterPriorityPregenEnabled(): Boolean = macroAfterPriorityPregenEnabled
    fun getMacroAfterPriorityPregenCommands(): List<String> = macroAfterPriorityPregen.toList()
    fun isMacroAfterAllPregenEnabled(): Boolean = macroAfterAllPregenEnabled
    fun getMacroAfterAllPregenCommands(): List<String> = macroAfterAllPregen.toList()
    fun getResourceConfig(type: String): ResourceConfig? = resourceConfigs[type.lowercase()]
    fun getAllResourceConfigs(): Map<String, ResourceConfig> = resourceConfigs.toMap()
}
