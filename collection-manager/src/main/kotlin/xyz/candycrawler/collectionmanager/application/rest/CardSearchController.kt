package xyz.candycrawler.collectionmanager.application.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CardCollectionInfo
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CardImageUris
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CardPrices
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CardResponse
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CardSearchResponse
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.collection.repository.CollectionEntryRepository
import xyz.candycrawler.collectionmanager.domain.card.model.CardSearchCriteria
import xyz.candycrawler.collectionmanager.domain.card.model.CardSortOrder
import xyz.candycrawler.collectionmanager.domain.card.model.SortDirection

private fun parseColorFilter(input: String?): List<String>? {
    if (input.isNullOrBlank()) return null
    val normalized = input.lowercase().replace(",", "").replace(" ", "")
    if (normalized.isEmpty()) return null
    return normalized.map { it.toString().uppercase() }.distinct()
}

@Tag(name = "Card Search", description = "Search cards in the local library")
@RestController
@RequestMapping("/api/v1/cards")
class CardSearchController(
    private val cardRepository: CardRepository,
    private val collectionEntryRepository: CollectionEntryRepository,
) {

    @Operation(
        summary = "Search cards in the local library",
        description = """
            Returns a paginated list of cards matching the search criteria.
            The query parameter searches across card name, type line, and oracle text.
            Results can be sorted and filtered by set code and rarity.
        """,
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Search results",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = CardSearchResponse::class),
                )],
            ),
        ],
    )
    @GetMapping("/search")
    fun searchCards(
        @Parameter(description = "Fulltext search query (searches name, type line, oracle text)")
        @RequestParam(required = false) q: String?,
        @Parameter(description = "Filter by set code (e.g. 'neo', 'dmu')")
        @RequestParam(required = false) set: String?,
        @Parameter(description = "Filter by collector number (exact match, use with set for single card)")
        @RequestParam(name = "collector_number", required = false) collectorNumber: String?,
        @Parameter(description = "Filter by colors (mana cost). Comma-separated or concatenated: w,u,b,r,g. E.g. 'wu' or 'w,u' for white-blue")
        @RequestParam(required = false) colors: String?,
        @Parameter(description = "Filter by color identity. Same format as colors")
        @RequestParam(name = "color_identity", required = false) colorIdentity: String?,
        @Parameter(description = "Filter by type (typeLine contains). E.g. 'creature', 'instant', 'land'")
        @RequestParam(required = false) type: String?,
        @Parameter(description = "Filter by rarity: common, uncommon, rare, mythic, special, bonus")
        @RequestParam(required = false) rarity: String?,
        @Parameter(description = "Sort order: name, set, released, rarity, color, usd, eur, cmc, power, toughness, artist")
        @RequestParam(required = false, defaultValue = "name") order: String,
        @Parameter(description = "Sort direction: auto, asc, desc")
        @RequestParam(required = false, defaultValue = "auto") dir: String,
        @Parameter(description = "Page number (1-based)")
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @Parameter(description = "Items per page (max ${CardSearchCriteria.MAX_PAGE_SIZE})")
        @RequestParam(required = false, defaultValue = "20") pageSize: Int,
    ): CardSearchResponse {
        val criteria = CardSearchCriteria(
            query = q,
            setCode = set,
            collectorNumber = collectorNumber,
            colors = parseColorFilter(colors),
            colorIdentity = parseColorFilter(colorIdentity),
            type = type,
            rarity = rarity,
            order = CardSortOrder.fromString(order),
            direction = SortDirection.fromString(dir),
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(1, CardSearchCriteria.MAX_PAGE_SIZE),
        )

        val result = cardRepository.search(criteria)

        val cardIds = result.cards.mapNotNull { it.id }
        val collectionByCardId = collectionEntryRepository.findByCardIds(cardIds)
            .groupBy { it.cardId }
            .mapValues { (_, entries) ->
                val nonFoil = entries.filter { !it.foil }.sumOf { it.quantity }
                val foil = entries.filter { it.foil }.sumOf { it.quantity }
                CardCollectionInfo(quantityNonFoil = nonFoil, quantityFoil = foil)
            }

        return CardSearchResponse(
            totalCards = result.totalCards,
            hasMore = result.hasMore,
            page = result.page,
            pageSize = result.pageSize,
            data = result.cards.map { it.toResponse(collectionByCardId[it.id]) },
        )
    }

    private fun Card.toResponse(collection: CardCollectionInfo? = null): CardResponse = CardResponse(
        id = id,
        scryfallId = scryfallId,
        oracleId = oracleId,
        name = name,
        lang = lang,
        layout = layout,
        manaCost = manaCost,
        cmc = cmc,
        typeLine = typeLine,
        oracleText = oracleText,
        colors = colors,
        colorIdentity = colorIdentity,
        keywords = keywords,
        power = power,
        toughness = toughness,
        loyalty = loyalty,
        setCode = setCode,
        setName = setName,
        collectorNumber = collectorNumber,
        rarity = rarity,
        releasedAt = releasedAt,
        imageUris = CardImageUris(
            small = imageUriSmall,
            normal = imageUriNormal,
            large = imageUriLarge,
            png = imageUriPng,
            artCrop = imageUriArtCrop,
            borderCrop = imageUriBorderCrop,
        ),
        prices = CardPrices(
            usd = priceUsd,
            usdFoil = priceUsdFoil,
            eur = priceEur,
            eurFoil = priceEurFoil,
        ),
        flavorText = flavorText,
        artist = artist,
        mtgaId = mtgaId,
        collection = collection,
    )
}
