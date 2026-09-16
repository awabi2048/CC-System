package com.awabi2048.ccsystem.core.gesturegui

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.wrappers.BukkitConverters
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import com.comphenix.protocol.wrappers.WrappedBlockData
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import kotlin.math.floor

/**
 * ProtocolLib による client-only virtual entity 描画 backend です。
 *
 * Bukkit Entity を spawn せず、viewer ごとに Spawn / Metadata / Destroy packet だけを送ります。
 * server world 上の GUI 描画 Entity 数は 0 になり、datapack の `@e` 走査・Paper tracking・
 * chunk lifecycle・他 plugin の Entity scan から切り離されます。
 * （入力吸収用の catcher Interaction だけは Bukkit Entity のまま残します。）
 *
 * metadata index・packing は実サーバー（Chiyogami 26.1.2）の NMS 実体で確定しています。
 * 取得元：Display 8-22・BlockDisplay 23・ItemDisplay 23/24・TextDisplay 23-27、
 * billboard FIXED=0、text flags shadow=1/seeThrough=2/defaultBg=4/left=8/right=16、
 * brightness pack(block, sky)=(block<<4)|(sky<<20)、ItemDisplayContext GUI=6。
 *
 * pose 移動は destroy+spawn を基本としますが、spawn 直後に絶対 teleport を送り、
 * float 精度の yaw/pitch へ収束させます（旧 Bukkit 経路の teleport 補正と同等）。
 * teleport 構造は実行時に適応解決し、版差では縮退します。
 */
internal class GestureGuiProtocolLibBackend(private val plugin: Plugin) {
    private val manager by lazy {
        runCatching { ProtocolLibrary.getProtocolManager() }.getOrElse { failure ->
            plugin.logger.log(Level.SEVERE, "ProtocolLib が見つかりません。Gesture GUI の仮想描画に必須です。", failure)
            throw IllegalStateException("ProtocolLib is required for virtual gesture rendering", failure)
        }
    }

    /** 実 Entity ID と衝突しない高位域から採番します。viewer 横断で一意です。 */
    private val idAllocator = AtomicInteger(VIRTUAL_ID_BASE)

    /**
     * BlockData 文字列表現から NMS BlockState ハンドルへの対応表です。
     * NMS BlockState は不変 singleton のため、使い回して安全です。
     * 取得時のみ不可視 template を同一 tick で生成・破棄し、定常 Entity 数は 0 を保ちます。
     */
    private val blockStateHandles = HashMap<String, Any>()

