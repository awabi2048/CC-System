package com.awabi2048.ccsystem.core.world

import com.awabi2048.ccsystem.api.world.WorldDirectoryEntry
import com.awabi2048.ccsystem.api.world.WorldDirectoryResolution
import com.awabi2048.ccsystem.api.world.WorldDirectoryService
import com.awabi2048.ccsystem.api.world.WorldDirectoryState
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import org.bukkit.NamespacedKey

class WorldDirectoryServiceImpl(
    worldContainer: Path,
    private val mainWorldName: String
) : WorldDirectoryService {
    private val worldContainer = worldContainer.toAbsolutePath().normalize()
    private val mainWorldDirectory = safeResolve(this.worldContainer, mainWorldName)

    init {
        require(isSimpleSegment(mainWorldName)) { "mainWorldName must be a single safe path segment" }
    }

    override fun inspect(key: NamespacedKey, legacyName: String?): WorldDirectoryResolution {
        val current = runCatching { dimensionDirectory(key) }.getOrElse {
            return WorldDirectoryResolution(key, WorldDirectoryState.UNSAFE, worldContainer)
        }
        val legacy = when {
            legacyName == null -> null
            !isSimpleSegment(legacyName) -> {
                return WorldDirectoryResolution(key, WorldDirectoryState.UNSAFE, current)
            }
            else -> safeResolve(worldContainer, legacyName)
        }
        val currentExists = isSafeDirectory(current, mainWorldDirectory)
        val legacyExists = legacy?.let { isSafeDirectory(it, worldContainer) } == true
        val state = when {
            currentExists && legacyExists && current != legacy -> WorldDirectoryState.CONFLICT
            currentExists -> WorldDirectoryState.CURRENT
            legacyExists -> WorldDirectoryState.LEGACY
            else -> WorldDirectoryState.MISSING
        }
        return WorldDirectoryResolution(key, state, current, legacy)
    }

    override fun existingDirectory(key: NamespacedKey): Path? {
        return dimensionDirectory(key).takeIf { isSafeDirectory(it, mainWorldDirectory) }
    }

    override fun creationDirectory(key: NamespacedKey): Path = dimensionDirectory(key)

    override fun dimensionDirectory(key: NamespacedKey): Path {
        require(isSimpleSegment(key.namespace)) { "Unsafe world namespace: ${key.namespace}" }
        require(isSimpleSegment(key.key)) { "Unsafe world key: ${key.key}" }
        return safeResolve(
            safeResolve(
                safeResolve(mainWorldDirectory, "dimensions"),
                key.namespace
            ),
            key.key
        )
    }

    override fun mainWorldDataDirectory(): Path = mainWorldDirectory

    override fun worldContainerDirectory(): Path = worldContainer

    override fun listByKeyPrefix(namespace: String, prefix: String): List<WorldDirectoryEntry> {
        require(isSimpleSegment(namespace)) { "Unsafe world namespace: $namespace" }
        require(prefix.isNotEmpty() && !prefix.contains('/') && !prefix.contains('\\')) {
            "Unsafe world key prefix: $prefix"
        }
        val namespaceDirectory = safeResolve(safeResolve(mainWorldDirectory, "dimensions"), namespace)
        if (!Files.isDirectory(namespaceDirectory)) {
            return emptyList()
        }
        return Files.newDirectoryStream(namespaceDirectory).use { paths ->
            paths
                .filter { isSafeDirectory(it, namespaceDirectory) }
                .filter { it.fileName.toString().startsWith(prefix) }
                .map { path ->
                    WorldDirectoryEntry(
                        NamespacedKey(namespace, path.fileName.toString()),
                        path.toAbsolutePath().normalize()
                    )
                }
                .sortedBy { it.key.toString() }
        }
    }

    override fun listLegacyByNamePrefix(prefix: String): List<Path> {
        require(prefix.isNotEmpty() && !prefix.contains('/') && !prefix.contains('\\')) {
            "Unsafe legacy world prefix: $prefix"
        }
        if (!Files.isDirectory(worldContainer)) {
            return emptyList()
        }
        return Files.newDirectoryStream(worldContainer).use { paths ->
            paths
                .filter { isSafeDirectory(it, worldContainer) }
                .filter { it.fileName.toString().startsWith(prefix) }
                .map { it.toAbsolutePath().normalize() }
                .sortedBy { it.fileName.toString() }
        }
    }

    private fun safeResolve(base: Path, child: String): Path {
        val resolved = base.resolve(child).toAbsolutePath().normalize()
        require(resolved.startsWith(base.toAbsolutePath().normalize())) {
            "Resolved path escaped the allowed boundary: $resolved"
        }
        return resolved
    }

    private fun isSimpleSegment(value: String): Boolean {
        return value.isNotBlank() &&
            value != "." &&
            value != ".." &&
            !value.contains('/') &&
            !value.contains('\\') &&
            !value.contains('\u0000')
    }

    private fun isSafeDirectory(path: Path, boundary: Path): Boolean {
        if (!path.startsWith(boundary)) {
            return false
        }
        var current = path
        while (current != boundary) {
            if (Files.isSymbolicLink(current)) {
                return false
            }
            current = current.parent ?: return false
        }
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
    }
}
