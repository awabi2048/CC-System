package com.awabi2048.ccsystem.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory

/** CC-Systemが管理するメニューの表示方式。 */
enum class MenuSurface {
    INVENTORY,
    DIALOG,
    FORM,
}

/** 既定音、明示的な無音、任意音を曖昧さなく表す。 */
sealed interface MenuSoundPolicy {
    data object Default : MenuSoundPolicy
    data object Silent : MenuSoundPolicy
    data class Custom(val sound: MenuSound) : MenuSoundPolicy
}

data class MenuActionSoundPolicy(
    val success: MenuSoundPolicy = MenuSoundPolicy.Default,
    val rejected: MenuSoundPolicy = MenuSoundPolicy.Default,
)

/**
 * 操作の副作用をドメイン側が明示するための区分です。
 *
 * Runtime はこの値から操作可否を推測しません。診断・監査の実行計画が、
 * 表示文言や素材ではなく、画面定義が宣言した事実を利用するための情報です。
 */
enum class MenuActionSafety {
    UNSPECIFIED,
    NAVIGATION_ONLY,
    REVERSIBLE,
    CONFIRM_ENTRY,
    IRREVERSIBLE,
    EXTERNAL_SIDE_EFFECT,
    INPUT_OR_EXTERNAL_SURFACE,
}

/** Inventory上の1要素。actionIdがない要素は表示専用として扱う。 */
sealed interface MenuInteraction {
    data object DisplayOnly : MenuInteraction

    class Action(
        val actionId: String,
        acceptedClicks: Set<ClickType> = MenuAcceptedClicks.STANDARD,
        payload: Map<String, String> = emptyMap(),
        val sounds: MenuActionSoundPolicy? = null,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        val capabilityId: String? = null,
        safetyByClick: Map<ClickType, MenuActionSafety> = emptyMap(),
        reversibleContract: MenuReversibleContract? = null,
        reversibleContractByClick: Map<ClickType, MenuReversibleContract> = emptyMap(),
    ) : MenuInteraction {
        val acceptedClicks: Set<ClickType> = MenuImmutableCollections.orderedSet(acceptedClicks, compareBy(ClickType::name))
        val payload: Map<String, String> = MenuImmutableCollections.strings(payload)
        val safetyByClick: Map<ClickType, MenuActionSafety> =
            MenuImmutableCollections.orderedMap(safetyByClick, compareBy(ClickType::name))
        val reversibleContract: MenuReversibleContract? = reversibleContract?.copy()
        val reversibleContractByClick: Map<ClickType, MenuReversibleContract> = MenuImmutableCollections.orderedMap(
            reversibleContractByClick.mapValues { (_, contract) -> contract.copy() },
            compareBy(ClickType::name),
        )
        init {
            require(actionId.isNotBlank()) { "actionId must not be blank" }
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
            require(safetyByClick.keys.all { it in acceptedClicks }) {
                "action safety may only be declared for accepted clicks"
            }
            require(reversibleContractByClick.keys.all { it in acceptedClicks }) {
                "action reversible contract may only be declared for accepted clicks"
            }
            requireReversibleContractSafety(
                acceptedClicks,
                { click -> safetyByClick[click] ?: safety },
                { click -> reversibleContractByClick[click] ?: reversibleContract },
                "action",
            )
        }

        fun safetyFor(click: ClickType): MenuActionSafety = safetyByClick[click] ?: safety

        fun reversibleContractFor(click: ClickType): MenuReversibleContract? =
            reversibleContractByClick[click] ?: reversibleContract

        fun copy(actionId: String = this.actionId, acceptedClicks: Set<ClickType> = this.acceptedClicks, payload: Map<String, String> = this.payload, sounds: MenuActionSoundPolicy? = this.sounds, safety: MenuActionSafety = this.safety, capabilityId: String? = this.capabilityId, safetyByClick: Map<ClickType, MenuActionSafety> = this.safetyByClick, reversibleContract: MenuReversibleContract? = this.reversibleContract, reversibleContractByClick: Map<ClickType, MenuReversibleContract> = this.reversibleContractByClick): Action = Action(actionId, acceptedClicks, payload, sounds, safety, capabilityId, safetyByClick, reversibleContract, reversibleContractByClick)
        operator fun component1(): String = actionId
        operator fun component2(): Set<ClickType> = acceptedClicks
        operator fun component3(): Map<String, String> = payload
        operator fun component4(): MenuActionSoundPolicy? = sounds
        operator fun component5(): MenuActionSafety = safety
        operator fun component6(): String? = capabilityId
        operator fun component7(): Map<ClickType, MenuActionSafety> = safetyByClick
        operator fun component8(): MenuReversibleContract? = reversibleContract
        operator fun component9(): Map<ClickType, MenuReversibleContract> = reversibleContractByClick
        override fun equals(other: Any?): Boolean = other is Action && actionId == other.actionId && acceptedClicks == other.acceptedClicks && payload == other.payload && sounds == other.sounds && safety == other.safety && capabilityId == other.capabilityId && safetyByClick == other.safetyByClick && reversibleContract == other.reversibleContract && reversibleContractByClick == other.reversibleContractByClick
        override fun hashCode(): Int = arrayOf(actionId, acceptedClicks, payload, sounds, safety, capabilityId, safetyByClick, reversibleContract, reversibleContractByClick).dataClassHashCode()
        override fun toString(): String = "Action(actionId=$actionId, acceptedClicks=$acceptedClicks, payload=$payload, sounds=$sounds, safety=$safety, capabilityId=$capabilityId, safetyByClick=$safetyByClick, reversibleContract=$reversibleContract, reversibleContractByClick=$reversibleContractByClick)"
    }

