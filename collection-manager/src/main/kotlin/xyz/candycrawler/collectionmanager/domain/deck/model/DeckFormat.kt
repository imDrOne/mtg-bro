package xyz.candycrawler.collectionmanager.domain.deck.model

private const val STANDARD_MIN_MAINBOARD_CARDS = 60
private const val LIMITED_MIN_MAINBOARD_CARDS = 40

enum class DeckFormat(val minMainboardCards: Int) {
    STANDARD(STANDARD_MIN_MAINBOARD_CARDS),
    SEALED(LIMITED_MIN_MAINBOARD_CARDS),
    DRAFT(LIMITED_MIN_MAINBOARD_CARDS),
}
