package com.awabi2048.ccsystem.core.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DisplayParticleLimitTypeTest {
    @Test
    fun `コマンド名と設定キーを一対一で解決する`() {
        val expected = mapOf(
            "global" to "particle.max_active",
            "owner" to "particle.max_active_per_owner",
            "per-tick" to "particle.max_spawned_per_tick_per_owner",
            "emission" to "particle.max_per_emission"
        )

        assertEquals(expected, DisplayParticleLimitType.entries.associate { it.commandName to it.configPath })
        expected.keys.forEach { name ->
            assertEquals(name, DisplayParticleLimitType.fromCommandName(name.uppercase())?.commandName)
        }
        assertNull(DisplayParticleLimitType.fromCommandName("unknown"))
    }

    @Test
    fun `運用上限の初期値と絶対設定範囲を固定する`() {
        assertEquals(512, DisplayParticleLimitType.GLOBAL.defaultValue)
        assertEquals(128, DisplayParticleLimitType.OWNER.defaultValue)
        assertEquals(64, DisplayParticleLimitType.PER_TICK.defaultValue)
        assertEquals(32, DisplayParticleLimitType.EMISSION.defaultValue)
        assertEquals(1..4096, DisplayParticleLimitType.GLOBAL.allowedRange)
        assertEquals(1..32, DisplayParticleLimitType.EMISSION.allowedRange)
    }
}
