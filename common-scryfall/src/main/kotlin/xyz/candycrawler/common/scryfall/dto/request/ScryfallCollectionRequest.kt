package xyz.candycrawler.common.scryfall.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class ScryfallCollectionRequest(val identifiers: List<ScryfallCardIdentifier>)

data class ScryfallCardIdentifier(
    @JsonProperty("set")
    val set: String?,
    @JsonProperty("collector_number")
    val collectorNumber: String?,
    @JsonProperty("id")
    val id: String?,
)
