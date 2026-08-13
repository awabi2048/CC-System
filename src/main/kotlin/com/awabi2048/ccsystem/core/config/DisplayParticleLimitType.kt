package com.awabi2048.ccsystem.core.config

/** Displayパーティクルの運用制限を、設定・コマンド・実行時判定で共通利用します。 */
internal enum class DisplayParticleLimitType(
    val commandName: String,
    val configPath: String,
    val defaultValue: Int,
    val allowedRange: IntRange
) {
    GLOBAL("global", "particle.max_active", 512, 1..4096),
    OWNER("owner", "particle.max_active_per_owner", 128, 1..4096),
    PER_TICK("per-tick", "particle.max_spawned_per_tick_per_owner", 64, 1..4096),
    EMISSION("emission", "particle.max_per_emission", 32, 1..32);

    companion object {
        fun fromCommandName(value: String): DisplayParticleLimitType? =
            entries.firstOrNull { it.commandName.equals(value, ignoreCase = true) }
    }
}
