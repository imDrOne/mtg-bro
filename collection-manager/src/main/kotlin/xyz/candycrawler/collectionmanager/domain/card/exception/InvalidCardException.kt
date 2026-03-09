package xyz.candycrawler.collectionmanager.domain.card.exception

class InvalidCardException(reason: String) : RuntimeException("Invalid card: $reason")
