package xyz.candycrawler.collectionmanager.domain.deck.model

enum class DeckFormat(val minMainboardCards: Int) {
    STANDARD(60),
    SEALED(40),
    DRAFT(40),
}
