package com.awabi2048.ccsystem.features.resourceworld.manager

import com.awabi2048.ccsystem.CCSystem
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion
import java.util.concurrent.CompletableFuture
import net.luckperms.api.LuckPerms
import net.luckperms.api.node.types.PermissionNode
import org.bukkit.GameRule
import org.bukkit.World

/** 資源ワールド固有の権限を一括管理し、旧ワールドのコンテキストを残さない。 */
object ResourceWorldPermissionPolicy {
    private val plugin: CCSystem get() = CCSystem.instance

    fun apply(world: World): CompletableFuture<Void> {
        world.setGameRule(GameRule.GLOBAL_SOUND_EVENTS, false)
        applyWorldGuardDefaults(world)
        return mutateLuckPerms(world.name, add = true)
    }

    fun clear(worldName: String): CompletableFuture<Void> = mutateLuckPerms(worldName, add = false)

    private fun applyWorldGuardDefaults(world: World) {
        if (!plugin.server.pluginManager.isPluginEnabled("WorldGuard")) return
        val manager = WorldGuard.getInstance().platform.regionContainer.get(BukkitAdapter.adapt(world)) ?: return
        val region = manager.getRegion(GLOBAL_REGION_ID) ?: GlobalProtectedRegion(GLOBAL_REGION_ID).also(manager::addRegion)
        if (region.getFlag(Flags.PVP) == null) {
            region.setFlag(Flags.PVP, StateFlag.State.DENY)
            manager.saveChanges()
        }
    }

    private fun mutateLuckPerms(worldName: String, add: Boolean): CompletableFuture<Void> {
        val luckPerms = plugin.server.servicesManager.getRegistration(LuckPerms::class.java)?.provider
            ?: return CompletableFuture.completedFuture(null)
        return luckPerms.groupManager.loadGroup(DEFAULT_GROUP)
            .thenCompose { optional ->
                val group = optional.orElse(null) ?: return@thenCompose CompletableFuture.completedFuture(null)
                MANAGED_PERMISSIONS.forEach { (permission, value) ->
                    val node = PermissionNode.builder(permission)
                        .withContext("world", worldName)
                        .value(value)
                        .build()
                    if (add) group.data().add(node) else group.data().remove(node)
                }
                luckPerms.groupManager.saveGroup(group)
            }
            .whenComplete { _, error ->
                if (error != null) {
                    plugin.logger.warning("Failed to ${if (add) "apply" else "clear"} resource permissions for $worldName: ${error.message}")
                }
            }
    }

    private const val GLOBAL_REGION_ID = "__global__"
    private const val DEFAULT_GROUP = "default"

    private val SAFARINET_ENTITIES = listOf(
        "BLAZE", "BOGGED", "CAVE_SPIDER", "CREAKING", "CREEPER", "DROWNED",
        "ELDER_GUARDIAN", "ENDERMAN", "ENDERMITE", "EVOKER", "GHAST", "GUARDIAN",
        "HOGLIN", "HUSK", "MAGMA_CUBE", "PARCHED", "PHANTOM", "PIGLIN",
        "PIGLIN_BRUTE", "PILLAGER", "RAVAGER", "SHULKER", "SILVERFISH", "SKELETON",
        "SLIME", "SPIDER", "STRAY", "VEX", "VINDICATOR", "WARDEN", "WITCH", "WITHER",
        "WITHER_SKELETON", "ZOGLIN", "ZOMBIE", "ZOMBIE_NAUTILUS", "ZOMBIE_VILLAGER",
        "ZOMBIFIED_PIGLIN"
    )

    private val MANAGED_PERMISSIONS: Map<String, Boolean> = buildMap {
        put("cmi.keepinventory", false)
        put("cmi.keepexp", false)
        put("deadchest.generate", true)
        put("cmi.command.flyc", false)
        SAFARINET_ENTITIES.forEach { entity ->
            put("safarinet.catch.$entity", true)
            put("safarinet.release.$entity", true)
        }
    }
}
