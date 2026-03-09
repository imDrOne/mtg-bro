package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import java.util.function.Function

@Component
class CardToCardRecordMapper : Function<Card, CardRecord> {

    override fun apply(card: Card): CardRecord = CardRecord(
        id = card.id,
        scryfallId = card.scryfallId,
        oracleId = card.oracleId,
        name = card.name,
        lang = card.lang,
        layout = card.layout,
        manaCost = card.manaCost,
        cmc = card.cmc,
        typeLine = card.typeLine,
        oracleText = card.oracleText,
        colors = card.colors,
        colorIdentity = card.colorIdentity,
        keywords = card.keywords,
        power = card.power,
        toughness = card.toughness,
        loyalty = card.loyalty,
        setCode = card.setCode,
        setName = card.setName,
        collectorNumber = card.collectorNumber,
        rarity = card.rarity,
        releasedAt = card.releasedAt,
        imageUriSmall = card.imageUriSmall,
        imageUriNormal = card.imageUriNormal,
        imageUriLarge = card.imageUriLarge,
        imageUriPng = card.imageUriPng,
        imageUriArtCrop = card.imageUriArtCrop,
        imageUriBorderCrop = card.imageUriBorderCrop,
        priceUsd = card.priceUsd,
        priceUsdFoil = card.priceUsdFoil,
        priceEur = card.priceEur,
        priceEurFoil = card.priceEurFoil,
        flavorText = card.flavorText,
        artist = card.artist,
    )
}
