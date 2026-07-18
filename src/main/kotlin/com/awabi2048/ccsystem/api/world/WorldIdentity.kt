package com.awabi2048.ccsystem.api.world

import org.bukkit.NamespacedKey

data class WorldIdentity(
    val key: NamespacedKey,
    val runtimeName: String,
    val displayName: String? = null
)
