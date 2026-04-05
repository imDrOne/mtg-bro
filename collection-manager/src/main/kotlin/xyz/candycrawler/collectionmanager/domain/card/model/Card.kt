package xyz.candycrawler.collectionmanager.domain.card.model

import xyz.candycrawler.collectionmanager.domain.card.exception.InvalidCardException
import java.time.LocalDate
import java.util.UUID

data class Card(
    val id: Long? = null,
    val scryfallId: UUID,
    val oracleId: UUID,
    val name: String,
    val lang: String = "en",
    val layout: String,
    val manaCost: String?,
    val cmc: Double,
    val typeLine: String,
    val oracleText: String?,
    val colors: List<String>,
    val colorIdentity: List<String>,
    val keywords: List<String>,
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    val setCode: String,
    val setName: String,
    val collectorNumber: String,
    val rarity: String,
    val releasedAt: LocalDate?,
    val imageUriSmall: String?,
    val imageUriNormal: String?,
    val imageUriLarge: String?,
    val imageUriPng: String?,
    val imageUriArtCrop: String?,
    val imageUriBorderCrop: String?,
    val priceUsd: String?,
    val priceUsdFoil: String?,
    val priceEur: String?,
    val priceEurFoil: String?,
    val flavorText: String?,
    val artist: String?,
    val mtgaId: String? = null,
) {
    init {
        fun invalid(reason: String): Nothing = throw InvalidCardException(reason)

        if (name.isBlank()) invalid("name must not be blank")
        if (layout.isBlank()) invalid("layout must not be blank")
        if (cmc < 0) invalid("cmc must be non-negative")
        if (typeLine.isBlank()) invalid("typeLine must not be blank")
        if (setCode.isBlank()) invalid("setCode must not be blank")
        if (setName.isBlank()) invalid("setName must not be blank")
        if (collectorNumber.isBlank()) invalid("collectorNumber must not be blank")
        if (rarity.isBlank()) invalid("rarity must not be blank")
        if (lang.isBlank()) invalid("lang must not be blank")
    }

    companion object {
        val ALLOWED_RARITIES = setOf("common", "uncommon", "rare", "mythic", "special", "bonus")
    }
}
