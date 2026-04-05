package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import java.util.function.Function

@Component
class CardRecordToCardMapper : Function<CardRecord, Card> {

    override fun apply(record: CardRecord): Card = Card(
        id = record.id,
        scryfallId = record.scryfallId,
        oracleId = record.oracleId,
        name = record.name,
        lang = record.lang,
        layout = record.layout,
        manaCost = record.manaCost,
        cmc = record.cmc,
        typeLine = record.typeLine,
        oracleText = record.oracleText,
        colors = record.colors,
        colorIdentity = record.colorIdentity,
        keywords = record.keywords,
        power = record.power,
        toughness = record.toughness,
        loyalty = record.loyalty,
        setCode = record.setCode,
        setName = record.setName,
        collectorNumber = record.collectorNumber,
        rarity = record.rarity,
        releasedAt = record.releasedAt,
        imageUriSmall = record.imageUriSmall,
        imageUriNormal = record.imageUriNormal,
        imageUriLarge = record.imageUriLarge,
        imageUriPng = record.imageUriPng,
        imageUriArtCrop = record.imageUriArtCrop,
        imageUriBorderCrop = record.imageUriBorderCrop,
        priceUsd = record.priceUsd,
        priceUsdFoil = record.priceUsdFoil,
        priceEur = record.priceEur,
        priceEurFoil = record.priceEurFoil,
        flavorText = record.flavorText,
        artist = record.artist,
        mtgaId = record.mtgaId,
    )
}
