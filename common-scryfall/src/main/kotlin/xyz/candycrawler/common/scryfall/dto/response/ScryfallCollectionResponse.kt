package xyz.candycrawler.common.scryfall.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import xyz.candycrawler.common.scryfall.dto.request.ScryfallCardIdentifier

data class ScryfallCollectionResponse(
    val data: List<ScryfallCardResponse>,
    @JsonProperty("not_found")
    val notFound: List<ScryfallCardIdentifier>,
)