    /**
     * クリック種別ごとに異なるActionへ分岐する宣言。
     * 表示案内とクリック受付を同じBranch群から生成するために使用する。
     */
    class Branches(
        branches: List<MenuActionBranch>,
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        val branches: List<MenuActionBranch> = MenuImmutableCollections.list(branches)
        init {
            require(branches.isNotEmpty()) { "branches must not be empty" }
            val accepted = branches.flatMap(MenuActionBranch::acceptedClicks)
            require(accepted.size == accepted.distinct().size) {
                "a click type cannot be assigned to multiple action branches"
            }
        }
        fun copy(branches: List<MenuActionBranch> = this.branches, sounds: MenuActionSoundPolicy? = this.sounds): Branches = Branches(branches, sounds)
        operator fun component1(): List<MenuActionBranch> = branches
        operator fun component2(): MenuActionSoundPolicy? = sounds
        override fun equals(other: Any?): Boolean = other is Branches && branches == other.branches && sounds == other.sounds
        override fun hashCode(): Int = arrayOf(branches, sounds).dataClassHashCode()
        override fun toString(): String = "Branches(branches=$branches, sounds=$sounds)"
    }

    /**
     * clickごとに異なる最終interactionを選択する分岐です。
     *
     * [Branches]は既存のAction handler分岐として維持します。Capabilityを含む異種の
     * interactionを同一slotへ置く場合だけ、この型を使用します。
     */
    class ClickBranches(
        branches: List<MenuInteractionBranch>,
    ) : MenuInteraction {
        val branches: List<MenuInteractionBranch> = MenuImmutableCollections.list(branches)
        init {
            require(branches.isNotEmpty()) { "branches must not be empty" }
            val accepted = branches.flatMap(MenuInteractionBranch::acceptedClicks)
            require(accepted.size == accepted.distinct().size) {
                "a click type cannot be assigned to multiple interaction branches"
            }
        }
        fun resolve(click: ClickType): MenuInteraction? =
            branches.singleOrNull { click in it.acceptedClicks }?.interaction
        fun copy(branches: List<MenuInteractionBranch> = this.branches): ClickBranches = ClickBranches(branches)
        operator fun component1(): List<MenuInteractionBranch> = branches
        override fun equals(other: Any?): Boolean = other is ClickBranches && branches == other.branches
        override fun hashCode(): Int = branches.hashCode()
        override fun toString(): String = "ClickBranches(branches=$branches)"
    }

