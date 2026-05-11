package xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStats
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.dto.response.CardStatsResponse

@Component
class CardStatsResponseMapper {

    fun toDomain(responses: List<CardStatsResponse>, setCode: String, matchType: String): List<CardLimitedStats> {
        val tiersByMtgaId = calculateTiers(responses)

        return responses.mapNotNull { response ->
            toDomain(response, setCode, matchType, tiersByMtgaId[response.mtgaId])
        }
    }

    fun toDomain(response: CardStatsResponse, setCode: String, matchType: String): CardLimitedStats? =
        toDomain(response, setCode, matchType, tier = null)

    private fun toDomain(
        response: CardStatsResponse,
        setCode: String,
        matchType: String,
        tier: String?,
    ): CardLimitedStats? = response.requiredRates()?.let { rates ->
        CardLimitedStats(
            name = response.name,
            mtgaId = response.mtgaId,
            setCode = setCode,
            matchType = matchType,
            tier = tier,
            color = response.color,
            rarity = response.rarity,
            url = response.url,
            urlBack = response.urlBack,
            types = response.types,
            layout = response.layout,
            seenCount = response.seenCount,
            avgSeen = response.avgSeen,
            pickCount = response.pickCount,
            avgPick = response.avgPick,
            gameCount = response.gameCount,
            poolCount = response.poolCount,
            playRate = rates.playRate,
            winRate = rates.winRate,
            openingHandGameCount = response.openingHandGameCount,
            openingHandWinRate = rates.openingHandWinRate,
            drawnGameCount = response.drawnGameCount,
            drawnWinRate = rates.drawnWinRate,
            everDrawnGameCount = response.everDrawnGameCount,
            everDrawnWinRate = rates.everDrawnWinRate,
            neverDrawnGameCount = response.neverDrawnGameCount,
            neverDrawnWinRate = rates.neverDrawnWinRate,
            drawnImprovementWinRate = rates.drawnImprovementWinRate,
        )
    }

    private fun calculateTiers(responses: List<CardStatsResponse>): Map<Int, String> {
        val cardsWithGihWinRate = responses.filter { it.everDrawnWinRate != null }
        if (cardsWithGihWinRate.size < MIN_CARDS_FOR_STANDARD_DEVIATION) return emptyMap()

        val values = cardsWithGihWinRate.map { requireNotNull(it.everDrawnWinRate) }
        val mean = values.average()
        val standardDeviation = kotlin.math.sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
        if (standardDeviation == 0.0) return emptyMap()

        return cardsWithGihWinRate.associate { response ->
            val zScore = (requireNotNull(response.everDrawnWinRate) - mean) / standardDeviation
            response.mtgaId to zScore.toGrade()
        }
    }

    private fun Double.toGrade(): String {
        val index = kotlin.math.floor(GRADE_STEPS_PER_STANDARD_DEVIATION * (this + GRADE_OFFSET)).toInt()
        return when {
            index < 0 -> "F"
            index >= GRADES_ASCENDING.size -> "A+"
            else -> GRADES_ASCENDING[index]
        }
    }

    private fun CardStatsResponse.requiredRates(): RequiredRates? = if (
        listOf(
            playRate,
            winRate,
            openingHandWinRate,
            drawnWinRate,
            everDrawnWinRate,
            neverDrawnWinRate,
            drawnImprovementWinRate,
        ).any { it == null }
    ) {
        null
    } else {
        RequiredRates(
            playRate = requireNotNull(playRate),
            winRate = requireNotNull(winRate),
            openingHandWinRate = requireNotNull(openingHandWinRate),
            drawnWinRate = requireNotNull(drawnWinRate),
            everDrawnWinRate = requireNotNull(everDrawnWinRate),
            neverDrawnWinRate = requireNotNull(neverDrawnWinRate),
            drawnImprovementWinRate = requireNotNull(drawnImprovementWinRate),
        )
    }

    private data class RequiredRates(
        val playRate: Double,
        val winRate: Double,
        val openingHandWinRate: Double,
        val drawnWinRate: Double,
        val everDrawnWinRate: Double,
        val neverDrawnWinRate: Double,
        val drawnImprovementWinRate: Double,
    )

    private companion object {
        private const val MIN_CARDS_FOR_STANDARD_DEVIATION = 15
        private const val GRADE_STEPS_PER_STANDARD_DEVIATION = 3.0
        private const val GRADE_OFFSET = 2.0 - 1.0 / 6.0

        private val GRADES_ASCENDING = listOf(
            "F",
            "D-",
            "D",
            "D+",
            "C-",
            "C",
            "C+",
            "B-",
            "B",
            "B+",
            "A-",
            "A",
            "A+",
        )
    }
}
