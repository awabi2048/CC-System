package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionContext
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuCloseContext
import com.awabi2048.ccsystem.api.gui.MenuCloseReason
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
import com.awabi2048.ccsystem.api.gui.PlayerInventoryInteraction
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
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin

internal class MenuRuntimeServiceImpl(
    private val plugin: JavaPlugin,
    private val navigation: MenuNavigationService,
    private val sounds: MenuSoundService,
    private val layouts: GuiLayoutService,
    private val presentations: MenuPresentationTracker,
) : MenuRuntimeService, Listener {
    private val closeReasons = MenuCloseReasonTracker<Inventory>()
    private val definitions = ConcurrentHashMap<RouteKey, InventoryMenuDefinition>()
    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val preserveCloseInventories = ConcurrentHashMap.newKeySet<Inventory>()
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
        val currentRoute = navigation.currentRoute(player) ?: return open(player, route)
        return navigateFrom(player, currentRoute, route)
    }

    override fun openEphemeral(player: Player, route: MenuRoute): Boolean =
        openDirect(player, route, playOpenSound = true, preserveHistory = true)

    override fun preserveHistoryOnClose(player: Player) {
        val inventory = player.openInventory.topInventory
        if (inventory.holder is MenuRuntimeHolder) {
            preserveCloseInventories += inventory
        }
    }

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
        if (!isDialogTransition(player)) {
            when (val openSound = request.openSound) {
                MenuSoundPolicy.Default -> sounds.onMenuOpen(player, request.route.id)
                MenuSoundPolicy.Silent -> Unit
                is MenuSoundPolicy.Custom -> sounds.play(player, openSound.sound)
            }
        }
        player.openInventory(request.inventory)
        presentations.markOpened(
            player,
            com.awabi2048.ccsystem.api.gui.MenuSurface.INVENTORY,
            request.route.owner,
            request.route.id,
        )
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
            definition.renderer.render(
                MenuRenderContext(player, session.route, navigation.canGoBack(player))
            )
        }.onFailure { failure ->
            plugin.logger.log(
                Level.SEVERE,
                "メニュー再描画に失敗しました: route=${definition.routeId} player=${player.uniqueId}",
                failure
            )
        }.getOrNull()
            ?.withHistoryNavigation(player)
            ?: return false
        val policy = inventoryPolicy(view, definition)
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
        closeInventory(player, MenuCloseReason.RUNTIME_CLOSED)
        presentations.markClosed(player)
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
            if (policy.playerInventoryInteraction == PlayerInventoryInteraction.BLOCKED) {
                event.isCancelled = true
                return
            }
            if (policy.playerInventoryInteraction == PlayerInventoryInteraction.INTERACTIVE) {
                if (PlayerInventoryTransferGuard.blocks(event.action)) event.isCancelled = true
                return
            }
            val session = sessions[player.uniqueId] ?: return
            if (session.route != holder.route) return
            val definition = definition(holder.route.owner, holder.route.id) ?: return
            val handler = definition.actions[MenuRuntimeActions.PLAYER_INVENTORY_CLICK] ?: return
            if (!PlayerInventoryActionAcceptance.accepts(
                    policy.capturesPlayerInventoryClick,
                    event.clickedInventory == player.inventory,
                    handlerPresent = true,
                    event.click,
                )
            ) return
            event.isCancelled = true
            if (!executing.add(player.uniqueId)) return
            val originRevision = presentations.current(player)?.revision
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
            applyResult(player, session, null, definition, MenuClickType.DEFAULT, result, originRevision)
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
        val originRevision = presentations.current(player)?.revision

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
        applyResult(player, session, element.sounds, definition, clickType, result, originRevision)
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
        val closeReason = closeReasons.consume(event.inventory)
        definition(holder.route.owner, holder.route.id)?.onClose?.let { handler ->
            runCatching {
                handler.handle(MenuCloseContext(player, holder.route, event.inventory, closeReason))
            }
                .onFailure { failure ->
                    plugin.logger.log(
                        Level.SEVERE,
                        "メニューClose処理に失敗しました: route=${holder.route.key()} player=${player.uniqueId}",
                        failure,
                    )
                }
        }
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val activePresentation = presentations.current(player)
            if (
                activePresentation != null &&
                activePresentation.surface != com.awabi2048.ccsystem.api.gui.MenuSurface.INVENTORY
            ) {
                return@Runnable
            }
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
            val preserveHistory = holder.preserveHistory || preserveCloseInventories.remove(event.inventory)
            if (removed && MenuSessionClosePolicy.shouldClearNavigation(preserveHistory)) {
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
        val view = runCatching {
            definition.renderer.render(MenuRenderContext(player, route, navigation.canGoBack(player)))
        }
            .onFailure { failure ->
                plugin.logger.log(Level.SEVERE, "メニュー描画に失敗しました: route=${definition.routeId}", failure)
            }
            .getOrNull()
            ?.withHistoryNavigation(player)
            ?: return false
        val policy = inventoryPolicy(view, definition)
        val holder = MenuRuntimeHolder(player.uniqueId, route, policy, preserveHistory)
        val inventory = Bukkit.createInventory(holder, view.size, view.title)
        holder.backingInventory = inventory
        applyView(inventory, view)
        sessions[player.uniqueId] = Session(route, view.elements.associateBy { it.slot }, preserveHistory)
        if (playOpenSound && !isDialogTransition(player)) sounds.onMenuOpen(player, route.id)
        val previousInventory = player.openInventory.topInventory
        val previousIsManaged = previousInventory.holder is MenuRuntimeHolder
        if (previousIsManaged) {
            closeReasons.mark(previousInventory, MenuCloseReason.ROUTE_REPLACED)
        }
        try {
            player.openInventory(inventory)
        } finally {
            if (previousIsManaged) {
                closeReasons.clear(previousInventory)
            }
        }
        presentations.markOpened(
            player,
            com.awabi2048.ccsystem.api.gui.MenuSurface.INVENTORY,
            route.owner,
            route.id,
        )
        return true
    }

    private fun applyView(inventory: org.bukkit.inventory.Inventory, view: InventoryMenuView) {
        if (view.standardFrame) layouts.applyStandardFrame(inventory)
        view.inputItems.forEach { (slot, item) -> inventory.setItem(slot, item.clone()) }
        view.elements.forEach { element -> inventory.setItem(element.slot, element.item.clone()) }
    }

    /**
     * 戻る要素の表示可否は画面実装へ委ねず、実際の履歴だけから決定する。
     * これにより、古い表示フラグやダイアログ復帰後の再描画が誤っていても、
     * 戻り先のないボタンや、戻り先があるのに消えるボタンをRuntime境界で防ぐ。
     */
    private fun InventoryMenuView.withHistoryNavigation(player: Player): InventoryMenuView {
        if (navigation.canGoBack(player)) return this
        val visibleElements = elements.filterNot { it.role == GuiElementRole.BACK }
        return if (visibleElements.size == elements.size) this else copy(elements = visibleElements)
    }

    private fun inventoryPolicy(
        view: InventoryMenuView,
        definition: InventoryMenuDefinition,
    ): GuiInventoryPolicy {
        val interaction = when {
            !view.allowPlayerInventoryInteraction -> PlayerInventoryInteraction.BLOCKED
            definition.actions.containsKey(MenuRuntimeActions.PLAYER_INVENTORY_CLICK) ->
                PlayerInventoryInteraction.SELECTION
            else -> PlayerInventoryInteraction.INTERACTIVE
        }
        return GuiInventoryPolicy(view.inputSlots, interaction)
    }

    private fun applyResult(
        player: Player,
        session: Session,
        elementSounds: com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy?,
        definition: InventoryMenuDefinition,
        clickType: MenuClickType,
        result: MenuActionResult,
        originRevision: Long?,
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
                val update = result.update
                if (!MenuStaleUpdatePolicy.shouldApply(
                        update,
                        originRevision,
                        presentations.current(player)?.revision,
                    )
                ) {
                    val current = presentations.current(player)
                    plugin.logger.warning(
                        "画面変更後の古いMenuUpdateを無視しました: " +
                            "route=${session.route.owner}:${session.route.id} update=${update::class.simpleName} " +
                            "originRevision=$originRevision currentRevision=${current?.revision} " +
                            "currentSurface=${current?.surface} current=${current?.owner}:${current?.id}"
                    )
                    return
                }
                when (update) {
                    MenuUpdate.None -> Unit
                    MenuUpdate.Refresh -> refresh(player)
                    MenuUpdate.Close -> close(player)
                    MenuUpdate.Back -> if (!back(player)) close(player)
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

    private fun isDialogTransition(player: Player): Boolean =
        presentations.current(player)?.surface ==
            com.awabi2048.ccsystem.api.gui.MenuSurface.DIALOG

    private fun closeMatching(owner: String, id: String?): Int {
        var closed = 0
        plugin.server.onlinePlayers.forEach { player ->
            val holder = player.openInventory.topInventory.holder as? MenuRuntimeHolder ?: return@forEach
            if (holder.route.owner == owner && (id == null || holder.route.id == id)) {
                closeInventory(player, MenuCloseReason.RUNTIME_CLOSED)
                sessions.remove(player.uniqueId)
                closed++
            }
        }
        return closed
    }

    private fun closeInventory(player: Player, reason: MenuCloseReason) {
        val inventory = player.openInventory.topInventory
        if (inventory.holder !is MenuRuntimeHolder) {
            player.closeInventory()
            return
        }
        closeReasons.mark(inventory, reason)
        try {
            player.closeInventory()
        } finally {
            closeReasons.clear(inventory)
        }
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

internal class MenuCloseReasonTracker<T : Any> {
    private val reasons = ConcurrentHashMap<T, MenuCloseReason>()

    fun mark(target: T, reason: MenuCloseReason) {
        reasons[target] = reason
    }

    fun consume(target: T): MenuCloseReason =
        reasons.remove(target) ?: MenuCloseReason.USER_DISMISSED

    fun clear(target: T) {
        reasons.remove(target)
    }
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

internal object PlayerInventoryTransferGuard {
    fun blocks(action: InventoryAction): Boolean =
        action == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
            action == InventoryAction.COLLECT_TO_CURSOR ||
            action == InventoryAction.UNKNOWN
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
