package xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ScryfallSearchResponse(
    @JsonProperty("object")
    val objectType: String,
    @JsonProperty("total_cards")
    val totalCards: Int,
    @JsonProperty("has_more")
    val hasMore: Boolean,
    @JsonProperty("next_page")
    val nextPage: String?,
    val data: List<ScryfallCardResponse>,
)
