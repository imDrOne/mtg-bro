package xyz.candycrawler.collectionmanager.application.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.ScryfallApiClient
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallSearchResponse

@Tag(name = "Scryfall Proxy", description = "Proxy endpoints for Scryfall API (for UI consumption)")
@RestController
@RequestMapping("/api/v1/scryfall")
class ScryfallProxyController(private val scryfallApiClient: ScryfallApiClient) {

    @Operation(
        summary = "Search cards via Scryfall API",
        description = """
            Proxies the search request to Scryfall's /cards/search endpoint.
            Supports the same fulltext search system as Scryfall's main site.
            Results are paginated, returning up to 175 cards at a time.
        """,
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Scryfall search results",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ScryfallSearchResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PreAuthorize("hasAuthority('PERM_api:scryfall:search')")
    @GetMapping("/cards/search")
    fun searchCards(
        @Parameter(description = "Scryfall fulltext search query", required = true)
        @RequestParam q: String,
        @Parameter(description = "Strategy for omitting similar cards: cards, art, prints")
        @RequestParam(required = false) unique: String?,
        @Parameter(
            description = "Sort order: name, set, released, rarity, color, usd, tix, eur, " +
                "cmc, power, toughness, edhrec, penny, artist, review",
        )
        @RequestParam(required = false) order: String?,
        @Parameter(description = "Sort direction: auto, asc, desc")
        @RequestParam(required = false) dir: String?,
        @Parameter(description = "Include extra cards (tokens, planes, etc)")
        @RequestParam(name = "include_extras", required = false) includeExtras: Boolean?,
        @Parameter(description = "Include cards in every supported language")
        @RequestParam(name = "include_multilingual", required = false) includeMultilingual: Boolean?,
        @Parameter(description = "Include rare card variants")
        @RequestParam(name = "include_variations", required = false) includeVariations: Boolean?,
        @Parameter(description = "Page number (default 1)")
        @RequestParam(required = false) page: Int?,
    ): ScryfallSearchResponse = scryfallApiClient.searchCards(
        query = q,
        unique = unique,
        order = order,
        dir = dir,
        includeExtras = includeExtras,
        includeMultilingual = includeMultilingual,
        includeVariations = includeVariations,
        page = page,
    )
}
