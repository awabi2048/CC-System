package com.awabi2048.ccsystem.core.cosmetic

import com.awabi2048.ccsystem.api.cosmetic.CosmeticId
import com.awabi2048.ccsystem.api.cosmetic.CosmeticProfileSnapshot
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal data class MutableCosmeticProfile(
    val ownedMedals: MutableSet<CosmeticId> = linkedSetOf(),
    val ownedParticles: MutableSet<CosmeticId> = linkedSetOf(),
    var equippedMedal: CosmeticId? = null,
    var equippedParticle: CosmeticId? = null
) {
    fun snapshot(playerId: UUID) = CosmeticProfileSnapshot(
        playerId,
        ownedMedals.toSet(),
        ownedParticles.toSet(),
        equippedMedal,
        equippedParticle
    )
}

internal class CosmeticProfileRepository(private val file: File) {
    private val profiles = linkedMapOf<UUID, MutableCosmeticProfile>()
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "cc-system-cosmetic-save").apply { isDaemon = true }
    }

    @Volatile
    var lastSaveFailure: String? = null
        private set

    init {
        load()
    }

    @Synchronized
    fun profile(playerId: UUID): MutableCosmeticProfile =
        profiles.getOrPut(playerId) { MutableCosmeticProfile() }

    @Synchronized
    fun snapshot(playerId: UUID): CosmeticProfileSnapshot = profile(playerId).snapshot(playerId)

    @Synchronized
    fun allSnapshots(): List<CosmeticProfileSnapshot> =
        profiles.map { (id, profile) -> profile.snapshot(id) }

    @Synchronized
    fun size(): Int = profiles.size

    fun saveAsync() {
        val snapshots = synchronized(this) { allSnapshots() }
        writer.execute { saveSnapshots(snapshots) }
    }

    fun shutdown() {
        saveAsync()
        writer.shutdown()
        writer.awaitTermination(10, TimeUnit.SECONDS)
    }

    private fun load() {
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.getConfigurationSection("players")?.getKeys(false)?.forEach { rawId ->
            val playerId = runCatching { UUID.fromString(rawId) }.getOrNull() ?: return@forEach
            val base = "players.$rawId"
            profiles[playerId] = MutableCosmeticProfile(
                ownedMedals = readIds(yaml.getStringList("$base.owned.medals")).toMutableSet(),
                ownedParticles = readIds(yaml.getStringList("$base.owned.particles")).toMutableSet(),
                equippedMedal = yaml.getString("$base.equipped.medal")?.let(::safeId),
                equippedParticle = yaml.getString("$base.equipped.particle")?.let(::safeId)
            )
        }
    }

    private fun saveSnapshots(snapshots: List<CosmeticProfileSnapshot>) {
        try {
            file.parentFile?.mkdirs()
            val yaml = YamlConfiguration()
            yaml.set("schema_version", 1)
            snapshots.forEach { profile ->
                val base = "players.${profile.playerId}"
                yaml.set("$base.owned.medals", profile.ownedMedals.map(CosmeticId::value).sorted())
                yaml.set("$base.owned.particles", profile.ownedParticles.map(CosmeticId::value).sorted())
                yaml.set("$base.equipped.medal", profile.equippedMedal?.value)
                yaml.set("$base.equipped.particle", profile.equippedParticle?.value)
            }
            val temporary = File(file.parentFile, "${file.name}.tmp")
            yaml.save(temporary)
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            lastSaveFailure = null
        } catch (failure: Exception) {
            lastSaveFailure = failure.message ?: failure.javaClass.simpleName
        }
    }

    private fun readIds(values: List<String>): Set<CosmeticId> = values.mapNotNull(::safeId).toSet()
    private fun safeId(value: String): CosmeticId? = runCatching { CosmeticId(value) }.getOrNull()
}
