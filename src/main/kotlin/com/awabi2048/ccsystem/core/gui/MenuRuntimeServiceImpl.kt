package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionContext
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuCloseContext
import com.awabi2048.ccsystem.api.gui.ManagedInventoryMenuRequest
import com.awabi2048.ccsystem.api.gui.ManagedMenuInteraction
import com.awabi2048.ccsystem.api.gui.ManagedMenuInteractionOutcome
import com.awabi2048.ccsystem.api.gui.ManagedMenuTransition
import com.awabi2048.ccsystem.api.gui.MenuRenderContext
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuRuntimeActions
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
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
    private val suppressOpenSound = ConcurrentHashMap.newKeySet<UUID>()
    private val presentedInventories = java.util.Collections.synchronizedMap(
        java.util.IdentityHashMap<org.bukkit.inventory.Inventory, ManagedPresentation>()
    )

    override fun register(definition: InventoryMenuDefinition) {
        val key = RouteKey(definition.owner, definition.id)
        check(definitions.putIfAbsent(key, definition) == null) {
            "Menu definition is already registered: ${definition.routeId}"
        }
        navigation.registerOpener(definition.owner, definition.id) { player, route ->
            if (definitions[key] == null) return@registerOpener false
            openDirect(player, route, playOpenSound = player.uniqueId !in suppressOpenSound)
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

    override fun open(player: Player, route: MenuRoute): Boolean = navigation.openRoot(player, route)

    override fun replace(player: Player, route: MenuRoute): Boolean =
        withoutOpenSound(player) { navigation.open(player, route) }

    override fun navigate(player: Player, route: MenuRoute): Boolean {
        val currentRoute = navigation.currentRoute(player) ?: return replace(player, route)
        return navigateFrom(player, currentRoute, route)
    }

    override fun openEphemeral(player: Player, route: MenuRoute): Boolean =
        openDirect(player, route, playOpenSound = true, preserveHistory = true)

    override fun present(player: Player, request: ManagedInventoryMenuRequest): Boolean {
        val transition = resolveTransition(player, request)
        when (transition) {
            ManagedMenuTransition.AUTOMATIC -> error("AUTOMATIC transition must be resolved before presentation")
            ManagedMenuTransition.ROOT -> {
                navigation.clear(player)
                navigation.recordCurrentRoute(player, request.route)
            }
            ManagedMenuTransition.REPLACE -> navigation.recordCurrentRoute(player, request.route)
            ManagedMenuTransition.NAVIGATE -> {
                navigation.currentRoute(player)?.let { navigation.push(player, it) }
                navigation.recordCurrentRoute(player, request.route)
            }
            ManagedMenuTransition.PRESERVE_HISTORY -> Unit
        }
        navigation.registerInventory(request.route.owner, request.inventory, request.policy)
        presentedInventories[request.inventory] = ManagedPresentation(player.uniqueId, request.route)
        when (val openSound = request.openSound) {
            MenuSoundPolicy.Default -> sounds.onMenuOpen(player, request.route.id)
            MenuSoundPolicy.Silent -> Unit
            is MenuSoundPolicy.Custom -> sounds.play(player, openSound.sound)
        }
        player.openInventory(request.inventory)
        return true
    }

    override fun feedback(player: Player, interaction: ManagedMenuInteraction) {
        val route = currentPresentedRoute(player) ?: navigation.currentRoute(player)
        val fallback = when (interaction.outcome) {
            ManagedMenuInteractionOutcome.SUCCESS -> MenuSoundPolicy.Default
            ManagedMenuInteractionOutcome.REJECTED -> MenuSoundPolicy.Custom(
                com.awabi2048.ccsystem.api.gui.MenuSound("BLOCK_NOTE_BLOCK_BASS", pitch = 0.8f)
            )
        }
        when (val resolved = interaction.sound) {
            MenuSoundPolicy.Silent -> Unit
            is MenuSoundPolicy.Custom -> sounds.play(player, resolved.sound)
            MenuSoundPolicy.Default -> when (fallback) {
                is MenuSoundPolicy.Custom -> sounds.play(player, fallback.sound)
                else -> sounds.onMenuClick(player, route?.id, interaction.clickType)
            }
        }
    }

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
        sessions[player.uniqueId] = Session(
            session.route,
            view.elements.associateBy { it.slot },
            session.preserveHistory,
        )
        return true
    }

    override fun close(player: Player) {
        player.closeInventory()
    }

    override fun back(player: Player): Boolean =
        withoutOpenSound(player) { navigation.openPrevious(player) }

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
            if (!policy.allowPlayerInventoryInteraction) {
                event.isCancelled = true
                return
            }
            val session = sessions[player.uniqueId] ?: return
            if (session.route != holder.route) return
            val definition = definition(holder.route.owner, holder.route.id) ?: return
            val handler = definition.actions[MenuRuntimeActions.PLAYER_INVENTORY_CLICK] ?: return
            if (!PlayerInventoryActionAcceptance.accepts(
                    policy.allowPlayerInventoryInteraction,
                    event.clickedInventory == player.inventory,
                    handlerPresent = true,
                    event.click,
                )
            ) return
            event.isCancelled = true
            if (!executing.add(player.uniqueId)) return
            val result = try {
                handler.handle(
                    MenuActionContext(
                        player = player,
                        route = holder.route,
                        actionId = MenuRuntimeActions.PLAYER_INVENTORY_CLICK,
                        payload = mapOf(
                            MenuRuntimeActions.PLAYER_INVENTORY_SLOT_PAYLOAD to event.slot.toString(),
                        ),
                        click = event.click,
                        item = (event.currentItem ?: ItemStack(Material.AIR)).clone(),
                        cursor = event.cursor.clone(),
                    ),
                )
            } catch (failure: Throwable) {
                plugin.logger.log(
                    Level.SEVERE,
                    "プレイヤーインベントリActionの実行に失敗しました: route=${definition.routeId} player=${player.uniqueId}",
                    failure,
                )
                MenuActionResult.Rejected()
            } finally {
                executing.remove(player.uniqueId)
            }
            applyResult(player, session, null, definition, MenuClickType.DEFAULT, result)
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
            handler.handle(
                MenuActionContext(
                    player,
                    holder.route,
                    actionId,
                    element.actionPayload,
                    event.click,
                    (event.currentItem ?: element.item).clone(),
                    event.cursor.clone(),
                ),
            )
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
        val presentation = presentedInventories.remove(event.inventory)
        if (presentation != null) {
            navigation.unregisterInventory(event.inventory)
            val player = event.player as? Player ?: return
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val activeRoute = currentPresentedRoute(player)
                if (
                    ManagedPresentationClosePolicy.shouldClear(
                        player.uniqueId,
                        presentation.playerId,
                        activeRoute,
                        navigation.currentRoute(player),
                        presentation.route,
                    )
                ) {
                    navigation.clear(player)
                }
            })
            return
        }
        val holder = event.inventory.holder as? MenuRuntimeHolder ?: return
        val player = event.player as? Player ?: return
        definition(holder.route.owner, holder.route.id)?.onClose?.let { handler ->
            runCatching { handler.handle(MenuCloseContext(player, holder.route)) }
                .onFailure { failure ->
                    plugin.logger.log(
                        Level.SEVERE,
                        "メニューClose処理に失敗しました: route=${holder.route.key()} player=${player.uniqueId}",
                        failure,
                    )
                }
        }
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val activeHolder = player.openInventory.topInventory.holder as? MenuRuntimeHolder
            var removed = false
            sessions.computeIfPresent(player.uniqueId) { _, session ->
                if (MenuSessionClosePolicy.shouldRemove(holder.route, activeHolder?.route, session.route)) {
                    removed = true
                    null
                } else {
                    session
                }
            }
            if (removed && MenuSessionClosePolicy.shouldClearNavigation(holder.preserveHistory)) {
                navigation.clear(player)
            }
        })
    }

    private fun openDirect(
        player: Player,
        route: MenuRoute,
        playOpenSound: Boolean,
        preserveHistory: Boolean = false,
    ): Boolean {
        val definition = definition(route.owner, route.id) ?: return false
        val view = runCatching { definition.renderer.render(MenuRenderContext(player, route)) }
            .onFailure { failure ->
                plugin.logger.log(Level.SEVERE, "メニュー描画に失敗しました: route=${definition.routeId}", failure)
            }
            .getOrNull() ?: return false
        val policy = GuiInventoryPolicy(view.inputSlots, view.allowPlayerInventoryInteraction)
        val holder = MenuRuntimeHolder(player.uniqueId, route, policy, preserveHistory)
        val inventory = Bukkit.createInventory(holder, view.size, view.title)
        holder.backingInventory = inventory
        applyView(inventory, view)
        sessions[player.uniqueId] = Session(route, view.elements.associateBy { it.slot }, preserveHistory)
        if (playOpenSound) sounds.onMenuOpen(player, route.id)
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
                    MenuUpdate.Back -> if (!back(player)) player.closeInventory()
                    is MenuUpdate.Replace -> replace(player, update.route)
                    is MenuUpdate.Navigate -> navigateFrom(player, session.route, update.route)
                }
            }
        }
    }

    private fun navigateFrom(player: Player, currentRoute: MenuRoute, targetRoute: MenuRoute): Boolean =
        withoutOpenSound(player) { navigation.pushAndOpen(player, currentRoute, targetRoute) }

    private fun <T> withoutOpenSound(player: Player, action: () -> T): T {
        suppressOpenSound.add(player.uniqueId)
        return try {
            action()
        } finally {
            suppressOpenSound.remove(player.uniqueId)
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

    private fun resolveTransition(
        player: Player,
        request: ManagedInventoryMenuRequest,
    ): ManagedMenuTransition {
        val current = currentPresentedRoute(player)
            ?: (player.openInventory.topInventory.holder as? MenuRuntimeHolder)?.route
            ?: navigation.currentRoute(player)
        return ManagedTransitionResolver.resolve(request.transition, current, request.route)
    }

    private fun currentPresentedRoute(player: Player): MenuRoute? =
        presentedInventories[player.openInventory.topInventory]
            ?.takeIf { it.playerId == player.uniqueId }
            ?.route

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
        val preserveHistory: Boolean,
    )

    private data class ManagedPresentation(
        val playerId: UUID,
        val route: MenuRoute,
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

internal object MenuSessionClosePolicy {
    fun shouldRemove(closedRoute: MenuRoute, activeRoute: MenuRoute?, sessionRoute: MenuRoute): Boolean =
        sessionRoute == closedRoute && activeRoute != closedRoute

    fun shouldClearNavigation(preserveHistory: Boolean): Boolean = !preserveHistory
}

internal object PlayerInventoryActionAcceptance {
    fun accepts(
        interactionAllowed: Boolean,
        playerInventoryClicked: Boolean,
        handlerPresent: Boolean,
        click: org.bukkit.event.inventory.ClickType,
    ): Boolean =
        interactionAllowed &&
            playerInventoryClicked &&
            handlerPresent &&
            MenuClickAcceptance.accepts(click)
}

internal object ManagedTransitionResolver {
    fun resolve(
        requested: ManagedMenuTransition,
        current: MenuRoute?,
        target: MenuRoute,
    ): ManagedMenuTransition {
        if (requested != ManagedMenuTransition.AUTOMATIC) return requested
        return when {
            current == null -> ManagedMenuTransition.ROOT
            current == target -> ManagedMenuTransition.REPLACE
            else -> ManagedMenuTransition.NAVIGATE
        }
    }
}

internal object ManagedPresentationClosePolicy {
    fun shouldClear(
        closingPlayerId: UUID,
        presentationPlayerId: UUID,
        activeRoute: MenuRoute?,
        currentRoute: MenuRoute?,
        closedRoute: MenuRoute,
    ): Boolean =
        closingPlayerId == presentationPlayerId &&
            activeRoute == null &&
            currentRoute == closedRoute
}
