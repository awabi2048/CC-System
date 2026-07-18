package com.awabi2048.ccsystem.core.resource

import com.awabi2048.ccsystem.api.resource.ResourceWorldGeneration
import com.awabi2048.ccsystem.api.resource.ResourceWorldState
import org.bukkit.NamespacedKey
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.Properties
import java.util.UUID

class ResourceWorldGenerationStore(private val file: Path) {
    fun load(): List<ResourceWorldGeneration> {
        if (!Files.exists(file)) return emptyList()
        val properties = Properties().also { values ->
            Files.newInputStream(file).use { input: InputStream -> values.load(input.reader(StandardCharsets.UTF_8)) }
        }
        return properties.stringPropertyNames()
            .asSequence()
            .filter { it.endsWith(".worldKey") }
            .mapNotNull { key ->
                val prefix = key.removeSuffix(".worldKey")
                runCatching {
                    ResourceWorldGeneration(
                        worldKey = requireNotNull(NamespacedKey.fromString(requireNotNull(properties.getProperty("$prefix.worldKey")))),
                        runtimeWorldName = requireNotNull(properties.getProperty("$prefix.runtimeWorldName")),
                        resourceType = requireNotNull(properties.getProperty("$prefix.resourceType")),
                        variation = requireNotNull(properties.getProperty("$prefix.variation")),
                        generationId = UUID.fromString(requireNotNull(properties.getProperty("$prefix.generationId"))),
                        state = ResourceWorldState.valueOf(requireNotNull(properties.getProperty("$prefix.state")))
                    )
                }.getOrNull()
            }
            .toList()
    }

    fun save(generations: Collection<ResourceWorldGeneration>) {
        Files.createDirectories(file.parent)
        val properties = Properties()
        generations.sortedBy { it.worldKey.toString() }.forEach { generation ->
            val encodedKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(generation.worldKey.toString().toByteArray(StandardCharsets.UTF_8))
            val prefix = "generation.$encodedKey"
            properties["$prefix.worldKey"] = generation.worldKey.toString()
            properties["$prefix.runtimeWorldName"] = generation.runtimeWorldName
            properties["$prefix.resourceType"] = generation.resourceType
            properties["$prefix.variation"] = generation.variation
            properties["$prefix.generationId"] = generation.generationId.toString()
            properties["$prefix.state"] = generation.state.name
        }

        val temporary = file.resolveSibling("${file.fileName}.tmp")
        Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use { writer ->
            properties.store(writer, "CC-System resource world generations")
        }
        runCatching {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
