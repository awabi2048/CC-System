package com.awabi2048.ccsystem.api.time

import java.time.LocalDate
import java.time.ZoneId
import org.bukkit.World

/** 現実日付をすべてのコンテンツで同じ季節へ解決する公開サービス。 */
interface SeasonService {
    fun currentSeason(): Season

    fun seasonAt(date: LocalDate): Season

    fun currentContext(world: World): SeasonContext

    fun overrideState(): SeasonOverride?

    fun setOverride(season: Season, actor: String)

    fun clearOverride(actor: String): Boolean
}

enum class Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER
}

data class SeasonContext(
    val season: Season,
    val date: LocalDate,
    val dayKey: String,
    val zoneId: ZoneId,
    val overridden: Boolean
)

data class SeasonOverride(
    val season: Season,
    val actor: String,
    val changedAt: String
)
