package xyz.candycrawler.common.scryfall.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ScryfallSetResponse(
    val code: String,
    val name: String,
    @JsonProperty("set_type") val setType: String,
    @JsonProperty("released_at") val releasedAt: String?,
    @JsonProperty("card_count") val cardCount: Int,
    val digital: Boolean,
)
