package xyz.candycrawler.wizardstataggregator.infrastructure.db.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CardLimitedStatsTable : LongIdTable("card_limited_stats") {
    val name = varchar("name", 255)
    val mtgaId = integer("mtga_id")
    val setCode = varchar("set_code", 10)
    val matchType = varchar("match_type", 50)
    val tier = varchar("tier", 20).nullable()
    val color = varchar("color", 50)
    val rarity = varchar("rarity", 50)
    val url = text("url")
    val urlBack = text("url_back")
    val types = array<String>("types")
    val layout = varchar("layout", 100)
    val seenCount = integer("seen_count")
    val avgSeen = double("avg_seen").nullable()
    val pickCount = integer("pick_count")
    val avgPick = double("avg_pick").nullable()
    val gameCount = integer("game_count")
    val poolCount = integer("pool_count")
    val playRate = double("play_rate")
    val winRate = double("win_rate")
    val openingHandGameCount = integer("opening_hand_game_count")
    val openingHandWinRate = double("opening_hand_win_rate")
    val drawnGameCount = integer("drawn_game_count")
    val drawnWinRate = double("drawn_win_rate")
    val everDrawnGameCount = integer("ever_drawn_game_count")
    val everDrawnWinRate = double("ever_drawn_win_rate")
    val neverDrawnGameCount = integer("never_drawn_game_count")
    val neverDrawnWinRate = double("never_drawn_win_rate")
    val drawnImprovementWinRate = double("drawn_improvement_win_rate")
}
