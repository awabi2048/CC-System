package com.awabi2048.ccsystem.core.config

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.Sound

/**
 * CC-System統合設定マネージャー
 * 
 * CrafterCrossingMiscとResourceWorldManagerの設定を統合管理します
 */
object ConfigManager {
    private var config: FileConfiguration? = null
    
    // === コア設定 ===
    private var language: String = "ja"
    private var debug: Boolean = false
    
    // === CrafterCrossingMisc設定 ===
    
    // Shift + F バインダー設定
    private var shiftFBinderCommands: List<String> = listOf("say %player_name%")
    
    // 音楽再生設定
    private var musicEnabled: Boolean = true
    private val worldMusicSettings = mutableMapOf<String, MusicSetting>()
    
    // globalSoundEvents 自動無効化
    private var globalSoundEventsAutoDisable: Boolean = true

    // PublicSign設定
    private var publicSignDefaultExpireDays: Int = 7
    private var publicSignContentLines: Int = 3
    
    // === ResourceWorldManager設定 ===
    
    // 避難コマンド
    private var evacuationCommand: String = "spawn"
    

    
    // 事前生成設定
    private var pregenPriorityDiameter: Int = 1000
    private var pregenDelayTicks: Long = 5
    private var pregenBatchSize: Int = 25
    
    // 足場設定
    private var scaffoldMaterial: Material = Material.GLASS
    private var scaffoldRadius: Int = 3
    private var actionBarMessage: String = "§bShift長押しでスポーンに戻ります"
    
    // パーティクル設定
    private var particleType: Particle = Particle.CLOUD
    private var particleCount: Int = 5
    private var particleSpeed: Double = 0.01
    private var particleInterval: Long = 2
    
    // サウンド設定
    private var soundStart: String = "BLOCK_NOTE_BLOCK_BELL"
    private var soundSuccess: String = "ENTITY_EXPERIENCE_ORB_PICKUP"
    
    // スポーン位置検索設定（通常世界）
    private var spawnSearchRadius: Int = 64
    private var spawnSearchAttempts: Int = 200
    private var spawnSafeBlocks: List<Material> = listOf(
        Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL,
        Material.STONE, Material.COBBLESTONE, Material.SAND, Material.SANDSTONE,
        Material.GRAVEL, Material.MOSS_BLOCK
    )
    
    // スポーン位置検索設定（ネザー）
    private var netherSpawnSearchRadius: Int = 64
    private var netherSpawnSearchAttempts: Int = 200
    private var netherSpawnSafeBlocks: List<Material> = listOf(
        Material.NETHERRACK, Material.SOUL_SAND, Material.SOUL_SOIL,
        Material.BASALT, Material.BLACKSTONE, Material.NETHER_BRICKS,
        Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM
    )
    
    // マクロ設定
    private val macroBeforeDelete = mutableListOf<String>()
    private val macroAfterGeneration = mutableListOf<String>()
    private val macroAfterPriorityPregen = mutableListOf<String>()
    private val macroAfterAllPregen = mutableListOf<String>()
    private var macroBeforeDeleteEnabled: Boolean = false
    private var macroAfterGenerationEnabled: Boolean = false
    private var macroAfterPriorityPregenEnabled: Boolean = false
    private var macroAfterAllPregenEnabled: Boolean = false
    
    // リソースタイプ設定
    private val resourceConfigs = mutableMapOf<String, ResourceConfig>()
    
    /**
     * 音楽設定データクラス
     */
    data class MusicSetting(
        val sound: String,
        val volume: Float,
        val pitch: Float,
        val duration: Int // 秒
    )
    
    /**
     * リソース設定データクラス
     */
    data class ResourceConfig(
        val baseName: String,
        val defaultBorder: Int,
        val variations: List<String>
    )
    
