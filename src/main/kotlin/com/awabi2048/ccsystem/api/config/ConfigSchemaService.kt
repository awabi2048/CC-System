package com.awabi2048.ccsystem.api.config

import java.nio.file.Path
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

enum class ConfigClassification {
    MANAGED_CONFIG,
    BUNDLED_DEFINITION
}

enum class ConfigPreparationState {
    CURRENT,
    MISSING,
    OUTDATED,
    UPDATED,
    FUTURE_VERSION,
    INVALID,
    FAILED,
    RESTART_REQUIRED
}

fun interface ConfigMigration {
    fun migrate(configuration: YamlConfiguration)
}

fun interface ConfigValidator {
    fun validate(configuration: YamlConfiguration)
}

fun interface ConfigReloadAction {
    fun reload()
}

data class ManagedConfigSpec(
    val owner: String,
    val sourcePlugin: JavaPlugin,
    val resourcePath: String,
    val targetPath: Path,
    val currentVersion: Int,
    val classification: ConfigClassification,
    val migrations: Map<Int, ConfigMigration>,
    val validator: ConfigValidator,
    val reloadAction: ConfigReloadAction?
)

data class ManagedConfigStatus(
    val owner: String,
    val resourcePath: String,
    val targetPath: Path,
    val detectedVersion: Int?,
    val requiredVersion: Int,
    val state: ConfigPreparationState,
    val backupPath: Path? = null,
    val message: String? = null
)

data class ConfigPreparationResult(
    val statuses: List<ManagedConfigStatus>
) {
    val successful: Boolean
        get() = statuses.none {
            it.state == ConfigPreparationState.FUTURE_VERSION ||
                it.state == ConfigPreparationState.INVALID ||
                it.state == ConfigPreparationState.FAILED
        }
}

interface ConfigSchemaService {
    fun register(owner: String, specifications: Collection<ManagedConfigSpec>)

    fun check(owner: String? = null): ConfigPreparationResult

    fun prepare(owner: String? = null): ConfigPreparationResult

    fun reload(owner: String? = null): ConfigPreparationResult

    fun status(owner: String? = null): List<ManagedConfigStatus>

    fun unregister(owner: String)
}