    private val serializers by lazy {
        VirtualSerializers(
            byteValue = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType as java.lang.reflect.Type, false),
            intValue = WrappedDataWatcher.Registry.get(Int::class.javaObjectType as java.lang.reflect.Type, false),
            floatValue = WrappedDataWatcher.Registry.get(Float::class.javaObjectType as java.lang.reflect.Type, false),
            chat = WrappedDataWatcher.Registry.getChatComponentSerializer(),
            blockState = WrappedDataWatcher.Registry.getBlockDataSerializer(false),
            itemStack = WrappedDataWatcher.Registry.getItemStackSerializer(false),
        )
    }

    private data class VirtualSerializers(
        val byteValue: WrappedDataWatcher.Serializer,
        val intValue: WrappedDataWatcher.Serializer,
        val floatValue: WrappedDataWatcher.Serializer,
        val chat: WrappedDataWatcher.Serializer,
        val blockState: WrappedDataWatcher.Serializer,
        val itemStack: WrappedDataWatcher.Serializer,
    )

    private fun vector(): WrappedDataWatcher.Serializer =
        vectorSerializer ?: error("vector serializer が未解決です。checkAvailable を先に呼んでください。")

    private fun quaternion(): WrappedDataWatcher.Serializer =
        quaternionSerializer ?: error("quaternion serializer が未解決です。checkAvailable を先に呼んでください。")

    /**
     * translation/scale・回転用の serializer 群です。
     *
     * NMS の型名は版で揺れる（org.joml.Vector3f / Vector3fc 等）ため、
     * 候補クラス順に解決し、駄目なら不可視 template の watcher から実物を抜きます。
     * 解決不能時は backend 全体を停止し、SEVERE を一度だけ出します。
     */
    private var vectorSerializer: WrappedDataWatcher.Serializer? = null
    private var quaternionSerializer: WrappedDataWatcher.Serializer? = null
    private var vectorResolutionDone = false
    private var backendDisabled = false

    /**
     * 仮想描画が利用可能かを返します。初回に serializer 群を確定します。
     * 利用不可の場合は以降の送信を止め、画面は表示されません（縮退停止）。
     */
    fun checkAvailable(sampleViewer: Player): Boolean {
        if (backendDisabled) return false
        if (vectorResolutionDone) return true
        vectorResolutionDone = true
        vectorSerializer = resolveSerializer(
            listOf(Vector3f::class.java, org.joml.Vector3fc::class.java),
        ) ?: serializerFromTemplate(sampleViewer, ID_TRANSLATION)
        quaternionSerializer = resolveSerializer(
            listOf(org.joml.Quaternionf::class.java, org.joml.Quaternionfc::class.java),
        ) ?: serializerFromTemplate(sampleViewer, ID_LEFT_ROTATION)
        if (vectorSerializer == null || quaternionSerializer == null) {
            backendDisabled = true
            plugin.logger.log(
                Level.SEVERE,
                "仮想 GUI の serializer を解決できず、Gesture GUI の仮想描画を停止します。",
            )
            return false
        }
        if (!verifyItemConverter()) {
            backendDisabled = true
            plugin.logger.log(
                Level.SEVERE,
                "仮想 GUI のアイテム変換が不正であり、Gesture GUI の仮想描画を停止します。",
            )
            return false
        }
        return true
    }

    /**
     * アイテム converter の出力形式を1回だけ検証します。
     *
     * Bukkit ラッパーが返るとエンコード例外で接続断に至るため、
     * NMS クラスであることを起動時に確定させます。
     */
    private fun verifyItemConverter(): Boolean {
        val handle = runCatching {
            BukkitConverters.getItemStackConverter()
                .getGeneric(org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD))
        }.getOrElse { failure ->
            plugin.logger.log(Level.WARNING, "アイテム converter の解決に失敗しました", failure)
            return false
        }
        val valid = handle.javaClass.name == NMS_ITEM_STACK_CLASS
        return valid
    }

    private fun resolveSerializer(candidates: List<Class<*>>): WrappedDataWatcher.Serializer? {
        // 登録名の版差を吸収するため、実装・界面の両方を試します。
        candidates.forEach { candidate ->
            runCatching {
                WrappedDataWatcher.Registry.get(candidate as java.lang.reflect.Type, false)
            }.onSuccess { return it }
        }
        return null
    }

    private fun serializerFromTemplate(sampleViewer: Player, index: Int): WrappedDataWatcher.Serializer? =
        runCatching {
            val template = sampleViewer.world.spawn(sampleViewer.location, BlockDisplay::class.java) {
                it.isVisibleByDefault = false
                it.isPersistent = false
            }
            try {
                val watcher = WrappedDataWatcher.getEntityWatcher(template)
                watcher.getWatchableObject(index)?.watcherObject?.serializer
                    ?: error("index $index の serializer が watcher にありません")
            } finally {
                template.remove()
            }
        }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "serializer の template 抽出に失敗しました: index=$index", failure)
        }.getOrNull()

    fun nextVirtualId(): Int = idAllocator.getAndIncrement()

    /**
     * viewer へ virtual Display を生成し、初期 metadata を一括送信します。
     *
     * 向きは entity 回転ではなく変形の leftRotation quaternion で与えます。
     * entity 回転 byte は常に 0 のため、角度の byte 量子化・欄割付の影響を受けません。
     * translation は quaternion と同一回転へ再構成済みであることが前提です。
     */
    fun spawnDisplay(
        viewer: Player,
        virtualId: Int,
        type: EntityType,
        x: Double,
        y: Double,
        z: Double,
        initialMetadata: List<WrappedDataValue>,
    ) {
        val packet = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY)
        packet.integers.write(0, virtualId)
        packet.integers.write(1, 0)
        packet.getUUIDs().write(0, UUID.randomUUID())
        packet.entityTypeModifier.write(0, type)
        packet.doubles.write(0, x)
        packet.doubles.write(1, y)
        packet.doubles.write(2, z)
        // 向きは変形 quaternion が担うため、entity 回転は 0 固定です。
        packet.bytes.write(0, 0.toByte())
        packet.bytes.write(1, 0.toByte())
        packet.bytes.write(2, 0.toByte())
        packet.vectors.write(0, Vector(0, 0, 0))
        send(viewer, packet)
        if (initialMetadata.isNotEmpty()) sendMetadata(viewer, virtualId, initialMetadata)
        GestureGuiRenderMetrics.virtualSpawns.incrementAndGet()
    }

    fun sendMetadata(viewer: Player, virtualId: Int, values: List<WrappedDataValue>) {
        if (values.isEmpty()) return
        val packet = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA)
        packet.integers.write(0, virtualId)
        packet.dataValueCollectionModifier.write(0, values)
        send(viewer, packet)
        GestureGuiRenderMetrics.virtualUpdates.incrementAndGet()
    }

    fun sendDestroy(viewer: Player, virtualIds: Collection<Int>) {
        if (virtualIds.isEmpty()) return
        val packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY)
        packet.intLists.write(0, virtualIds.toList())
        send(viewer, packet)
        GestureGuiRenderMetrics.virtualDestroys.addAndGet(virtualIds.size.toLong())
    }

    private fun send(viewer: Player, packet: com.comphenix.protocol.events.PacketContainer) {
        runCatching { manager.sendServerPacket(viewer, packet) }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "仮想 GUI packet の送信に失敗しました: viewer=${viewer.name}", failure)
        }
    }

    // -- metadata 構築 -------------------------------------------------------

    /**
     * Display 共通の初期 metadata です。translation/scale/回転は呼び出し側が指定します。
     *
     * 発光の有無にかかわらず flags・glow 色を常時送ります。null（解除）時に
     * 送らないと、以前の発光が client 側に残り続けるためです。
     * 補間（開始差分 0・変形期間 3・位置回転期間 1）は旧 Bukkit 経路の
     * prepareDisplay（teleportDuration=1・delay=0・duration=3）と同値であり、
     * 開閉波・変形を client 側で tween させます。
     * 向きは entity 回転（常に 0）ではなく leftRotation quaternion で与えるため、
     * 角度の byte 量子化・欄割付の影響を受けません。
     *
     * @param billboard 向き追従指定。既定は FIXED です。
     * @param interpTicks 変形補間の期間。hover 等の即時追従は 0 を指定します。
     */
    fun displayBaseValues(
        translation: Vector3f,
        scale: Vector3f,
        rotation: org.joml.Quaternionf,
        glowColorRgb: Int?,
        billboard: Byte = BILLBOARD_FIXED,
        interpTicks: Int = TRANSFORM_INTERP_TICKS,
    ): List<WrappedDataValue> = buildList {
        val s = serializers
        val vec = vector()
        add(WrappedDataValue(ID_TRANSLATION, vec, Vector3f(translation)))
        add(WrappedDataValue(ID_SCALE, vec, Vector3f(scale)))
        add(WrappedDataValue(ID_LEFT_ROTATION, quaternion(), org.joml.Quaternionf(rotation)))
        add(WrappedDataValue(ID_BILLBOARD, s.byteValue, billboard))
        add(WrappedDataValue(ID_BRIGHTNESS, s.intValue, packBrightness(15, 15)))
        add(WrappedDataValue(ID_TRANSFORM_START, s.intValue, 0))
        add(WrappedDataValue(ID_TRANSFORM_DURATION, s.intValue, interpTicks))
        add(WrappedDataValue(ID_POSROT_DURATION, s.intValue, POSROT_INTERP_TICKS))
        add(WrappedDataValue(ID_SHARED_FLAGS, s.byteValue, if (glowColorRgb != null) FLAG_GLOWING else 0.toByte()))
        add(WrappedDataValue(ID_GLOW_COLOR, s.intValue, glowColorRgb?.and(0xFFFFFF) ?: NO_GLOW_COLOR))
    }

    fun blockStateValue(blockData: BlockData, world: World, sampleAt: Location): WrappedDataValue =
        WrappedDataValue(ID_BLOCK_STATE, serializers.blockState, nmsBlockState(blockData, world, sampleAt))

    fun itemStackValue(item: ItemStack): WrappedDataValue =
        WrappedDataValue(
            ID_ITEM_STACK,
            serializers.itemStack,
            nmsItemStack(item),
        )

    /**
     * Bukkit ItemStack に対応する NMS ハンドルを返します。
     *
     * 形式検証は起動時に1回だけ行い、ここでは変換のみ行います。
     * template watcher の値は Bukkit ラッパーの場合があり、そのまま送ると
     * エンコード例外で接続断に至るため使用しません。
     */
    fun nmsItemStack(item: ItemStack): Any =
        BukkitConverters.getItemStackConverter().getGeneric(item.clone())

    fun itemDisplayTypeValue(): WrappedDataValue =
        WrappedDataValue(ID_ITEM_DISPLAY_TYPE, serializers.byteValue, ITEM_DISPLAY_GUI)

    fun textValues(
        textJson: String,
        lineWidth: Int,
        seeThrough: Boolean,
        alignmentFlags: Byte,
    ): List<WrappedDataValue> {
        val s = serializers
        return listOf(
            WrappedDataValue(ID_TEXT, s.chat, WrappedChatComponent.fromJson(textJson).handle),
            WrappedDataValue(ID_LINE_WIDTH, s.intValue, lineWidth),
            WrappedDataValue(ID_TEXT_FLAGS, s.byteValue, textFlags(seeThrough, alignmentFlags)),
        )
    }

    /**
     * Bukkit BlockData に対応する NMS BlockState ハンドルを返します。
     * 未知の内容は不可視 template から読み取って覚え、template は同一 tick で破棄します。
     */
    fun nmsBlockState(blockData: BlockData, world: World, sampleAt: Location): Any =
        blockStateHandles.getOrPut(blockData.asString) { readBlockStateHandle(blockData, world, sampleAt) }

    private fun readBlockStateHandle(blockData: BlockData, world: World, sampleAt: Location): Any {
        val template = world.spawn(sampleAt, BlockDisplay::class.java) {
            it.isVisibleByDefault = false
            it.isPersistent = false
            it.block = blockData
        }
        try {
            val watcher = WrappedDataWatcher.getEntityWatcher(template)
            val stored = watcher.getWatchableObject(ID_BLOCK_STATE)?.value
                ?: error("BlockState の読み取りに失敗しました: ${blockData.asString}")
            // ProtocolLib の格納形に依らず NMS ハンドルへ正規化します。
            return if (stored is WrappedBlockData) stored.handle else stored
        } finally {
            template.remove()
        }
    }

    internal companion object {
        const val VIRTUAL_ID_BASE: Int = 1_600_000_000

        // NMS 実体で確定した metadata index です（Entity 基底 0-7・Display 8-22）。
        const val ID_SHARED_FLAGS: Int = 0
        const val ID_TRANSFORM_START: Int = 8
        const val ID_TRANSFORM_DURATION: Int = 9
        const val ID_POSROT_DURATION: Int = 10
        const val ID_TRANSLATION: Int = 11
        const val ID_SCALE: Int = 12
        const val ID_LEFT_ROTATION: Int = 13
        const val ID_BILLBOARD: Int = 15
        const val ID_BRIGHTNESS: Int = 16
        const val ID_GLOW_COLOR: Int = 22
        const val ID_BLOCK_STATE: Int = 23
        const val ID_ITEM_STACK: Int = 23
        const val ID_ITEM_DISPLAY_TYPE: Int = 24
        const val ID_TEXT: Int = 23
        const val ID_LINE_WIDTH: Int = 24
        const val ID_TEXT_FLAGS: Int = 27
        /**
         * TextDisplay 背景色の index（25）です。
         * 既定値（0x40000000）が Bukkit 既定と一致するため送信しませんが、
         * index 対応のずれ検出用に定義します。
         */
        const val ID_BACKGROUND_ALIAS_CHECK: Int = 25

        const val BILLBOARD_FIXED: Byte = 0
        const val FLAG_GLOWING: Byte = 0x40
        /** 発光なしを示す glow 色です。vanilla の既定値と同一です。 */
        const val NO_GLOW_COLOR: Int = -1
        /** NMS ItemStack のクラス名です。converter 出力の検証に使います。 */
        const val NMS_ITEM_STACK_CLASS: String = "net.minecraft.world.item.ItemStack"
        /** 変形補間の期間（tick）です。追従の滑らかさと応答性の均衡点です。 */
        const val TRANSFORM_INTERP_TICKS: Int = 2
        /** 位置回転補間の期間（tick）です。旧経路の teleportDuration と同値です。 */
        const val POSROT_INTERP_TICKS: Int = 1
        const val ITEM_DISPLAY_GUI: Byte = 6

        /** text flags の alignment 部分です。Bukkit TextAlignment からの変換に使います。 */
        const val TEXT_ALIGN_LEFT: Byte = 8
        const val TEXT_ALIGN_RIGHT: Byte = 16

        /** LightCoords.pack(block, sky) と同一式です。brightness(15,15)=0xF000F0 になります。 */
        fun packBrightness(block: Int, sky: Int): Int = ((block and 15) shl 4) or ((sky and 15) shl 20)

        /** seeThrough と alignment を TextDisplay style flags へ合成します。 */
        fun textFlags(seeThrough: Boolean, alignmentFlags: Byte): Byte {
            var flags: Byte = 0
            if (seeThrough) flags = (flags.toInt() or 2).toByte()
            return (flags.toInt() or alignmentFlags.toInt()).toByte()
        }

        /**
         * Bukkit yaw/pitch（度）を packet 用 byte へ変換します。
         *
         * NMS（Mth.floor）と同一の切り下げを用います。ゼロ方向切捨てでは
         * 負角が 1 段階（1.40625°）ずれるため、NMS に合わせます。
         */
        fun toPackedByte(degrees: Float): Byte = floor(degrees * 256.0f / 360.0f).toInt().toByte()

        /** adventure Component を packet 用 JSON へ変換します。 */
        fun componentJson(text: net.kyori.adventure.text.Component): String =
            GsonComponentSerializer.gson().serialize(text)
    }
}
