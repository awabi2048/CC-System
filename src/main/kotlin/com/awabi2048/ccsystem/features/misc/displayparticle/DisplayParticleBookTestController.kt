package com.awabi2048.ccsystem.features.misc.displayparticle

import com.awabi2048.ccsystem.api.displayeffect.DisplayEffectStartResult
import com.awabi2048.ccsystem.core.config.LanguageManager
import com.awabi2048.ccsystem.core.displayeffect.DisplayEffectServiceImpl
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBookJsonParser
import com.awabi2048.ccsystem.core.displayeffect.DisplayParticleBookStringChoiceException
import com.awabi2048.ccsystem.core.displayeffect.PreparedDisplayParticleBook
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
    private val pendingFixes = mutableMapOf<UUID, PendingFix>()
    private val cacheIdKey = NamespacedKey(plugin, "display-particle-book-cache")
    private val fixTokenKey = NamespacedKey(plugin, "display-particle-book-fix")
    // Material Registryの準備後に生成し、各編集イベントで全ブロックを列挙し直さないよう保持します。
    private val blockChoices by lazy(DisplayParticleBookJsonParser::availableBlockChoices)

    fun toggle(player: Player): Boolean {
        val enabled = if (enabledPlayers.remove(player.uniqueId)) false else {
            enabledPlayers += player.uniqueId
            true
        }
        if (!enabled) {
            cachedBooks.remove(player.uniqueId)
            pendingFixes.remove(player.uniqueId)
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
        // 新しい編集結果を受け取った時点で、過去の修正案は再利用できないよう失効させます。
        pendingFixes.remove(player.uniqueId)
        editedMeta.persistentDataContainer.remove(fixTokenKey)
        // 署名すると本と羽ペンではなくなるため、テスト用キャッシュとしては扱いません。
        if (event.isSigning) {
            cachedBooks.remove(player.uniqueId)
            pendingFixes.remove(player.uniqueId)
            editedMeta.persistentDataContainer.remove(cacheIdKey)
            editedMeta.persistentDataContainer.remove(fixTokenKey)
            event.newBookMeta = editedMeta
            return
        }

        val serializer = PlainTextComponentSerializer.plainText()
        val pages = editedMeta.pages().map(serializer::serialize)
        runCatching { DisplayParticleBookJsonParser.preparePages(pages, blockChoices) }
            .onSuccess { prepared ->
                if (prepared.removedPaths.isNotEmpty()) {
                    val token = UUID.randomUUID().toString()
                    editedMeta.persistentDataContainer.remove(cacheIdKey)
                    editedMeta.persistentDataContainer.set(fixTokenKey, PersistentDataType.STRING, token)
                    event.newBookMeta = editedMeta
                    cachedBooks.remove(player.uniqueId)
                    pendingFixes[player.uniqueId] = PendingFix(token, prepared)
                    sendDestructiveFixOffer(player, prepared, token)
                    return@onSuccess
                }

                // 不足項目の追加だけで済む場合は、その場で本を正規化してからキャッシュします。
                editedMeta.pages(prepared.pages.map(Component::text))
                editedMeta.persistentDataContainer.remove(fixTokenKey)
                cachePreparedBook(player, editedMeta, prepared) { event.newBookMeta = editedMeta }
                if (prepared.addedPaths.isNotEmpty()) {
                    player.sendMessage(message(player, "management.debug.particle_test_fix.added", mapOf(
                        "fields" to prepared.addedPaths.joinToString()
                    )))
                }
            }
            .onFailure { failure ->
                cachedBooks.remove(player.uniqueId)
                pendingFixes.remove(player.uniqueId)
                editedMeta.persistentDataContainer.remove(cacheIdKey)
                editedMeta.persistentDataContainer.remove(fixTokenKey)
                event.newBookMeta = editedMeta
                sendValidationFailure(player, failure)
            }
    }

    fun showGuide(player: Player, genreName: String? = null) {
        val index = GUIDE_GENRES.indexOfFirst { it.id.equals(genreName, true) }.takeIf { it >= 0 } ?: 0
        val genre = GUIDE_GENRES[index]
        player.sendMessage(message(player, "management.debug.particle_test_guide.header", mapOf(
            "genre" to genre.id,
            "page" to index + 1,
            "total" to GUIDE_GENRES.size
        )))
        genre.fields.forEach { field ->
            player.sendMessage(
                Component.text("- ${field.path}", NamedTextColor.GOLD)
                    .hoverEvent(LanguageManager.deserializeLegacy(message(player, field.descriptionKey)))
                    .clickEvent(ClickEvent.copyToClipboard(field.path))
            )
        }
        val navigation = Component.text()
        if (index > 0) navigation.append(guideNavigation(player, GUIDE_GENRES[index - 1], "previous"))
        if (index > 0 && index < GUIDE_GENRES.lastIndex) navigation.append(Component.space())
        if (index < GUIDE_GENRES.lastIndex) navigation.append(guideNavigation(player, GUIDE_GENRES[index + 1], "next"))
        player.sendMessage(navigation.build())
    }

    fun applyPendingFix(player: Player, token: String) {
        val pending = pendingFixes[player.uniqueId]
        val item = player.inventory.itemInMainHand
        if (item.type != Material.WRITABLE_BOOK) {
            player.sendMessage(message(player, "management.debug.particle_test_fix.expired"))
            return
        }
        val meta = item.itemMeta as? BookMeta
        val itemToken = meta?.persistentDataContainer?.get(fixTokenKey, PersistentDataType.STRING)
        if (pending == null || meta == null || pending.token != token || itemToken != token) {
            player.sendMessage(message(player, "management.debug.particle_test_fix.expired"))
            return
        }
        meta.pages(pending.prepared.pages.map(Component::text))
        meta.persistentDataContainer.remove(fixTokenKey)
        cachePreparedBook(player, meta, pending.prepared) { item.itemMeta = meta }
        pendingFixes.remove(player.uniqueId)
        player.sendMessage(message(player, "management.debug.particle_test_fix.applied"))
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
        pendingFixes.remove(event.player.uniqueId)
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
                .clickEvent(ClickEvent.copyToClipboard(choice.value))
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

    private data class PendingFix(val token: String, val prepared: PreparedDisplayParticleBook)

    private fun cachePreparedBook(
        player: Player,
        meta: BookMeta,
        prepared: PreparedDisplayParticleBook,
        persistMeta: () -> Unit
    ) {
        val cacheId = UUID.randomUUID().toString()
        meta.persistentDataContainer.set(cacheIdKey, PersistentDataType.STRING, cacheId)
        // 本へのページ・PDC反映が完了してから、メモリ上のキャッシュを有効化します。
        persistMeta()
        cachedBooks[player.uniqueId] = CachedBook(cacheId, prepared.parsed)
        player.sendMessage(
            LanguageManager.deserializeLegacy(message(player, "management.debug.particle_test_cached"))
                .append(Component.space())
                .append(
                    LanguageManager.deserializeLegacy("§8[§6ガイドを表示§8]")
                        .clickEvent(ClickEvent.runCommand("/cc debug particle_test_guide textures"))
                        .hoverEvent(LanguageManager.deserializeLegacy(message(player, "management.debug.particle_test_guide.open_hover")))
                )
        )
    }

    private fun sendDestructiveFixOffer(player: Player, prepared: PreparedDisplayParticleBook, token: String) {
        player.sendMessage(message(player, "management.debug.particle_test_fix.requires_removal", mapOf(
            "fields" to prepared.removedPaths.joinToString()
        )))
        player.sendMessage(
            LanguageManager.deserializeLegacy(message(player, "management.debug.particle_test_fix.apply"))
                .clickEvent(ClickEvent.runCommand("/cc debug apply_particle_book_fix $token"))
                .hoverEvent(LanguageManager.deserializeLegacy(message(player, "management.debug.particle_test_fix.apply_hover")))
        )
    }

    private fun guideNavigation(player: Player, genre: GuideGenre, direction: String): Component =
        LanguageManager.deserializeLegacy(message(player, "management.debug.particle_test_guide.$direction"))
            .clickEvent(ClickEvent.runCommand("/cc debug particle_test_guide ${genre.id}"))
            .hoverEvent(LanguageManager.deserializeLegacy(message(player, "management.debug.particle_test_guide.navigation_hover", mapOf(
                "genre" to genre.id
            ))))

    private companion object {
        const val DISPLAY_DISTANCE_BLOCKS = 4.0
        const val CHOICES_PER_LINE = 24
        val GUIDE_GENRES = linkedMapOf(
            "textures" to listOf("textures[].block", "textures[].weight"),
            "scale" to listOf("scale.initial", "scale.peak", "scale.peak_progress", "scale.scale_in_ticks", "scale.variation"),
            "rotation" to listOf("rotation.random_initial", "rotation.angular_velocity", "rotation.variation"),
            "lifetime" to listOf("lifetime.ticks", "lifetime.variation", "lifetime.fade_out_ticks", "lifetime.fade_variation", "lifetime.spawn_delay"),
            "motion" to listOf("motion.preset", "motion.initial_velocity", "motion.acceleration", "motion.retention", "motion.turbulence", "motion.frequency", "motion.radial_speed", "motion.spawn_radius", "motion.orbit_speed", "motion.radial_pull", "motion.attraction", "motion.max_speed"),
            "collision" to listOf("collision.mode", "collision.restitution"),
            "emission" to listOf("emission.offset", "emission.delta", "emission.speed", "emission.count", "emission.visibility")
        ).map { (id, paths) ->
            GuideGenre(id, paths.map { path -> GuideField(path, "management.debug.particle_test_guide.field.${path.replace("[]", "_entry").replace('.', '_')}") })
        }
    }

    private data class GuideField(val path: String, val descriptionKey: String)
    private data class GuideGenre(val id: String, val fields: List<GuideField>)
}
