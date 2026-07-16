package com.awabi2048.ccsystem.api.lwc

/**
 * 外部プラグインが管理するアーカイブ済みワールドをLWC集計から除外する公開拡張点。
 */
interface WorldArchiveStatusProvider {
    fun getId(): String

    fun isArchived(worldName: String): Boolean
}
