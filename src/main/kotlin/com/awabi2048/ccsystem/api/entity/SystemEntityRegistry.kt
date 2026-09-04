package com.awabi2048.ccsystem.api.entity

import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

/**
 * システムが生成・管理するEntityを識別するための共通契約です。
 *
 * KantanCommanderなどの汎用操作系は、この契約を通じてシステムEntityを
 * 対象候補から除外します。識別情報はEntityのPDCへ保存されるため、再起動後も
 * 同じEntityを保護できます。
 */
interface SystemEntityRegistry {
    /** Entityを指定Pluginのシステム管理Entityとして記録します。 */
    fun mark(entity: Entity, owner: Plugin)

    /** Entityがシステム管理Entityとして記録済みか判定します。 */
    fun isSystemEntity(entity: Entity): Boolean
}
