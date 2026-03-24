package xyz.candycrawler.collectionmanager.application.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.collectionmanager.application.rest.dto.response.TribalStatsResponse
import xyz.candycrawler.collectionmanager.application.service.TribalAnalysisService

@Tag(name = "Tribal Analysis", description = "Analyze tribal depth in the local collection")
@RestController
@RequestMapping("/api/v1/cards/tribal")
class TribalAnalysisController(private val tribalAnalysisService: TribalAnalysisService) {

    @Operation(summary = "Analyze tribal depth for a given creature type")
    @GetMapping("/{tribe}")
    fun analyze(@PathVariable tribe: String): TribalStatsResponse {
        val stats = tribalAnalysisService.analyze(tribe)
        return TribalStatsResponse(
            tribe = stats.tribe,
            totalCards = stats.totalCards,
            byCmc = stats.byCmc,
            byRole = TribalStatsResponse.ByRoleResponse(
                creatures = stats.creatures,
                tribalSpells = stats.tribalSpells,
                tribalSupport = stats.tribalSupport,
            ),
            colorSpread = stats.colorSpread,
            hasLord = stats.hasLord,
            hasCommander = stats.hasCommander,
            deckViability = stats.deckViability,
        )
    }
}
