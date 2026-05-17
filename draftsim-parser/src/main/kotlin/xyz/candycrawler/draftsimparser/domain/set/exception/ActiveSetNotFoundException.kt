package xyz.candycrawler.draftsimparser.domain.set.exception

class ActiveSetNotFoundException(code: String) : RuntimeException("ActiveSet not found: $code")
