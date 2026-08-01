package com.awabi2048.ccsystem.api.gui

/** ルートpayloadは生成時点でキー順の防御的copyへ切り離します。 */
class MenuRoute(
    val owner: String,
    val id: String,
    payload: Map<String, String> = emptyMap(),
) {
    val payload: Map<String, String> = MenuImmutableCollections.strings(payload)

    init {
        require(owner.isNotBlank()) { "owner must not be blank" }
        require(id.isNotBlank()) { "id must not be blank" }
    }

    fun key(): String {
        if (payload.isEmpty()) return "$owner:$id"
        return "$owner:$id:" + payload.entries.joinToString("&") { (key, value) -> "$key=$value" }
    }

    fun copy(
        owner: String = this.owner,
        id: String = this.id,
        payload: Map<String, String> = this.payload,
    ): MenuRoute = MenuRoute(owner, id, payload)

    operator fun component1(): String = owner
    operator fun component2(): String = id
    operator fun component3(): Map<String, String> = payload

    override fun equals(other: Any?): Boolean =
        other is MenuRoute && owner == other.owner && id == other.id && payload == other.payload

    override fun hashCode(): Int = 31 * (31 * owner.hashCode() + id.hashCode()) + payload.hashCode()

    override fun toString(): String = "MenuRoute(owner=$owner, id=$id, payload=$payload)"
}
