package com.awabi2048.ccsystem.core.config

import com.awabi2048.ccsystem.api.config.ConfigPreparationResult
import com.awabi2048.ccsystem.api.config.ConfigPreparationState
import com.awabi2048.ccsystem.api.config.ConfigSchemaService
import com.awabi2048.ccsystem.api.config.ManagedConfigSpec
import com.awabi2048.ccsystem.api.config.ManagedConfigStatus
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.configuration.file.YamlConfiguration

class ConfigSchemaServiceImpl : ConfigSchemaService {
    private val specifications = ConcurrentHashMap<String, LinkedHashMap<Path, ManagedConfigSpec>>()
    private val backupFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")

    override fun register(owner: String, specifications: Collection<ManagedConfigSpec>) {
        require(owner.isNotBlank()) { "owner must not be blank" }
        val normalized = LinkedHashMap<Path, ManagedConfigSpec>()
        specifications.forEach { spec ->
            require(spec.owner == owner) { "ManagedConfigSpec owner mismatch: ${spec.owner} != $owner" }
            require(spec.currentVersion >= 1) { "currentVersion must be positive: ${spec.resourcePath}" }
            require(spec.resourcePath.isNotBlank()) { "resourcePath must not be blank" }
            val target = spec.targetPath.toAbsolutePath().normalize()
            require(normalized.put(target, spec.copy(targetPath = target)) == null) {
                "Duplicate managed config target: $target"
            }
        }
        this.specifications[owner] = normalized
    }

    override fun check(owner: String?): ConfigPreparationResult {
        return ConfigPreparationResult(selected(owner).map { inspect(it, false) })
    }

    override fun prepare(owner: String?): ConfigPreparationResult {
        return ConfigPreparationResult(selected(owner).map { inspect(it, true) })
    }

    override fun reload(owner: String?): ConfigPreparationResult {
        val prepared = prepare(owner)
        if (!prepared.successful) {
            return prepared
        }
        val reloadStatuses = mutableListOf<ManagedConfigStatus>()
        selected(owner).groupBy(ManagedConfigSpec::owner).forEach { (_, specs) ->
            val reload = specs.firstNotNullOfOrNull(ManagedConfigSpec::reloadAction)
            if (reload == null) {
                reloadStatuses += specs.map { spec ->
                    currentStatus(spec).copy(
                        state = ConfigPreparationState.RESTART_REQUIRED,
                        message = "reload action is not registered"
                    )
                }
            } else {
                runCatching(reload::reload).onFailure { failure ->
                    reloadStatuses += specs.map { spec ->
                        currentStatus(spec).copy(
                            state = ConfigPreparationState.FAILED,
                            message = failure.message
                        )
                    }
                }
            }
        }
        return if (reloadStatuses.isEmpty()) prepared else ConfigPreparationResult(prepared.statuses + reloadStatuses)
    }

    override fun status(owner: String?): List<ManagedConfigStatus> = selected(owner).map(::currentStatus)

    override fun unregister(owner: String) {
        specifications.remove(owner)
    }

    private fun selected(owner: String?): List<ManagedConfigSpec> {
        return if (owner == null) {
            specifications.toSortedMap().values.flatMap { it.values }
        } else {
            specifications[owner]?.values?.toList().orEmpty()
        }
    }

    private fun currentStatus(spec: ManagedConfigSpec): ManagedConfigStatus {
        if (!Files.isRegularFile(spec.targetPath)) {
            return status(spec, null, ConfigPreparationState.MISSING)
        }
        return runCatching {
            val yaml = loadStrict(spec.targetPath)
            val version = version(yaml)
            val state = when {
                version > spec.currentVersion -> ConfigPreparationState.FUTURE_VERSION
                version < spec.currentVersion -> ConfigPreparationState.OUTDATED
                else -> ConfigPreparationState.CURRENT
            }
            status(spec, version, state)
        }.getOrElse { status(spec, null, ConfigPreparationState.INVALID, message = it.message) }
    }

