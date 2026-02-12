package com.awabi2048.ccsystem.core.queue.model

/**
 * チャンク生成を伴うコンテンツの種別
 */
enum class ContentType {
    SUKIMA_DUNGEON,
    ARENA,
    RESOURCE;

    companion object {
        /**
         * 文字列からContentTypeを取得します。
         * 不正な文字列の場合はnullを返します。
         */
        fun fromString(value: String): ContentType? {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}
