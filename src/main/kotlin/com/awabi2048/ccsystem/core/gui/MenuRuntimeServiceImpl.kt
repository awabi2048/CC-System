package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionContext
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuCloseContext
import com.awabi2048.ccsystem.api.gui.MenuCloseReason
import com.awabi2048.ccsystem.api.gui.MenuContractValidator
import com.awabi2048.ccsystem.api.gui.ManagedInventoryMenuRequest
import com.awabi2048.ccsystem.api.gui.ManagedMenuInteraction
import com.awabi2048.ccsystem.api.gui.ManagedMenuInteractionOutcome
import com.awabi2048.ccsystem.api.gui.ManagedMenuTransition
import com.awabi2048.ccsystem.api.gui.MenuRenderContext
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuRuntimeActions
import com.awabi2048.ccsystem.api.gui.MenuRuntimeActionResultKind
import com.awabi2048.ccsystem.api.gui.MenuRuntimeBranchSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickDisposition
import com.awabi2048.ccsystem.api.gui.MenuRuntimeClickTrace
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInteractionKind
import com.awabi2048.ccsystem.api.gui.MenuRuntimeRouteSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeSlotKind
import com.awabi2048.ccsystem.api.gui.MenuRuntimeSlotSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateKind
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateSnapshot
import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuSurface
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import com.awabi2048.ccsystem.api.gui.PlayerInventoryInteraction
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin

