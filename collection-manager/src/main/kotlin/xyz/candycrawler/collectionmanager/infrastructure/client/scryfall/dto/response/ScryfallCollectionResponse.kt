package xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.CardIdentifier

data class ScryfallCollectionResponse(
    val data: List<ScryfallCardResponse>,
    @JsonProperty("not_found")
    val notFound: List<CardIdentifier>,
)
