package com.awabi2048.ccsystem.core.gui

import com.awabi2048.ccsystem.api.gui.MenuRoute
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import org.bukkit.configuration.file.YamlConfiguration

/** UUIDだけを識別子として使い、次回ログインで一度だけ消費するGUIルート保存領域。 */
class MenuRouteStateStore(private val file: File) {
    @Synchronized
    fun load(): Map<UUID, MenuRoute> {
        if (!file.isFile) return emptyMap()
        val yaml = runCatching { YamlConfiguration.loadConfiguration(file) }.getOrNull() ?: return emptyMap()
        val routes = yaml.getConfigurationSection("routes") ?: return emptyMap()
        return routes.getKeys(false).mapNotNull { rawUuid ->
            val uuid = runCatching { UUID.fromString(rawUuid) }.getOrNull() ?: return@mapNotNull null
            val route = decode(yaml, "routes.$rawUuid") ?: return@mapNotNull null
            uuid to route
        }.toMap()
    }

    @Synchronized
    fun save(routes: Map<UUID, MenuRoute>) {
        val yaml = YamlConfiguration()
        routes.forEach { (uuid, route) ->
            if (isPersistable(route)) encode(yaml, "routes.$uuid", route)
        }
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        yaml.save(temporary)
        try {
            Files.move(temporary.toPath(), file.toPath(), REPLACE_EXISTING, ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), REPLACE_EXISTING)
        }
    }

    private fun encode(yaml: YamlConfiguration, path: String, route: MenuRoute) {
        yaml.set("$path.owner", route.owner)
        yaml.set("$path.id", route.id)
        route.payload.forEach { (key, value) -> yaml.set("$path.payload.$key", value) }
    }

    private fun decode(yaml: YamlConfiguration, path: String): MenuRoute? {
        val owner = yaml.getString("$path.owner")?.takeIf(::isSafeValue) ?: return null
        val id = yaml.getString("$path.id")?.takeIf(::isSafeValue) ?: return null
        val payloadSection = yaml.getConfigurationSection("$path.payload")
        val payload = payloadSection?.getKeys(false)?.associateWith { key ->
            payloadSection.getString(key).orEmpty()
        }.orEmpty()
        if (payload.size > MAX_PAYLOAD_ENTRIES || payload.any { !isSafeKey(it.key) || !isSafeValue(it.value) }) {
            return null
        }
        return runCatching { MenuRoute(owner, id, payload) }.getOrNull()
    }

    private fun isPersistable(route: MenuRoute): Boolean {
        return isSafeValue(route.owner) && isSafeValue(route.id) &&
            route.payload.size <= MAX_PAYLOAD_ENTRIES &&
            route.payload.all { isSafeKey(it.key) && isSafeValue(it.value) }
    }

    private fun isSafeKey(value: String): Boolean {
        return value.length in 1..64 && value.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }

    private fun isSafeValue(value: String): Boolean {
        return value.isNotBlank() && value.length <= MAX_VALUE_LENGTH && value.none(Char::isISOControl)
    }

    private companion object {
        private const val MAX_PAYLOAD_ENTRIES = 32
        private const val MAX_VALUE_LENGTH = 256
    }
}