    class Capability(
        val capabilityId: String,
        arguments: Map<String, String> = emptyMap(),
        attributes: Map<String, Any> = emptyMap(),
        acceptedClicks: Set<ClickType> = MenuAcceptedClicks.STANDARD,
        val sounds: MenuActionSoundPolicy? = null,
        val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
        safetyByClick: Map<ClickType, MenuActionSafety> = emptyMap(),
        reversibleContract: MenuReversibleContract? = null,
        reversibleContractByClick: Map<ClickType, MenuReversibleContract> = emptyMap(),
    ) : MenuInteraction {
        val arguments: Map<String, String> = MenuImmutableCollections.strings(arguments)
        /** 任意attribute値はcapture callback用live handleを許すため、containerだけを防御copyします。 */
        val attributes: Map<String, Any> = MenuImmutableCollections.orderedMap(attributes, compareBy<String> { it })
        val acceptedClicks: Set<ClickType> = MenuImmutableCollections.orderedSet(acceptedClicks, compareBy(ClickType::name))
        val safetyByClick: Map<ClickType, MenuActionSafety> =
            MenuImmutableCollections.orderedMap(safetyByClick, compareBy(ClickType::name))
        val reversibleContract: MenuReversibleContract? = reversibleContract?.copy()
        val reversibleContractByClick: Map<ClickType, MenuReversibleContract> = MenuImmutableCollections.orderedMap(
            reversibleContractByClick.mapValues { (_, contract) -> contract.copy() },
            compareBy(ClickType::name),
        )
        init {
            require(capabilityId.isNotBlank()) { "capabilityId must not be blank" }
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
            require(safetyByClick.keys.all { it in acceptedClicks }) {
                "capability safety may only be declared for accepted clicks"
            }
            require(reversibleContractByClick.keys.all { it in acceptedClicks }) {
                "capability reversible contract may only be declared for accepted clicks"
            }
            requireReversibleContractSafety(
                acceptedClicks,
                { click -> safetyByClick[click] ?: safety },
                { click -> reversibleContractByClick[click] ?: reversibleContract },
                "capability",
            )
        }

        fun safetyFor(click: ClickType): MenuActionSafety = safetyByClick[click] ?: safety

        fun reversibleContractFor(click: ClickType): MenuReversibleContract? =
            reversibleContractByClick[click] ?: reversibleContract

        fun copy(capabilityId: String = this.capabilityId, arguments: Map<String, String> = this.arguments, attributes: Map<String, Any> = this.attributes, acceptedClicks: Set<ClickType> = this.acceptedClicks, sounds: MenuActionSoundPolicy? = this.sounds, safety: MenuActionSafety = this.safety, safetyByClick: Map<ClickType, MenuActionSafety> = this.safetyByClick, reversibleContract: MenuReversibleContract? = this.reversibleContract, reversibleContractByClick: Map<ClickType, MenuReversibleContract> = this.reversibleContractByClick): Capability = Capability(capabilityId, arguments, attributes, acceptedClicks, sounds, safety, safetyByClick, reversibleContract, reversibleContractByClick)
        operator fun component1(): String = capabilityId
        operator fun component2(): Map<String, String> = arguments
        operator fun component3(): Map<String, Any> = attributes
        operator fun component4(): Set<ClickType> = acceptedClicks
        operator fun component5(): MenuActionSoundPolicy? = sounds
        operator fun component6(): MenuActionSafety = safety
        operator fun component7(): Map<ClickType, MenuActionSafety> = safetyByClick
        operator fun component8(): MenuReversibleContract? = reversibleContract
        operator fun component9(): Map<ClickType, MenuReversibleContract> = reversibleContractByClick
        override fun equals(other: Any?): Boolean = other is Capability && capabilityId == other.capabilityId && arguments == other.arguments && attributes == other.attributes && acceptedClicks == other.acceptedClicks && sounds == other.sounds && safety == other.safety && safetyByClick == other.safetyByClick && reversibleContract == other.reversibleContract && reversibleContractByClick == other.reversibleContractByClick
        override fun hashCode(): Int = arrayOf(capabilityId, arguments, attributes, acceptedClicks, sounds, safety, safetyByClick, reversibleContract, reversibleContractByClick).dataClassHashCode()
        override fun toString(): String = "Capability(capabilityId=$capabilityId, arguments=$arguments, attributes=$attributes, acceptedClicks=$acceptedClicks, sounds=$sounds, safety=$safety, safetyByClick=$safetyByClick, reversibleContract=$reversibleContract, reversibleContractByClick=$reversibleContractByClick)"
    }

