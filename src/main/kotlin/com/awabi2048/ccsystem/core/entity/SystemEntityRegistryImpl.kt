package com.awabi2048.ccsystem.core.entity

import com.awabi2048.ccsystem.api.entity.SystemEntityRegistry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * PDCを正規の永続マーカーとして使用するシステムEntity台帳です。
 *
 * 旧実装が既に生成したEntityも操作対象から漏らさないよう、各機能が従来から
 * 使用していた予約タグ・PDCも読み取り専用の後方認識対象にしています。
 */
internal class SystemEntityRegistryImpl : SystemEntityRegistry {
    private val markerKey = NamespacedKey(NAMESPACE, MARKER_KEY)
    private val ownerKey = NamespacedKey(NAMESPACE, OWNER_KEY)

    override fun mark(entity: Entity, owner: Plugin) {
        val container = entity.persistentDataContainer
        container.set(markerKey, PersistentDataType.BYTE, 1.toByte())
        container.set(ownerKey, PersistentDataType.STRING, owner.name)
        entity.addScoreboardTag(SYSTEM_ENTITY_TAG)
    }

    override fun isSystemEntity(entity: Entity): Boolean {
        // PlayerはシステムEntityとして扱わない契約です。誤ってPDCやタグが付いても、
        // プレイヤー対象コマンドまで巻き込まないように明示的に除外します。
        if (entity is Player) return false

        val container = entity.persistentDataContainer
        if (container.get(markerKey, PersistentDataType.BYTE) == 1.toByte()) return true

        val tags = entity.scoreboardTags
        if (SYSTEM_ENTITY_TAG in tags || tags.any(::isLegacySystemTag)) return true

        return container.keys.any(::isLegacySystemKey)
    }

    private fun isLegacySystemTag(tag: String): Boolean {
        return tag in LEGACY_SYSTEM_TAGS || LEGACY_SYSTEM_TAG_PREFIXES.any(tag::startsWith)
    }

    private fun isLegacySystemKey(key: NamespacedKey): Boolean {
        val namespace = key.namespace
        val value = key.key
        return when (namespace) {
            "kantancommander" -> value == "summoned_entity" || value == "summoned_by_script"
            "cc-system" -> value.startsWith("gesture_gui_") || value.startsWith("display-effect")
            "cc-content" -> value in LEGACY_CC_CONTENT_MOB_KEYS
            "cccontent" -> value.startsWith("crops_")
            "myworldmanager" -> value == "portal_display_id"
            else -> false
        }
    }

    private companion object {
        const val NAMESPACE = "ccsystem"
        const val MARKER_KEY = "system_entity"
        const val OWNER_KEY = "system_entity_owner"
        const val SYSTEM_ENTITY_TAG = "ccsystem.system_entity"

        val LEGACY_SYSTEM_TAGS = setOf(
            "kantan_commander_display",
            "command_block_for_building_display",
            "cbf_summoned",
            "ccsystem.display-effect",
            "ccsystem.display-particle",
            "sd.portal.return",
            "sd.return_portal_marker",
            "world_sprout_marker",
        )

        // 構造マーカー、カスタムMobの補助Entity、ダウン状態の補助Entityなど、
        // 旧機能が用途別に付けていた予約タグをまとめて認識します。
        val LEGACY_SYSTEM_TAG_PREFIXES = setOf(
            "cc.mob.",
            "sd.marker.",
            "arena.marker.",
            "arena.down.",
            "marker.facing.",
        )

        val LEGACY_CC_CONTENT_MOB_KEYS = setOf(
            "mob_type_id",
            "mob_definition_id",
            "mob_feature_id",
            "is_custom_mob",
        )
    }
}