    /**
     * 設定をロードする
     */
    fun load(fileConfig: FileConfiguration) {
        config = fileConfig
        
        // コア設定の読み込み
        val coreSection = fileConfig.getConfigurationSection("core")
        language = coreSection?.getString("language") ?: "ja"
        debug = coreSection?.getBoolean("debug") ?: false
        
        // === CrafterCrossingMisc設定の読み込み ===
        
        // Shift + F バインダー
        val shiftFBinderSection = fileConfig.getConfigurationSection("shift_f_binder")
        shiftFBinderCommands = shiftFBinderSection?.getStringList("commands") ?: listOf("say %player_name%")
        
        // 音楽再生
        val musicSection = fileConfig.getConfigurationSection("music")
        musicEnabled = musicSection?.getBoolean("enabled") ?: true
        
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
        
        // globalSoundEvents 自動無効化
        val globalSoundSection = fileConfig.getConfigurationSection("global_sound_events")
        globalSoundEventsAutoDisable = globalSoundSection?.getBoolean("enabled") ?: true

        // PublicSign設定
        val publicSignSection = fileConfig.getConfigurationSection("public_sign")
        publicSignDefaultExpireDays = publicSignSection?.getInt("default_expire_days") ?: 7
        publicSignContentLines = (publicSignSection?.getInt("content_lines") ?: 3).coerceIn(1, 20)
        
        // === ResourceWorldManager設定の読み込み ===
        
        // 基本設定
        evacuationCommand = fileConfig.getString("evacuation_command") ?: "spawn"
        

        
        // 事前生成設定
        val pregenSection = fileConfig.getConfigurationSection("pregen")
        pregenPriorityDiameter = pregenSection?.getInt("priority_diameter") ?: 1000
        pregenDelayTicks = (pregenSection?.getInt("delay_ticks") ?: 5).toLong()
        pregenBatchSize = pregenSection?.getInt("batch_size") ?: 25
        
        // 足場設定
        val scaffoldSection = fileConfig.getConfigurationSection("scaffold")
        scaffoldMaterial = Material.matchMaterial(scaffoldSection?.getString("material") ?: "GLASS") ?: Material.GLASS
        scaffoldRadius = scaffoldSection?.getInt("radius") ?: 3
        actionBarMessage = scaffoldSection?.getString("action_bar_message") ?: "§bShift長押しでスポーンに戻ります"
        
        // パーティクル設定
        val particleSection = scaffoldSection?.getConfigurationSection("particle")
        particleType = Particle.valueOf(particleSection?.getString("type")?.uppercase() ?: "CLOUD")
        particleCount = particleSection?.getInt("count") ?: 5
        particleSpeed = particleSection?.getDouble("speed") ?: 0.01
        particleInterval = (particleSection?.getInt("interval") ?: 2).toLong()
        
        // サウンド設定
        val soundSection = scaffoldSection?.getConfigurationSection("sound")
        soundStart = soundSection?.getString("start") ?: "BLOCK_NOTE_BLOCK_BELL"
        soundSuccess = soundSection?.getString("success") ?: "ENTITY_EXPERIENCE_ORB_PICKUP"
        
        // スポーン位置検索設定（通常世界）
        val spawnSection = fileConfig.getConfigurationSection("spawn")
        spawnSearchRadius = spawnSection?.getInt("search_radius") ?: 64
        spawnSearchAttempts = spawnSection?.getInt("search_attempts") ?: 200
        
        val safeBlocksList = spawnSection?.getStringList("safe_blocks")
        if (safeBlocksList != null && safeBlocksList.isNotEmpty()) {
            spawnSafeBlocks = safeBlocksList.mapNotNull {
                Material.matchMaterial(it.uppercase())
            }.filter { it.isBlock }
        }
        
        // スポーン位置検索設定（ネザー）
        val netherSpawnSection = fileConfig.getConfigurationSection("nether_spawn")
        netherSpawnSearchRadius = netherSpawnSection?.getInt("search_radius") ?: 64
        netherSpawnSearchAttempts = netherSpawnSection?.getInt("search_attempts") ?: 200
        val netherSafeBlocksList = netherSpawnSection?.getStringList("safe_blocks")
        if (netherSafeBlocksList != null && netherSafeBlocksList.isNotEmpty()) {
            netherSpawnSafeBlocks = netherSafeBlocksList.mapNotNull {
                Material.matchMaterial(it.uppercase())
            }.filter { it.isBlock }
        }
        
        // マクロ設定の読み込み
        val macroSection = fileConfig.getConfigurationSection("macros")
        
        val beforeDeleteSection = macroSection?.getConfigurationSection("before_delete")
        macroBeforeDeleteEnabled = beforeDeleteSection?.getBoolean("enabled") ?: false
        macroBeforeDelete.clear()
        if (macroBeforeDeleteEnabled) {
            macroBeforeDelete.addAll(beforeDeleteSection?.getStringList("commands") ?: emptyList())
        }
        
        val afterGenSection = macroSection?.getConfigurationSection("after_generation")
        macroAfterGenerationEnabled = afterGenSection?.getBoolean("enabled") ?: false
        macroAfterGeneration.clear()
        if (macroAfterGenerationEnabled) {
            macroAfterGeneration.addAll(afterGenSection?.getStringList("commands") ?: emptyList())
        }
        
        val afterPrioritySection = macroSection?.getConfigurationSection("after_priority_pregen")
        macroAfterPriorityPregenEnabled = afterPrioritySection?.getBoolean("enabled") ?: false
        macroAfterPriorityPregen.clear()
        if (macroAfterPriorityPregenEnabled) {
            macroAfterPriorityPregen.addAll(afterPrioritySection?.getStringList("commands") ?: emptyList())
        }
        
        val afterAllSection = macroSection?.getConfigurationSection("after_all_pregen")
        macroAfterAllPregenEnabled = afterAllSection?.getBoolean("enabled") ?: false
        macroAfterAllPregen.clear()
        if (macroAfterAllPregenEnabled) {
            macroAfterAllPregen.addAll(afterAllSection?.getStringList("commands") ?: emptyList())
        }
        
        // リソースタイプ設定の読み込み
        resourceConfigs.clear()
        val resourcesSection = fileConfig.getConfigurationSection("resources")
        if (resourcesSection != null) {
            for (type in listOf("normal", "nether", "end")) {
                val typeSection = resourcesSection.getConfigurationSection(type)
                if (typeSection != null) {
                    val baseName = typeSection.getString("base_name") ?: "resource_$type"
                    val defaultBorder = typeSection.getInt("default_border", when (type) {
                        "normal" -> 5000
                        "nether" -> 2500
                        "end" -> 3000
                        else -> 5000
                    })
                    val variations = typeSection.getStringList("variations")
                    
                    resourceConfigs[type] = ResourceConfig(baseName, defaultBorder, variations)
                }
            }
        }
    }
    
