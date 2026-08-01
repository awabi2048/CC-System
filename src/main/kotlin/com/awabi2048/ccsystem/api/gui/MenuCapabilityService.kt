package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.UUID

data class MenuCapabilityContext(
    val player: Player,
    val arguments: Map<String, String> = emptyMap(),
    val attributes: Map<String, Any> = emptyMap(),
)

/** 表示・inspectから呼ばれる読取専用の可用性判定です。 */
fun interface MenuCapabilityAvailability {
    fun isAvailable(context: MenuCapabilityContext): Boolean

    companion object {
        @JvmStatic
        fun reasoned(provider: MenuCapabilityAvailabilityProvider): MenuCapabilityAvailability = provider
    }
}

/** 可否と利用不能理由を1回の評価で返すavailabilityです。 */
fun interface MenuCapabilityAvailabilityProvider : MenuCapabilityAvailability {
    fun resolve(context: MenuCapabilityContext): MenuAvailabilityResult

    override fun isAvailable(context: MenuCapabilityContext): Boolean =
        resolve(context) is MenuAvailabilityResult.Available
}

fun MenuCapabilityAvailability.resolveAvailability(context: MenuCapabilityContext): MenuAvailabilityResult =
    if (this is MenuCapabilityAvailabilityProvider) resolve(context)
    else if (isAvailable(context)) MenuAvailabilityResult.Available
    else MenuAvailabilityResult.UnavailableUnknown

data class MenuCapabilityPresentation(
    val item: GuiItemSpec,
    val glint: Boolean? = null,
    val playerHeadOwner: UUID? = null,
    val embeddedLoreBlocks: List<GuiLoreBlock> = emptyList(),
)

/** 表示・inspectから呼ばれる読取専用のpresentation生成です。 */
fun interface MenuCapabilityPresentationProvider {
    fun resolve(context: MenuCapabilityContext): MenuCapabilityPresentation
}

data class MenuCapabilityActionContext(
    val player: Player,
    val click: ClickType,
    val arguments: Map<String, String> = emptyMap(),
    val attributes: Map<String, Any> = emptyMap(),
)

fun interface MenuCapabilityActionHandler {
    fun handle(context: MenuCapabilityActionContext): MenuActionResult
}

/** 表示・inspectから呼ばれる読取専用の操作文言生成です。 */
fun interface MenuCapabilityActionTextProvider {
    fun resolve(context: MenuCapabilityContext): String
}

enum class MenuCapabilityTrigger(
    val clicks: Set<ClickType>,
) {
    LEFT(setOf(ClickType.LEFT)),
    RIGHT(setOf(ClickType.RIGHT)),
    SHIFT_LEFT(setOf(ClickType.SHIFT_LEFT)),
    SHIFT_RIGHT(setOf(ClickType.SHIFT_RIGHT)),
    LEFT_RIGHT(setOf(ClickType.LEFT, ClickType.RIGHT)),
    ANY(MenuAcceptedClicks.STANDARD),
}

data class MenuCapabilityAction(
    val id: String,
    val trigger: MenuCapabilityTrigger,
    val textProvider: MenuCapabilityActionTextProvider,
    val availability: MenuCapabilityAvailability =
        MenuCapabilityAvailability { true },
    val sounds: MenuActionSoundPolicy = MenuActionSoundPolicy(),
    val handler: MenuCapabilityActionHandler,
    val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
    val reversibleContract: MenuReversibleContract? = null,
) {
    init {
        require(id.isNotBlank()) { "action id must not be blank" }
        requireReversibleContractSafety(
            trigger.clicks,
            { safety },
            { reversibleContract },
            "capability action",
        )
    }
}

data class MenuCapabilityDefinition(
    val owner: String,
    val id: String,
    val placement: String,
    val availability: MenuCapabilityAvailability,
    val presentationProvider: MenuCapabilityPresentationProvider,
    val actions: List<MenuCapabilityAction>,
) {
    val capabilityId: String
        get() = "$owner:$id"

    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(placement.isNotBlank()) { "placement must not be blank" }
        require(actions.map(MenuCapabilityAction::id).distinct().size == actions.size) {
            "capability action ids must be unique"
        }
        val duplicateClicks = actions
            .flatMap { it.trigger.clicks }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateClicks.isEmpty()) {
            "capability actions must not accept the same click: $duplicateClicks"
        }
        actions.forEach { action ->
            requireReversibleContractSafety(
                action.trigger.clicks,
                { action.safety },
                { action.reversibleContract },
                "capability definition action ${action.id}",
            )
        }
    }
}