    data class Unavailable(
        val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.STANDARD,
        val message: Component? = null,
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        init {
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        }
    }

    data class Back(
        val acceptedClicks: Set<ClickType> = MenuAcceptedClicks.STANDARD,
        val sounds: MenuActionSoundPolicy? = null,
    ) : MenuInteraction {
        init {
            require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        }
    }
}

class MenuActionBranch(
    val actionId: String,
    acceptedClicks: Set<ClickType>,
    payload: Map<String, String> = emptyMap(),
    val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
    reversibleContract: MenuReversibleContract? = null,
) {
    val acceptedClicks: Set<ClickType> = MenuImmutableCollections.orderedSet(acceptedClicks, compareBy(ClickType::name))
    val payload: Map<String, String> = MenuImmutableCollections.strings(payload)
    val reversibleContract: MenuReversibleContract? = reversibleContract?.copy()
    init {
        require(actionId.isNotBlank()) { "actionId must not be blank" }
        require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        requireReversibleContractSafety(
            acceptedClicks,
            { safety },
            { reversibleContract },
            "action branch",
        )
    }
    fun copy(actionId: String = this.actionId, acceptedClicks: Set<ClickType> = this.acceptedClicks, payload: Map<String, String> = this.payload, safety: MenuActionSafety = this.safety, reversibleContract: MenuReversibleContract? = this.reversibleContract): MenuActionBranch = MenuActionBranch(actionId, acceptedClicks, payload, safety, reversibleContract)
    operator fun component1(): String = actionId
    operator fun component2(): Set<ClickType> = acceptedClicks
    operator fun component3(): Map<String, String> = payload
    operator fun component4(): MenuActionSafety = safety
    operator fun component5(): MenuReversibleContract? = reversibleContract
    override fun equals(other: Any?): Boolean = other is MenuActionBranch && actionId == other.actionId && acceptedClicks == other.acceptedClicks && payload == other.payload && safety == other.safety && reversibleContract == other.reversibleContract
    override fun hashCode(): Int = arrayOf(actionId, acceptedClicks, payload, safety, reversibleContract).dataClassHashCode()
    override fun toString(): String = "MenuActionBranch(actionId=$actionId, acceptedClicks=$acceptedClicks, payload=$payload, safety=$safety, reversibleContract=$reversibleContract)"
}

internal fun requireReversibleContractSafety(
    acceptedClicks: Set<ClickType>,
    safetyFor: (ClickType) -> MenuActionSafety,
    contractFor: (ClickType) -> MenuReversibleContract?,
    subject: String,
) {
    acceptedClicks.forEach { click ->
        val reversible = safetyFor(click) == MenuActionSafety.REVERSIBLE
        val hasContract = contractFor(click) != null
        require(reversible == hasContract) {
            "$subject reversible contract must be declared exactly for REVERSIBLE clicks: $click"
        }
    }
}

object MenuAcceptedClicks {
    val STANDARD: Set<ClickType> = setOf(
        ClickType.LEFT,
        ClickType.RIGHT,
        ClickType.SHIFT_LEFT,
        ClickType.SHIFT_RIGHT,
        ClickType.MIDDLE,
    )
    val LEFT: Set<ClickType> = setOf(ClickType.LEFT, ClickType.SHIFT_LEFT)
    val RIGHT: Set<ClickType> = setOf(ClickType.RIGHT, ClickType.SHIFT_RIGHT)
    val LEFT_RIGHT: Set<ClickType> = LEFT + RIGHT
    val PLAIN_LEFT: Set<ClickType> = setOf(ClickType.LEFT)
    val PLAIN_RIGHT: Set<ClickType> = setOf(ClickType.RIGHT)
    val PLAIN_LEFT_RIGHT: Set<ClickType> = PLAIN_LEFT + PLAIN_RIGHT
    val SHIFT_LEFT: Set<ClickType> = setOf(ClickType.SHIFT_LEFT)
    val SHIFT_RIGHT: Set<ClickType> = setOf(ClickType.SHIFT_RIGHT)
    val SHIFT_LEFT_RIGHT: Set<ClickType> = SHIFT_LEFT + SHIFT_RIGHT
    val MIDDLE: Set<ClickType> = setOf(ClickType.MIDDLE)
}

