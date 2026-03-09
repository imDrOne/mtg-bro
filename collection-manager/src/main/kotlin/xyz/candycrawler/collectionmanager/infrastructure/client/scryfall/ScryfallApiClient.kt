package xyz.candycrawler.collectionmanager.infrastructure.client.scryfall

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.ScryfallCollectionRequest
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallCollectionResponse

interface ScryfallApiClient {

    @PostExchange("/cards/collection")
    fun fetchCollection(@RequestBody request: ScryfallCollectionRequest): ScryfallCollectionResponse
}