data class ResolvedMenuCapabilityAction(
    val id: String,
    val trigger: MenuCapabilityTrigger,
    val text: String,
    val safety: MenuActionSafety = MenuActionSafety.UNSPECIFIED,
    val reversibleContract: MenuReversibleContract? = null,
) {
    init {
        requireReversibleContractSafety(
            trigger.clicks,
            { safety },
            { reversibleContract },
            "resolved capability action",
        )
    }
}

data class ResolvedMenuCapability(
    val capabilityId: String,
    val presentation: MenuCapabilityPresentation,
    val actions: List<ResolvedMenuCapabilityAction>,
) {
    val actionable: Boolean
        get() = actions.isNotEmpty()

    var availabilityResult: MenuAvailabilityResult = MenuAvailabilityResult.Available
        internal set

    val unavailableReason: net.kyori.adventure.text.Component?
        get() = (availabilityResult as? MenuAvailabilityResult.Unavailable)?.reason

    val acceptedClicks: Set<ClickType>
        get() = actions.flatMapTo(linkedSetOf()) { it.trigger.clicks }

    /** 全clickで同一の安全区分だけを単値として返します。混在時は推測しません。 */
    val safety: MenuActionSafety
        get() = actions.map(ResolvedMenuCapabilityAction::safety)
            .distinct()
            .singleOrNull()
            ?: MenuActionSafety.UNSPECIFIED

    /** clickごとの安全区分です。定義時にclickの重複は禁止されています。 */
    val safetyByClick: Map<ClickType, MenuActionSafety>
        get() = buildMap {
            actions.forEach { action ->
                action.trigger.clicks.forEach { click -> put(click, action.safety) }
            }
        }

    val reversibleContractByClick: Map<ClickType, MenuReversibleContract>
        get() = buildMap {
            actions.forEach { action ->
                action.reversibleContract?.let { contract ->
                    action.trigger.clicks.forEach { click -> put(click, contract) }
                }
            }
        }
}

/** registry定義から読取専用で取得できる静的なclick・安全契約です。 */
data class MenuCapabilityStaticContract(
    val acceptedClicks: Set<ClickType>,
    val safetyByClick: Map<ClickType, MenuActionSafety>,
    val reversibleContractByClick: Map<ClickType, MenuReversibleContract> = emptyMap(),
) {
    val safety: MenuActionSafety
        get() = safetyByClick.values.distinct().singleOrNull() ?: MenuActionSafety.UNSPECIFIED
}

fun MenuCapabilityDefinition.staticContract(): MenuCapabilityStaticContract =
    MenuCapabilityStaticContract(
        acceptedClicks = actions.flatMapTo(linkedSetOf()) { it.trigger.clicks },
        safetyByClick = buildMap {
            actions.forEach { action ->
                action.trigger.clicks.forEach { click -> put(click, action.safety) }
            }
        },
        reversibleContractByClick = buildMap {
            actions.forEach { action ->
                action.reversibleContract?.let { contract ->
                    action.trigger.clicks.forEach { click -> put(click, contract) }
                }
            }
        },
    )

interface MenuCapabilityService {
    fun register(definition: MenuCapabilityDefinition)

    fun unregisterOwner(owner: String)

    fun definition(capabilityId: String): MenuCapabilityDefinition?

    fun definitions(): List<MenuCapabilityDefinition>

    fun definitions(placement: String): List<MenuCapabilityDefinition>

    /**
     * 表示生成用の動的解決です。availability、文言、presentation providerを実行します。
     * inspectや契約検証からは呼ばれません。実装側はrendererから呼ばれることを前提に、
     * 読取専用で実装してください。
     */
    fun resolve(
        capabilityId: String,
        player: Player,
        arguments: Map<String, String> = emptyMap(),
        attributes: Map<String, Any> = emptyMap(),
    ): ResolvedMenuCapability?

    fun execute(
        capabilityId: String,
        player: Player,
        click: ClickType,
        arguments: Map<String, String> = emptyMap(),
        attributes: Map<String, Any> = emptyMap(),
    ): MenuActionResult
}
