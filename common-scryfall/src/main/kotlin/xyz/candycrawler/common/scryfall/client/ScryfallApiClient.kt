package xyz.candycrawler.common.scryfall.client

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import xyz.candycrawler.common.scryfall.dto.request.ScryfallCollectionRequest
import xyz.candycrawler.common.scryfall.dto.response.ScryfallCatalogResponse
import xyz.candycrawler.common.scryfall.dto.response.ScryfallCollectionResponse
import xyz.candycrawler.common.scryfall.dto.response.ScryfallSearchResponse
import xyz.candycrawler.common.scryfall.dto.response.ScryfallSetsResponse

interface ScryfallApiClient {

    @PostExchange("/cards/collection")
    fun fetchCollection(@RequestBody request: ScryfallCollectionRequest): ScryfallCollectionResponse

    @GetExchange("/catalog/creature-types")
    fun getCreatureTypes(): ScryfallCatalogResponse

    @GetExchange("/cards/search")
    fun searchCards(
        @RequestParam("q") query: String,
        @RequestParam("unique") unique: String?,
        @RequestParam("order") order: String?,
        @RequestParam("dir") dir: String?,
        @RequestParam("include_extras") includeExtras: Boolean?,
        @RequestParam("include_multilingual") includeMultilingual: Boolean?,
        @RequestParam("include_variations") includeVariations: Boolean?,
        @RequestParam("page") page: Int?,
    ): ScryfallSearchResponse

    @GetExchange("/sets")
    fun getSets(): ScryfallSetsResponse
}
