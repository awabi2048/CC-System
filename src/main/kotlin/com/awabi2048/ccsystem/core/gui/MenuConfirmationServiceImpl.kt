package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.api.gui.GuiElementRole
import com.awabi2048.ccsystem.api.gui.GuiLoreSpec
import com.awabi2048.ccsystem.api.gui.GuiMenuDisplaySpec
import com.awabi2048.ccsystem.api.gui.GuiMenuActionIntent
import com.awabi2048.ccsystem.api.gui.GuiStructuredMenuEntrySpec
import com.awabi2048.ccsystem.api.gui.GuiLayoutService
import com.awabi2048.ccsystem.api.gui.InventoryMenuDefinition
import com.awabi2048.ccsystem.api.gui.InventoryMenuView
import com.awabi2048.ccsystem.api.gui.MenuActionContext
import com.awabi2048.ccsystem.api.gui.MenuActionHandler
import com.awabi2048.ccsystem.api.gui.MenuActionResult
import com.awabi2048.ccsystem.api.gui.MenuActionSoundPolicy
import com.awabi2048.ccsystem.api.gui.MenuCloseContext
import com.awabi2048.ccsystem.api.gui.MenuCloseHandler
import com.awabi2048.ccsystem.api.gui.MenuConfirmationDraft
import com.awabi2048.ccsystem.api.gui.MenuConfirmationService
import com.awabi2048.ccsystem.api.gui.MenuRoute
import com.awabi2048.ccsystem.api.gui.MenuRuntimeService
import com.awabi2048.ccsystem.api.gui.MenuViewCategory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player

internal class MenuConfirmationServiceImpl(
    private val runtime: MenuRuntimeService,
    private val layouts: GuiLayoutService,
) : MenuConfirmationService {
    private val registeredOwners = ConcurrentHashMap.newKeySet<String>()
    private val drafts = ConcurrentHashMap<UUID, OwnedDraft>()

    override fun prepare(player: Player, draft: MenuConfirmationDraft): MenuRoute {
        require(draft.owner.isNotBlank()) { "Confirmation draft owner must not be blank" }
        ensureRegistered(draft.owner)
        val token = UUID.randomUUID()
        drafts[token] = OwnedDraft(
            player.uniqueId,
            draft,
        )
        return MenuRoute(
            draft.owner,
            ROUTE_ID,
            mapOf(TOKEN to token.toString(), MENU_ID to draft.menuId),
        )
    }

    override fun open(player: Player, draft: MenuConfirmationDraft): Boolean {
        val route = prepare(player, draft)
        val opened = runtime.navigate(player, route)
        if (!opened) token(route)?.let(drafts::remove)
        return opened
    }

    override fun clearOwner(owner: String) {
        registeredOwners.remove(owner)
        drafts.entries.removeIf { it.value.draft.owner == owner }
    }

    private fun ensureRegistered(owner: String) {
        if (!registeredOwners.add(owner)) return
        runtime.register(
            InventoryMenuDefinition(
                owner = owner,
                id = ROUTE_ID,
                renderer = { context -> render(context.player, context.route) },
                actions = mapOf(
                    ACTION_CONFIRM to MenuActionHandler(::confirm),
                    ACTION_CANCEL to MenuActionHandler(::cancel),
                ),
                onClose = MenuCloseHandler(::closed),
                openSound = com.awabi2048.ccsystem.api.gui.MenuSoundPresets.CONFIRMATION_OPEN,
            ),
        )
    }

    private fun render(player: Player, route: MenuRoute): InventoryMenuView {
        val draft = ownedDraft(route).draft
        val layout = layouts.confirmation45()
        val elements = CCSystem.getAPI().getGuiElementService()
        return InventoryMenuView(
            size = layout.size,
            title = draft.title,
            elements = listOf(
                elements.menuStructuredEntry(
                    player,
                    GuiStructuredMenuEntrySpec(
                        layout.confirmSlot,
                        draft.confirmItem.copy(
                            lore = GuiLoreSpec.NameOnly,
                            role = GuiElementRole.CONFIRM,
                        ),
                        listOf(GuiMenuActionIntent.Confirm(
                            ACTION_CONFIRM,
                            draft.confirmActionText,
                        )),
                        sounds = MenuActionSoundPolicy(success = draft.confirmSound),
                    ),
                ),
                elements.menuDisplay(
                    GuiMenuDisplaySpec(
                        layout.previewSlot,
                        draft.previewItem,
                        playerHeadOwner = draft.previewPlayerHeadOwner,
                    ),
                ),
                elements.menuStructuredEntry(
                    player,
                    GuiStructuredMenuEntrySpec(
                        layout.cancelSlot,
                        draft.cancelItem.copy(
                            lore = GuiLoreSpec.NameOnly,
                            role = GuiElementRole.CANCEL,
                        ),
                        listOf(GuiMenuActionIntent.Cancel(
                            ACTION_CANCEL,
                            draft.cancelActionText,
                        )),
                        sounds = MenuActionSoundPolicy(success = draft.cancelSound),
                    ),
                ),
            ),
            category = MenuViewCategory.CONFIRMATION,
        )
    }

    private fun confirm(context: MenuActionContext): MenuActionResult =
        removeOwned(context.player, context.route)?.draft?.onConfirm?.invoke()
            ?: MenuActionResult.Rejected()

    private fun cancel(context: MenuActionContext): MenuActionResult =
        removeOwned(context.player, context.route)?.draft?.onCancel?.invoke()
            ?: MenuActionResult.Rejected()

    private fun closed(context: MenuCloseContext) {
        removeOwned(context.player, context.route)?.draft?.onAbandon?.invoke()
    }

    private fun ownedDraft(route: MenuRoute): OwnedDraft =
        token(route)?.let(drafts::get) ?: error("確認画面Draftが見つかりません")

    private fun removeOwned(player: Player, route: MenuRoute): OwnedDraft? {
        val token = token(route) ?: return null
        val draft = drafts[token] ?: return null
        if (draft.playerId != player.uniqueId) return null
        return drafts.remove(token)
    }

    private fun token(route: MenuRoute): UUID? =
        route.payload[TOKEN]?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private data class OwnedDraft(
        val playerId: UUID,
        val draft: MenuConfirmationDraft,
    )

    private companion object {
        const val ROUTE_ID = "cc_confirmation"
        const val TOKEN = "token"
        const val MENU_ID = "menu_id"
        const val ACTION_CONFIRM = "confirm"
        const val ACTION_CANCEL = "cancel"
    }
}
