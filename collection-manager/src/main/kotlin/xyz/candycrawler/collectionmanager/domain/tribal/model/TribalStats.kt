package xyz.candycrawler.collectionmanager.domain.tribal.model

class TribalStats(
    val tribe: String,
    val totalCards: Int,
    val byCmc: Map<String, Int>,
    val creatures: Int,
    val tribalSpells: Int,
    val tribalSupport: Int,
    val colorSpread: Map<String, Int>,
    val hasLord: Boolean,
    val hasCommander: Boolean,
    val deckViability: String,
) {
    init {
        require(tribe.isNotBlank()) { "Tribe must not be blank" }
        require(totalCards >= 0) { "Total cards must be non-negative" }
    }
}
