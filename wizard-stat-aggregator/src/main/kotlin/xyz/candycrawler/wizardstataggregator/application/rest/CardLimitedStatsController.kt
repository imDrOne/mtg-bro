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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.wizardstataggregator.application.rest.dto.request.CollectCardLimitedStatsRequest
import xyz.candycrawler.wizardstataggregator.application.rest.dto.request.UpsertTrackedLimitedStatSetRequest
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.CollectCardLimitedStatsResponse
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.TrackedLimitedStatSetResponse
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsCollectionService
import xyz.candycrawler.wizardstataggregator.application.service.TrackedLimitedStatSetService
import xyz.candycrawler.wizardstataggregator.configuration.coroutine.ApplicationCoroutineScope
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.TrackedLimitedStatSet
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/card-limited-stats")
class CardLimitedStatsController(
    private val collectionService: CardLimitedStatsCollectionService,
    private val trackedSetService: TrackedLimitedStatSetService,
    private val coroutineScope: ApplicationCoroutineScope,
) {

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
