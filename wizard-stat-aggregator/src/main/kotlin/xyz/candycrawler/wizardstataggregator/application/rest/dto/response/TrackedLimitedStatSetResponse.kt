package xyz.candycrawler.wizardstataggregator.application.rest.dto.response

import java.time.LocalDate

data class TrackedLimitedStatSetResponse(val setCode: String, val watchUntil: LocalDate, val active: Boolean)
