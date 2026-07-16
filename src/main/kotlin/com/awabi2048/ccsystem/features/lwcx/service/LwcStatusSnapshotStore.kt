package com.awabi2048.ccsystem.features.lwcx.service

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties

class LwcStatusSnapshotStore(private val file: File) {
    fun load(): LwcSnapshot? {
        if (!file.isFile) return null
        return runCatching {
            val properties = Properties()
            Files.newInputStream(file.toPath()).use { properties.load(it) }
            LwcSnapshot(
                total = properties.getProperty("total")?.toIntOrNull() ?: return null,
                existing = properties.getProperty("existing")?.toIntOrNull() ?: return null,
                missing = properties.getProperty("missing")?.toIntOrNull() ?: return null
            )
        }.getOrNull()
    }

    fun save(snapshot: LwcSnapshot) {
        file.parentFile?.mkdirs()
        val properties = Properties()
        properties.setProperty("total", snapshot.total.toString())
        properties.setProperty("existing", snapshot.existing.toString())
        properties.setProperty("missing", snapshot.missing.toString())
        Files.newOutputStream(file.toPath()).use { output ->
            properties.store(output, "CC-System LWC snapshot")
        }
    }
}
