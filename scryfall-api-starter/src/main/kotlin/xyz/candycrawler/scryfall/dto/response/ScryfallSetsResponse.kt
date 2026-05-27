package xyz.candycrawler.scryfall.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ScryfallSetsResponse(
    @JsonProperty("object") val objectType: String,
    @JsonProperty("has_more") val hasMore: Boolean,
    val data: List<ScryfallSetResponse>,
)
