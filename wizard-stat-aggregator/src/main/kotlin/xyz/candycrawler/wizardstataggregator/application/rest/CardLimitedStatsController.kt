package xyz.candycrawler.wizardstataggregator.application.rest

import kotlinx.coroutines.launch
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.wizardstataggregator.application.rest.dto.request.CollectCardLimitedStatsRequest
import xyz.candycrawler.wizardstataggregator.application.rest.dto.request.UpsertTrackedLimitedStatSetRequest
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.CardLimitedStatsSearchResponse
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.CollectCardLimitedStatsResponse
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.TrackedLimitedStatSetResponse
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.toResponse
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsCollectionService
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsSearchService
import xyz.candycrawler.wizardstataggregator.application.service.TrackedLimitedStatSetService
import xyz.candycrawler.wizardstataggregator.configuration.coroutine.ApplicationCoroutineScope
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSearchCriteria
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSortDirection
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSortOrder
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.TrackedLimitedStatSet
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/card-limited-stats")
class CardLimitedStatsController(
    private val collectionService: CardLimitedStatsCollectionService,
    private val searchService: CardLimitedStatsSearchService,
    private val trackedSetService: TrackedLimitedStatSetService,
    private val coroutineScope: ApplicationCoroutineScope,
) {

    @GetMapping
    fun search(
        @RequestParam(name = "set_code") setCode: String,
        @RequestParam(name = "match_type") matchType: String,
        @RequestParam(required = false) names: List<String>?,
        @RequestParam(name = "mtga_ids", required = false) mtgaIds: List<Int>?,
        @RequestParam(name = "min_win_rate", required = false) minWinRate: Double?,
        @RequestParam(name = "max_win_rate", required = false) maxWinRate: Double?,
        @RequestParam(required = false, defaultValue = "win_rate") sort: String,
        @RequestParam(name = "sort_dir", required = false, defaultValue = "desc") sortDir: String,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(name = "page_size", required = false, defaultValue = "20") pageSize: Int,
    ): CardLimitedStatsSearchResponse {
        val criteria = CardLimitedStatsSearchCriteria(
            setCode = setCode,
            matchType = matchType.toLands17MatchType(),
            names = names.orEmpty().mapNotNull { it.trim().takeIf(String::isNotEmpty) },
            mtgaIds = mtgaIds.orEmpty().filter { it > 0 }.distinct(),
            minWinRate = minWinRate,
            maxWinRate = maxWinRate,
            order = CardLimitedStatsSortOrder.fromString(sort),
            direction = CardLimitedStatsSortDirection.fromString(sortDir),
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(
                1,
                CardLimitedStatsSearchCriteria.MAX_PAGE_SIZE,
            ),
        )

        return searchService.search(criteria).toResponse()
    }

    @PreAuthorize("hasAuthority('PERM_api:stats:collect')")
    @PostMapping("/collect")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun collect(@RequestBody request: CollectCardLimitedStatsRequest): CollectCardLimitedStatsResponse {
        coroutineScope.launch {
            collectionService.collectAll(request.setCode)
        }

        return CollectCardLimitedStatsResponse(
            setCode = request.setCode,
            message = "Collection started for set ${request.setCode}",
        )
    }

    @PreAuthorize("hasAuthority('PERM_api:stats:tracked-sets:manage')")
    @GetMapping("/tracked-sets")
    fun getTrackedSets(): List<TrackedLimitedStatSetResponse> {
        val today = LocalDate.now()
        return trackedSetService.findAll().map { it.toResponse(today) }
    }

    @PreAuthorize("hasAuthority('PERM_api:stats:tracked-sets:manage')")
    @PutMapping("/tracked-sets/{setCode}")
    fun upsertTrackedSet(
        @PathVariable setCode: String,
        @RequestBody request: UpsertTrackedLimitedStatSetRequest,
    ): TrackedLimitedStatSetResponse {
        val today = LocalDate.now()
        return trackedSetService.upsert(setCode, request.watchUntil).toResponse(today)
    }

    @PreAuthorize("hasAuthority('PERM_api:stats:tracked-sets:manage')")
    @DeleteMapping("/tracked-sets/{setCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTrackedSet(@PathVariable setCode: String) {
        trackedSetService.delete(setCode)
    }

    private fun TrackedLimitedStatSet.toResponse(today: LocalDate): TrackedLimitedStatSetResponse =
        TrackedLimitedStatSetResponse(
            setCode = setCode,
            watchUntil = watchUntil,
            active = isActive(today),
        )
}

private fun String.toLands17MatchType(): String = when (trim().lowercase().replace("_", "").replace("-", "")) {
    "quickdraft" -> "QuickDraft"
    "sealed" -> "Sealed"
    else -> trim()
}
