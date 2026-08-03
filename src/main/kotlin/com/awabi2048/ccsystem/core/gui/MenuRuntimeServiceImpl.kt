package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiInventoryPolicy
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionContext
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuClickType
import com.awabi2048.ccsystem.api.gui.MenuInteraction
import com.awabi2048.ccsystem.api.gui.MenuElement
import com.awabi2048.ccsystem.api.gui.MenuElementPresentationSemantics
import com.awabi2048.ccsystem.api.gui.copyPreservingCapabilityComposition
import com.awabi2048.ccsystem.api.gui.MenuPresentationProfile
import com.awabi2048.ccsystem.api.gui.MenuCloseContext
import com.awabi2048.ccsystem.api.gui.MenuCloseReason
import com.awabi2048.ccsystem.api.gui.MenuContractViolation
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
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateApplication
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperation
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationFailureReason
import com.awabi2048.ccsystem.api.gui.MenuRuntimeOperationResult
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionResult
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionSlotSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionInteractionSnapshot
import com.awabi2048.ccsystem.api.gui.MenuRuntimeInspectionMode
import com.awabi2048.ccsystem.api.gui.MenuActionSafety
import com.awabi2048.ccsystem.api.gui.MenuReversibleContract
import com.awabi2048.ccsystem.api.gui.MenuReversibleInteractionContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleOpaqueState
import com.awabi2048.ccsystem.api.gui.MenuReversibleProviderCaptureResult
import com.awabi2048.ccsystem.api.gui.MenuReversibleProviderRestoreResult
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateCaptureContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateCaptureResult
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateFailure
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateFailureReason
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateProviderRegistry
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateRestoreContext
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateRestoreResult
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateSnapshot
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateTraceBindingResult
import com.awabi2048.ccsystem.api.gui.MenuReversibleStateToken
import com.awabi2048.ccsystem.api.gui.MenuSnapshotValueException
import com.awabi2048.ccsystem.api.gui.MenuRuntimeReversibleContractSnapshot
import com.awabi2048.ccsystem.api.gui.MenuSurface
import com.awabi2048.ccsystem.api.gui.MenuSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuSoundService
import com.awabi2048.ccsystem.api.gui.MenuUpdate
import com.awabi2048.ccsystem.api.gui.MenuNavigationService
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import com.awabi2048.ccsystem.api.gui.PlayerInventoryInteraction
import com.awabi2048.ccsystem.api.gui.MenuRuntimeUpdateApplicationState
import com.awabi2048.ccsystem.api.gui.rethrowIfUnrecoverableMenuRuntimeFailure
import java.util.UUID
import java.util.concurrent.CompletableFuture
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
    private val reversibleProviders: MenuReversibleStateProviderRegistry = MenuReversibleStateProviderRegistryImpl(),
) : MenuRuntimeService, Listener {
    private val semanticElements = GuiElementServiceImpl()
    private val closeReasons = MenuCloseReasonTracker<Inventory>()
    private val definitions = ConcurrentHashMap<RouteKey, InventoryMenuDefinition>()
    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val preserveCloseInventories = ConcurrentHashMap.newKeySet<Inventory>()
    private val externalSuspensions = ConcurrentHashMap<UUID, MenuRoute>()
    private val externalFinishResults = MenuRuntimeExternalFinishResultStore()
    private val executing = ConcurrentHashMap.newKeySet<UUID>()
    private val suppressOpenSound = ConcurrentHashMap.newKeySet<UUID>()
    private val clickTraces = MenuRuntimeClickTraceStore()
    private val reversibleTokens = MenuReversibleStateTokenStore()
    private val reversibleProviderInvalidation = reversibleProviders.addInvalidationListener { registration ->
        reversibleTokens.clearProviderGeneration(registration.generation)
    }
    private val presentedInventories = java.util.Collections.synchronizedMap(
        java.util.IdentityHashMap<org.bukkit.inventory.Inventory, ManagedPresentation>()
    )

    override fun register(definition: InventoryMenuDefinition) {
        val key = RouteKey(definition.owner, definition.id)
        check(definitions.putIfAbsent(key, definition) == null) {
            "Menu definition is already registered: ${definition.routeId}"
        }
        navigation.registerResultOpener(definition.owner, definition.id) { player, route ->
            if (definitions[key] == null) {
                return@registerResultOpener MenuRuntimeOperationResult.failed(
                    MenuRuntimeOperation.OPEN,
                    route,
                    MenuRuntimeOperationFailureReason.MISSING_DEFINITION,
                )
            }
            // 既定音の抑制判定はopenDirectResult側で行う。確認画面の明示音だけは
            // 多段階遷移でも再生し、通常画面の遷移音抑制は従来どおり維持する。
            openDirectResult(player, route, playOpenSound = true)
        }
    }

    override fun unregister(owner: String, id: String) {
        definitions.remove(RouteKey(owner, id))
        closeMatching(owner, id)
        clickTraces.clearOwner(owner)
        reversibleTokens.clearOwner(owner)
    }

    override fun unregisterOwner(owner: String) {
        definitions.keys.removeIf { it.owner == owner }
        closeMatching(owner, null)
        navigation.unregisterOwner(owner)
        clickTraces.clearOwner(owner)
        reversibleTokens.clearOwner(owner)
    }

    override fun definitions(): List<InventoryMenuDefinition> =
        definitions.values.sortedWith(compareBy(InventoryMenuDefinition::owner, InventoryMenuDefinition::id))

    override fun definition(owner: String, id: String): InventoryMenuDefinition? =
        definitions[RouteKey(owner, id)]

    override fun snapshot(player: Player): MenuRuntimeSnapshot? = snapshotCurrent(player)

    override fun startClickTraceRun(player: Player): String {
        val runId = clickTraces.start(player.uniqueId)
        reversibleTokens.clear(player.uniqueId)
        return runId
    }

    override fun startClickTraceRun(player: Player, runId: String): String {
        val startedRunId = clickTraces.start(player.uniqueId, runId)
        reversibleTokens.clear(player.uniqueId)
        return startedRunId
    }

    override fun clickTraces(player: Player): List<MenuRuntimeClickTrace> =
        clickTraces.all(player.uniqueId)

    override fun latestClickTrace(player: Player): MenuRuntimeClickTrace? =
        clickTraces.latest(player.uniqueId)

    override fun clearClickTraces(player: Player) {
        clickTraces.clear(player.uniqueId)
        reversibleTokens.clear(player.uniqueId)
    }

    override fun captureReversibleState(
        player: Player,
        slot: Int,
        click: ClickType,
    ): MenuReversibleStateCaptureResult {
        val holder = player.openInventory.topInventory.holder as? MenuRuntimeHolder
            ?: return reversibleCaptureFailure(MenuReversibleStateFailureReason.NO_ACTIVE_SESSION)
        val session = sessions[player.uniqueId]
            ?.takeIf { it.route == holder.route && holder.playerId == player.uniqueId }
            ?: return reversibleCaptureFailure(MenuReversibleStateFailureReason.NO_ACTIVE_SESSION)
        val before = snapshotCurrent(player)
            ?.takeIf { it.surface == MenuSurface.INVENTORY && it.route == holder.route.runtimeSnapshot() }
            ?: return reversibleCaptureFailure(MenuReversibleStateFailureReason.ROUTE_MISMATCH)
        val element = session.elements[slot]
            ?: return reversibleCaptureFailure(MenuReversibleStateFailureReason.SLOT_NOT_ACTIONABLE)
        val resolved = element.resolvedInteraction().resolveReversibleInteraction(click)
            ?: return reversibleCaptureFailure(MenuReversibleStateFailureReason.CLICK_UNACCEPTED)
        if (resolved.safety != MenuActionSafety.REVERSIBLE) {
            return reversibleCaptureFailure(MenuReversibleStateFailureReason.NOT_REVERSIBLE)
        }
        val contract = resolved.contract
            ?: return reversibleCaptureFailure(MenuReversibleStateFailureReason.MISSING_CONTRACT)
        val provider = reversibleProviders.registration(contract.providerId)
            ?: return reversibleCaptureFailure(
                MenuReversibleStateFailureReason.UNKNOWN_PROVIDER,
                "provider is not registered: ${contract.providerId}",
            )
        val runId = clickTraces.currentRunId(player.uniqueId)
            ?: return reversibleCaptureFailure(MenuReversibleStateFailureReason.NO_ACTIVE_RUN)
        val interaction = MenuReversibleInteractionContext(
            slot = slot,
            click = click,
            actionId = resolved.actionId,
            capabilityId = resolved.capabilityId,
            contract = contract,
            revision = before.revision,
            arguments = resolved.arguments.toSortedMap(),
            attributes = resolved.attributes.toMap(),
            routePayload = holder.route.payload.toSortedMap(),
        )
        val capture = try {
            provider.definition.provider.capture(
                MenuReversibleStateCaptureContext(player, holder.route, runId, interaction),
            )
        } catch (failure: Throwable) {
            if (failure is Error) throw failure
            return reversibleCaptureFailure(
                MenuReversibleStateFailureReason.CAPTURE_EXCEPTION,
                exceptionType = failure.javaClass.name,
            )
        }
        val opaqueState = when (capture) {
            is MenuReversibleProviderCaptureResult.Captured -> capture.state
            is MenuReversibleProviderCaptureResult.Rejected -> return reversibleCaptureFailure(
                MenuReversibleStateFailureReason.CAPTURE_REJECTED,
                capture.reason,
            )
        }
        val state = (opaqueState as? MenuReversibleOpaqueState)?.let { opaque ->
            try {
                MenuReversibleStateSnapshot.capture(opaque)
            } catch (failure: MenuSnapshotValueException) {
                return reversibleCaptureFailure(
                    when (failure.reason) {
                        com.awabi2048.ccsystem.api.gui.MenuSnapshotFailureReason.INVALID_VALUE_TYPE -> MenuReversibleStateFailureReason.INVALID_STATE_TYPE
                        com.awabi2048.ccsystem.api.gui.MenuSnapshotFailureReason.INVALID_STATE_SIZE -> MenuReversibleStateFailureReason.INVALID_STATE_SIZE
                        com.awabi2048.ccsystem.api.gui.MenuSnapshotFailureReason.INVALID_STATE_DEPTH -> MenuReversibleStateFailureReason.INVALID_STATE_DEPTH
                    },
                    failure.message,
                    failure.javaClass.name,
                )
            } catch (failure: Throwable) {
                if (failure is Error) throw failure
                return reversibleCaptureFailure(
                    MenuReversibleStateFailureReason.CAPTURE_EXCEPTION,
                    exceptionType = failure.javaClass.name,
                )
            }
        } ?: return reversibleCaptureFailure(
            MenuReversibleStateFailureReason.INVALID_STATE_TYPE,
            "provider state must implement MenuReversibleOpaqueState",
            opaqueState.javaClass.name,
        )
        if (!matchesReversibleSession(player, before.route, before.revision)) {
            return reversibleCaptureFailure(MenuReversibleStateFailureReason.ROUTE_REVISION_MISMATCH)
        }
        val token = when (val issued = reversibleTokens.issue(
            player.uniqueId,
            runId,
            holder.route,
            interaction,
            state,
            provider.generation,
            providerCurrent = {
                reversibleProviders.registration(contract.providerId)?.generation == provider.generation
            },
            runCurrent = { clickTraces.currentRunId(player.uniqueId) == runId },
        )) {
            is MenuReversibleStateTokenStore.IssueResult.Issued -> issued.token
            is MenuReversibleStateTokenStore.IssueResult.Invalidated ->
                return reversibleCaptureFailure(issued.reason)
        }
        return MenuReversibleStateCaptureResult.Captured(
            token,
            contract.providerId,
            before.route,
            before.revision,
        )
    }

    override fun restoreReversibleState(
        player: Player,
        token: MenuReversibleStateToken,
    ): MenuReversibleStateRestoreResult {
        if (!player.isOnline) return reversibleRestoreFailure(MenuReversibleStateFailureReason.PLAYER_OFFLINE)
        val preview = when (val found = reversibleTokens.peek(token, player.uniqueId)) {
            is MenuReversibleStateTokenStore.PeekResult.Found -> found.entry
            is MenuReversibleStateTokenStore.PeekResult.Failed -> return reversibleRestoreFailure(found.reason)
        }
        validateReversibleRestore(player, preview)?.let { reason -> return reversibleRestoreFailure(reason) }
        val entry = when (val taken = reversibleTokens.take(token, player.uniqueId)) {
            is MenuReversibleStateTokenStore.TakeResult.Taken -> taken.entry
            is MenuReversibleStateTokenStore.TakeResult.Failed -> return reversibleRestoreFailure(taken.reason)
        }
        validateReversibleRestore(player, entry)?.let { reason -> return reversibleRestoreFailure(reason) }
        val provider = reversibleProviders.registration(entry.interaction.contract.providerId)
            ?: return reversibleRestoreFailure(MenuReversibleStateFailureReason.PROVIDER_UNREGISTERED)
        if (provider.generation != entry.providerGeneration) {
            return reversibleRestoreFailure(MenuReversibleStateFailureReason.PROVIDER_GENERATION_MISMATCH)
        }
        val restored = try {
            provider.definition.provider.restore(
                MenuReversibleStateRestoreContext(
                    player,
                    entry.route,
                    entry.runId,
                    entry.interaction.restoreContext(),
                    entry.state,
                ),
            )
        } catch (failure: Throwable) {
            if (failure is Error) throw failure
            return reversibleRestoreFailure(
                MenuReversibleStateFailureReason.RESTORE_EXCEPTION,
                exceptionType = failure.javaClass.name,
            )
        }
        return when (restored) {
            MenuReversibleProviderRestoreResult.Restored -> MenuReversibleStateRestoreResult.Restored(
                entry.interaction.contract.providerId,
                entry.route.runtimeSnapshot(),
                entry.interaction.revision,
            )
            is MenuReversibleProviderRestoreResult.Rejected -> reversibleRestoreFailure(
                MenuReversibleStateFailureReason.RESTORE_REJECTED,
                restored.reason,
            )
        }
    }

    override fun restoreReversibleState(token: MenuReversibleStateToken): MenuReversibleStateRestoreResult {
        reversibleTokens.missingReason(token)?.let { reason ->
            return reversibleRestoreFailure(reason)
        }
        val playerId = reversibleTokens.boundPlayerId(token)
            ?: return reversibleRestoreFailure(
                reversibleTokens.missingReason(token) ?: MenuReversibleStateFailureReason.TOKEN_UNKNOWN,
            )
        val player = Bukkit.getPlayer(playerId)
            ?: return reversibleRestoreFailure(MenuReversibleStateFailureReason.PLAYER_OFFLINE)
        return restoreReversibleState(player, token)
    }

    override fun bindReversibleStateToTrace(
        player: Player,
        token: MenuReversibleStateToken,
        runId: String,
        sequence: Long,
    ): MenuReversibleStateTraceBindingResult {
        if (!player.isOnline) return reversibleBindingFailure(MenuReversibleStateFailureReason.PLAYER_OFFLINE)
        val entry = when (val found = reversibleTokens.peek(token, player.uniqueId)) {
            is MenuReversibleStateTokenStore.PeekResult.Found -> found.entry
            is MenuReversibleStateTokenStore.PeekResult.Failed -> return reversibleBindingFailure(found.reason)
        }
        if (entry.runId != runId || clickTraces.currentRunId(player.uniqueId) != runId) {
            return reversibleBindingFailure(MenuReversibleStateFailureReason.RUN_MISMATCH)
        }
        val provider = reversibleProviders.registration(entry.interaction.contract.providerId)
            ?: return reversibleBindingFailure(MenuReversibleStateFailureReason.PROVIDER_UNREGISTERED)
        if (provider.generation != entry.providerGeneration) {
            return reversibleBindingFailure(MenuReversibleStateFailureReason.PROVIDER_GENERATION_MISMATCH)
        }
        val trace = clickTraces.terminal(player.uniqueId, runId, sequence)
            ?: return reversibleBindingFailure(MenuReversibleStateFailureReason.TRACE_NOT_TERMINAL)
        if (!trace.matchesReversibleCapture(entry)) {
            return reversibleBindingFailure(MenuReversibleStateFailureReason.TRACE_MISMATCH)
        }
        val afterRoute = trace.afterRoute
            ?: return reversibleBindingFailure(MenuReversibleStateFailureReason.TRACE_MISMATCH)
        val afterRevision = trace.afterRevision
            ?: return reversibleBindingFailure(MenuReversibleStateFailureReason.TRACE_MISMATCH)
        if (!matchesReversibleSession(player, afterRoute, afterRevision)) {
            return reversibleBindingFailure(MenuReversibleStateFailureReason.ROUTE_REVISION_MISMATCH)
        }
        when (val binding = reversibleTokens.bind(
            token,
            player.uniqueId,
            MenuReversibleStateTokenStore.TraceBinding(runId, sequence, afterRoute, afterRevision),
        )) {
            MenuReversibleStateTokenStore.BindResult.Bound -> Unit
            is MenuReversibleStateTokenStore.BindResult.Failed -> return reversibleBindingFailure(binding.reason)
        }
        return MenuReversibleStateTraceBindingResult.Bound(
            entry.interaction.contract.providerId,
            afterRoute,
            afterRevision,
            runId,
            sequence,
        )
    }

    override fun bindReversibleStateToTrace(
        token: MenuReversibleStateToken,
        runId: String,
        sequence: Long,
    ): MenuReversibleStateTraceBindingResult {
        reversibleTokens.missingReason(token)?.let { reason -> return reversibleBindingFailure(reason) }
        val playerId = reversibleTokens.boundPlayerId(token)
            ?: return reversibleBindingFailure(
                reversibleTokens.missingReason(token) ?: MenuReversibleStateFailureReason.TOKEN_UNKNOWN,
            )
        val player = Bukkit.getPlayer(playerId)
            ?: return reversibleBindingFailure(MenuReversibleStateFailureReason.PLAYER_OFFLINE)
        return bindReversibleStateToTrace(player, token, runId, sequence)
    }

    override fun clearReversibleStates(player: Player) {
        reversibleTokens.clear(player.uniqueId)
    }

    override fun inspect(
        player: Player,
        route: MenuRoute,
        mode: MenuRuntimeInspectionMode,
    ): MenuRuntimeInspectionResult {
        val definition = definition(route.owner, route.id)
            ?: return inspectionFailure(route, MenuRuntimeOperationFailureReason.MISSING_DEFINITION)
        val inspectionNavigation = MenuRuntimeInspectionNavigationContextResolver.resolve(
            mode,
            navigation.currentRoute(player),
            navigation.breadcrumbs(player),
            route,
        )
            ?: return inspectionFailure(route, MenuRuntimeOperationFailureReason.INVALID_INSPECTION_CONTEXT)
        return MenuRuntimeViewPreparation.inspect(
            definition,
            MenuRenderContext(player, route, inspectionNavigation.canGoBack),
            capabilities,
            reversibleProviders,
        ) { view ->
            inspectionSnapshot(
                player,
                route,
                inspectionNavigation,
                view.withHistoryNavigation(inspectionNavigation.canGoBack),
            )
        }
    }

    override fun terminalClickTrace(player: Player, runId: String, sequence: Long): MenuRuntimeClickTrace? =
        clickTraces.terminal(player.uniqueId, runId, sequence)

    override fun awaitTerminalClickTrace(
        player: Player,
        runId: String,
        sequence: Long,
    ): CompletableFuture<MenuRuntimeClickTrace> =
        clickTraces.awaitTerminal(player.uniqueId, runId, sequence)

    override fun open(player: Player, route: MenuRoute): Boolean = openResult(player, route).successful

    override fun openResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult {
        completeExternal(player)
        return navigation.openRootResult(player, route).forOperation(MenuRuntimeOperation.OPEN)
    }

    override fun replace(player: Player, route: MenuRoute): Boolean = replaceResult(player, route).successful

    override fun replaceResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult {
        completeExternal(player)
        return withoutOpenSound(player) {
            navigation.openResult(player, route).forOperation(MenuRuntimeOperation.REPLACE)
        }
    }

    override fun reopenCurrent(player: Player): Boolean = reopenCurrentResult(player).successful

    override fun reopenCurrentResult(player: Player): MenuRuntimeOperationResult {
        val route = navigation.currentRoute(player) ?: return MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.REOPEN_CURRENT,
            null,
            MenuRuntimeOperationFailureReason.NO_ACTIVE_SESSION,
        )
        return replaceResult(player, route).forOperation(MenuRuntimeOperation.REOPEN_CURRENT)
    }

    override fun navigate(player: Player, route: MenuRoute): Boolean = navigateResult(player, route).successful

    override fun navigateResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult {
        completeExternal(player)
        val currentRoute = navigation.currentRoute(player) ?: return openResult(player, route)
            .forOperation(MenuRuntimeOperation.NAVIGATE)
        return navigateFromResult(player, currentRoute, route)
    }

    override fun openEphemeral(player: Player, route: MenuRoute): Boolean =
        openEphemeralResult(player, route).successful

    override fun openEphemeralResult(player: Player, route: MenuRoute): MenuRuntimeOperationResult =
        openDirectResult(player, route, playOpenSound = true, preserveHistory = true)
            .forOperation(MenuRuntimeOperation.OPEN_EPHEMERAL)

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

    override fun resumeFromExternal(player: Player): Boolean = resumeFromExternalResult(player).successful

    override fun resumeFromExternalResult(player: Player): MenuRuntimeOperationResult {
        val route = externalSuspensions.remove(player.uniqueId) ?: return MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.RESUME_EXTERNAL,
            navigation.currentRoute(player),
            MenuRuntimeOperationFailureReason.NO_ACTIVE_SESSION,
        )
        if (navigation.currentRoute(player) != route) {
            return MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.RESUME_EXTERNAL,
                route,
                MenuRuntimeOperationFailureReason.ROUTE_MISMATCH,
            )
        }
        return reopenCurrentResult(player).forOperation(MenuRuntimeOperation.RESUME_EXTERNAL)
    }

    override fun finishExternal(player: Player): Boolean = finishExternalResult(player).successful

    override fun finishExternalResult(player: Player): MenuRuntimeOperationResult =
        finishExternalResult(player, null)

    private fun finishExternalResult(
        player: Player,
        terminalTrace: MenuRuntimeClickTraceStore.Identity?,
    ): MenuRuntimeOperationResult {
        val route = externalSuspensions.remove(player.uniqueId) ?: return MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.FINISH_EXTERNAL,
            navigation.currentRoute(player),
            MenuRuntimeOperationFailureReason.NO_ACTIVE_SESSION,
        )
        if (navigation.currentRoute(player) != route) {
            return MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.FINISH_EXTERNAL,
                route,
                MenuRuntimeOperationFailureReason.ROUTE_MISMATCH,
            )
        }
        externalFinishResults.clear(player.uniqueId)
        return try {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val completion = try {
                    if (player.isOnline && navigation.currentRoute(player) == route) {
                        reopenCurrentResult(player).forOperation(MenuRuntimeOperation.FINISH_EXTERNAL)
                    } else {
                        MenuRuntimeOperationResult.failed(
                            MenuRuntimeOperation.FINISH_EXTERNAL,
                            route,
                            MenuRuntimeOperationFailureReason.ROUTE_MISMATCH,
                        )
                    }
                } catch (failure: Throwable) {
                    failure.rethrowIfUnrecoverableMenuRuntimeFailure()
                    plugin.logger.log(
                        Level.SEVERE,
                        "外部画面終了後のメニュー再表示に失敗しました: route=${route.id} player=${player.uniqueId}",
                        failure,
                    )
                    MenuRuntimeOperationResult.failed(
                        MenuRuntimeOperation.FINISH_EXTERNAL,
                        route,
                        MenuRuntimeOperationFailureReason.INVENTORY_OPEN_FAILED,
                        exceptionType = failure.javaClass.name,
                    )
                }
                externalFinishResults.record(player.uniqueId, completion)
                terminalTrace?.let { completePendingTrace(player, it, completion) }
            })
            MenuRuntimeOperationResult.pending(MenuRuntimeOperation.FINISH_EXTERNAL, route)
        } catch (failure: Throwable) {
            failure.rethrowIfUnrecoverableMenuRuntimeFailure()
            val result = MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.FINISH_EXTERNAL,
                route,
                MenuRuntimeOperationFailureReason.INVENTORY_OPEN_FAILED,
                exceptionType = failure.javaClass.name,
            )
            externalFinishResults.record(player.uniqueId, result)
            result
        }
    }

    override fun latestExternalFinishResult(player: Player): MenuRuntimeOperationResult? =
        externalFinishResults.latest(player.uniqueId)

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

    override fun refresh(player: Player): Boolean = refreshResult(player).successful

    override fun refreshResult(player: Player): MenuRuntimeOperationResult {
        val session = sessions[player.uniqueId] ?: return MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.REFRESH,
            navigation.currentRoute(player),
            MenuRuntimeOperationFailureReason.NO_ACTIVE_SESSION,
        )
        val holder = player.openInventory.topInventory.holder as? MenuRuntimeHolder
            ?: return openDirectResult(player, session.route, playOpenSound = false)
                .forOperation(MenuRuntimeOperation.REFRESH)
        if (holder.route != session.route) return MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.REFRESH,
            session.route,
            MenuRuntimeOperationFailureReason.ROUTE_MISMATCH,
        )
        val definition = definition(session.route.owner, session.route.id)
            ?: return MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.REFRESH,
                session.route,
                MenuRuntimeOperationFailureReason.MISSING_DEFINITION,
            )
        val view = when (
            val prepared = MenuRuntimeViewPreparation.renderValidated(
                definition,
                MenuRenderContext(player, session.route, navigation.canGoBack(player)),
                capabilities,
                reversibleProviders,
            )
        ) {
            is MenuRuntimePreparedViewResult.Ready -> prepared.view.withHistoryNavigation(navigation.canGoBack(player))
            is MenuRuntimePreparedViewResult.RenderFailed -> {
                plugin.logger.severe(
                    "メニュー再描画に失敗しました: route=${definition.routeId} " +
                        "player=${player.uniqueId} exception=${prepared.exceptionType}",
                )
                return MenuRuntimeOperationResult.failed(
                    MenuRuntimeOperation.REFRESH,
                    session.route,
                    MenuRuntimeOperationFailureReason.RENDER_FAILED,
                    exceptionType = prepared.exceptionType,
                )
            }
            is MenuRuntimePreparedViewResult.ContractInvalid -> {
                logContractInvalid(definition, player, prepared.violations)
                return MenuRuntimeOperationResult.failed(
                    MenuRuntimeOperation.REFRESH,
                    session.route,
                    MenuRuntimeOperationFailureReason.CONTRACT_INVALID,
                    contractViolations = prepared.violations,
                )
            }
        }
        val policy = inventoryPolicy(view)
        if (
            player.openInventory.topInventory.size != view.size ||
            player.openInventory.title() != view.title ||
            holder.guiInventoryPolicy() != policy
        ) {
            return openDirectResult(player, session.route, playOpenSound = false)
                .forOperation(MenuRuntimeOperation.REFRESH)
        }

        return try {
            val inventory = player.openInventory.topInventory
            val inputItems = policy.inputSlots.associateWith { inventory.getItem(it)?.clone() }
            inventory.clear()
            applyView(inventory, view)
            inputItems.forEach { (slot, item) -> inventory.setItem(slot, item) }
            val runtimeElements = canonicalElements(view)
            sessions[player.uniqueId] = Session(
                session.route,
                runtimeElements.associateBy { it.slot },
                session.preserveHistory,
                view.standardFrame,
                policy.inputSlots,
            )
            presentations.markRefreshed(player)
            MenuRuntimeOperationResult.succeeded(MenuRuntimeOperation.REFRESH, session.route)
        } catch (failure: Throwable) {
            failure.rethrowIfUnrecoverableMenuRuntimeFailure()
            plugin.logger.log(
                Level.SEVERE,
                "メニュー再描画の反映に失敗しました: route=${definition.routeId} player=${player.uniqueId}",
                failure,
            )
            MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.REFRESH,
                session.route,
                MenuRuntimeOperationFailureReason.INVENTORY_OPEN_FAILED,
                exceptionType = failure.javaClass.name,
            )
        }
    }

    override fun close(player: Player) {
        completeExternal(player)
        closeInventory(player, MenuCloseReason.RUNTIME_CLOSED)
        presentations.markClosed(player)
    }

    override fun clear(player: Player) {
        completeExternal(player)
        externalFinishResults.clear(player.uniqueId)
        reversibleTokens.clear(player.uniqueId)
        sessions.remove(player.uniqueId)
        navigation.clear(player)
        presentations.markClosed(player)
    }

    override fun back(player: Player): Boolean = backResult(player).successful

    override fun backResult(player: Player): MenuRuntimeOperationResult {
        completeExternal(player)
        return withoutOpenSound(player) {
            navigation.openPreviousResult(player)
                ?: MenuRuntimeOperationResult.failed(
                    MenuRuntimeOperation.BACK,
                    navigation.breadcrumbs(player).lastOrNull(),
                    MenuRuntimeOperationFailureReason.NO_HISTORY,
                )
        }.forOperation(MenuRuntimeOperation.BACK)
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
                failure.rethrowIfUnrecoverableMenuRuntimeFailure()
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
            val application = applyResult(
                player,
                session,
                null,
                definition,
                MenuClickType.DEFAULT,
                result,
                originRevision,
                trace.beforeRoute,
                trace.identity,
            )
            recordClickTrace(
                trace,
                player,
                event.isCancelled,
                if (actionFailure == null) MenuRuntimeClickDisposition.HANDLED else MenuRuntimeClickDisposition.EXCEPTION,
                accepted = true,
                result = result,
                application = application,
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
        val declaredInteraction = element.resolvedInteraction()
        val interaction = if (declaredInteraction is MenuInteraction.ClickBranches) {
            declaredInteraction.resolve(event.click) ?: run {
                recordClickTrace(
                    trace,
                    player,
                    event.isCancelled,
                    MenuRuntimeClickDisposition.UNACCEPTED,
                    interaction = declaredInteraction,
                )
                return
            }
        } else {
            declaredInteraction
        }
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
                val result = MenuActionResult.Success(MenuUpdate.Back)
                val application = applyResult(
                    player,
                    session,
                    interaction.sounds,
                    definition,
                    clickType,
                    result,
                    trace.beforeRevision,
                    trace.beforeRoute,
                    trace.identity,
                    playSound = false,
                )
                recordClickTrace(
                    trace,
                    player,
                    event.isCancelled,
                    MenuRuntimeClickDisposition.BACK,
                    accepted = true,
                    interaction = interaction,
                    result = result,
                    application = application,
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
            is MenuInteraction.ClickBranches -> error("click branches must resolve before execution")
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
                is MenuInteraction.ClickBranches -> error("click branches must resolve before execution")
            }
        } catch (failure: Throwable) {
            failure.rethrowIfUnrecoverableMenuRuntimeFailure()
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
            is MenuInteraction.ClickBranches -> error("click branches must resolve before execution")
        }
        val application = applyResult(
            player,
            session,
            interactionSounds,
            definition,
            clickType,
            result,
            originRevision,
            trace.beforeRoute,
            trace.identity,
        )
        recordClickTrace(
            trace,
            player,
            event.isCancelled,
            if (actionFailure == null) MenuRuntimeClickDisposition.HANDLED else MenuRuntimeClickDisposition.EXCEPTION,
            accepted = true,
            interaction = interaction,
            result = result,
            application = application,
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
            try {
                handler.handle(MenuCloseContext(player, holder.route, event.inventory, closeReason))
            } catch (failure: Throwable) {
                failure.rethrowIfUnrecoverableMenuRuntimeFailure()
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
        externalFinishResults.clear(event.player.uniqueId)
        reversibleTokens.clear(event.player.uniqueId)
        // 外部画面（Dialog等）を開いたまま退出した場合も、サスペンド状態と表示状態を解放する。
        completeExternal(event.player)
        presentations.markClosed(event.player)
    }

    private fun openDirectResult(
        player: Player,
        route: MenuRoute,
        playOpenSound: Boolean,
        preserveHistory: Boolean = false,
    ): MenuRuntimeOperationResult {
        val definition = definition(route.owner, route.id)
            ?: return MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.OPEN,
                route,
                MenuRuntimeOperationFailureReason.MISSING_DEFINITION,
            )
        val view = when (
            val prepared = MenuRuntimeViewPreparation.renderValidated(
                definition,
                MenuRenderContext(player, route, navigation.canGoBack(player)),
                capabilities,
                reversibleProviders,
            )
        ) {
            is MenuRuntimePreparedViewResult.Ready -> prepared.view.withHistoryNavigation(navigation.canGoBack(player))
            is MenuRuntimePreparedViewResult.RenderFailed -> {
                plugin.logger.severe(
                    "メニュー描画に失敗しました: route=${definition.routeId} exception=${prepared.exceptionType}",
                )
                return MenuRuntimeOperationResult.failed(
                    MenuRuntimeOperation.OPEN,
                    route,
                    MenuRuntimeOperationFailureReason.RENDER_FAILED,
                    exceptionType = prepared.exceptionType,
                )
            }
            is MenuRuntimePreparedViewResult.ContractInvalid -> {
                logContractInvalid(definition, player, prepared.violations)
                return MenuRuntimeOperationResult.failed(
                    MenuRuntimeOperation.OPEN,
                    route,
                    MenuRuntimeOperationFailureReason.CONTRACT_INVALID,
                    contractViolations = prepared.violations,
                )
            }
        }
        val policy = inventoryPolicy(view)
        val holder = MenuRuntimeHolder(player.uniqueId, route, policy, preserveHistory)
        val inventory = try {
            Bukkit.createInventory(holder, view.size, view.title).also { created ->
                holder.backingInventory = created
                applyView(created, view)
            }
        } catch (failure: Throwable) {
            failure.rethrowIfUnrecoverableMenuRuntimeFailure()
            plugin.logger.log(
                Level.SEVERE,
                "メニューInventoryの作成に失敗しました: route=${definition.routeId} player=${player.uniqueId}",
                failure,
            )
            return MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.OPEN,
                route,
                MenuRuntimeOperationFailureReason.INVENTORY_OPEN_FAILED,
                exceptionType = failure.javaClass.name,
            )
        }
        val previousInventory = player.openInventory.topInventory
        val previousIsManaged = previousInventory.holder is MenuRuntimeHolder
        if (previousIsManaged) {
            closeReasons.mark(previousInventory, MenuCloseReason.ROUTE_REPLACED)
            preserveCloseInventories += previousInventory
        }
        // MenuSoundServiceImplは、現在のtopInventoryがCRAFTINGかどうかで
        // 「閉じた状態からの初回開封」を判定するため、Inventoryを表示する前に呼び出す。
        // 表示後に呼ぶと新しいInventoryがCHESTになり、初回開封音まで抑止される。
        // 確認画面だけは、通常の遷移音抑制を越えて明示した表示音を再生する。
        // これにより、確認画面の1段目・2段目や戻る操作でも表示音の仕様がぶれない。
        playDefinitionOpenSound(player, route, definition, playOpenSound)
        try {
            player.openInventory(inventory)
        } catch (failure: Throwable) {
            failure.rethrowIfUnrecoverableMenuRuntimeFailure()
            plugin.logger.log(
                Level.SEVERE,
                "メニューInventoryを開けませんでした: route=${definition.routeId} player=${player.uniqueId}",
                failure,
            )
            return MenuRuntimeOperationResult.failed(
                MenuRuntimeOperation.OPEN,
                route,
                MenuRuntimeOperationFailureReason.INVENTORY_OPEN_FAILED,
                exceptionType = failure.javaClass.name,
            )
        } finally {
            if (previousIsManaged) {
                closeReasons.clear(previousInventory)
            }
        }
        val runtimeElements = canonicalElements(view)
        sessions[player.uniqueId] = Session(
            route,
            runtimeElements.associateBy { it.slot },
            preserveHistory,
            view.standardFrame,
            policy.inputSlots,
        )
        presentations.markOpened(
            player,
            com.awabi2048.ccsystem.api.gui.MenuSurface.INVENTORY,
            route.owner,
            route.id,
        )
        return MenuRuntimeOperationResult.succeeded(MenuRuntimeOperation.OPEN, route)
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
    private fun InventoryMenuView.withHistoryNavigation(canGoBack: Boolean): InventoryMenuView {
        if (canGoBack) return this
        val visibleElements = elements.filterNot { it.role == GuiElementRole.BACK }
        return if (visibleElements.size == elements.size) this else copy(elements = visibleElements)
    }

    private fun inventoryPolicy(view: InventoryMenuView): GuiInventoryPolicy =
        GuiInventoryPolicy(view.inputSlots, view.playerInventoryInteraction)

    private fun logContractInvalid(
        definition: InventoryMenuDefinition,
        player: Player,
        violations: List<MenuContractViolation>,
    ) {
        plugin.logger.warning(
            "メニュー契約が不正です: route=${definition.routeId} player=${player.uniqueId} " +
                "violations=${violations.joinToString { violation ->
                    "slot=${violation.slot} action=${violation.actionId} ${violation.message}"
                }}",
        )
    }

    private fun canonicalElements(view: InventoryMenuView): List<MenuElement> =
        standardBackgroundElements(view) + view.elements

    private fun standardBackgroundElements(view: InventoryMenuView): List<MenuElement> {
        if (!view.standardFrame) return emptyList()
        val occupied = view.elements.mapTo(mutableSetOf()) { it.slot } + view.inputSlots
        return (0 until view.size).filterNot(occupied::contains).map { slot ->
            val material = if (slot < 9 || slot >= view.size - 9) {
                Material.BLACK_STAINED_GLASS_PANE
            } else {
                Material.GRAY_STAINED_GLASS_PANE
            }
            semanticElements.backgroundEntry(slot, material)
        }
    }

    private fun MenuRuntimeOperationResult.asUpdateOutcome(): MenuUpdateApplicationOutcome =
        if (!terminal) {
            MenuUpdateApplicationOutcome(
                false,
                MenuRuntimeUpdateFailureReason.PENDING,
                this,
                MenuRuntimeUpdateApplicationState.PENDING,
            )
        } else if (successful) {
            MenuUpdateApplicationOutcome(true, MenuRuntimeUpdateFailureReason.NONE, this)
        } else {
            MenuUpdateApplicationOutcome(
                false,
                failure!!.reason.toUpdateFailureReason(),
                this,
            )
        }

    private fun MenuRuntimeOperationFailureReason.toUpdateFailureReason(): MenuRuntimeUpdateFailureReason = when (this) {
        MenuRuntimeOperationFailureReason.MISSING_OPENER -> MenuRuntimeUpdateFailureReason.MISSING_OPENER
        MenuRuntimeOperationFailureReason.MISSING_DEFINITION -> MenuRuntimeUpdateFailureReason.MISSING_DEFINITION
        MenuRuntimeOperationFailureReason.RENDER_FAILED -> MenuRuntimeUpdateFailureReason.RENDER_FAILED
        MenuRuntimeOperationFailureReason.CONTRACT_INVALID -> MenuRuntimeUpdateFailureReason.CONTRACT_INVALID
        MenuRuntimeOperationFailureReason.INVENTORY_OPEN_FAILED -> MenuRuntimeUpdateFailureReason.INVENTORY_OPEN_FAILED
        MenuRuntimeOperationFailureReason.OPENER_RETURNED_FALSE -> MenuRuntimeUpdateFailureReason.OPENER_RETURNED_FALSE
        MenuRuntimeOperationFailureReason.OPENER_EXCEPTION -> MenuRuntimeUpdateFailureReason.OPENER_EXCEPTION
        MenuRuntimeOperationFailureReason.NO_ACTIVE_SESSION -> MenuRuntimeUpdateFailureReason.NO_ACTIVE_SESSION
        MenuRuntimeOperationFailureReason.ROUTE_MISMATCH -> MenuRuntimeUpdateFailureReason.ROUTE_MISMATCH
        MenuRuntimeOperationFailureReason.NO_HISTORY -> MenuRuntimeUpdateFailureReason.NO_HISTORY
        MenuRuntimeOperationFailureReason.INVALID_INSPECTION_CONTEXT -> MenuRuntimeUpdateFailureReason.NOT_APPLICABLE
    }

    private fun applyResult(
        player: Player,
        session: Session,
        elementSounds: com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy?,
        definition: InventoryMenuDefinition,
        clickType: MenuClickType,
        result: MenuActionResult,
        originRevision: Long?,
        beforeRoute: MenuRuntimeRouteSnapshot?,
        traceIdentity: MenuRuntimeClickTraceStore.Identity? = null,
        playSound: Boolean = true,
    ): MenuRuntimeUpdateApplication {
        val declaredUpdate = MenuRuntimeUpdateSnapshot.from(result)
        fun notAttempted(
            reason: MenuRuntimeUpdateFailureReason = MenuRuntimeUpdateFailureReason.NOT_APPLICABLE,
        ) = MenuRuntimeUpdateApplication.notAttempted(
            kind = declaredUpdate?.kind,
            expectedRoute = declaredUpdate?.route,
            beforeRevision = originRevision,
            failureReason = reason,
        )

        when (result) {
            MenuActionResult.Ignored -> return notAttempted()
            is MenuActionResult.Rejected -> {
                if (playSound) {
                    playResolved(
                        player,
                        result.sound,
                        MenuSoundPolicyResolver.rejectedPolicy(elementSounds, definition.sounds),
                        clickType,
                    )
                }
                result.message?.let(player::sendMessage)
                return notAttempted()
            }
            is MenuActionResult.Success -> {
                if (playSound) {
                    playResolved(
                        player,
                        result.sound,
                        MenuSoundPolicyResolver.successPolicy(elementSounds, definition.sounds),
                        clickType,
                    )
                }
                val update = result.update
                val expectedRoute = when (update) {
                    is MenuUpdate.Navigate -> update.route.runtimeSnapshot()
                    is MenuUpdate.Replace -> update.route.runtimeSnapshot()
                    MenuUpdate.Refresh, MenuUpdate.Resume -> beforeRoute
                    MenuUpdate.Back -> navigation.breadcrumbs(player).lastOrNull()?.runtimeSnapshot()
                    MenuUpdate.Close, MenuUpdate.None -> null
                }
                if (!MenuStaleUpdatePolicy.shouldApply(
                        update,
                        originRevision,
                        presentations.current(player)?.revision,
                    )
                ) {
                    return MenuRuntimeUpdateApplication(
                        attempted = true,
                        applied = false,
                        kind = declaredUpdate?.kind,
                        expectedRoute = expectedRoute,
                        observedRoute = null,
                        beforeRevision = originRevision,
                        afterRevision = null,
                        failureReason = MenuRuntimeUpdateFailureReason.STALE_REVISION,
                    )
                }

                val outcome = try {
                    when (update) {
                        MenuUpdate.None -> MenuUpdateApplicationOutcome(
                            false,
                            MenuRuntimeUpdateFailureReason.NOT_APPLICABLE,
                        )
                        MenuUpdate.Refresh -> refreshResult(player).asUpdateOutcome()
                        MenuUpdate.Resume ->
                            finishExternalResult(player, traceIdentity).asUpdateOutcome()
                        MenuUpdate.Close -> {
                            close(player)
                            MenuUpdateApplicationOutcome(true, MenuRuntimeUpdateFailureReason.NONE)
                        }
                        MenuUpdate.Back -> {
                            val backResult = backResult(player)
                            if (backResult.successful) {
                                backResult.asUpdateOutcome()
                            } else {
                                close(player)
                                backResult.asUpdateOutcome()
                            }
                        }
                        is MenuUpdate.Replace -> replaceResult(player, update.route).asUpdateOutcome()
                        is MenuUpdate.Navigate -> navigateFromResult(player, session.route, update.route).asUpdateOutcome()
                    }
                } catch (failure: Throwable) {
                    failure.rethrowIfUnrecoverableMenuRuntimeFailure()
                    plugin.logger.log(
                        Level.SEVERE,
                        "メニュー更新の適用に失敗しました: route=${definition.routeId} player=${player.uniqueId}",
                        failure,
                    )
                    MenuUpdateApplicationOutcome(
                        false,
                        MenuRuntimeUpdateFailureReason.EXCEPTION,
                    )
                }
                return MenuRuntimeUpdateApplication(
                    attempted = update != MenuUpdate.None,
                    applied = outcome.applied,
                    kind = declaredUpdate?.kind,
                    expectedRoute = expectedRoute,
                    observedRoute = null,
                    beforeRevision = originRevision,
                    afterRevision = null,
                    failureReason = outcome.failureReason,
                    operationResult = outcome.operationResult,
                    state = if (update == MenuUpdate.None) {
                        MenuRuntimeUpdateApplicationState.NOT_ATTEMPTED
                    } else {
                        outcome.state
                    },
                )
            }
        }
    }

    private fun navigateFrom(player: Player, currentRoute: MenuRoute, targetRoute: MenuRoute): Boolean =
        navigateFromResult(player, currentRoute, targetRoute).successful

    private fun navigateFromResult(
        player: Player,
        currentRoute: MenuRoute,
        targetRoute: MenuRoute,
    ): MenuRuntimeOperationResult = withoutOpenSound(player) {
        navigation.pushAndOpenResult(player, currentRoute, targetRoute)
            .forOperation(MenuRuntimeOperation.NAVIGATE)
    }

    private fun <T> withoutOpenSound(player: Player, action: () -> T): T {
        suppressOpenSound.add(player.uniqueId)
        return try {
            action()
        } finally {
            suppressOpenSound.remove(player.uniqueId)
        }
    }

    private fun playDefinitionOpenSound(
        player: Player,
        route: MenuRoute,
        definition: InventoryMenuDefinition,
        playOpenSound: Boolean,
    ) {
        if (playOpenSound && !isDialogTransition(player)) {
            val policy = definition.openSoundResolver?.invoke(route) ?: definition.openSound
            if (player.uniqueId in suppressOpenSound && policy !is MenuSoundPolicy.Custom) return
            when (policy) {
                MenuSoundPolicy.Default -> sounds.onMenuOpen(player, route.id)
                MenuSoundPolicy.Silent -> Unit
                is MenuSoundPolicy.Custom -> sounds.play(player, policy.sound)
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

    private fun inspectionFailure(
        route: MenuRoute,
        reason: MenuRuntimeOperationFailureReason,
        contractViolations: List<MenuContractViolation> = emptyList(),
        exceptionType: String? = null,
    ): MenuRuntimeInspectionResult = MenuRuntimeInspectionResult(
        MenuRuntimeOperationResult.failed(
            MenuRuntimeOperation.INSPECT,
            route,
            reason,
            contractViolations,
            exceptionType,
        ),
    )

    private fun inspectionSnapshot(
        player: Player,
        route: MenuRoute,
        inspectionNavigation: MenuRuntimeInspectionNavigationContext,
        view: InventoryMenuView,
    ): MenuRuntimeInspectionSnapshot = MenuRuntimeInspectionSnapshot(
        route = route.runtimeSnapshot(),
        breadcrumbs = inspectionNavigation.breadcrumbs.map { it.runtimeSnapshot() },
        canGoBack = inspectionNavigation.canGoBack,
        title = view.title,
        size = view.size,
        revision = presentations.current(player)?.revision ?: 0L,
        slots = canonicalElements(view).associateBy { it.slot }.values.sortedBy { it.slot }.map { element ->
            val item = element.item
            val meta = item.itemMeta
            MenuRuntimeInspectionSlotSnapshot(
                slot = element.slot,
                material = item.type,
                amount = item.amount,
                name = meta?.displayName(),
                lore = meta?.lore().orEmpty(),
                glint = hasGlint(item),
                role = element.role,
                enabled = element.enabled,
                interaction = element.resolvedInteraction().inspectionSnapshot(),
            ).also {
                it.presentationSemantics = element.effectivePresentationSemantics()
                it.capabilityComposition = it.presentationSemantics.capabilityComposition
            }
        },
    )

    private fun MenuInteraction.inspectionSnapshot(): MenuRuntimeInspectionInteractionSnapshot =
        MenuRuntimeInspectionInteractionSnapshotFactory.create(this)

    private fun completePendingTrace(
        player: Player,
        identity: MenuRuntimeClickTraceStore.Identity,
        completion: MenuRuntimeOperationResult,
    ) {
        val after = snapshotCurrent(player)
        clickTraces.update(player.uniqueId, identity) { trace ->
            val application = trace.application
            if (application.state != MenuRuntimeUpdateApplicationState.PENDING) return@update trace
            trace.copy(
                afterRevision = after?.revision,
                afterRoute = after?.route,
                application = application.copy(
                    applied = completion.successful,
                    observedRoute = after?.route,
                    afterRevision = after?.revision,
                    failureReason = if (completion.successful) {
                        MenuRuntimeUpdateFailureReason.NONE
                    } else {
                        completion.failure!!.reason.toUpdateFailureReason()
                    },
                    operationResult = completion,
                    state = MenuRuntimeUpdateApplicationState.TERMINAL,
                ),
            )
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
            is MenuInteraction.ClickBranches -> MenuRuntimeSlotKind.ACTION to MenuRuntimeInteractionKind.CLICK_BRANCHES
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
                    branch.reversibleContract?.diagnosticSnapshot(),
                )
            }
            .orEmpty()
        val acceptedClicks = when (interaction) {
            is MenuInteraction.Action -> interaction.acceptedClicks
            is MenuInteraction.Branches -> interaction.branches.flatMapTo(linkedSetOf()) { it.acceptedClicks }
            is MenuInteraction.ClickBranches -> interaction.branches.flatMapTo(linkedSetOf()) { it.acceptedClicks }
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
            is MenuInteraction.ClickBranches -> interaction.branches
                .map { it.interaction.safetyForSnapshot() }
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
            is MenuInteraction.ClickBranches -> buildMap {
                interaction.branches.forEach { branch ->
                    branch.acceptedClicks.forEach { click ->
                        put(click, branch.interaction.safetyForSnapshot(click))
                    }
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
            interaction?.inspectionSnapshot(),
            interaction?.reversibleContractsByClick().orEmpty(),
        ).also { snapshot ->
            snapshot.presentationSemantics = element?.effectivePresentationSemantics()
                ?: com.awabi2048.ccsystem.api.gui.MenuElementPresentationSemantics.opaque()
            snapshot.capabilityComposition = snapshot.presentationSemantics.capabilityComposition
        }
    }

    private fun MenuElement.effectivePresentationSemantics(): MenuElementPresentationSemantics {
        val resolved = resolvedInteraction()
        return when {
            resolved is MenuInteraction.Unavailable && resolved.message != null &&
                presentationSemantics.profile != MenuPresentationProfile.DISABLED ->
                presentationSemantics.copyPreservingCapabilityComposition(
                    profile = MenuPresentationProfile.DISABLED,
                    disabledReason = resolved.message,
                )
            resolved == MenuInteraction.DisplayOnly && presentationSemantics.profile in setOf(
                MenuPresentationProfile.SINGLE_STANDARD_ACTION,
                MenuPresentationProfile.SINGLE_CUSTOM_ACTION,
                MenuPresentationProfile.MULTI_ACTION,
                MenuPresentationProfile.PAGE_NAVIGATION,
            ) -> presentationSemantics.copyPreservingCapabilityComposition(profile = MenuPresentationProfile.DISPLAY_ONLY)
            else -> presentationSemantics
        }
    }

    private fun hasGlint(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return item.enchantments.isNotEmpty()
        return item.enchantments.isNotEmpty() ||
            (meta.hasEnchantmentGlintOverride() && meta.enchantmentGlintOverride)
    }

    private fun MenuRoute.runtimeSnapshot(): MenuRuntimeRouteSnapshot =
        MenuRuntimeRouteSnapshot(owner, id, payload.toSortedMap())

    private fun reversibleCaptureFailure(
        reason: MenuReversibleStateFailureReason,
        message: String? = null,
        exceptionType: String? = null,
    ): MenuReversibleStateCaptureResult =
        MenuReversibleStateCaptureResult.Failed(MenuReversibleStateFailure(reason, message, exceptionType))

    private fun reversibleRestoreFailure(
        reason: MenuReversibleStateFailureReason,
        message: String? = null,
        exceptionType: String? = null,
    ): MenuReversibleStateRestoreResult =
        MenuReversibleStateRestoreResult.Failed(MenuReversibleStateFailure(reason, message, exceptionType))

    private fun reversibleBindingFailure(
        reason: MenuReversibleStateFailureReason,
        message: String? = null,
        exceptionType: String? = null,
    ): MenuReversibleStateTraceBindingResult =
        MenuReversibleStateTraceBindingResult.Failed(MenuReversibleStateFailure(reason, message, exceptionType))

    private fun matchesReversibleSession(
        player: Player,
        expectedRoute: MenuRuntimeRouteSnapshot,
        expectedRevision: Long,
    ): Boolean {
        val holder = player.openInventory.topInventory.holder as? MenuRuntimeHolder ?: return false
        val session = sessions[player.uniqueId]
            ?.takeIf { it.route == holder.route && holder.playerId == player.uniqueId }
            ?: return false
        val current = snapshotCurrent(player) ?: return false
        return current.surface == MenuSurface.INVENTORY &&
            session.route.runtimeSnapshot() == expectedRoute &&
            current.route == expectedRoute &&
            current.revision == expectedRevision
    }

    private fun validateReversibleRestore(
        player: Player,
        entry: MenuReversibleStateTokenStore.Entry,
    ): MenuReversibleStateFailureReason? {
        val binding = entry.binding ?: return MenuReversibleStateFailureReason.TOKEN_UNBOUND
        if (binding.runId != entry.runId || clickTraces.currentRunId(player.uniqueId) != entry.runId) {
            return MenuReversibleStateFailureReason.RUN_MISMATCH
        }
        val provider = reversibleProviders.registration(entry.interaction.contract.providerId)
            ?: return MenuReversibleStateFailureReason.PROVIDER_UNREGISTERED
        if (provider.generation != entry.providerGeneration) {
            return MenuReversibleStateFailureReason.PROVIDER_GENERATION_MISMATCH
        }
        val trace = clickTraces.terminal(player.uniqueId, binding.runId, binding.sequence)
            ?: return MenuReversibleStateFailureReason.TRACE_NOT_TERMINAL
        if (!trace.matchesReversibleCapture(entry) ||
            trace.afterRoute != binding.route ||
            trace.afterRevision != binding.revision
        ) {
            return MenuReversibleStateFailureReason.TRACE_MISMATCH
        }
        val latest = clickTraces.latest(player.uniqueId)
        if (latest?.runId != binding.runId || latest.sequence != binding.sequence || !latest.application.terminal) {
            return MenuReversibleStateFailureReason.TRACE_MISMATCH
        }
        if (!matchesReversibleSession(player, binding.route, binding.revision)) {
            return MenuReversibleStateFailureReason.ROUTE_REVISION_MISMATCH
        }
        return null
    }

    private fun MenuRuntimeClickTrace.matchesReversibleCapture(
        entry: MenuReversibleStateTokenStore.Entry,
    ): Boolean =
        accepted &&
            result == MenuRuntimeActionResultKind.SUCCESS &&
            beforeRoute == entry.route.runtimeSnapshot() &&
            beforeRevision == entry.interaction.revision &&
            slot == entry.interaction.slot &&
            click == entry.interaction.click &&
            actionId == entry.interaction.actionId &&
            capabilityId == entry.interaction.capabilityId &&
            safety == MenuActionSafety.REVERSIBLE &&
            reversibleContract == entry.interaction.contract.diagnosticSnapshot()

    private fun MenuInteraction.resolveReversibleInteraction(click: ClickType): ReversibleInteraction? = when (this) {
        MenuInteraction.DisplayOnly,
        is MenuInteraction.Unavailable,
        is MenuInteraction.Back -> null
        is MenuInteraction.Action ->
            takeIf { click in acceptedClicks }?.let {
                ReversibleInteraction(
                    actionId,
                    capabilityId,
                    safetyFor(click),
                    reversibleContractFor(click),
                    payload,
                    emptyMap(),
                )
            }
        is MenuInteraction.Branches -> branches.singleOrNull { click in it.acceptedClicks }?.let { branch ->
            ReversibleInteraction(
                branch.actionId,
                null,
                branch.safety,
                branch.reversibleContract,
                branch.payload,
                emptyMap(),
            )
        }
        is MenuInteraction.ClickBranches -> resolve(click)?.resolveReversibleInteraction(click)
        is MenuInteraction.Capability ->
            takeIf { click in acceptedClicks }?.let {
                ReversibleInteraction(
                    null,
                    capabilityId,
                    safetyFor(click),
                    reversibleContractFor(click),
                    arguments,
                    attributes,
                )
            }
    }

    private fun MenuInteraction.reversibleContractsByClick(): Map<ClickType, MenuRuntimeReversibleContractSnapshot> = when (this) {
        MenuInteraction.DisplayOnly,
        is MenuInteraction.Unavailable,
        is MenuInteraction.Back -> emptyMap()
        is MenuInteraction.Action -> acceptedClicks.mapNotNull { click ->
            reversibleContractFor(click)?.let { click to it.diagnosticSnapshot() }
        }.toMap()
        is MenuInteraction.Branches -> branches.flatMap { branch ->
            branch.reversibleContract?.let { contract ->
                branch.acceptedClicks.map { click -> click to contract.diagnosticSnapshot() }
            }.orEmpty()
        }.toMap()
        is MenuInteraction.ClickBranches -> branches.flatMap { branch ->
            branch.interaction.reversibleContractsByClick()
                .filterKeys { it in branch.acceptedClicks }
                .entries
                .map { it.key to it.value }
        }.toMap()
        is MenuInteraction.Capability -> acceptedClicks.mapNotNull { click ->
            reversibleContractFor(click)?.let { click to it.diagnosticSnapshot() }
        }.toMap()
    }.toSortedMap(compareBy(ClickType::name))

    private fun MenuReversibleContract.diagnosticSnapshot(): MenuRuntimeReversibleContractSnapshot =
        MenuRuntimeReversibleContractSnapshot(providerId, diagnosticArguments())

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
        application: MenuRuntimeUpdateApplication? = null,
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
        val resolvedApplication = (application ?: MenuRuntimeUpdateApplication.notAttempted(
            kind = update?.kind,
            expectedRoute = update?.route,
            beforeRevision = context.beforeRevision,
            failureReason = if (exception == null) {
                MenuRuntimeUpdateFailureReason.NOT_APPLICABLE
            } else {
                MenuRuntimeUpdateFailureReason.EXCEPTION
            },
        )).let { candidate ->
            if (exception != null && !candidate.attempted) {
                candidate.copy(failureReason = MenuRuntimeUpdateFailureReason.EXCEPTION)
            } else {
                candidate
            }
        }.copy(
            observedRoute = after?.route,
            afterRevision = after?.revision,
        )
        clickTraces.append(
            player.uniqueId,
            context.identity,
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
                resolvedApplication,
                details.reversibleContract,
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
            reversibleContractFor(click)?.diagnosticSnapshot(),
        )
        is MenuInteraction.Branches -> {
            val branch = branches.singleOrNull { click in it.acceptedClicks }
            InteractionTraceDetails(
                MenuRuntimeInteractionKind.BRANCHES,
                branch?.actionId,
                null,
                branch?.payload?.toSortedMap().orEmpty(),
                branch?.safety ?: MenuActionSafety.UNSPECIFIED,
                branch?.reversibleContract?.diagnosticSnapshot(),
            )
        }
        is MenuInteraction.ClickBranches -> {
            val branch = branches.singleOrNull { click in it.acceptedClicks }
            branch?.interaction?.traceDetails(click) ?: InteractionTraceDetails(
                MenuRuntimeInteractionKind.CLICK_BRANCHES,
                null,
                null,
                emptyMap(),
                MenuActionSafety.UNSPECIFIED,
            )
        }
        is MenuInteraction.Capability -> InteractionTraceDetails(
            MenuRuntimeInteractionKind.CAPABILITY,
            null,
            capabilityId,
            arguments.toSortedMap(),
            safetyFor(click),
            reversibleContractFor(click)?.diagnosticSnapshot(),
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

    private fun MenuInteraction.safetyForSnapshot(): MenuActionSafety = when (this) {
        MenuInteraction.DisplayOnly,
        is MenuInteraction.Unavailable -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Action -> safety
        is MenuInteraction.Branches,
        is MenuInteraction.ClickBranches -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Capability -> safety
        is MenuInteraction.Back -> MenuActionSafety.NAVIGATION_ONLY
    }

    private fun MenuInteraction.safetyForSnapshot(click: ClickType): MenuActionSafety = when (this) {
        MenuInteraction.DisplayOnly,
        is MenuInteraction.Unavailable -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Action -> safetyFor(click)
        is MenuInteraction.Branches,
        is MenuInteraction.ClickBranches -> MenuActionSafety.UNSPECIFIED
        is MenuInteraction.Capability -> safetyFor(click)
        is MenuInteraction.Back -> MenuActionSafety.NAVIGATION_ONLY
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

    private data class MenuUpdateApplicationOutcome(
        val applied: Boolean,
        val failureReason: MenuRuntimeUpdateFailureReason,
        val operationResult: MenuRuntimeOperationResult? = null,
        val state: MenuRuntimeUpdateApplicationState = MenuRuntimeUpdateApplicationState.TERMINAL,
    )

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
        val reversibleContract: MenuRuntimeReversibleContractSnapshot? = null,
    )

    private data class ReversibleInteraction(
        val actionId: String?,
        val capabilityId: String?,
        val safety: MenuActionSafety,
        val contract: MenuReversibleContract?,
        val arguments: Map<String, String>,
        val attributes: Map<String, Any>,
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

/** 非同期finishが完了した後も、呼出し元が詳細診断を取得できるよう保持します。 */
internal class MenuRuntimeExternalFinishResultStore {
    private val results = ConcurrentHashMap<UUID, MenuRuntimeOperationResult>()

    fun record(playerId: UUID, result: MenuRuntimeOperationResult) {
        require(result.operation == MenuRuntimeOperation.FINISH_EXTERNAL) {
            "external finish store accepts FINISH_EXTERNAL results only"
        }
        results[playerId] = result
    }

    fun latest(playerId: UUID): MenuRuntimeOperationResult? = results[playerId]

    fun clear(playerId: UUID) {
        results.remove(playerId)
    }
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
