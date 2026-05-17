package xyz.candycrawler.draftsimparser.domain.set.model

import java.time.LocalDate

data class ActiveSet(val code: String, val name: String, val releasedAt: LocalDate, val setType: String)
