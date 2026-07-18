package com.awabi2048.ccsystem.api.world

import java.nio.file.Path
import org.bukkit.NamespacedKey

enum class WorldDirectoryState {
    CURRENT,
    LEGACY,
    MISSING,
    CONFLICT,
    UNSAFE
}

data class WorldDirectoryResolution(
    val key: NamespacedKey,
    val state: WorldDirectoryState,
    val currentDirectory: Path,
    val legacyDirectory: Path? = null
) {
    fun existingDirectory(): Path? = when (state) {
        WorldDirectoryState.CURRENT -> currentDirectory
        WorldDirectoryState.LEGACY -> legacyDirectory
        else -> null
    }
}

data class WorldDirectoryEntry(
    val key: NamespacedKey,
    val directory: Path
)

interface WorldDirectoryService {
    fun inspect(key: NamespacedKey, legacyName: String? = null): WorldDirectoryResolution

    fun existingDirectory(key: NamespacedKey): Path?

    fun creationDirectory(key: NamespacedKey): Path

    fun dimensionDirectory(key: NamespacedKey): Path

    fun mainWorldDataDirectory(): Path

    fun listByKeyPrefix(namespace: String, prefix: String): List<WorldDirectoryEntry>

    fun worldContainerDirectory(): Path

    fun listLegacyByNamePrefix(prefix: String): List<Path>
}
