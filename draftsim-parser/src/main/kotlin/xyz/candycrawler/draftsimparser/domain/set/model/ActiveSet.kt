package xyz.candycrawler.draftsimparser.domain.set.model

import java.time.LocalDate

data class ActiveSet(val code: String, val name: String, val releasedAt: LocalDate, val setType: String) {
    init {
        require(code.isNotBlank()) { "ActiveSet code must not be blank" }
        require(name.isNotBlank()) { "ActiveSet name must not be blank" }
        require(setType.isNotBlank()) { "ActiveSet setType must not be blank" }
    }
}
