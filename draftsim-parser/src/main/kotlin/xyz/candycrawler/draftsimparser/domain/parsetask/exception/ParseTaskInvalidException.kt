package xyz.candycrawler.draftsimparser.domain.parsetask.exception

class ParseTaskInvalidException(reason: String) : RuntimeException("Invalid parse task: $reason")