    private fun inspect(spec: ManagedConfigSpec, write: Boolean): ManagedConfigStatus {
        var created = false
        if (!Files.isRegularFile(spec.targetPath)) {
            if (!write) {
                return status(spec, null, ConfigPreparationState.MISSING)
            }
            copyBundledResource(spec)
            created = true
        }
        return runCatching {
            val original = loadStrict(spec.targetPath)
            val detectedVersion = version(original)
            if (detectedVersion > spec.currentVersion) {
                return status(spec, detectedVersion, ConfigPreparationState.FUTURE_VERSION)
            }
            if (!write) {
                spec.validator.validate(original)
                return status(
                    spec,
                    detectedVersion,
                    if (detectedVersion == spec.currentVersion) ConfigPreparationState.CURRENT else ConfigPreparationState.OUTDATED
                )
            }

            var backupPath: Path? = null
            if (detectedVersion < spec.currentVersion) {
                backupPath = backup(spec.targetPath)
                if (spec.classification == com.awabi2048.ccsystem.api.config.ConfigClassification.BUNDLED_DEFINITION) {
                    copyBundledResource(spec)
                    val replaced = loadStrict(spec.targetPath)
                    spec.validator.validate(replaced)
                    return status(spec, spec.currentVersion, ConfigPreparationState.UPDATED, backupPath)
                }
                var next = detectedVersion
                while (next < spec.currentVersion) {
                    val migration = spec.migrations[next]
                    if (migration != null) {
                        migration.migrate(original)
                    } else if (next == 0) {
                        mergeBundledDefaults(spec, original)
                    } else {
                        error("Missing migration ${next}->${next + 1} for ${spec.resourcePath}")
                    }
                    next++
                    original.set("config_version", next)
                }
                spec.validator.validate(original)
                atomicSave(original, spec.targetPath)
            } else {
                spec.validator.validate(original)
            }
            status(
                spec,
                spec.currentVersion,
                if (created || detectedVersion < spec.currentVersion) ConfigPreparationState.UPDATED else ConfigPreparationState.CURRENT,
                backupPath
            )
        }.getOrElse { failure ->
            status(spec, null, ConfigPreparationState.FAILED, message = failure.message)
        }
    }

    private fun copyBundledResource(spec: ManagedConfigSpec) {
        val target = spec.targetPath
        Files.createDirectories(target.parent)
        val stream = spec.sourcePlugin.getResource(spec.resourcePath)
            ?: error("Bundled config resource is missing: ${spec.resourcePath}")
        stream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun mergeBundledDefaults(spec: ManagedConfigSpec, target: YamlConfiguration) {
        val defaults = YamlConfiguration()
        spec.sourcePlugin.getResource(spec.resourcePath)?.use {
            defaults.load(it.reader(Charsets.UTF_8))
        } ?: error("Bundled config resource is missing: ${spec.resourcePath}")
        defaults.getKeys(true)
            .filter { it != "config_version" }
            .filterNot(defaults::isConfigurationSection)
            .filterNot(target::contains)
            .forEach { target.set(it, defaults.get(it)) }
    }

    private fun loadStrict(path: Path): YamlConfiguration {
        val yaml = YamlConfiguration()
        yaml.load(path.toFile())
        return yaml
    }

    private fun version(yaml: YamlConfiguration): Int {
        val raw = yaml.get("config_version") ?: return 0
        require(raw is Number) { "config_version must be an integer" }
        val value = raw.toInt()
        require(value >= 0 && raw.toDouble() == value.toDouble()) { "config_version must be a non-negative integer" }
        return value
    }

    private fun backup(path: Path): Path {
        val backup = path.resolveSibling("${path.fileName}.bak-${LocalDateTime.now().format(backupFormatter)}")
        Files.copy(path, backup, StandardCopyOption.COPY_ATTRIBUTES)
        return backup
    }

    private fun atomicSave(yaml: YamlConfiguration, target: Path) {
        Files.createDirectories(target.parent)
        val temporary = target.resolveSibling("${target.fileName}.tmp")
        yaml.save(temporary.toFile())
        loadStrict(temporary)
        runCatching {
            Files.move(
                temporary,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.getOrElse {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun status(
        spec: ManagedConfigSpec,
        detectedVersion: Int?,
        state: ConfigPreparationState,
        backupPath: Path? = null,
        message: String? = null
    ): ManagedConfigStatus {
        return ManagedConfigStatus(
            spec.owner,
            spec.resourcePath,
            spec.targetPath,
            detectedVersion,
            spec.currentVersion,
            state,
            backupPath,
            message
        )
    }
}
