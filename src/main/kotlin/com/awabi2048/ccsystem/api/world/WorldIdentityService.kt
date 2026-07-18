package com.awabi2048.ccsystem.api.world

import org.bukkit.NamespacedKey
import org.bukkit.World

interface WorldIdentityService {
    fun identity(world: World): WorldIdentity

    fun loadedWorld(key: NamespacedKey): World?

    fun loadedWorld(identity: WorldIdentity): World?

    fun keyString(world: World): String
}