internal class MenuRuntimeServiceImpl(
    private val plugin: JavaPlugin,
    private val navigation: MenuNavigationService,
    private val sounds: MenuSoundService,
    private val layouts: GuiLayoutService,
    private val presentations: MenuPresentationTracker,
    private val capabilities: com.awabi2048.ccsystem.api.gui.MenuCapabilityService,
) : MenuRuntimeService, Listener {
    private val closeReasons = MenuCloseReasonTracker<Inventory>()
    private val definitions = ConcurrentHashMap<RouteKey, InventoryMenuDefinition>()
    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val preserveCloseInventories = ConcurrentHashMap.newKeySet<Inventory>()
    private val externalSuspensions = ConcurrentHashMap<UUID, MenuRoute>()
    private val executing = ConcurrentHashMap.newKeySet<UUID>()
    private val suppressOpenSound = ConcurrentHashMap.newKeySet<UUID>()
    private val clickTraces = MenuRuntimeClickTraceStore()
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
        clickTraces.clearOwner(owner)
    }

    override fun unregisterOwner(owner: String) {
        definitions.keys.removeIf { it.owner == owner }
        closeMatching(owner, null)
        navigation.unregisterOwner(owner)
        clickTraces.clearOwner(owner)
    }

    override fun definitions(): List<InventoryMenuDefinition> =
        definitions.values.sortedWith(compareBy(InventoryMenuDefinition::owner, InventoryMenuDefinition::id))

    override fun definition(owner: String, id: String): InventoryMenuDefinition? =
        definitions[RouteKey(owner, id)]

    override fun snapshot(player: Player): MenuRuntimeSnapshot? = snapshotCurrent(player)

    override fun startClickTraceRun(player: Player): String =
        clickTraces.start(player.uniqueId)

    override fun startClickTraceRun(player: Player, runId: String): String =
        clickTraces.start(player.uniqueId, runId)

    override fun clickTraces(player: Player): List<MenuRuntimeClickTrace> =
        clickTraces.all(player.uniqueId)

    override fun latestClickTrace(player: Player): MenuRuntimeClickTrace? =
        clickTraces.latest(player.uniqueId)

    override fun clearClickTraces(player: Player) {
        clickTraces.clear(player.uniqueId)
    }

    override fun open(player: Player, route: MenuRoute): Boolean {
        completeExternal(player)
        return navigation.openRoot(player, route)
    }

    override fun replace(player: Player, route: MenuRoute): Boolean {
        completeExternal(player)
        return withoutOpenSound(player) { navigation.open(player, route) }
    }

    override fun reopenCurrent(player: Player): Boolean {
        val route = navigation.currentRoute(player) ?: return false
        return replace(player, route)
    }

    override fun navigate(player: Player, route: MenuRoute): Boolean {
        completeExternal(player)
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

    override fun suspendForExternal(player: Player): Boolean {
        val route = navigation.currentRoute(player) ?: return false
        externalSuspensions[player.uniqueId] = route
        preserveHistoryOnClose(player)
        closeInventory(player, MenuCloseReason.ROUTE_REPLACED)
        presentations.markClosed(player)
        return true
    }

    override fun resumeFromExternal(player: Player): Boolean {
        val route = externalSuspensions.remove(player.uniqueId) ?: return false
        if (navigation.currentRoute(player) != route) return false
        return reopenCurrent(player)
    }

    override fun finishExternal(player: Player): Boolean {
        val route = externalSuspensions.remove(player.uniqueId) ?: return false
        if (navigation.currentRoute(player) != route) return false
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (player.isOnline && navigation.currentRoute(player) == route) {
                reopenCurrent(player)
            }
        })
        return true
    }

    override fun completeExternal(player: Player) {
        externalSuspensions.remove(player.uniqueId)
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
        val policy = inventoryPolicy(view)
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
            view.standardFrame,
            policy.inputSlots,
        )
        presentations.markRefreshed(player)
        return true
    }

    override fun close(player: Player) {
        completeExternal(player)
        closeInventory(player, MenuCloseReason.RUNTIME_CLOSED)
        presentations.markClosed(player)
    }

    override fun clear(player: Player) {
        completeExternal(player)
        sessions.remove(player.uniqueId)
        navigation.clear(player)
        presentations.markClosed(player)
    }

    override fun back(player: Player): Boolean {
        completeExternal(player)
        return withoutOpenSound(player) { navigation.openPrevious(player) }
    }

    override fun closeOwnedMenus(owner: String): Int = closeMatching(owner, null)

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? MenuRuntimeHolder ?: return
        val player = event.whoClicked as? Player ?: return
        val trace = beginClickTrace(player, event.rawSlot, event.click, holder.route)
        val policy = holder.guiInventoryPolicy()
        if (holder.playerId != player.uniqueId) {
            event.isCancelled = true
            recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.OWNER_MISMATCH)
            return
        }
        if (event.clickedInventory != event.view.topInventory) {
            if (policy.playerInventoryInteraction == PlayerInventoryInteraction.BLOCKED) {
                event.isCancelled = true
                recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.PLAYER_INVENTORY_BLOCKED)
                return
            }
            if (policy.playerInventoryInteraction == PlayerInventoryInteraction.INTERACTIVE) {
                if (PlayerInventoryTransferGuard.blocks(event.action)) event.isCancelled = true
                recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.PLAYER_INVENTORY_INTERACTIVE)
                return
            }
            val session = sessions[player.uniqueId] ?: run {
                recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.NO_SESSION)
                return
            }
            if (session.route != holder.route) {
                recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.ROUTE_MISMATCH)
                return
            }
            val definition = definition(holder.route.owner, holder.route.id) ?: run {
                recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.MISSING_HANDLER)
                return
            }
            val handler = definition.actions[MenuRuntimeActions.PLAYER_INVENTORY_CLICK] ?: run {
                recordClickTrace(
                    trace,
                    player,
                    event.isCancelled,
                    MenuRuntimeClickDisposition.PLAYER_INVENTORY_UNACCEPTED,
                    interactionKind = MenuRuntimeInteractionKind.PLAYER_INVENTORY,
                    actionId = MenuRuntimeActions.PLAYER_INVENTORY_CLICK,
                    payload = mapOf(MenuRuntimeActions.PLAYER_INVENTORY_SLOT_PAYLOAD to event.slot.toString()),
                    safety = MenuActionSafety.INPUT_OR_EXTERNAL_SURFACE,
                )
                return
            }
            if (!PlayerInventoryActionAcceptance.accepts(
                    policy.capturesPlayerInventoryClick,
                    event.clickedInventory == player.inventory,
                    handlerPresent = true,
                    event.click,
                )
            ) {
                recordClickTrace(
                    trace,
                    player,
                    event.isCancelled,
                    MenuRuntimeClickDisposition.PLAYER_INVENTORY_UNACCEPTED,
                    interactionKind = MenuRuntimeInteractionKind.PLAYER_INVENTORY,
                    actionId = MenuRuntimeActions.PLAYER_INVENTORY_CLICK,
                    payload = mapOf(MenuRuntimeActions.PLAYER_INVENTORY_SLOT_PAYLOAD to event.slot.toString()),
                    safety = MenuActionSafety.INPUT_OR_EXTERNAL_SURFACE,
                )
                return
            }
            event.isCancelled = true
            if (!executing.add(player.uniqueId)) {
                recordClickTrace(
                    trace,
                    player,
                    event.isCancelled,
                    MenuRuntimeClickDisposition.EXECUTING,
                    accepted = true,
                    interactionKind = MenuRuntimeInteractionKind.PLAYER_INVENTORY,
                    actionId = MenuRuntimeActions.PLAYER_INVENTORY_CLICK,
                    payload = mapOf(MenuRuntimeActions.PLAYER_INVENTORY_SLOT_PAYLOAD to event.slot.toString()),
                    safety = MenuActionSafety.INPUT_OR_EXTERNAL_SURFACE,
                )
                return
            }
            val originRevision = presentations.current(player)?.revision
            var actionFailure: Throwable? = null
            val result = try {
                handler.handle(
                    MenuActionContext(
                        player = player,
                        route = holder.route,
                        actionId = MenuRuntimeActions.PLAYER_INVENTORY_CLICK,
                        slot = event.slot,
                        payload = mapOf(
                            MenuRuntimeActions.PLAYER_INVENTORY_SLOT_PAYLOAD to event.slot.toString(),
                        ),
                        click = event.click,
                        item = (event.currentItem ?: ItemStack(Material.AIR)).clone(),
                        cursor = event.cursor.clone(),
                    ),
                )
            } catch (failure: Throwable) {
                actionFailure = failure
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
            recordClickTrace(
                trace,
                player,
                event.isCancelled,
                if (actionFailure == null) MenuRuntimeClickDisposition.HANDLED else MenuRuntimeClickDisposition.EXCEPTION,
                accepted = true,
                result = result,
                exception = actionFailure,
                interactionKind = MenuRuntimeInteractionKind.PLAYER_INVENTORY,
                actionId = MenuRuntimeActions.PLAYER_INVENTORY_CLICK,
                payload = mapOf(MenuRuntimeActions.PLAYER_INVENTORY_SLOT_PAYLOAD to event.slot.toString()),
                safety = MenuActionSafety.INPUT_OR_EXTERNAL_SURFACE,
            )
            return
        }
        if (policy.acceptsTopSlot(event.rawSlot)) {
            recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.INPUT)
            return
        }
        event.isCancelled = true

        val session = sessions[player.uniqueId] ?: run {
            recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.NO_SESSION)
            return
        }
        if (session.route != holder.route) {
            recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.ROUTE_MISMATCH)
            return
        }
        val element = session.elements[event.rawSlot] ?: run {
            recordClickTrace(
                trace,
                player,
                event.isCancelled,
                session.slotKind(event.view.topInventory, event.rawSlot).toClickDisposition(),
            )
            return
        }
        if (!MenuClickAcceptance.accepts(event.click)) {
            recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.UNSUPPORTED_CLICK)
            return
        }
        val definition = definition(holder.route.owner, holder.route.id) ?: run {
            recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.MISSING_HANDLER)
            return
        }
        val clickType = clickType(element.role)
        val interaction = element.resolvedInteraction()
        when (interaction) {
            MenuInteraction.DisplayOnly -> {
                recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.DISPLAY_ONLY, interaction = interaction)
                return
            }
            is MenuInteraction.Unavailable -> {
                if (event.click !in interaction.acceptedClicks) {
                    recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.UNACCEPTED, interaction = interaction)
                    return
                }
                playResolved(
                    player,
                    MenuSoundPolicy.Default,
                    MenuSoundPolicyResolver.rejectedPolicy(interaction.sounds, definition.sounds),
                    clickType,
                )
                interaction.message?.let(player::sendMessage)
                recordClickTrace(
                    trace,
                    player,
                    event.isCancelled,
                    MenuRuntimeClickDisposition.UNAVAILABLE,
                    accepted = true,
                    interaction = interaction,
                    result = MenuActionResult.Rejected(),
                )
                return
            }
            is MenuInteraction.Back -> {
                if (event.click !in interaction.acceptedClicks) {
                    recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.UNACCEPTED, interaction = interaction)
                    return
                }
                playResolved(
                    player,
                    MenuSoundPolicy.Default,
                    MenuSoundPolicyResolver.successPolicy(interaction.sounds, definition.sounds),
                    clickType,
                )
                val update = if (back(player)) {
                    MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.BACK)
                } else {
                    close(player)
                    MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.CLOSE)
                }
                recordClickTrace(
                    trace,
                    player,
                    event.isCancelled,
                    MenuRuntimeClickDisposition.BACK,
                    accepted = true,
                    interaction = interaction,
                    result = MenuActionResult.Success(MenuUpdate.None),
                    update = update,
                )
                return
            }
            is MenuInteraction.Action -> if (event.click !in interaction.acceptedClicks) {
                recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.UNACCEPTED, interaction = interaction)
                return
            }
            is MenuInteraction.Branches ->
                if (interaction.branches.none { event.click in it.acceptedClicks }) {
                    recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.UNACCEPTED, interaction = interaction)
                    return
                }
            is MenuInteraction.Capability ->
                if (event.click !in interaction.acceptedClicks) {
                    recordClickTrace(trace, player, event.isCancelled, MenuRuntimeClickDisposition.UNACCEPTED, interaction = interaction)
                    return
                }
        }
        if (!executing.add(player.uniqueId)) {
            recordClickTrace(
                trace,
                player,
                event.isCancelled,
                MenuRuntimeClickDisposition.EXECUTING,
                accepted = true,
                interaction = interaction,
            )
            return
        }
        val originRevision = presentations.current(player)?.revision
        var actionFailure: Throwable? = null

        val result = try {
            when (interaction) {
                is MenuInteraction.Action -> {
                    val handler = definition.actions[interaction.actionId] ?: run {
                        recordClickTrace(
                            trace,
                            player,
                            event.isCancelled,
                            MenuRuntimeClickDisposition.MISSING_HANDLER,
                            accepted = true,
                            interaction = interaction,
                        )
                        return
                    }
                    handler.handle(
                        MenuActionContext(
                            player,
                            holder.route,
                            interaction.actionId,
                            event.slot,
                            interaction.payload,
                            event.click,
                            (event.currentItem ?: element.item).clone(),
                            event.cursor.clone(),
                        ),
                    )
                }
                is MenuInteraction.Branches -> {
                    val branch = interaction.branches.single { event.click in it.acceptedClicks }
                    val handler = definition.actions[branch.actionId] ?: run {
                        recordClickTrace(
                            trace,
                            player,
                            event.isCancelled,
                            MenuRuntimeClickDisposition.MISSING_HANDLER,
                            accepted = true,
                            interaction = interaction,
                        )
                        return
                    }
                    handler.handle(
                        MenuActionContext(
                            player,
                            holder.route,
                            branch.actionId,
                            event.slot,
                            branch.payload,
                            event.click,
                            (event.currentItem ?: element.item).clone(),
                            event.cursor.clone(),
                        ),
                    )
                }
                is MenuInteraction.Capability ->
                    capabilities.execute(
                        interaction.capabilityId,
                        player,
                        event.click,
                        interaction.arguments,
                        interaction.attributes,
                    )
            }
        } catch (failure: Throwable) {
            actionFailure = failure
            plugin.logger.log(
                Level.SEVERE,
                "メニュー操作の実行に失敗しました: route=${definition.routeId} interaction=${interaction.javaClass.simpleName} player=${player.uniqueId}",
                failure,
            )
            MenuActionResult.Rejected()
        } finally {
            executing.remove(player.uniqueId)
        }
        val interactionSounds = when (interaction) {
            is MenuInteraction.Action -> interaction.sounds
            is MenuInteraction.Branches -> interaction.sounds
            is MenuInteraction.Capability -> interaction.sounds
        }
        applyResult(
            player,
            session,
            interactionSounds,
            definition,
            clickType,
            result,
            originRevision,
        )
        recordClickTrace(
            trace,
            player,
            event.isCancelled,
            if (actionFailure == null) MenuRuntimeClickDisposition.HANDLED else MenuRuntimeClickDisposition.EXCEPTION,
            accepted = true,
            interaction = interaction,
            result = result,
            exception = actionFailure,
        )
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

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clickTraces.clear(event.player.uniqueId)
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
            ?.also { rendered -> MenuContractValidator.requireValid(definition, rendered) }
            ?.withHistoryNavigation(player)
            ?: return false
        val policy = inventoryPolicy(view)
        val holder = MenuRuntimeHolder(player.uniqueId, route, policy, preserveHistory)
        val inventory = Bukkit.createInventory(holder, view.size, view.title)
        holder.backingInventory = inventory
        applyView(inventory, view)
        sessions[player.uniqueId] = Session(
            route,
            view.elements.associateBy { it.slot },
            preserveHistory,
            view.standardFrame,
            policy.inputSlots,
        )
        if (playOpenSound && !isDialogTransition(player)) sounds.onMenuOpen(player, route.id)
        val previousInventory = player.openInventory.topInventory
        val previousIsManaged = previousInventory.holder is MenuRuntimeHolder
        if (previousIsManaged) {
            closeReasons.mark(previousInventory, MenuCloseReason.ROUTE_REPLACED)
            preserveCloseInventories += previousInventory
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

    private fun inventoryPolicy(view: InventoryMenuView): GuiInventoryPolicy =
        GuiInventoryPolicy(view.inputSlots, view.playerInventoryInteraction)

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
                    return
                }
                when (update) {
                    MenuUpdate.None -> Unit
                    MenuUpdate.Refresh -> refresh(player)
                    MenuUpdate.Resume -> finishExternal(player)
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

    private fun snapshotCurrent(player: Player): MenuRuntimeSnapshot? {
        val top = player.openInventory.topInventory
        val holder = top.holder as? MenuRuntimeHolder
        val session = sessions[player.uniqueId]
            ?.takeIf { holder?.route == it.route && holder.playerId == player.uniqueId }
        val presented = currentPresentedRoute(player)
        val presentation = presentations.current(player)
        val route = session?.route ?: presented ?: navigation.currentRoute(player) ?: presentation
            ?.takeIf { it.owner != null && it.id != null }
            ?.let { MenuRoute(requireNotNull(it.owner), requireNotNull(it.id)) }
            ?: return null
        val surface = presentation?.surface ?: MenuSurface.INVENTORY
        val revision = presentation?.revision ?: 0L
        if (surface != MenuSurface.INVENTORY) {
            return MenuRuntimeSnapshot(
                player.uniqueId,
                route.runtimeSnapshot(),
                navigation.breadcrumbs(player).map { breadcrumb -> breadcrumb.runtimeSnapshot() },
                navigation.canGoBack(player),
                surface,
                Component.empty(),
                0,
                revision,
                emptyList(),
            )
        }
        val slots = (0 until top.size).map { slot ->
            snapshotSlot(top, session, slot)
        }
        return MenuRuntimeSnapshot(
            player.uniqueId,
            route.runtimeSnapshot(),
            navigation.breadcrumbs(player).map { breadcrumb -> breadcrumb.runtimeSnapshot() },
            navigation.canGoBack(player),
            MenuSurface.INVENTORY,
            player.openInventory.title(),
            top.size,
            revision,
            slots,
        )
    }

    private fun snapshotSlot(
        inventory: Inventory,
        session: Session?,
        slot: Int,
    ): MenuRuntimeSlotSnapshot {
        val element = session?.elements?.get(slot)
        val item = inventory.getItem(slot)?.takeUnless { it.type.isAir }
        val meta = item?.itemMeta
        val interaction = element?.resolvedInteraction()
        val (kind, interactionKind) = when (interaction) {
            MenuInteraction.DisplayOnly -> MenuRuntimeSlotKind.DISPLAY_ONLY to MenuRuntimeInteractionKind.DISPLAY_ONLY
            is MenuInteraction.Action -> MenuRuntimeSlotKind.ACTION to MenuRuntimeInteractionKind.ACTION
            is MenuInteraction.Branches -> MenuRuntimeSlotKind.ACTION to MenuRuntimeInteractionKind.BRANCHES
            is MenuInteraction.Capability -> MenuRuntimeSlotKind.ACTION to MenuRuntimeInteractionKind.CAPABILITY
            is MenuInteraction.Unavailable -> MenuRuntimeSlotKind.UNAVAILABLE to MenuRuntimeInteractionKind.UNAVAILABLE
            is MenuInteraction.Back -> MenuRuntimeSlotKind.BACK to MenuRuntimeInteractionKind.BACK
            null -> when {
                session != null && slot in session.inputSlots -> MenuRuntimeSlotKind.INPUT to null
                item == null -> MenuRuntimeSlotKind.EMPTY to null
                session?.standardFrame == true -> MenuRuntimeSlotKind.FRAME to null
                else -> MenuRuntimeSlotKind.DISPLAY_ONLY to null
            }
        }
        val branches = (interaction as? MenuInteraction.Branches)
            ?.branches
            ?.map { branch ->
                MenuRuntimeBranchSnapshot(
                    branch.actionId,
                    branch.acceptedClicks.toSet(),
                    branch.payload.toSortedMap(),
                    branch.safety,
                )
            }
            .orEmpty()
        val acceptedClicks = when (interaction) {
            is MenuInteraction.Action -> interaction.acceptedClicks
            is MenuInteraction.Branches -> interaction.branches.flatMapTo(linkedSetOf()) { it.acceptedClicks }
            is MenuInteraction.Capability -> interaction.acceptedClicks
            is MenuInteraction.Unavailable -> interaction.acceptedClicks
            is MenuInteraction.Back -> interaction.acceptedClicks
            MenuInteraction.DisplayOnly,
            null -> emptySet()
        }
        val payload = when (interaction) {
            is MenuInteraction.Action -> interaction.payload.toSortedMap()
            is MenuInteraction.Capability -> interaction.arguments.toSortedMap()
            else -> emptyMap()
        }
        val safety = when (interaction) {
            is MenuInteraction.Action -> interaction.safety
            is MenuInteraction.Branches -> branches.map(MenuRuntimeBranchSnapshot::safety)
                .distinct()
                .singleOrNull()
                ?: MenuActionSafety.UNSPECIFIED
            is MenuInteraction.Capability -> interaction.safety
            is MenuInteraction.Back -> MenuActionSafety.NAVIGATION_ONLY
            is MenuInteraction.Unavailable,
            MenuInteraction.DisplayOnly,
            null -> MenuActionSafety.UNSPECIFIED
        }
        val safetyByClick = when (interaction) {
            is MenuInteraction.Action -> interaction.safetyByClick.toSortedMap(compareBy(ClickType::name))
            is MenuInteraction.Branches -> buildMap {
                interaction.branches.forEach { branch ->
                    branch.acceptedClicks.forEach { click -> put(click, branch.safety) }
                }
            }.toSortedMap(compareBy(ClickType::name))
            is MenuInteraction.Capability -> interaction.safetyByClick.toSortedMap(compareBy(ClickType::name))
            else -> emptyMap()
        }
        return MenuRuntimeSlotSnapshot(
            slot,
            kind,
            item?.type,
            item?.amount ?: 0,
            meta?.displayName(),
            meta?.lore().orEmpty(),
            item?.let(::hasGlint) ?: false,
            element?.role ?: GuiItemMarker.role(item),
            interactionKind,
            when (interaction) {
                is MenuInteraction.Action -> interaction.actionId
                else -> null
            },
            when (interaction) {
                is MenuInteraction.Action -> interaction.capabilityId
                is MenuInteraction.Capability -> interaction.capabilityId
                else -> null
            },
            acceptedClicks.toSet(),
            payload,
            element?.enabled ?: false,
            safety,
            safetyByClick,
            branches,
        )
    }

    private fun hasGlint(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return item.enchantments.isNotEmpty()
        return item.enchantments.isNotEmpty() ||
            (meta.hasEnchantmentGlintOverride() && meta.enchantmentGlintOverride)
    }

    private fun MenuRoute.runtimeSnapshot(): MenuRuntimeRouteSnapshot =
        MenuRuntimeRouteSnapshot(owner, id, payload.toSortedMap())

    private fun beginClickTrace(
        player: Player,
        slot: Int,
        click: ClickType,
        route: MenuRoute,
    ): ClickTraceContext {
        val before = snapshotCurrent(player)
        return ClickTraceContext(
            clickTraces.next(player.uniqueId),
            before?.revision,
            before?.route ?: route.runtimeSnapshot(),
            slot,
            click,
        )
    }

    private fun recordClickTrace(
        context: ClickTraceContext,
        player: Player,
        cancelled: Boolean,
        disposition: MenuRuntimeClickDisposition,
        accepted: Boolean = false,
        interaction: MenuInteraction? = null,
        result: MenuActionResult? = null,
        update: MenuRuntimeUpdateSnapshot? = result?.traceUpdate(),
        exception: Throwable? = null,
        interactionKind: MenuRuntimeInteractionKind? = null,
        actionId: String? = null,
        capabilityId: String? = null,
        payload: Map<String, String> = emptyMap(),
        safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
    ) {
        val details = interaction?.traceDetails(context.click) ?: InteractionTraceDetails(
            interactionKind,
            actionId,
            capabilityId,
            payload.toSortedMap(),
            safety,
        )
        val after = snapshotCurrent(player)
        clickTraces.append(
            player.uniqueId,
            MenuRuntimeClickTrace(
                context.identity.runId,
                context.identity.sequence,
                player.uniqueId,
                context.beforeRevision,
                context.beforeRoute,
                context.slot,
                context.click,
                cancelled,
                accepted,
                disposition,
                details.kind,
                details.actionId,
                details.capabilityId,
                details.payload,
                details.safety,
                result?.traceResultKind(),
                update,
                exception?.javaClass?.name,
                after?.revision,
                after?.route,
            ),
        )
    }

    private fun MenuInteraction.traceDetails(click: ClickType): InteractionTraceDetails = when (this) {
        MenuInteraction.DisplayOnly -> InteractionTraceDetails(
            MenuRuntimeInteractionKind.DISPLAY_ONLY,
            null,
            null,
            emptyMap(),
            MenuActionSafety.UNSPECIFIED,
        )
        is MenuInteraction.Action -> InteractionTraceDetails(
            MenuRuntimeInteractionKind.ACTION,
            actionId,
            capabilityId,
            payload.toSortedMap(),
            safetyFor(click),
        )
        is MenuInteraction.Branches -> {
            val branch = branches.singleOrNull { click in it.acceptedClicks }
            InteractionTraceDetails(
                MenuRuntimeInteractionKind.BRANCHES,
                branch?.actionId,
                null,
                branch?.payload?.toSortedMap().orEmpty(),
                branch?.safety ?: MenuActionSafety.UNSPECIFIED,
            )
        }
        is MenuInteraction.Capability -> InteractionTraceDetails(
            MenuRuntimeInteractionKind.CAPABILITY,
            null,
            capabilityId,
            arguments.toSortedMap(),
            safetyFor(click),
        )
        is MenuInteraction.Unavailable -> InteractionTraceDetails(
            MenuRuntimeInteractionKind.UNAVAILABLE,
            null,
            null,
            emptyMap(),
            MenuActionSafety.UNSPECIFIED,
        )
        is MenuInteraction.Back -> InteractionTraceDetails(
            MenuRuntimeInteractionKind.BACK,
            null,
            null,
            emptyMap(),
            MenuActionSafety.NAVIGATION_ONLY,
        )
    }

    private fun MenuActionResult.traceResultKind(): MenuRuntimeActionResultKind = when (this) {
        MenuActionResult.Ignored -> MenuRuntimeActionResultKind.IGNORED
        is MenuActionResult.Rejected -> MenuRuntimeActionResultKind.REJECTED
        is MenuActionResult.Success -> MenuRuntimeActionResultKind.SUCCESS
    }

    private fun MenuActionResult.traceUpdate(): MenuRuntimeUpdateSnapshot? = when (this) {
        MenuActionResult.Ignored,
        is MenuActionResult.Rejected -> null
        is MenuActionResult.Success -> when (val update = update) {
            MenuUpdate.None -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.NONE)
            MenuUpdate.Refresh -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.REFRESH)
            MenuUpdate.Resume -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.RESUME)
            MenuUpdate.Close -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.CLOSE)
            MenuUpdate.Back -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.BACK)
            is MenuUpdate.Replace -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.REPLACE, update.route.runtimeSnapshot())
            is MenuUpdate.Navigate -> MenuRuntimeUpdateSnapshot(MenuRuntimeUpdateKind.NAVIGATE, update.route.runtimeSnapshot())
        }
    }

    private fun Session.slotKind(inventory: Inventory, slot: Int): MenuRuntimeSlotKind =
        snapshotSlot(inventory, this, slot).kind

    private fun MenuRuntimeSlotKind.toClickDisposition(): MenuRuntimeClickDisposition = when (this) {
        MenuRuntimeSlotKind.EMPTY -> MenuRuntimeClickDisposition.EMPTY
        MenuRuntimeSlotKind.FRAME -> MenuRuntimeClickDisposition.FRAME
        MenuRuntimeSlotKind.INPUT -> MenuRuntimeClickDisposition.INPUT
        MenuRuntimeSlotKind.DISPLAY_ONLY -> MenuRuntimeClickDisposition.DISPLAY_ONLY
        MenuRuntimeSlotKind.ACTION,
        MenuRuntimeSlotKind.BACK,
        MenuRuntimeSlotKind.UNAVAILABLE -> MenuRuntimeClickDisposition.NO_SESSION
    }

    private data class ClickTraceContext(
        val identity: MenuRuntimeClickTraceStore.Identity,
        val beforeRevision: Long?,
        val beforeRoute: MenuRuntimeRouteSnapshot?,
        val slot: Int,
        val click: ClickType,
    )

    private data class InteractionTraceDetails(
        val kind: MenuRuntimeInteractionKind?,
        val actionId: String?,
        val capabilityId: String?,
        val payload: Map<String, String>,
        val safety: MenuActionSafety,
    )

    private data class RouteKey(val owner: String, val id: String)

    private data class Session(
        val route: MenuRoute,
        val elements: Map<Int, com.awabi2048.ccsystem.api.gui.MenuElement>,
        val preserveHistory: Boolean,
        val standardFrame: Boolean,
        val inputSlots: Set<Int>,
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
        org.bukkit.event.inventory.ClickType.MIDDLE,
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
