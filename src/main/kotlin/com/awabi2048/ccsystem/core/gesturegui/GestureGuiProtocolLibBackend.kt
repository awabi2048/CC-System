package com.awabi2048.ccsystem.core.gesturegui

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import kotlin.math.floor
import kotlin.math.roundToInt

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
        val className = handle.javaClass.name
        val valid = className == NMS_ITEM_STACK_CLASS
        plugin.logger.info("[GestureGuiProbe] item converter=$className valid=$valid")
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
     * 初回 spawn 時に基準 template との突合を行います（診断用・原因特定後に除去）。
     *
     * 不可視 Bukkit Display を同一 tick で生成・破棄し、watcher の実 index・値を
     * 本 backend の想定と突き合わせます。不一致は向き・寸法ずれの直接原因になります。
     */
    private var probeDone = false

    private fun runOneShotProbe(sampleViewer: Player) {
        if (probeDone) return
        probeDone = true
        // block・text は互いに独立させ、片方の失敗で他方を欠落させません。
        runCatching { probeBlock(sampleViewer) }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "[GestureGuiProbe] block 突合に失敗しました", failure)
        }
        runCatching { probeText(sampleViewer) }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "[GestureGuiProbe] text 突合に失敗しました", failure)
        }
        runCatching { probeItem(sampleViewer) }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "[GestureGuiProbe] item 突合に失敗しました", failure)
        }
        runCatching { interceptReference(sampleViewer) }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "[GestureGuiProbe] 参照差分に失敗しました", failure)
        }
    }

    /** 検証結果の記録です。失敗しても後続の検証を継続します。 */
    private fun verifyProbe(label: String, failures: MutableList<String>, condition: Boolean, lazyMessage: () -> String) {
        if (!condition) {
            val message = lazyMessage()
            failures += message
            plugin.logger.warning("[GestureGuiProbe] $label: $message")
        }
    }

    private fun probeBlock(sampleViewer: Player) {
        val template = sampleViewer.world.spawn(sampleViewer.location, BlockDisplay::class.java) {
            it.isVisibleByDefault = false
            it.isPersistent = false
            it.billboard = org.bukkit.entity.Display.Billboard.CENTER
            it.brightness = org.bukkit.entity.Display.Brightness(7, 9)
            it.isGlowing = true
            it.setGlowColorOverride(org.bukkit.Color.fromRGB(1, 2, 3))
            it.block = org.bukkit.Bukkit.createBlockData(org.bukkit.Material.STONE)
        }
        try {
            val watcher = WrappedDataWatcher.getEntityWatcher(template)
            val at = { index: Int -> watcher.getWatchableObject(index)?.value }
            plugin.logger.info(
                "[GestureGuiProbe] block translation[11]=${at(11)} scale[12]=${at(12)} " +
                    "billboard[15]=${at(15)} brightness[16]=${at(16)} glowFlags[0]=${at(0)} " +
                    "glowColor[22]=${at(22)} blockState[23]=${at(23)?.javaClass?.name}",
            )
            val failures = mutableListOf<String>()
            verifyProbe("block", failures, (at(15) as? Byte) == 3.toByte()) { "billboard CENTER が 3 ではありません: ${at(15)}" }
            verifyProbe("block", failures, at(16) == packBrightness(7, 9)) { "brightness pack が不一致です: ${at(16)}" }
            verifyProbe("block", failures, (at(0) as? Byte)?.toInt()?.and(0x40) != 0) { "glow bit が立っていません: ${at(0)}" }
            verifyProbe("block", failures, at(23) != null) { "blockState が空です" }
            if (failures.isEmpty()) plugin.logger.info("[GestureGuiProbe] block OK")
            else plugin.logger.warning("[GestureGuiProbe] block NG(${failures.size}件)")
        } finally {
            template.remove()
        }
    }

    private fun probeText(sampleViewer: Player) {
        val template = sampleViewer.world.spawn(sampleViewer.location, org.bukkit.entity.TextDisplay::class.java) {
            it.isVisibleByDefault = false
            it.isPersistent = false
            it.text(net.kyori.adventure.text.Component.text("PROBE-12345"))
            it.lineWidth = 123
            it.isSeeThrough = true
            it.alignment = org.bukkit.entity.TextDisplay.TextAlignment.RIGHT
        }
        try {
            val watcher = WrappedDataWatcher.getEntityWatcher(template)
            val at = { index: Int -> watcher.getWatchableObject(index)?.value }
            plugin.logger.info(
                "[GestureGuiProbe] text[23]=${at(23)} lineWidth[24]=${at(24)} flags[27]=${at(27)}",
            )
            val failures = mutableListOf<String>()
            verifyProbe("text", failures, at(24) == 123) { "lineWidth が不一致です: ${at(24)}" }
            verifyProbe("text", failures, (at(27) as? Byte) == (2 or 16).toByte()) { "text flags が不一致です: ${at(27)}" }
            // chat handle の往復検証：JSON→NMS→JSON で内容が保たれることを確認します。
            val roundTripped = runCatching {
                val handle = WrappedChatComponent.fromJson("{\"text\":\"PROBE-12345\"}").handle
                WrappedChatComponent.fromHandle(handle).json
            }.getOrNull()
            verifyProbe("text", failures, roundTripped?.contains("PROBE-12345") == true) {
                "chat 往復に失敗しました: $roundTripped"
            }
            if (failures.isEmpty()) plugin.logger.info("[GestureGuiProbe] text OK")
            else plugin.logger.warning("[GestureGuiProbe] text NG(${failures.size}件)")
        } finally {
            template.remove()
        }
    }

    /**
     * アイテム変換の検証です（診断用・原因特定後に除去）。
     *
     * template 読取と converter の双方を試し、NMS 内容を突き合わせます。
     */
    private fun probeItem(sampleViewer: Player) {
        val item = org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD)
        val template = sampleViewer.world.spawn(sampleViewer.location, org.bukkit.entity.ItemDisplay::class.java) {
            it.isVisibleByDefault = false
            it.isPersistent = false
            it.setItemStack(item.clone())
        }
        try {
            val watcher = WrappedDataWatcher.getEntityWatcher(template)
            val stored = watcher.getWatchableObject(ID_ITEM_STACK)?.value
            plugin.logger.info("[GestureGuiProbe] item[23]=$stored")
            val failures = mutableListOf<String>()
            verifyProbe("item", failures, stored != null) { "itemstack が空です" }
            verifyProbe("item", failures, stored.toString().contains("diamond_sword", ignoreCase = true)) {
                "itemstack 内容が不一致です: $stored"
            }
            // 送信経路（converter）の形式も確定させます。Bukkit 形の混入は切断級のため不可です。
            val converted = runCatching { nmsItemStack(item) }.getOrNull()
            verifyProbe("item", failures, converted?.javaClass?.name == NMS_ITEM_STACK_CLASS) {
                "converter 出力が NMS ではありません: ${converted?.javaClass?.name}"
            }
            if (failures.isEmpty()) plugin.logger.info("[GestureGuiProbe] item OK")
            else plugin.logger.warning("[GestureGuiProbe] item NG(${failures.size}件)")
        } finally {
            template.remove()
        }
    }

    private var interceptArmed = false
    private val referenceIds = ConcurrentHashMap.newKeySet<Int>()

    /**
     * Bukkit 参照 Display との packet 差分です（診断用・原因特定後に除去）。
     *
     * 同一内容・同一向きの Bukkit 実体を不可視生成し、実際に送信される
     * spawn・metadata を捕捉して自前の想定と突き合わせます。
     * 参照は viewer 上空へ出し、5tick 後に破棄します。
     */
    private fun interceptReference(sampleViewer: Player) {
        if (interceptArmed) return
        interceptArmed = true
        manager.addPacketListener(
            object : PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.ENTITY_METADATA,
                PacketType.Play.Server.ENTITY_TELEPORT,
            ) {
                override fun onPacketSending(event: PacketEvent) {
                    runCatching { dumpReferencePacket(event) }
                }
            },
        )
        val loc = sampleViewer.location.clone().add(0.0, 25.0, 0.0)
        val block = sampleViewer.world.spawn(loc, BlockDisplay::class.java) {
            it.isVisibleByDefault = false
            it.isPersistent = false
            it.setRotation(45.5f, -12.25f)
            it.setTransformation(
                org.bukkit.util.Transformation(
                    Vector3f(7f, 8f, 9f),
                    org.joml.AxisAngle4f(),
                    Vector3f(2f, 3f, 4f),
                    org.joml.AxisAngle4f(),
                ),
            )
            it.block = org.bukkit.Bukkit.createBlockData(org.bukkit.Material.STONE)
        }
        val text = sampleViewer.world.spawn(loc, org.bukkit.entity.TextDisplay::class.java) {
            it.isVisibleByDefault = false
            it.isPersistent = false
            it.setRotation(45.5f, -12.25f)
            it.text(net.kyori.adventure.text.Component.text("REF-77"))
            it.lineWidth = 77
        }
        referenceIds += block.entityId
        referenceIds += text.entityId
        sampleViewer.showEntity(plugin, block)
        sampleViewer.showEntity(plugin, text)
        plugin.logger.info(
            "[GestureGuiIntercept] 参照実体を生成しました id=${block.entityId},${text.entityId}（teleport 後に破棄）",
        )
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            runCatching {
                // 正規 teleport packet を捕捉するため、Bukkit 経路で再配置します。
                val dest = loc.clone().add(1.5, -0.25, 2.5)
                dest.yaw = 30.5f
                dest.pitch = -7.25f
                block.teleport(dest)
                text.teleport(dest)
            }
        }, 3L)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            runCatching {
                sampleViewer.hideEntity(plugin, block)
                sampleViewer.hideEntity(plugin, text)
            }
            block.remove()
            text.remove()
        }, 8L)
    }

    private fun dumpReferencePacket(event: PacketEvent) {
        val packet = event.packet
        when (event.packetType) {
            PacketType.Play.Server.SPAWN_ENTITY -> {
                val id = packet.integers.read(0)
                if (id !in referenceIds) return
                plugin.logger.info(
                    "[GestureGuiIntercept] spawn id=$id type=${packet.entityTypeModifier.read(0)} " +
                        "xyz=${packet.doubles.read(0)},${packet.doubles.read(1)},${packet.doubles.read(2)} " +
                        "bytes=${packet.bytes.read(0)},${packet.bytes.read(1)},${packet.bytes.read(2)} " +
                        "data=${packet.integers.read(1)}",
                )
            }
            PacketType.Play.Server.ENTITY_METADATA -> {
                val id = packet.integers.read(0)
                if (id !in referenceIds) return
                val values = packet.dataValueCollectionModifier.read(0)
                plugin.logger.info(
                    "[GestureGuiIntercept] meta id=$id " +
                        values.joinToString(";") { "${it.index}=${summarizeValue(it.value)}" },
                )
            }
            PacketType.Play.Server.ENTITY_TELEPORT -> {
                val id = packet.integers.read(0)
                if (id !in referenceIds) return
                val structures = packet.structures
                val inner = if (structures.size() == 1) {
                    val move = structures.read(0)
                    "doubles=${move.doubles.values} floats=${move.float.values}"
                } else {
                    "legacy doubles=${packet.doubles.values} bytes=${packet.bytes.values}"
                }
                plugin.logger.info(
                    "[GestureGuiIntercept] teleport id=$id $inner bools=${packet.booleans.values}",
                )
            }
            else -> {}
        }
    }

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
        runOneShotProbe(viewer)
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
        verifySpawnRoundTrip(packet, virtualId, type, x, y, z)
        send(viewer, packet)
        if (initialMetadata.isNotEmpty()) sendMetadata(viewer, virtualId, initialMetadata)
        GestureGuiRenderMetrics.virtualSpawns.incrementAndGet()
        // 生成明細ログ（診断用・原因特定後に除去）。向き・寸法の突合に使います。
        plugin.logger.info(
            "[GestureGuiSpawnDiag] viewer=${viewer.name} id=$virtualId type=$type " +
                "pos=(${"%.3f".format(x)},${"%.3f".format(y)},${"%.3f".format(z)}) " +
                "meta=${initialMetadata.joinToString(";") { "${it.index}=${summarizeValue(it.value)}" }}",
        )
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

    /**
     * 相対移動（＋向き）を送信します。ダミー追従の連続微動用です。
     *
     * short 差分は 1/4096 ブロック単位・±8 ブロックのため、呼び出し側で範囲を
     * 検証してください。範囲外・drift 超過時は destroy+spawn で再同期します。
     * NMS 宣言順（yRot, xRot）のため bytes 0 が yaw・1 が pitch です。
     */
    fun sendRelativeMove(
        viewer: Player,
        virtualId: Int,
        dxShort: Int,
        dyShort: Int,
        dzShort: Int,
        yawDegrees: Float,
        pitchDegrees: Float,
    ) {
        val packet = manager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK)
        packet.integers.write(0, virtualId)
        packet.shorts.write(0, dxShort.toShort())
        packet.shorts.write(1, dyShort.toShort())
        packet.shorts.write(2, dzShort.toShort())
        packet.bytes.write(0, toPackedByte(yawDegrees))
        packet.bytes.write(1, toPackedByte(pitchDegrees))
        packet.booleans.write(0, false)
        send(viewer, packet)
        GestureGuiRenderMetrics.virtualMoves.incrementAndGet()
    }

    private fun send(viewer: Player, packet: com.comphenix.protocol.events.PacketContainer) {
        runCatching { manager.sendServerPacket(viewer, packet) }.onFailure { failure ->
            plugin.logger.log(Level.WARNING, "仮想 GUI packet の送信に失敗しました: viewer=${viewer.name}", failure)
        }
    }

    private var roundTripWarned = false

    /**
     * 構築直後の読戻し検証です（診断用・原因特定後に除去）。
     *
     * 同一 modifier での往復のため欄順の誤り自体は検出できませんが、
     * 欄数・型の実行時不一致は検出できます。不一致は初回のみ出します。
     */
    private fun verifySpawnRoundTrip(
        packet: com.comphenix.protocol.events.PacketContainer,
        virtualId: Int,
        type: EntityType,
        x: Double,
        y: Double,
        z: Double,
    ) {
        if (roundTripWarned) return
        val failures = runCatching {
            buildList {
                if (packet.integers.read(0) != virtualId) add("id")
                if (packet.integers.read(1) != 0) add("data")
                if (packet.entityTypeModifier.read(0) != type) add("type")
                if (packet.doubles.read(0) != x) add("x")
                if (packet.doubles.read(1) != y) add("y")
                if (packet.doubles.read(2) != z) add("z")
                if (packet.bytes.read(0) != 0.toByte()) add("pitch")
                if (packet.bytes.read(1) != 0.toByte()) add("yaw")
                if (packet.bytes.read(2) != 0.toByte()) add("headYaw")
                if (packet.getUUIDs().read(0) == null) add("uuid")
            }
        }.getOrElse { return }
        if (failures.isNotEmpty()) {
            roundTripWarned = true
            plugin.logger.warning("[GestureGuiSpawnDiag] spawn 欄不一致: ${failures.joinToString(",")}")
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
     */
    fun displayBaseValues(
        translation: Vector3f,
        scale: Vector3f,
        rotation: org.joml.Quaternionf,
        glowColorRgb: Int?,
    ): List<WrappedDataValue> = buildList {
        val s = serializers
        val vec = vector()
        add(WrappedDataValue(ID_TRANSLATION, vec, Vector3f(translation)))
        add(WrappedDataValue(ID_SCALE, vec, Vector3f(scale)))
        add(WrappedDataValue(ID_LEFT_ROTATION, quaternion(), org.joml.Quaternionf(rotation)))
        add(WrappedDataValue(ID_BILLBOARD, s.byteValue, BILLBOARD_FIXED))
        add(WrappedDataValue(ID_BRIGHTNESS, s.intValue, packBrightness(15, 15)))
        add(WrappedDataValue(ID_TRANSFORM_START, s.intValue, 0))
        add(WrappedDataValue(ID_TRANSFORM_DURATION, s.intValue, TRANSFORM_INTERP_TICKS))
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
        /** 相対移動 short の安全域です。NMS 上限（±32767）に余裕を持たせます。 */
        const val REL_SHORT_LIMIT: Int = 30000
        /** 量子化 drift の再同期閾値（ブロック）です。視認限界以下にします。 */
        const val REL_DRIFT_TOLERANCE: Double = 0.004
        /** 変形補間の期間（tick）です。旧経路の interpolationDuration と同値です。 */
        const val TRANSFORM_INTERP_TICKS: Int = 3
        /** 位置回転補間の期間（tick）です。旧経路の teleportDuration と同値です。 */
        const val POSROT_INTERP_TICKS: Int = 1
        const val ITEM_DISPLAY_GUI: Byte = 6

        /**
         * 相対移動の 1/4096 量子化です。範囲外（±8 ブロック超）は null を返し、
         * 呼び出し側で destroy+spawn 再同期します。
         */
        fun relativeShort(deltaBlocks: Double): Int? {
            val quantized = (deltaBlocks * 4096.0).roundToInt()
            return quantized.takeIf { it in -REL_SHORT_LIMIT..REL_SHORT_LIMIT }
        }

        /** 量子化 drift が視認閾値を超えたかを返します。超えたら再同期します。 */
        fun exceedsDrift(trueValue: Double, assumedValue: Double): Boolean =
            kotlin.math.abs(trueValue - assumedValue) > REL_DRIFT_TOLERANCE

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

        /** 診断ログ用の値要約です。長大な NMS 文字列を抑えます。 */
        fun summarizeValue(value: Any?): String {
            val text = value.toString()
            return if (text.length <= 120) text else text.take(120) + "…(${text.length})"
        }

        /** adventure Component を packet 用 JSON へ変換します。 */
        fun componentJson(text: net.kyori.adventure.text.Component): String =
            GsonComponentSerializer.gson().serialize(text)
    }
}