    /**
     * 設定をリロードする
     */
    fun reload(fileConfig: FileConfiguration) {
        load(fileConfig)
    }
    
    // === ゲッターメソッド ===
    
    // コア設定
    fun getLanguage(): String = language
    fun isDebug(): Boolean = debug
    
    // CrafterCrossingMisc設定
    fun getShiftFBinderCommands(): List<String> = shiftFBinderCommands
    fun isMusicEnabled(): Boolean = musicEnabled
    fun getMusicSetting(worldName: String): MusicSetting? = worldMusicSettings[worldName]
    fun getAllMusicSettings(): Map<String, MusicSetting> = worldMusicSettings.toMap()
    fun isGlobalSoundEventsAutoDisable(): Boolean = globalSoundEventsAutoDisable
    fun getPublicSignDefaultExpireDays(): Int = publicSignDefaultExpireDays
    fun getPublicSignContentLines(): Int = publicSignContentLines
    
    // ResourceWorldManager設定
    fun getEvacuationCommand(): String = evacuationCommand
    

    
    fun getPregenPriorityDiameter(): Int = pregenPriorityDiameter
    fun getPregenDelayTicks(): Long = pregenDelayTicks
    fun getPregenBatchSize(): Int = pregenBatchSize
    
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
    
    // マクロ設定
    fun isMacroBeforeDeleteEnabled(): Boolean = macroBeforeDeleteEnabled
    fun getMacroBeforeDeleteCommands(): List<String> = macroBeforeDelete.toList()
    
    fun isMacroAfterGenerationEnabled(): Boolean = macroAfterGenerationEnabled
    fun getMacroAfterGenerationCommands(): List<String> = macroAfterGeneration.toList()
    
    fun isMacroAfterPriorityPregenEnabled(): Boolean = macroAfterPriorityPregenEnabled
    fun getMacroAfterPriorityPregenCommands(): List<String> = macroAfterPriorityPregen.toList()
    
    fun isMacroAfterAllPregenEnabled(): Boolean = macroAfterAllPregenEnabled
    fun getMacroAfterAllPregenCommands(): List<String> = macroAfterAllPregen.toList()
    
    // リソースタイプ設定
    fun getResourceConfig(type: String): ResourceConfig? = resourceConfigs[type.lowercase()]
    fun getAllResourceConfigs(): Map<String, ResourceConfig> = resourceConfigs.toMap()
}
