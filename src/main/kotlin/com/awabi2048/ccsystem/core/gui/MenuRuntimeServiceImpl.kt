package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionContext
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuRenderContext
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.plugin.java.JavaPlugin

internal class MenuRuntimeServiceImpl(
    private val plugin: JavaPlugin,
    private val navigation: MenuNavigationService,
    private val sounds: MenuSoundService,
    private val layouts: GuiLayoutService,
) : MenuRuntimeService, Listener {
    private val definitions = ConcurrentHashMap<RouteKey, InventoryMenuDefinition>()
    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val executing = ConcurrentHashMap.newKeySet<UUID>()

    override fun register(definition: InventoryMenuDefinition) {
        val key = RouteKey(definition.owner, definition.id)
        check(definitions.putIfAbsent(key, definition) == null) {
            "Menu definition is already registered: ${definition.routeId}"
        }
        navigation.registerOpener(definition.owner, definition.id) { player, route ->
            if (definitions[key] == null) return@registerOpener false
            openDirect(player, route, playOpenSound = true)
        }
    }

    override fun unregister(owner: String, id: String) {
        definitions.remove(RouteKey(owner, id))
        closeMatching(owner, id)
    }

    override fun unregisterOwner(owner: String) {
        definitions.keys.removeIf { it.owner == owner }
        closeMatching(owner, null)
        navigation.unregisterOwner(owner)
    }

    override fun definitions(): List<InventoryMenuDefinition> =
        definitions.values.sortedWith(compareBy(InventoryMenuDefinition::owner, InventoryMenuDefinition::id))

    override fun definition(owner: String, id: String): InventoryMenuDefinition? =
        definitions[RouteKey(owner, id)]

    override fun open(player: Player, route: MenuRoute): Boolean = navigation.open(player, route)

    override fun refresh(player: Player): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        val holder = player.openInventory.topInventory.holder as? MenuRuntimeHolder
            ?: return openDirect(player, session.route, playOpenSound = false)
        if (holder.route != session.route) return false
        val definition = definition(session.route.owner, session.route.id) ?: return false
        val view = runCatching {
            definition.renderer.render(MenuRenderContext(player, session.route))
        }.onFailure { failure ->
            plugin.logger.log(
                Level.SEVERE,
                "メニュー再描画に失敗しました: route=${definition.routeId} player=${player.uniqueId}",
                failure
            )
        }.getOrNull() ?: return false
        val policy = GuiInventoryPolicy(view.inputSlots, view.allowPlayerInventoryInteraction)
        if (
            player.openInventory.topInventory.size != view.size ||
            player.openInventory.title() != view.title ||
            holder.guiInventoryPolicy() != policy
        ) {
            return openDirect(player, session.route, playOpenSound = false)
        }

        val inventory = player.openInventory.topInventory
        val inputItems = policy.inputSlots.associateWith { inventory.getItem(it)?.clone() }
        inventory.clear()
        applyView(inventory, view)
        inputItems.forEach { (slot, item) -> inventory.setItem(slot, item) }
        sessions[player.uniqueId] = Session(session.route, view.elements.associateBy { it.slot })
        return true
    }

    override fun back(player: Player): Boolean = navigation.openPrevious(player)

    override fun closeOwnedMenus(owner: String): Int = closeMatching(owner, null)

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? MenuRuntimeHolder ?: return
        val player = event.whoClicked as? Player ?: return
        val policy = holder.guiInventoryPolicy()
        if (holder.playerId != player.uniqueId) {
            event.isCancelled = true
            return
        }
        if (event.clickedInventory != event.view.topInventory) {
            if (!policy.allowPlayerInventoryInteraction) event.isCancelled = true
            return
        }
        if (policy.acceptsTopSlot(event.rawSlot)) return
        event.isCancelled = true

        val session = sessions[player.uniqueId] ?: return
        if (session.route != holder.route) return
        val element = session.elements[event.rawSlot] ?: return
        val actionId = element.actionId ?: return
        if (!MenuClickAcceptance.accepts(event.click)) return
        val definition = definition(holder.route.owner, holder.route.id) ?: return
        val clickType = clickType(element.role)
        if (!element.enabled) {
            playResolved(
                player,
                MenuSoundPolicy.Default,
                MenuSoundPolicyResolver.rejectedPolicy(element.sounds, definition.sounds),
                clickType,
            )
            return
        }
        val handler = definition.actions[actionId] ?: return
        if (!executing.add(player.uniqueId)) return

        val result = try {
            handler.handle(MenuActionContext(player, holder.route, actionId, element.actionPayload, event.click))
        } catch (failure: Throwable) {
            plugin.logger.log(
                Level.SEVERE,
                "メニューActionの実行に失敗しました: route=${definition.routeId} action=$actionId player=${player.uniqueId}",
                failure,
            )
            MenuActionResult.Rejected()
        } finally {
            executing.remove(player.uniqueId)
        }
        applyResult(player, session, element.sounds, definition, clickType, result)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? MenuRuntimeHolder ?: return
        val player = event.player as? Player ?: return
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val activeHolder = player.openInventory.topInventory.holder as? MenuRuntimeHolder
            if (activeHolder == null || activeHolder.route != holder.route) {
                sessions.remove(player.uniqueId, sessions[player.uniqueId])
            }
        })
    }

    private fun openDirect(player: Player, route: MenuRoute, playOpenSound: Boolean): Boolean {
        val definition = definition(route.owner, route.id) ?: return false
        val view = runCatching { definition.renderer.render(MenuRenderContext(player, route)) }
            .onFailure { failure ->
                plugin.logger.log(Level.SEVERE, "メニュー描画に失敗しました: route=${definition.routeId}", failure)
            }
            .getOrNull() ?: return false
        val policy = GuiInventoryPolicy(view.inputSlots, view.allowPlayerInventoryInteraction)
        val holder = MenuRuntimeHolder(player.uniqueId, route, policy)
        val inventory = Bukkit.createInventory(holder, view.size, view.title)
        holder.backingInventory = inventory
        applyView(inventory, view)
        sessions[player.uniqueId] = Session(route, view.elements.associateBy { it.slot })
        if (playOpenSound) sounds.onMenuOpen(player, definition.routeId)
        player.openInventory(inventory)
        return true
    }

    private fun applyView(inventory: org.bukkit.inventory.Inventory, view: InventoryMenuView) {
        if (view.standardFrame) layouts.applyStandardFrame(inventory)
        view.elements.forEach { element -> inventory.setItem(element.slot, element.item.clone()) }
    }

    private fun applyResult(
        player: Player,
        session: Session,
        elementSounds: com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy?,
        definition: InventoryMenuDefinition,
        clickType: MenuClickType,
        result: MenuActionResult,
    ) {
        when (result) {
            MenuActionResult.Ignored -> return
            is MenuActionResult.Rejected -> {
                playResolved(
                    player,
                    result.sound,
                    MenuSoundPolicyResolver.rejectedPolicy(elementSounds, definition.sounds),
                    clickType,
                )
                result.message?.let(player::sendMessage)
            }
            is MenuActionResult.Success -> {
                playResolved(
                    player,
                    result.sound,
                    MenuSoundPolicyResolver.successPolicy(elementSounds, definition.sounds),
                    clickType,
                )
                when (val update = result.update) {
                    MenuUpdate.None -> Unit
                    MenuUpdate.Refresh -> refresh(player)
                    MenuUpdate.Close -> player.closeInventory()
                    MenuUpdate.Back -> if (!navigation.openPrevious(player)) player.closeInventory()
                    is MenuUpdate.Replace -> navigation.open(player, update.route)
                    is MenuUpdate.Navigate -> navigation.pushAndOpen(player, session.route, update.route)
                }
            }
        }
    }

    private fun playResolved(
        player: Player,
        policy: MenuSoundPolicy,
        fallback: MenuSoundPolicy,
        clickType: MenuClickType,
    ) {
        MenuSoundPolicyResolver.resolve(policy, fallback, clickType)?.let { sounds.play(player, it) }
    }

    private fun clickType(role: GuiElementRole): MenuClickType = when (role) {
        GuiElementRole.CONFIRM -> MenuClickType.CONFIRM
        GuiElementRole.CANCEL, GuiElementRole.BACK -> MenuClickType.CANCEL
        GuiElementRole.NAVIGATION -> MenuClickType.NAVIGATION
        else -> MenuClickType.DEFAULT
    }

    private fun closeMatching(owner: String, id: String?): Int {
        var closed = 0
        plugin.server.onlinePlayers.forEach { player ->
            val holder = player.openInventory.topInventory.holder as? MenuRuntimeHolder ?: return@forEach
            if (holder.route.owner == owner && (id == null || holder.route.id == id)) {
                player.closeInventory()
                sessions.remove(player.uniqueId)
                closed++
            }
        }
        return closed
    }

    private data class RouteKey(val owner: String, val id: String)

    private data class Session(
        val route: MenuRoute,
        val elements: Map<Int, com.awabi2048.ccsystem.api.gui.MenuElement>,
    )
}

internal object MenuClickAcceptance {
    fun accepts(click: org.bukkit.event.inventory.ClickType): Boolean = click in setOf(
        org.bukkit.event.inventory.ClickType.LEFT,
        org.bukkit.event.inventory.ClickType.RIGHT,
        org.bukkit.event.inventory.ClickType.SHIFT_LEFT,
        org.bukkit.event.inventory.ClickType.SHIFT_RIGHT,
    )
}
