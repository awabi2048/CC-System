package com.awabi2048.ccsystem.features.misc.displayparticle

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStartResult
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectServiceImpl
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBookJsonParser
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBookStringChoiceException
import com.awabi2048.ccsystem.core.displayeffect.ParsedDisplayParticleBook
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

internal class DisplayParticleBookTestController(
    private val plugin: Plugin,
    private val displayEffectService: DisplayEffectServiceImpl
) : Listener {
    private val enabledPlayers = mutableSetOf<UUID>()
    private val lastHandledTick = mutableMapOf<UUID, Int>()
    private val cachedBooks = mutableMapOf<UUID, CachedBook>()
    private val cacheIdKey = NamespacedKey(plugin, "display-particle-book-cache")
    // Material Registryの準備後に生成し、各編集イベントで全ブロックを列挙し直さないよう保持します。
    private val blockChoices by lazy(DisplayParticleBookJsonParser::availableBlockChoices)

    fun toggle(player: Player): Boolean {
        val enabled = if (enabledPlayers.remove(player.uniqueId)) false else {
            enabledPlayers += player.uniqueId
            true
        }
        if (!enabled) {
            cachedBooks.remove(player.uniqueId)
            lastHandledTick.remove(player.uniqueId)
        }
        player.sendMessage(message(player, if (enabled) "management.debug.particle_test_enabled" else "management.debug.particle_test_disabled"))
        return enabled
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBookClose(event: PlayerEditBookEvent) {
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return

        val editedMeta = event.newBookMeta
        // 署名すると本と羽ペンではなくなるため、テスト用キャッシュとしては扱いません。
        if (event.isSigning) {
            cachedBooks.remove(player.uniqueId)
            editedMeta.persistentDataContainer.remove(cacheIdKey)
            event.newBookMeta = editedMeta
            return
        }

        val serializer = PlainTextComponentSerializer.plainText()
        val pages = editedMeta.pages().map(serializer::serialize)
        runCatching { DisplayParticleBookJsonParser.parsePages(pages, blockChoices) }
            .onSuccess { parsed ->
                // PDCの識別子とプレイヤー単位キャッシュを対応させ、別の本や編集前の本の誤使用を防ぎます。
                val cacheId = UUID.randomUUID().toString()
                editedMeta.persistentDataContainer.set(cacheIdKey, PersistentDataType.STRING, cacheId)
                event.newBookMeta = editedMeta
                cachedBooks[player.uniqueId] = CachedBook(cacheId, parsed)
                player.sendMessage(
                    LanguageManager.deserializeLegacy(message(player, "management.debug.particle_test_cached"))
                        .append(Component.space())
                        .append(
                            LanguageManager.deserializeLegacy("§8[§6ガイドを表示§8]")
                                .clickEvent(ClickEvent.runCommand("/cc debug particle_test_guide"))
                                .hoverEvent(LanguageManager.deserializeLegacy(
                                    message(player, "management.debug.particle_test_guide.open_hover")
                                ))
                        )
                )
            }
            .onFailure { failure ->
                cachedBooks.remove(player.uniqueId)
                editedMeta.persistentDataContainer.remove(cacheIdKey)
                event.newBookMeta = editedMeta
                sendValidationFailure(player, failure)
            }
    }

    fun showGuide(player: Player) {
        player.sendMessage(message(player, "management.debug.particle_test_guide.header"))
        PARTICLE_GUIDE_FIELDS.chunked(GUIDE_FIELDS_PER_LINE).forEach { line ->
            val components = line.map { field ->
                Component.text(field.path, NamedTextColor.GOLD)
                    .hoverEvent(LanguageManager.deserializeLegacy(message(player, field.descriptionKey)))
            }
            player.sendMessage(
                Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.DARK_GRAY)), components)
            )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onLeftClick(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) return
        val player = event.player
        if (player.uniqueId !in enabledPlayers) return
        if (player.inventory.itemInMainHand.type != Material.WRITABLE_BOOK) return
        // テスト用の本を振った操作は、通常の左クリック連携へ重複して送りません。
        event.isCancelled = true
        val tick = Bukkit.getCurrentTick()
        if (lastHandledTick.put(player.uniqueId, tick) == tick) return

        val meta = player.inventory.itemInMainHand.itemMeta as? BookMeta ?: return
        val cacheId = meta.persistentDataContainer.get(cacheIdKey, PersistentDataType.STRING)
        val cached = cachedBooks[player.uniqueId]
        if (cacheId == null || cached == null || cached.cacheId != cacheId) {
            player.sendMessage(message(player, "management.debug.particle_test_not_cached"))
            return
        }

        runCatching {
            val parsed = cached.parsed
            // 視線に追従する確認位置を基準にし、JSONのoffsetはその基準位置への微調整として適用します。
            val eyeLocation = player.eyeLocation
            val direction = eyeLocation.direction.normalize()
            val rayHit = player.world.rayTraceBlocks(
                eyeLocation,
                direction,
                DISPLAY_DISTANCE_BLOCKS,
                FluidCollisionMode.NEVER,
                true
            )
            val location = rayHit?.hitPosition?.toLocation(player.world)
                ?: eyeLocation.clone().add(direction.multiply(DISPLAY_DISTANCE_BLOCKS))
            location
                .add(parsed.offset.x, parsed.offset.y, parsed.offset.z)
            // キャッシュされた設定は再利用しつつ、発生ごとの個体差は固定されないようシードだけを更新します。
            val request = parsed.request.copy(randomSeed = System.nanoTime())
            displayEffectService.emitTransientDisplayParticles(plugin, location, parsed.preset, request)
        }.onSuccess { result ->
            when (result) {
                // 連続表示でチャットを埋めないよう、正常開始時は通知しません。
                is DisplayEffectStartResult.Started -> Unit
                is DisplayEffectStartResult.Rejected -> player.sendMessage(
                    message(player, "management.debug.particle_test_rejected", mapOf("detail" to result.message))
                )
            }
        }.onFailure { failure ->
            sendValidationFailure(player, failure)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        enabledPlayers.remove(event.player.uniqueId)
        lastHandledTick.remove(event.player.uniqueId)
        cachedBooks.remove(event.player.uniqueId)
    }

    private fun message(player: Player, key: String, placeholders: Map<String, Any> = emptyMap()): String =
        LanguageManager.getUnified().getString(player, key, placeholders)

    private fun Throwable.detail(): String = message ?: this::class.simpleName.orEmpty()

    private fun sendValidationFailure(player: Player, failure: Throwable) {
        player.sendMessage(
            message(player, "management.debug.particle_test_invalid_json", mapOf("detail" to failure.detail()))
        )
        if (failure !is DisplayParticleBookStringChoiceException || failure.choices.isEmpty()) return

        player.sendMessage(message(player, "management.debug.particle_test_choice.header", mapOf("field" to failure.field)))
        // 候補ごとに説明を持たせつつ、ブロック候補が多い場合も単一メッセージへまとめます。
        val choices = failure.choices.map { choice ->
            Component.text(choice.value, NamedTextColor.WHITE)
                .hoverEvent(LanguageManager.deserializeLegacy(message(player, choice.descriptionKey)))
        }
        choices.chunked(CHOICES_PER_LINE).forEach { line ->
            player.sendMessage(
                Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), line)
            )
        }
    }

    private data class CachedBook(
        val cacheId: String,
        val parsed: ParsedDisplayParticleBook
    )

    private companion object {
        const val DISPLAY_DISTANCE_BLOCKS = 4.0
        const val CHOICES_PER_LINE = 24
        const val GUIDE_FIELDS_PER_LINE = 5
        val PARTICLE_GUIDE_FIELDS = listOf(
            "textures[].block", "textures[].weight",
            "scale.initial", "scale.peak", "scale.peak_progress", "scale.scale_in_ticks", "scale.variation",
            "rotation.random_initial", "rotation.angular_velocity", "rotation.variation",
            "lifetime.ticks", "lifetime.variation", "lifetime.fade_out_ticks", "lifetime.fade_variation", "lifetime.spawn_delay",
            "motion.preset", "motion.initial_velocity", "motion.acceleration", "motion.retention", "motion.turbulence",
            "motion.frequency", "motion.radial_speed", "motion.spawn_radius", "motion.orbit_speed", "motion.radial_pull",
            "motion.attraction", "motion.max_speed",
            "collision.mode", "collision.restitution",
            "emission.offset", "emission.delta", "emission.speed", "emission.count", "emission.visibility"
        ).map { path ->
            GuideField(path, "management.debug.particle_test_guide.field.${path.replace("[]", "_entry").replace('.', '_')}")
        }
    }

    private data class GuideField(val path: String, val descriptionKey: String)
}
