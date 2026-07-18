package com.awabi2048.ccsystem.api.sound

import org.bukkit.Sound

interface SoundResolutionService {
    fun resolve(raw: String): Sound

    fun resolveOrNull(raw: String): Sound?

    fun canonicalId(raw: String): String
}
