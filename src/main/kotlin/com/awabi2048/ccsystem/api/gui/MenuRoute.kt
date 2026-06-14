package com.awabi2048.ccsystem.api.gui

data class MenuRoute(
    val owner: String,
    val id: String,
    val payload: Map<String, String> = emptyMap()
) {
    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
    }

    fun key(): String {
        if (payload.isEmpty()) return "$owner:$id"
        return "$owner:$id:" + payload.toSortedMap().entries.joinToString("&") { (key, value) -> "$key=$value" }
    }
}
