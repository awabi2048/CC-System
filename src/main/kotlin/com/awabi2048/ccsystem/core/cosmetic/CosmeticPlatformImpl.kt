package com.awabi2048.ccsystem.core.cosmetic

import com.awabi2048.ccsystem.api.cosmetic.*
import org.bukkit.Bukkit
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.UUID

internal class CosmeticPlatformImpl(
    private val plugin: JavaPlugin,
    dataFolder: File
) : CosmeticPlatform, CosmeticFactRegistry, Listener {
    private val repository = CosmeticProfileRepository(File(dataFolder, "data/cosmetics/profiles.yml"))
    private val medals = linkedMapOf<CosmeticId, MedalDefinition>()
    private val particles = linkedMapOf<CosmeticId, ParticleDefinition>()
    private val factProviders = linkedMapOf<String, (UUID) -> Map<String, Any>>()
    private val medalRuntime = mutableMapOf<UUID, ActiveMedal>()
    private val particleRuntime = mutableMapOf<UUID, ActiveParticle>()
    private val interactionOwners = mutableMapOf<UUID, UUID>()
    private var tick: Long = 0
    private var task: BukkitTask? = null

    private data class ActiveMedal(
        val id: CosmeticId,
        val revision: Long,
        val verticalOffset: Double,
        val display: TextDisplay,
        val interaction: Interaction?,
        val onClick: ((Player) -> Unit)?
    )

    private data class ActiveParticle(
        val id: CosmeticId,
        val revision: Long,
        val program: ParticleProgram,
        val facts: CosmeticFacts
    )

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { runtimeTick() }, 1L, 1L)
    }

    @Synchronized
    override fun registerMedal(definition: MedalDefinition): CosmeticRegistrationResult {
        if (definition.sourceId.isBlank()) return CosmeticRegistrationResult.Rejected("sourceId is blank")
        val previous = medals[definition.id]
        if (previous != null && previous.sourceId != definition.sourceId) {
            return CosmeticRegistrationResult.Rejected("ID is owned by ${previous.sourceId}")
        }
        medals[definition.id] = definition
        refreshEquipped(definition.id, CosmeticType.MEDAL)
        return if (previous == null) CosmeticRegistrationResult.Registered else CosmeticRegistrationResult.Replaced
    }

    @Synchronized
    override fun registerParticle(definition: ParticleDefinition): CosmeticRegistrationResult {
        if (definition.sourceId.isBlank()) return CosmeticRegistrationResult.Rejected("sourceId is blank")
        val previous = particles[definition.id]
        if (previous != null && previous.sourceId != definition.sourceId) {
            return CosmeticRegistrationResult.Rejected("ID is owned by ${previous.sourceId}")
        }
        particles[definition.id] = definition
        refreshEquipped(definition.id, CosmeticType.PARTICLE)
        return if (previous == null) CosmeticRegistrationResult.Registered else CosmeticRegistrationResult.Replaced
    }

    @Synchronized
    override fun unregisterSource(sourceId: String) {
        val medalIds = medals.filterValues { it.sourceId == sourceId }.keys
        val particleIds = particles.filterValues { it.sourceId == sourceId }.keys
        medals.keys.removeAll(medalIds)
        particles.keys.removeAll(particleIds)
        repository.allSnapshots().forEach { profile ->
            if (profile.equippedMedal in medalIds || profile.equippedParticle in particleIds) {
                refresh(profile.playerId)
            }
        }
    }

    @Synchronized
    override fun setOwned(
        playerId: UUID,
        type: CosmeticType,
        id: CosmeticId,
        owned: Boolean
    ): CosmeticOwnershipResult {
        val profile = repository.profile(playerId)
        val ownedSet = if (type == CosmeticType.MEDAL) profile.ownedMedals else profile.ownedParticles
        val changed = if (owned) ownedSet.add(id) else ownedSet.remove(id)
        if (!changed) return CosmeticOwnershipResult.Unchanged
        if (!owned) {
            when (type) {
                CosmeticType.MEDAL -> if (profile.equippedMedal == id) profile.equippedMedal = null
                CosmeticType.PARTICLE -> if (profile.equippedParticle == id) profile.equippedParticle = null
            }
            refresh(playerId)
        }
        repository.saveAsync()
        return CosmeticOwnershipResult.Changed
    }

    @Synchronized
    override fun equip(playerId: UUID, type: CosmeticType, id: CosmeticId?): CosmeticEquipResult {
        val profile = repository.profile(playerId)
        if (id != null) {
            val owned = if (type == CosmeticType.MEDAL) id in profile.ownedMedals else id in profile.ownedParticles
            if (!owned) return CosmeticEquipResult.NotOwned(id)
        }
        val previous = if (type == CosmeticType.MEDAL) profile.equippedMedal else profile.equippedParticle
        if (previous == id) return CosmeticEquipResult.Unchanged
        if (type == CosmeticType.MEDAL) profile.equippedMedal = id else profile.equippedParticle = id
        repository.saveAsync()
        refresh(playerId)
        return if (id == null) CosmeticEquipResult.Unequipped else CosmeticEquipResult.Equipped
    }

    override fun profile(playerId: UUID): CosmeticProfileSnapshot = repository.snapshot(playerId)
    override fun facts(): CosmeticFactRegistry = this

    @Synchronized
    override fun register(providerId: String, provider: (UUID) -> Map<String, Any>): Boolean {
        if (providerId.isBlank() || factProviders.containsKey(providerId)) return false
        factProviders[providerId] = provider
        return true
    }

    @Synchronized
    override fun unregister(providerId: String): Boolean = factProviders.remove(providerId) != null

    @Synchronized
    override fun snapshot(playerId: UUID): CosmeticFacts {
        val values = linkedMapOf<String, Any>()
        factProviders.forEach { (providerId, provider) ->
            runCatching { provider(playerId) }.onSuccess { facts ->
                facts.forEach { (key, value) -> values["$providerId.$key"] = value }
            }.onFailure {
                plugin.logger.warning("[Cosmetic] fact provider $providerId failed: ${it.message}")
            }
        }
        return CosmeticFacts.of(values)
    }

    override fun notifyChanged(playerId: UUID) {
        val medalId = repository.snapshot(playerId).equippedMedal
        val particleId = repository.snapshot(playerId).equippedParticle
        if (medalId?.let(medals::get)?.refreshPolicies?.contains(CosmeticRefreshPolicy.FACT_CHANGE) == true ||
            particleId?.let(particles::get)?.refreshPolicies?.contains(CosmeticRefreshPolicy.FACT_CHANGE) == true
        ) {
            refresh(playerId)
        }
    }

    @Synchronized
    override fun refresh(playerId: UUID) {
        clearMedal(playerId)
        particleRuntime.remove(playerId)
        val player = Bukkit.getPlayer(playerId)?.takeIf(Player::isOnline) ?: return
        val profile = repository.snapshot(playerId)
        val facts = snapshot(playerId)

        profile.equippedMedal?.let { id ->
            val definition = medals[id] ?: return@let
            val presentation = runCatching { definition.resolve(playerId, facts) }.getOrNull() ?: return@let
            val location = medalLocation(player, presentation.verticalOffset)
            val display = player.world.spawn(location, TextDisplay::class.java) {
                it.text(presentation.text)
                it.isPersistent = false
                it.billboard = org.bukkit.entity.Display.Billboard.CENTER
                it.isSeeThrough = true
                it.teleportDuration = 1
            }
            val interaction = if (
                presentation.onClick != null &&
                presentation.interactionWidth != null &&
                presentation.interactionHeight != null
            ) {
                player.world.spawn(location, Interaction::class.java) {
                    it.isPersistent = false
                    it.interactionWidth = presentation.interactionWidth
                    it.interactionHeight = presentation.interactionHeight
                    it.isResponsive = true
                }.also { interactionOwners[it.uniqueId] = playerId }
            } else null
            medalRuntime[playerId] = ActiveMedal(
                id,
                definition.revision,
                presentation.verticalOffset,
                display,
                interaction,
                presentation.onClick
            )
        }

        profile.equippedParticle?.let { id ->
            val definition = particles[id] ?: return@let
            val program = runCatching { definition.resolve(playerId, facts) }.getOrNull() ?: return@let
            particleRuntime[playerId] = ActiveParticle(id, definition.revision, program, facts)
        }
    }

    @Synchronized
    override fun diagnostics(): CosmeticDiagnosticSnapshot {
        val snapshots = repository.allSnapshots()
        return CosmeticDiagnosticSnapshot(
            medals.size,
            particles.size,
            repository.size(),
            medalRuntime.size,
            particleRuntime.size,
            snapshots.mapNotNull { it.equippedMedal }.filterNot(medals::containsKey).toSet(),
            snapshots.mapNotNull { it.equippedParticle }.filterNot(particles::containsKey).toSet(),
            repository.lastSaveFailure
        )
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) = refresh(event.player.uniqueId)

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        clearRuntime(event.player.uniqueId)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        Bukkit.getScheduler().runTask(plugin, Runnable { refresh(event.player.uniqueId) })
    }

    @EventHandler
    fun onWorldChanged(event: PlayerChangedWorldEvent) = refresh(event.player.uniqueId)

    @EventHandler(ignoreCancelled = true)
    fun onInteraction(event: PlayerInteractEntityEvent) {
        val ownerId = interactionOwners[event.rightClicked.uniqueId] ?: return
        medalRuntime[ownerId]?.onClick?.invoke(event.player)
        event.isCancelled = true
    }

    @Synchronized
    internal fun shutdown() {
        task?.cancel()
        task = null
        (medalRuntime.keys + particleRuntime.keys).toSet().forEach(::clearRuntime)
        repository.shutdown()
    }

    @Synchronized
    private fun runtimeTick() {
        tick++
        medalRuntime.entries.toList().forEach { (playerId, active) ->
            val player = Bukkit.getPlayer(playerId)
            if (player == null || !player.isOnline || !active.display.isValid) {
                clearMedal(playerId)
            } else {
                val definition = medals[active.id]
                if (definition == null || definition.revision != active.revision) {
                    refresh(playerId)
                } else {
                    val location = medalLocation(player, active.verticalOffset)
                    active.display.teleport(location)
                    active.interaction?.teleport(location)
                }
            }
        }

        var globalBudget = 512
        particleRuntime.entries.toList().forEach { (playerId, active) ->
            if (globalBudget <= 0) return@forEach
            val player = Bukkit.getPlayer(playerId)
            val definition = particles[active.id]
            if (player == null || !player.isOnline || definition == null) {
                particleRuntime.remove(playerId)
                return@forEach
            }
            if (definition.revision != active.revision) {
                refresh(playerId)
                return@forEach
            }
            val emissions = runCatching {
                active.program.tick(ParticleProgramContext(player, player.location.clone(), tick, active.facts))
            }.getOrElse {
                plugin.logger.warning("[Cosmetic] particle ${active.id.value} failed: ${it.message}")
                emptyList()
            }
            var playerBudget = 64
            emissions.forEach { emission ->
                if (globalBudget <= 0 || playerBudget <= 0) return@forEach
                val count = emission.count.coerceIn(0, minOf(globalBudget, playerBudget))
                if (count == 0) return@forEach
                val location = player.location.clone().add(emission.offsetX, emission.offsetY, emission.offsetZ)
                if (emission.data == null) {
                    player.world.spawnParticle(
                        emission.particle, location, count,
                        emission.spreadX, emission.spreadY, emission.spreadZ, emission.extra
                    )
                } else {
                    player.world.spawnParticle(
                        emission.particle, location, count,
                        emission.spreadX, emission.spreadY, emission.spreadZ, emission.extra, emission.data
                    )
                }
                globalBudget -= count
                playerBudget -= count
            }
        }
    }

    private fun medalLocation(player: Player, offset: Double) =
        player.location.clone().add(0.0, player.boundingBox.height + offset, 0.0)

    @Synchronized
    private fun refreshEquipped(id: CosmeticId, type: CosmeticType) {
        repository.allSnapshots().forEach { profile ->
            val equipped = if (type == CosmeticType.MEDAL) profile.equippedMedal else profile.equippedParticle
            if (equipped == id) refresh(profile.playerId)
        }
    }

    @Synchronized
    private fun clearRuntime(playerId: UUID) {
        clearMedal(playerId)
        particleRuntime.remove(playerId)
    }

    @Synchronized
    private fun clearMedal(playerId: UUID) {
        val active = medalRuntime.remove(playerId) ?: return
        interactionOwners.remove(active.interaction?.uniqueId)
        active.interaction?.remove()
        active.display.remove()
    }
}
