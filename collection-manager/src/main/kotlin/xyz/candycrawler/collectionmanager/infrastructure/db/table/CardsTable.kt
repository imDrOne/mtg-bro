package xyz.candycrawler.collectionmanager.infrastructure.db.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object CardsTable : LongIdTable("cards") {
    val scryfallId = uuid("scryfall_id")
    val oracleId = uuid("oracle_id")
    val name = varchar("name", 255)
    val lang = varchar("lang", 10).default("en")
    val layout = varchar("layout", 50)
    val manaCost = varchar("mana_cost", 100).nullable()
    val cmc = double("cmc")
    val typeLine = varchar("type_line", 255)
    val oracleText = text("oracle_text").nullable()
    val colors = array<String>("colors")
    val colorIdentity = array<String>("color_identity")
    val keywords = array<String>("keywords")
    val power = varchar("power", 10).nullable()
    val toughness = varchar("toughness", 10).nullable()
    val loyalty = varchar("loyalty", 10).nullable()
    val setCode = varchar("set_code", 10)
    val setName = varchar("set_name", 255)
    val collectorNumber = varchar("collector_number", 20)
    val rarity = varchar("rarity", 20)
    val releasedAt = date("released_at").nullable()
    val imageUriSmall = text("image_uri_small").nullable()
    val imageUriNormal = text("image_uri_normal").nullable()
    val imageUriLarge = text("image_uri_large").nullable()
    val imageUriPng = text("image_uri_png").nullable()
    val imageUriArtCrop = text("image_uri_art_crop").nullable()
    val imageUriBorderCrop = text("image_uri_border_crop").nullable()
    val priceUsd = varchar("price_usd", 20).nullable()
    val priceUsdFoil = varchar("price_usd_foil", 20).nullable()
    val priceEur = varchar("price_eur", 20).nullable()
    val priceEurFoil = varchar("price_eur_foil", 20).nullable()
    val flavorText = text("flavor_text").nullable()
    val artist = varchar("artist", 255).nullable()
    val mtgaId = text("mtga_id").nullable()

    init {
        uniqueIndex("uq_cards_scryfall_id", scryfallId)
        uniqueIndex("uq_cards_set_collector_lang", setCode, collectorNumber, lang)
    }
}
