package xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class ScryfallCollectionRequest(
    val identifiers: List<CardIdentifier>,
)

data class CardIdentifier(
    @JsonProperty("set")
    val set: String,
    @JsonProperty("collector_number")
    val collectorNumber: String,
)
