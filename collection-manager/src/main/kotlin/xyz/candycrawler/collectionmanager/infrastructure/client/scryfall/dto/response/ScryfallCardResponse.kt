package xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ScryfallCardResponse(
    val id: UUID,
    @JsonProperty("oracle_id")
    val oracleId: UUID,
    val name: String,
    val lang: String,
    val layout: String,
    @JsonProperty("mana_cost")
    val manaCost: String?,
    val cmc: Double,
    @JsonProperty("type_line")
    val typeLine: String,
    @JsonProperty("oracle_text")
    val oracleText: String?,
    val colors: List<String>?,
    @JsonProperty("color_identity")
    val colorIdentity: List<String>,
    val keywords: List<String>,
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    @JsonProperty("set")
    val setCode: String,
    @JsonProperty("set_name")
    val setName: String,
    @JsonProperty("collector_number")
    val collectorNumber: String,
    val rarity: String,
    @JsonProperty("released_at")
    val releasedAt: String?,
    @JsonProperty("image_uris")
    val imageUris: ImageUris?,
    val prices: Prices?,
    @JsonProperty("flavor_text")
    val flavorText: String?,
    val artist: String?,
)

data class ImageUris(
    val small: String?,
    val normal: String?,
    val large: String?,
    val png: String?,
    @JsonProperty("art_crop")
    val artCrop: String?,
    @JsonProperty("border_crop")
    val borderCrop: String?,
)

data class Prices(
    val usd: String?,
    @JsonProperty("usd_foil")
    val usdFoil: String?,
    val eur: String?,
    @JsonProperty("eur_foil")
    val eurFoil: String?,
)
