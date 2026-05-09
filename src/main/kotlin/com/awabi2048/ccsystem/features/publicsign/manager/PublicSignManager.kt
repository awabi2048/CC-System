package com.awabi2048.ccsystem.features.publicsign.manager

import com.awabi2048.ccsystem.CCSystem
import com.awabi2048.ccsystem.core.config.ConfigManager
import com.awabi2048.ccsystem.core.time.DatePolicy
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object PublicSignManager {
    const val MARKER_TEXT = "[PublicSign]"

    data class SignLocation(
        val world: String,
        val x: Int,
        val y: Int,
        val z: Int
    )

    data class PublicSignData(
        val location: SignLocation,
        val content: List<String>,
        val owner: UUID?,
        val expireDate: LocalDate
    )

    const val ENABLED_MARKER_TEXT = "§3[PublicSign]"
    const val WARNING_MARKER_TEXT = "§c[PublicSign]"

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val signs = mutableMapOf<SignLocation, PublicSignData>()
    private lateinit var dataFile: File
    private lateinit var config: YamlConfiguration

    fun load() {
        dataFile = File(CCSystem.instance.dataFolder, "data/public_sign/public_sign_data.yml")
        if (!dataFile.exists()) {
            dataFile.parentFile?.mkdirs()
            dataFile.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(dataFile)
        signs.clear()

        val section = config.getConfigurationSection("signs") ?: return
        for (key in section.getKeys(false)) {
            val signSection = section.getConfigurationSection(key) ?: continue

            val world = signSection.getString("world") ?: continue
            val x = signSection.getInt("x")
            val y = signSection.getInt("y")
            val z = signSection.getInt("z")
            val ownerRaw = signSection.getString("owner")
            val expireRaw = signSection.getString("expire_date") ?: continue

            val owner = if (ownerRaw.isNullOrBlank()) {
                null
            } else {
                try {
                    UUID.fromString(ownerRaw)
                } catch (_: IllegalArgumentException) {
                    continue
                }
            }

            val expireDate = try {
                LocalDate.parse(expireRaw, dateFormatter)
            } catch (_: Exception) {
                continue
            }

            val location = SignLocation(world, x, y, z)
            val content = normalizeContent(signSection.getStringList("content"))

            signs[location] = PublicSignData(location, content, owner, expireDate)
        }
    }

    fun save() {
        config.set("signs", null)

        for ((location, data) in signs) {
            val basePath = "signs.${toKey(location)}"
            config.set("$basePath.world", location.world)
            config.set("$basePath.x", location.x)
            config.set("$basePath.y", location.y)
            config.set("$basePath.z", location.z)
            config.set("$basePath.content", data.content)
            config.set("$basePath.owner", data.owner?.toString())
            config.set("$basePath.expire_date", data.expireDate.format(dateFormatter))
        }

        config.save(dataFile)
    }

    fun register(location: SignLocation, content: List<String>, expireDate: LocalDate) {
        val normalizedContent = normalizeContent(content)
        signs[location] = PublicSignData(location, normalizedContent, null, expireDate)
        save()
    }

    fun updateContent(location: SignLocation, content: List<String>) {
        val data = signs[location] ?: return
        val normalizedContent = normalizeContent(content)
        signs[location] = data.copy(content = normalizedContent)
        save()
    }

    fun setOwner(location: SignLocation, owner: UUID) {
        val data = signs[location] ?: return
        signs[location] = data.copy(owner = owner)
        save()
    }

    fun createDefaultContent(): List<String> {
        return normalizeContent(emptyList())
    }

    fun get(location: SignLocation): PublicSignData? = signs[location]

    fun isRegistered(location: SignLocation): Boolean = signs.containsKey(location)

    fun remove(location: SignLocation) {
        signs.remove(location)
        save()
    }

    fun toLocation(world: String, x: Int, y: Int, z: Int): SignLocation {
        return SignLocation(world, x, y, z)
    }

    fun updateDay(today: LocalDate = LocalDate.now()): Int {
        val expiredLocations = mutableListOf<SignLocation>()

        for ((location, data) in signs) {
            when {
                DatePolicy.isExpired(today, data.expireDate) -> {
                    expiredLocations.add(location)
                }

                today.plusDays(1).isEqual(data.expireDate) -> {
                    updateMarkerColor(location, WARNING_MARKER_TEXT)
                }

                else -> {
                    updateMarkerColor(location, ENABLED_MARKER_TEXT)
                }
            }
        }

        for (location in expiredLocations) {
            resetSign(location)
            signs.remove(location)
        }

        save()
        return expiredLocations.size
    }

    private fun resetSign(location: SignLocation) {
        val world = Bukkit.getWorld(location.world) ?: return
        val block = world.getBlockAt(location.x, location.y, location.z)
        val state = block.state
        if (state !is Sign) {
            return
        }

        val side = state.getSide(Side.FRONT)
        side.line(0, Component.text(MARKER_TEXT))
        side.line(1, Component.empty())
        side.line(2, Component.empty())
        side.line(3, Component.empty())
        state.update(true, false)
    }

    private fun updateMarkerColor(location: SignLocation, marker: String) {
        val world = Bukkit.getWorld(location.world) ?: return
        val block = world.getBlockAt(location.x, location.y, location.z)
        val state = block.state
        if (state !is Sign) {
            return
        }

        state.getSide(Side.FRONT).line(0, Component.text(marker))
        state.update(true, false)
    }

    private fun toKey(location: SignLocation): String {
        return "${location.world};${location.x};${location.y};${location.z}"
    }

    private fun normalizeContent(content: List<String>): List<String> {
        val lineCount = ConfigManager.getPublicSignContentLines()
        return (0 until lineCount).map { index -> content.getOrNull(index) ?: "" }
    }
}
