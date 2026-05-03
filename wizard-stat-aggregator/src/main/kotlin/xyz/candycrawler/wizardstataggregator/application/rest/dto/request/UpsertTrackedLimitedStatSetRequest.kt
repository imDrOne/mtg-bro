package xyz.candycrawler.wizardstataggregator.application.rest.dto.request

import java.time.LocalDate

data class UpsertTrackedLimitedStatSetRequest(val watchUntil: LocalDate)
