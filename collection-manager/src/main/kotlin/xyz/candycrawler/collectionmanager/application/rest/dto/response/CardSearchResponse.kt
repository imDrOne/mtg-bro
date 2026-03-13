package xyz.candycrawler.collectionmanager.application.rest.dto.response

import java.time.LocalDate
import java.util.UUID

data class CardSearchResponse(
    val totalCards: Long,
    val hasMore: Boolean,
    val page: Int,
    val pageSize: Int,
    val data: List<CardResponse>,
)

data class CardResponse(
    val id: Long?,
    val scryfallId: UUID,
    val oracleId: UUID,
    val name: String,
    val lang: String,
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
    val imageUris: CardImageUris?,
    val prices: CardPrices?,
    val flavorText: String?,
    val artist: String?,
)

data class CardImageUris(
    val small: String?,
    val normal: String?,
    val large: String?,
    val png: String?,
    val artCrop: String?,
    val borderCrop: String?,
)

data class CardPrices(
    val usd: String?,
    val usdFoil: String?,
    val eur: String?,
    val eurFoil: String?,
)
