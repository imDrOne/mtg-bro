package xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class CardStatsResponse(
    val name: String,
    @JsonProperty("mtga_id")
    val mtgaId: Int,
    val color: String,
    val rarity: String,
    val url: String,
    @JsonProperty("url_back")
    val urlBack: String,
    val types: List<String>,
    val layout: String,
    @JsonProperty("seen_count")
    val seenCount: Int,
    @JsonProperty("avg_seen")
    val avgSeen: Double?,
    @JsonProperty("pick_count")
    val pickCount: Int,
    @JsonProperty("avg_pick")
    val avgPick: Double?,
    @JsonProperty("game_count")
    val gameCount: Int,
    @JsonProperty("pool_count")
    val poolCount: Int,
    @JsonProperty("play_rate")
    val playRate: Double,
    @JsonProperty("win_rate")
    val winRate: Double,
    @JsonProperty("opening_hand_game_count")
    val openingHandGameCount: Int,
    @JsonProperty("opening_hand_win_rate")
    val openingHandWinRate: Double,
    @JsonProperty("drawn_game_count")
    val drawnGameCount: Int,
    @JsonProperty("drawn_win_rate")
    val drawnWinRate: Double,
    @JsonProperty("ever_drawn_game_count")
    val everDrawnGameCount: Int,
    @JsonProperty("ever_drawn_win_rate")
    val everDrawnWinRate: Double,
    @JsonProperty("never_drawn_game_count")
    val neverDrawnGameCount: Int,
    @JsonProperty("never_drawn_win_rate")
    val neverDrawnWinRate: Double,
    @JsonProperty("drawn_improvement_win_rate")
    val drawnImprovementWinRate: Double,
)
