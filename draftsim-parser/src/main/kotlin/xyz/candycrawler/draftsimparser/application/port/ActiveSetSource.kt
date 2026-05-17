package xyz.candycrawler.draftsimparser.application.port

import xyz.candycrawler.draftsimparser.domain.set.model.ActiveSet

interface ActiveSetSource {
    fun fetchActiveSets(): List<ActiveSet>
}
