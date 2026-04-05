package xyz.candycrawler.collectionmanager.domain.deck.exception

class DeckNotFoundException(id: Long) : RuntimeException("Deck not found: $id")
