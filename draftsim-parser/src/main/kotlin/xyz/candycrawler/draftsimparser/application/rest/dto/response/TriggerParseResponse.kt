package xyz.candycrawler.draftsimparser.application.rest.dto.response

import java.util.UUID

data class TriggerParseResponse(val queuedSets: Int, val tasks: Map<String, UUID?>)
