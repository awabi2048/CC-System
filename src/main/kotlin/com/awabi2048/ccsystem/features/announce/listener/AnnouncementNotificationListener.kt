package com.awabi2048.ccsystem.features.announce.listener

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.data.PlayerDataManager
import com.awabi2048.ccsystem.features.announce.manager.AnnouncementManager
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import java.lang.reflect.Method
import java.time.Instant
import java.util.UUID

class AnnouncementNotificationListener : Listener {
    private data class AfkState(
        val isAfk: Boolean,
        val afkSince: Instant?
    )

    private val cmiAfkBridge = CmiAfkBridge()
    private val afkStateByPlayer = mutableMapOf<UUID, AfkState>()
    private var afkPollTask: BukkitTask? = null
    private var uncheckedReminderTask: BukkitTask? = null

    init {
        if (cmiAfkBridge.isAvailable()) {
            afkPollTask = CCSystem.instance.server.scheduler.runTaskTimer(
                CCSystem.instance,
                Runnable { pollAfkStates() },
                60L,
                60L
            )
            CCSystem.instance.logger.info("[Announce] CMI AFK連携を有効化しました。")
        } else {
            CCSystem.instance.logger.info("[Announce] CMI未検出のためAFK判定はスキップされます。")
        }

        val intervalMinutes = ConfigManager.getAnnounceUncheckedNotifyIntervalMinutes().coerceAtLeast(1L)
        val intervalTicks = intervalMinutes * 60L * 20L
        uncheckedReminderTask = CCSystem.instance.server.scheduler.runTaskTimer(
            CCSystem.instance,
            Runnable { sendUncheckedReminderMessages() },
            intervalTicks,
            intervalTicks
        )
    }

    fun shutdown() {
        afkPollTask?.cancel()
        afkPollTask = null
        uncheckedReminderTask?.cancel()
        uncheckedReminderTask = null
        afkStateByPlayer.clear()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        CCSystem.instance.server.scheduler.runTaskLater(
            CCSystem.instance,
            Runnable {
                if (!player.isOnline) {
                    return@Runnable
                }

                val lastLogoutAt = AnnouncementManager.parseInstant(
                    PlayerDataManager.getString(player.uniqueId, AnnouncementManager.PLAYER_DATA_LAST_LOGOUT_AT)
                )
                val lastCheckedAt = AnnouncementManager.parseInstant(
                    PlayerDataManager.getString(player.uniqueId, AnnouncementManager.PLAYER_DATA_LAST_CHECKED_AT)
                )
                val count = AnnouncementManager.getNotificationTargetAnnouncements(lastLogoutAt, lastCheckedAt).size
                sendNotifyMessages(player, count)
            },
            30L
        )

        if (cmiAfkBridge.isAvailable()) {
            val nowAfk = cmiAfkBridge.isAfk(player) ?: false
            afkStateByPlayer[player.uniqueId] = AfkState(nowAfk, if (nowAfk) Instant.now() else null)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        afkStateByPlayer.remove(event.player.uniqueId)
    }

    private fun pollAfkStates() {
        if (!cmiAfkBridge.isAvailable()) {
            return
        }

        val now = Instant.now()
        val onlineIds = CCSystem.instance.server.onlinePlayers.map { it.uniqueId }.toSet()
        afkStateByPlayer.keys.removeIf { it !in onlineIds }

        for (player in CCSystem.instance.server.onlinePlayers) {
            val currentAfk = cmiAfkBridge.isAfk(player) ?: continue
            val previous = afkStateByPlayer[player.uniqueId]

            if (previous == null) {
                afkStateByPlayer[player.uniqueId] = AfkState(currentAfk, if (currentAfk) now else null)
                continue
            }

            if (!previous.isAfk && currentAfk) {
                afkStateByPlayer[player.uniqueId] = AfkState(true, now)
                continue
            }

            if (previous.isAfk && !currentAfk) {
                val afkSince = previous.afkSince
                afkStateByPlayer[player.uniqueId] = AfkState(false, null)
                if (afkSince != null) {
                    val count = AnnouncementManager.getAnnouncementsUpdatedAfter(afkSince).size
                    sendNotifyMessages(player, count)
                }
            }
        }
    }

    private fun sendUncheckedReminderMessages() {
        for (player in CCSystem.instance.server.onlinePlayers) {
            if (!isNonAfk(player)) {
                continue
            }

            val lastCheckedAt = AnnouncementManager.parseInstant(
                PlayerDataManager.getString(player.uniqueId, AnnouncementManager.PLAYER_DATA_LAST_CHECKED_AT)
            )
            val count = AnnouncementManager.getUncheckedAnnouncements(lastCheckedAt).size
            sendNotifyMessages(player, count)
        }
    }

    private fun isNonAfk(player: Player): Boolean {
        if (!cmiAfkBridge.isAvailable()) {
            return true
        }
        return !(cmiAfkBridge.isAfk(player) ?: false)
    }

    private fun sendNotifyMessages(player: Player, count: Int) {
        if (count <= 0) {
            return
        }

        player.sendMessage(LanguageManager.getMessage(player, "announce.notify.toast_title", "count" to count.toString()))
        player.sendMessage(LanguageManager.getMessage(player, "announce.notify.toast_description"))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.1f)
    }
}

private class CmiAfkBridge {
    private val cmiClass: Class<*>? = runCatching { Class.forName("com.Zrips.CMI.CMI") }.getOrNull()
    private val getInstanceMethod: Method? = cmiClass?.methods?.firstOrNull {
        it.name == "getInstance" && it.parameterCount == 0
    }

    fun isAvailable(): Boolean {
        val pluginEnabled = Bukkit.getPluginManager().isPluginEnabled("CMI")
        return pluginEnabled && getInstanceMethod != null
    }

    fun isAfk(player: Player): Boolean? {
        if (!isAvailable()) {
            return null
        }

        return runCatching {
            val cmiInstance = getInstanceMethod?.invoke(null) ?: return null
            val playerManager = cmiInstance.javaClass.methods
                .firstOrNull { it.name == "getPlayerManager" && it.parameterCount == 0 }
                ?.invoke(cmiInstance) ?: return null

            val user = resolveUser(playerManager, player) ?: return null
            val afkMethod = user.javaClass.methods.firstOrNull {
                it.parameterCount == 0 && (it.name.equals("isAfk", ignoreCase = true) || it.name.equals("isAFK", ignoreCase = true))
            } ?: return null

            afkMethod.invoke(user) as? Boolean
        }.getOrNull()
    }

    private fun resolveUser(playerManager: Any, player: Player): Any? {
        val methods = playerManager.javaClass.methods.filter {
            it.name.equals("getUser", ignoreCase = true) && it.parameterCount == 1
        }

        for (method in methods) {
            val paramType = method.parameterTypes[0]
            val result = runCatching {
                when {
                    Player::class.java.isAssignableFrom(paramType) -> method.invoke(playerManager, player)
                    UUID::class.java.isAssignableFrom(paramType) -> method.invoke(playerManager, player.uniqueId)
                    String::class.java.isAssignableFrom(paramType) -> method.invoke(playerManager, player.name)
                    else -> null
                }
            }.getOrNull()

            if (result != null) {
                return result
            }
        }
        return null
    }
}
