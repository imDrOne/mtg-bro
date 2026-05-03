package xyz.candycrawler.collectionmanager.application.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.collectionmanager.application.rest.dto.request.SaveDeckRequest
import xyz.candycrawler.collectionmanager.application.rest.dto.response.DeckDetailResponse
import xyz.candycrawler.collectionmanager.application.rest.dto.response.DeckEntryResponse
import xyz.candycrawler.collectionmanager.application.rest.dto.response.DeckHeaderResponse
import xyz.candycrawler.collectionmanager.application.rest.dto.response.DeckListResponse
import xyz.candycrawler.collectionmanager.application.security.userId
import xyz.candycrawler.collectionmanager.application.service.DeckService
import xyz.candycrawler.collectionmanager.domain.deck.exception.InvalidDeckException
import xyz.candycrawler.collectionmanager.domain.deck.model.Deck
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckFormat
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckHeader

@Tag(name = "Decks", description = "Deck building and management")
@RestController
@RequestMapping("/api/v1/decks")
class DeckController(private val deckService: DeckService) {

    @PreAuthorize("hasAuthority('PERM_api:decks:write')")
    @Operation(summary = "Save a deck")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun saveDeck(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: SaveDeckRequest): DeckDetailResponse {
        val format = runCatching { DeckFormat.valueOf(request.format.uppercase()) }
            .getOrElse {
                throw InvalidDeckException(
                    "Unknown format: ${request.format}. Allowed values: ${DeckFormat.entries.joinToString { it.name }}",
                )
            }

        val deck = deckService.save(
            userId = jwt.userId(),
            name = request.name,
            format = format,
            comment = request.comment,
            mainboard = request.mainboard.map { Triple(it.setCode, it.collectorNumber, it.quantity) },
            sideboard = request.sideboard.map { Triple(it.setCode, it.collectorNumber, it.quantity) },
        )
        return deck.toDetailResponse()
    }

    @PreAuthorize("hasAuthority('PERM_api:decks:read')")
    @Operation(summary = "List all decks")
    @GetMapping
    fun listDecks(@AuthenticationPrincipal jwt: Jwt): DeckListResponse =
        DeckListResponse(decks = deckService.findAll(jwt.userId()).map { it.toHeaderResponse() })

    @PreAuthorize("hasAuthority('PERM_api:decks:read')")
    @Operation(summary = "Get a deck by ID with all entries")
    @GetMapping("/{id}")
    fun getDeck(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long): DeckDetailResponse =
        deckService.findById(jwt.userId(), id).toDetailResponse()

    private fun DeckHeader.toHeaderResponse() = DeckHeaderResponse(
        id = id,
        name = name,
        format = format.name,
        colorIdentity = colorIdentity,
        comment = comment,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun Deck.toDetailResponse() = DeckDetailResponse(
        id = id!!,
        name = name,
        format = format.name,
        colorIdentity = colorIdentity,
        comment = comment,
        mainboard = entries.filter { !it.isSideboard }.map { DeckEntryResponse(it.cardId, it.quantity) },
        sideboard = entries.filter { it.isSideboard }.map { DeckEntryResponse(it.cardId, it.quantity) },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