/** [MenuInteraction.ClickBranches]の1 click範囲と最終interactionです。 */
data class MenuInteractionBranch(
    val acceptedClicks: Set<ClickType>,
    val interaction: MenuInteraction,
) {
    init {
        require(acceptedClicks.isNotEmpty()) { "acceptedClicks must not be empty" }
        require(interaction !is MenuInteraction.Branches && interaction !is MenuInteraction.ClickBranches) {
            "interaction branches must resolve to a final interaction"
        }
        require(interaction.acceptedClicksForBranch() == acceptedClicks) {
            "interaction branch clicks must match its final interaction contract"
        }
    }
}

private fun MenuInteraction.acceptedClicksForBranch(): Set<ClickType> = when (this) {
    MenuInteraction.DisplayOnly -> emptySet()
    is MenuInteraction.Action -> acceptedClicks
    is MenuInteraction.Branches,
    is MenuInteraction.ClickBranches -> error("nested interaction branches are not final")
    is MenuInteraction.Capability -> acceptedClicks
    is MenuInteraction.Unavailable -> acceptedClicks
    is MenuInteraction.Back -> acceptedClicks
}

private fun Array<out Any?>.dataClassHashCode(): Int {
    if (isEmpty()) return 0
    var result = this[0]?.hashCode() ?: 0
    for (index in 1 until size) result = 31 * result + (this[index]?.hashCode() ?: 0)
    return result
}

data class MenuElement(
    val slot: Int,
    val item: ItemStack,
    val role: GuiElementRole,
    val actionId: String? = null,
    val actionPayload: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val sounds: MenuActionSoundPolicy? = null,
    val interaction: MenuInteraction? = null,
    val actionSafety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
) {
    init {
        require(slot >= 0) { "slot must not be negative" }
        require(actionId?.isNotBlank() != false) { "actionId must not be blank" }
        require(actionId != null || actionPayload.isEmpty()) { "display-only elements cannot have action payload" }
        require(role != GuiElementRole.DECORATION || actionId == null) {
            "decoration elements cannot have an action"
        }
        require(
            interaction !is MenuInteraction.Action &&
                interaction !is MenuInteraction.Capability &&
                interaction !is MenuInteraction.ClickBranches ||
                role != GuiElementRole.DECORATION
        ) {
            "decoration elements cannot have an interaction action"
        }
        require(interaction !is MenuInteraction.Back || role == GuiElementRole.BACK) {
            "back interaction requires BACK role"
        }
    }

    fun resolvedInteraction(): MenuInteraction = interaction ?: when {
        role == GuiElementRole.BACK -> MenuInteraction.Back(sounds = sounds)
        !enabled -> MenuInteraction.Unavailable(sounds = sounds)
        actionId != null -> MenuInteraction.Action(
            actionId,
            payload = actionPayload,
            sounds = sounds,
            safety = actionSafety,
        )
        else -> MenuInteraction.DisplayOnly
    }
}

