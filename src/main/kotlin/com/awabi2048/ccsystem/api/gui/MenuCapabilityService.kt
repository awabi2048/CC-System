package com.awabi2048.ccsystem.api.gui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.UUID
import java.security.MessageDigest
import org.bukkit.Material

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

enum class MenuCapabilityCompositionMode {
    FULL_ITEM,
    HOST_AUGMENTATION,
}

class MenuCapabilityAugmentationSource(
    val capabilityId: String,
    sourceBlocks: List<GuiLoreBlock>,
) {
    val blocks: List<GuiLoreBlock> = sourceBlocks.map { GuiLoreBlock(it.lines.toList()) }
    val fingerprint: String = MenuCapabilityCanonicalFingerprint.of(blocks)

    init {
        require(capabilityId.isNotBlank()) { "capabilityId must not be blank" }
        require(sourceBlocks.isNotEmpty()) { "augmentation blocks must not be empty" }
    }
}

enum class MenuCapabilityInsertionBoundary {
    AFTER_HOST_BLOCKS_BEFORE_ACTIONS,
}

class MenuCapabilityCompositionSnapshot(
    val contributorCapabilityId: String,
    val mode: MenuCapabilityCompositionMode,
    val insertionBoundary: MenuCapabilityInsertionBoundary,
    augmentationBlocks: List<GuiLoreBlock>,
    val hostBlocksFingerprint: String,
    val actionsFingerprint: String,
    val hostItemFingerprint: String,
    val completedCompositionFingerprint: String,
) {
    val augmentationBlocks: List<GuiLoreBlock> = augmentationBlocks.map { GuiLoreBlock(it.lines.toList()) }
    val augmentationBlockFingerprints: List<String> = this.augmentationBlocks.map(MenuCapabilityCanonicalFingerprint::of)
    val augmentationFingerprint: String = MenuCapabilityCanonicalFingerprint.of(this.augmentationBlocks)
}

data class MenuCapabilityCompositionResult(
    val lore: GuiLoreSpec,
    val snapshot: MenuCapabilityCompositionSnapshot,
)

object MenuCapabilityComposer {
    @JvmStatic
    fun composeHostAugmentation(
        capability: ResolvedMenuCapability,
        hostItem: GuiItemSpec,
        hostBlocks: List<GuiLoreBlock>,
        actions: List<GuiLoreLine.Interaction> = emptyList(),
    ): MenuCapabilityCompositionResult {
        require(capability.compositionMode == MenuCapabilityCompositionMode.HOST_AUGMENTATION) {
            "composeHostAugmentation requires HOST_AUGMENTATION"
        }
        val augmentation = requireNotNull(capability.augmentationSource) {
            "resolved host augmentation requires augmentation source"
        }.blocks.map { GuiLoreBlock(it.lines.toList()) }
        val copiedHost = hostBlocks.map { GuiLoreBlock(it.lines.toList()) }
        val completed = buildList {
            addAll(copiedHost)
            addAll(augmentation)
            if (actions.isNotEmpty()) add(GuiLoreBlock(actions.toList()))
        }
        return MenuCapabilityCompositionResult(
            lore = GuiLoreSpec.Blocks(completed),
            snapshot = MenuCapabilityCompositionSnapshot(
                contributorCapabilityId = capability.capabilityId,
                mode = capability.compositionMode,
                insertionBoundary = MenuCapabilityInsertionBoundary.AFTER_HOST_BLOCKS_BEFORE_ACTIONS,
                augmentationBlocks = augmentation,
                hostBlocksFingerprint = MenuCapabilityCanonicalFingerprint.of(copiedHost),
                actionsFingerprint = MenuCapabilityCanonicalFingerprint.of(actions),
                hostItemFingerprint = MenuCapabilityCanonicalFingerprint.of(hostItem),
                completedCompositionFingerprint = MenuCapabilityCanonicalFingerprint.of(completed),
            ),
        )
    }
}

