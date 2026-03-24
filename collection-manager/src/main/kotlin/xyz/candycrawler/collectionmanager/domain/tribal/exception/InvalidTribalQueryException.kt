package xyz.candycrawler.collectionmanager.domain.tribal.exception

class InvalidTribalQueryException(tribe: String) : RuntimeException("Unknown creature type: '$tribe'")