data class InventoryMenuView(
    val size: Int,
    val title: Component,
    val elements: List<MenuElement>,
    val standardFrame: Boolean = true,
    val inputSlots: Set<Int> = emptySet(),
    val inputItems: Map<Int, ItemStack> = emptyMap(),
    val playerInventoryInteraction: PlayerInventoryInteraction = PlayerInventoryInteraction.INTERACTIVE,
) {
    @Deprecated(
        message = "playerInventoryInteractionで画面の入力モードを明示してください",
        replaceWith = ReplaceWith("playerInventoryInteraction != PlayerInventoryInteraction.BLOCKED"),
    )
    val allowPlayerInventoryInteraction: Boolean
        get() = playerInventoryInteraction != PlayerInventoryInteraction.BLOCKED

    constructor(
        size: Int,
        title: Component,
        elements: List<MenuElement>,
        standardFrame: Boolean,
        inputSlots: Set<Int>,
        inputItems: Map<Int, ItemStack>,
        allowPlayerInventoryInteraction: Boolean,
    ) : this(
        size = size,
        title = title,
        elements = elements,
        standardFrame = standardFrame,
        inputSlots = inputSlots,
        inputItems = inputItems,
        playerInventoryInteraction =
            if (allowPlayerInventoryInteraction) {
                PlayerInventoryInteraction.INTERACTIVE
            } else {
                PlayerInventoryInteraction.BLOCKED
            },
    )

    @Suppress("UNUSED_PARAMETER")
    constructor(
        size: Int,
        title: Component,
        elements: List<MenuElement>,
        standardFrame: Boolean,
        inputSlots: Set<Int>?,
        allowPlayerInventoryInteraction: Boolean,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        size = size,
        title = title,
        elements = elements,
        standardFrame = if (mask and 0x08 != 0) true else standardFrame,
        inputSlots = if (mask and 0x10 != 0) emptySet() else requireNotNull(inputSlots),
        inputItems = emptyMap(),
        playerInventoryInteraction =
            if (mask and 0x20 != 0 || allowPlayerInventoryInteraction) {
                PlayerInventoryInteraction.INTERACTIVE
            } else {
                PlayerInventoryInteraction.BLOCKED
            },
    )

    @Suppress("UNUSED_PARAMETER")
    constructor(
        size: Int,
        title: Component,
        elements: List<MenuElement>,
        standardFrame: Boolean,
        inputSlots: Set<Int>?,
        inputItems: Map<Int, ItemStack>?,
        allowPlayerInventoryInteraction: Boolean,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        size = size,
        title = title,
        elements = elements,
        standardFrame = if (mask and 0x08 != 0) true else standardFrame,
        inputSlots = if (mask and 0x10 != 0) emptySet() else requireNotNull(inputSlots),
        inputItems = if (mask and 0x20 != 0) emptyMap() else requireNotNull(inputItems),
        playerInventoryInteraction =
            if (mask and 0x40 != 0 || allowPlayerInventoryInteraction) {
                PlayerInventoryInteraction.INTERACTIVE
            } else {
                PlayerInventoryInteraction.BLOCKED
            },
    )

    init {
        require(size > 0 && size % 9 == 0) { "inventory menu size must be a positive multiple of 9" }
        require(elements.all { it.slot < size }) { "menu element slot is outside the inventory" }
        require(elements.map { it.slot }.distinct().size == elements.size) { "menu element slots must be unique" }
        require(inputSlots.all { it in 0 until size }) { "input slot is outside the inventory" }
        require(elements.none { it.slot in inputSlots }) { "input slots cannot contain rendered menu elements" }
        require(inputItems.keys.all { it in inputSlots }) { "input items must be placed in declared input slots" }
    }
}

data class MenuRenderContext(
    val player: Player,
    val route: MenuRoute,
    val canGoBack: Boolean = false,
)

/**
 * 完成viewを生成する読取専用rendererです。
 *
 * Runtimeはopen/refreshだけでなくinspectでもこの関数を呼びます。ワールド、プレイヤー、
 * 設定、Capability registry、外部サービスを変更せず、同じcontextから同じ論理viewを返してください。
 */
fun interface InventoryMenuRenderer {
    fun render(context: MenuRenderContext): InventoryMenuView
}

data class MenuActionContext(
    val player: Player,
    val route: MenuRoute,
    val actionId: String,
    val slot: Int,
    val payload: Map<String, String>,
    val click: ClickType,
    val item: ItemStack,
    val cursor: ItemStack,
)

/** Runtimeがプレイヤーインベントリ側の選択をActionへ渡す際に使用する予約Action。 */
object MenuRuntimeActions {
    const val PLAYER_INVENTORY_CLICK = "__player_inventory_click"
    const val PLAYER_INVENTORY_SLOT_PAYLOAD = "slot"
}

