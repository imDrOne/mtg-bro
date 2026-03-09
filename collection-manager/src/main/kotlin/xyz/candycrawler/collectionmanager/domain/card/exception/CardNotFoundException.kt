package xyz.candycrawler.collectionmanager.domain.card.exception

class CardNotFoundException : RuntimeException {
    constructor(id: Long) : super("Card with id=$id not found")
    constructor(message: String) : super("Card $message")
}