internal object MenuCapabilityCanonicalFingerprint {
    fun of(value: Any?): String = MessageDigest.getInstance("SHA-256")
        .digest(canonical(value).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun canonical(value: Any?): String = when (value) {
        null -> "null"
        is List<*> -> value.joinToString(prefix = "[", postfix = "]", separator = ",") { canonical(it) }
        is GuiLoreBlock -> "block:${canonical(value.lines)}"
        is GuiLoreLine.Interaction -> "interaction:${canonical(value.gesture)}:${value.label}"
        is GuiItemSpec -> "item:${value.material.name}:${canonical(value.name)}:${canonical(value.lore)}:${value.role}:${value.amount}"
        is GuiLoreSpec.Blocks -> "blocks:${canonical(value.blocks)}"
        else -> "${value::class.qualifiedName}:${value}"
    }
}

data class MenuCapabilityPresentation(
    val item: GuiItemSpec,
    val glint: Boolean? = null,
    val playerHeadOwner: UUID? = null,
    val embeddedLoreBlocks: List<GuiLoreBlock> = emptyList(),
) {
    /** FULL_ITEMではitemが完成表示を所有し、embeddedLoreBlocksはitem loreを置換してactionより前へ配置します。 */
    var compositionMode: MenuCapabilityCompositionMode = MenuCapabilityCompositionMode.FULL_ITEM
        internal set

    var actionGuidance: MenuCapabilityActionGuidance = MenuCapabilityActionGuidance.STANDARD

    companion object {
        @JvmStatic
        fun hostAugmentation(embeddedLoreBlocks: List<GuiLoreBlock>): MenuCapabilityPresentation =
            MenuCapabilityPresentation(
                item = GuiItemSpec(
                    material = Material.AIR,
                    name = GuiNameSpec.Empty,
                    lore = GuiLoreSpec.None,
                    role = GuiElementRole.CONTENT,
                    amount = 1,
                ),
                embeddedLoreBlocks = embeddedLoreBlocks.map { GuiLoreBlock(it.lines.toList()) },
            ).also {
                it.compositionMode = MenuCapabilityCompositionMode.HOST_AUGMENTATION
                MenuCapabilityPresentationValidator.requireValid(it)
            }
    }
}

fun MenuCapabilityPresentation.copyPreservingCompositionMetadata(
    item: GuiItemSpec = this.item,
    glint: Boolean? = this.glint,
    playerHeadOwner: UUID? = this.playerHeadOwner,
    embeddedLoreBlocks: List<GuiLoreBlock> = this.embeddedLoreBlocks,
): MenuCapabilityPresentation = copy(item, glint, playerHeadOwner, embeddedLoreBlocks).also {
    it.compositionMode = compositionMode
    it.actionGuidance = actionGuidance
}

enum class MenuCapabilityActionGuidance {
    STANDARD,
    HIDDEN,
}

object MenuCapabilityPresentationValidator {
    @JvmStatic
    fun violations(presentation: MenuCapabilityPresentation): List<String> = buildList {
        if (presentation.compositionMode == MenuCapabilityCompositionMode.HOST_AUGMENTATION) {
            if (presentation.embeddedLoreBlocks.isEmpty()) add("HOST_AUGMENTATION_BLOCKS_EMPTY")
            if (presentation.item.material != Material.AIR) add("HOST_AUGMENTATION_OWNS_MATERIAL")
            if (presentation.item.name != GuiNameSpec.Empty) add("HOST_AUGMENTATION_OWNS_NAME")
            if (presentation.item.lore != GuiLoreSpec.None) add("HOST_AUGMENTATION_OWNS_LORE")
            if (presentation.item.role != GuiElementRole.CONTENT || presentation.item.amount != 1) {
                add("HOST_AUGMENTATION_OWNS_ITEM_PROPERTIES")
            }
            if (presentation.glint != null || presentation.playerHeadOwner != null) {
                add("HOST_AUGMENTATION_OWNS_ITEM_METADATA")
            }
        }
    }

    @JvmStatic
    fun requireValid(presentation: MenuCapabilityPresentation) {
        val violations = violations(presentation)
        require(violations.isEmpty()) { "Invalid capability presentation: ${violations.joinToString()}" }
    }
}

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
    /** 定義で宣言された配置先です。既存の主コンストラクタABIを維持するため本文で保持します。 */
    var placement: String = ""
        internal set

    val actionable: Boolean
        get() = actions.isNotEmpty()

    var availabilityResult: MenuAvailabilityResult = MenuAvailabilityResult.Available
        internal set

    var compositionMode: MenuCapabilityCompositionMode = presentation.compositionMode
        internal set

    var augmentationSource: MenuCapabilityAugmentationSource? = null
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

/**
 * 解決後に得た配置・availabilityを保持したまま、表示などの主コンストラクタ値を変更します。
 * data classのgenerated copyは主コンストラクタ外の解決メタデータを複製しないため、
 * 解決済みインスタンスを加工するconsumerはこちらを使用します。
 */
fun ResolvedMenuCapability.copyPreservingResolutionMetadata(
    capabilityId: String = this.capabilityId,
    presentation: MenuCapabilityPresentation = this.presentation,
    actions: List<ResolvedMenuCapabilityAction> = this.actions,
): ResolvedMenuCapability = copy(capabilityId, presentation, actions).also {
    it.placement = placement
    it.availabilityResult = availabilityResult
    it.compositionMode = compositionMode
    it.augmentationSource = augmentationSource
}

/**
 * 利用不能表示へ変換された機能の出所です。
 * 呼び出し元の可変Mapから切り離したスナップショットとして保持します。
 */
class MenuCapabilitySource(
    val capabilityId: String,
    val placement: String,
    arguments: Map<String, String>,
    attributes: Map<String, Any>,
) {
    val arguments: Map<String, String> = MenuImmutableCollections.strings(arguments)
    val attributes: Map<String, Any> = MenuImmutableCollections.orderedMap(attributes)

    init {
        require(capabilityId.isNotBlank()) { "capabilityId must not be blank" }
        require(placement.isNotBlank()) { "placement must not be blank" }
    }

    override fun equals(other: Any?): Boolean = other is MenuCapabilitySource &&
        capabilityId == other.capabilityId && placement == other.placement &&
        arguments == other.arguments && attributes == other.attributes

    override fun hashCode(): Int = arrayOf(capabilityId, placement, arguments, attributes).contentHashCode()

    override fun toString(): String =
        "MenuCapabilitySource(capabilityId=$capabilityId, placement=$placement, arguments=$arguments, attributes=$attributes)"
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
