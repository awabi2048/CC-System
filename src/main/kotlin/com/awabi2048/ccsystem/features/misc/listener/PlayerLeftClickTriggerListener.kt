package com.awabi2048.ccsystem.features.misc.listener

import com.awabi2048.ccsystem.api.event.PlayerLeftClickPlayerEvent
import com.awabi2048.ccsystem.CCSystem
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerQuitEvent

class PlayerLeftClickTriggerListener : Listener {

    private val lastTriggeredTicks = HashMap<java.util.UUID, Int>()

    @EventHandler(ignoreCancelled = true)
    fun onPlayerAnimation(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) {
            return
        }

        val player = event.player
        val currentTick = Bukkit.getCurrentTick()
        if (lastTriggeredTicks[player.uniqueId] == currentTick) {
            return
        }
        val target = resolveTargetPlayer(player) ?: return
        lastTriggeredTicks[player.uniqueId] = currentTick
        Bukkit.getPluginManager().callEvent(PlayerLeftClickPlayerEvent(player, target))
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        lastTriggeredTicks.remove(event.player.uniqueId)
        CCSystem.getAPI().getPlayerInteractionClaimService().releaseAll(event.player.uniqueId)
    }

    private fun resolveTargetPlayer(player: Player): Player? {
        val maxDistance = (player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.value ?: 3.0)
            .coerceAtLeast(1.0)
        val eyeLocation = player.eyeLocation
        val hit = player.world.rayTrace(
            eyeLocation,
            eyeLocation.direction,
            maxDistance,
            FluidCollisionMode.NEVER,
            true,
            0.1
        ) { candidate ->
            candidate is Player && candidate.uniqueId != player.uniqueId
        }
        return hit?.hitEntity as? Player
    }
}
