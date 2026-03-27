package xyz.candycrawler.draftsimparser.domain.parsetask.exception

import java.util.UUID

class ParseTaskNotFoundException(id: UUID) : RuntimeException("ParseTask with id=$id not found")
