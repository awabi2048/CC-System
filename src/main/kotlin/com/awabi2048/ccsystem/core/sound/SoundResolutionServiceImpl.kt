package com.awabi2048.ccsystem.core.sound

import com.awabi2048.ccsystem.api.sound.SoundResolutionService
import java.util.Locale
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound

class SoundResolutionServiceImpl : SoundResolutionService {
    companion object {
        @JvmStatic
        fun normalizedResourceId(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                return null
            }
            if (trimmed.matches(Regex("[A-Z0-9_]+"))) {
                return null
            }
            val normalized = trimmed.lowercase(Locale.ROOT)
            return if (':' in normalized) normalized else "minecraft:$normalized"
        }
    }

    override fun resolve(raw: String): Sound {
        return resolveOrNull(raw)
            ?: throw IllegalArgumentException("Unknown sound id: $raw")
    }

    override fun resolveOrNull(raw: String): Sound? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        if (trimmed.matches(Regex("[A-Z0-9_]+"))) {
            Registry.SOUND_EVENT.keyStream()
                .filter { key ->
                    key.key.uppercase(Locale.ROOT)
                        .replace('.', '_')
                        .replace('/', '_') == trimmed
                }
                .findFirst()
                .map(Registry.SOUND_EVENT::get)
                .orElse(null)
                ?.let { return it }
        }
        val normalized = normalizedResourceId(trimmed) ?: return null
        val key = NamespacedKey.fromString(normalized) ?: return null
        return Registry.SOUND_EVENT.get(key)
    }

    override fun canonicalId(raw: String): String {
        return Registry.SOUND_EVENT.getKey(resolve(raw)).toString()
    }
}
