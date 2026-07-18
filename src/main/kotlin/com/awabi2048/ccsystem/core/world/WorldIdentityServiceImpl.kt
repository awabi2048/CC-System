package com.awabi2048.ccsystem.core.world

import com.awabi2048.ccsystem.api.world.WorldIdentity
import com.awabi2048.ccsystem.api.world.WorldIdentityService
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World

class WorldIdentityServiceImpl : WorldIdentityService {
    override fun identity(world: World): WorldIdentity {
        return WorldIdentity(
            key = world.key,
            runtimeName = world.name
        )
    }

    override fun loadedWorld(key: NamespacedKey): World? = Bukkit.getWorld(key)

    override fun loadedWorld(identity: WorldIdentity): World? = loadedWorld(identity.key)

    override fun keyString(world: World): String = world.key.toString()
}