fun interface MenuActionHandler {
    fun handle(context: MenuActionContext): MenuActionResult
}

enum class MenuCloseReason {
    USER_DISMISSED,
    ROUTE_REPLACED,
    RUNTIME_CLOSED,
}

data class MenuCloseContext(
    val player: Player,
    val route: MenuRoute,
    val inventory: Inventory,
    val reason: MenuCloseReason,
) {
    constructor(
        player: Player,
        route: MenuRoute,
        inventory: Inventory,
    ) : this(player, route, inventory, MenuCloseReason.USER_DISMISSED)
}

fun interface MenuCloseHandler {
    fun handle(context: MenuCloseContext)
}

sealed interface MenuUpdate {
    data object None : MenuUpdate
    data object Refresh : MenuUpdate
    data object Resume : MenuUpdate
    data object Close : MenuUpdate
    data object Back : MenuUpdate
    data class Replace(val route: MenuRoute) : MenuUpdate
    data class Navigate(val route: MenuRoute) : MenuUpdate
}

enum class ManagedMenuTransition {
    AUTOMATIC,
    ROOT,
    REPLACE,
    NAVIGATE,
    PRESERVE_HISTORY,
}

data class ManagedInventoryMenuRequest(
    val route: MenuRoute,
    val inventory: Inventory,
    val transition: ManagedMenuTransition = ManagedMenuTransition.AUTOMATIC,
    val policy: GuiInventoryPolicy = GuiInventoryPolicy(),
    val openSound: MenuSoundPolicy = MenuSoundPolicy.Default,
)

object MenuRouteIds {
    fun fromInventory(inventory: Inventory): String {
        val holderName = inventory.holder?.javaClass?.simpleName
            ?.takeIf(String::isNotBlank)
            ?: return "inventory"
        return fromHolderName(holderName)
    }

    fun fromHolderName(holderName: String): String {
        require(holderName.isNotBlank()) { "holderName must not be blank" }
        val withoutSuffix = holderName
            .removeSuffix("InventoryHolder")
            .removeSuffix("Holder")
            .ifBlank { holderName }
        return withoutSuffix
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2")
            .lowercase()
    }
}

enum class ManagedMenuInteractionOutcome {
    SUCCESS,
    REJECTED,
}

data class ManagedMenuInteraction(
    val outcome: ManagedMenuInteractionOutcome,
    val clickType: MenuClickType = MenuClickType.DEFAULT,
    val sound: MenuSoundPolicy = MenuSoundPolicy.Default,
)

sealed interface MenuActionResult {
    data class Success(
        val update: MenuUpdate = MenuUpdate.Refresh,
        val sound: MenuSoundPolicy = MenuSoundPolicy.Default,
    ) : MenuActionResult

    data class Rejected(
        val message: Component? = null,
        val sound: MenuSoundPolicy = MenuSoundPolicy.Default,
    ) : MenuActionResult

    data object Ignored : MenuActionResult
}

data class InventoryMenuDefinition(
    val owner: String,
    val id: String,
    val renderer: InventoryMenuRenderer,
    val actions: Map<String, MenuActionHandler>,
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy(),
    val onClose: MenuCloseHandler? = null,
    val actionContracts: Map<String, MenuActionContract> = emptyMap(),
) {
    @Suppress("UNUSED_PARAMETER")
    constructor(
        owner: String,
        id: String,
        renderer: InventoryMenuRenderer,
        actions: Map<String, MenuActionHandler>,
        sounds: MenuActionSoundPolicy?,
        mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?,
    ) : this(
        owner = owner,
        id = id,
        renderer = renderer,
        actions = actions,
        sounds = if (mask and 0x10 != 0) MenuActionSoundPolicy() else requireNotNull(sounds),
        onClose = null,
        actionContracts = emptyMap(),
    )

    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(actions.keys.all { it.isNotBlank() }) { "action ids must not be blank" }
        require(actionContracts.keys.all { it in actions }) {
            "action contracts must reference registered handlers"
        }
    }

    val routeId: String
        get() = "$owner:$id"
}
