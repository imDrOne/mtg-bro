package xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallCardResponse
import java.time.LocalDate
import java.util.function.Function

@Component
class ScryfallCardResponseToCardMapper : Function<ScryfallCardResponse, Card> {

    override fun apply(response: ScryfallCardResponse): Card = Card(
        scryfallId = response.id,
        oracleId = response.oracleId,
        name = response.name,
        lang = response.lang,
        layout = response.layout,
        manaCost = response.manaCost,
        cmc = response.cmc,
        typeLine = response.typeLine,
        oracleText = response.oracleText,
        colors = response.colors ?: emptyList(),
        colorIdentity = response.colorIdentity,
        keywords = response.keywords,
        power = response.power,
        toughness = response.toughness,
        loyalty = response.loyalty,
        setCode = response.setCode,
        setName = response.setName,
        collectorNumber = response.collectorNumber,
        rarity = response.rarity,
        releasedAt = response.releasedAt?.let { LocalDate.parse(it) },
        imageUriSmall = response.imageUris?.small,
        imageUriNormal = response.imageUris?.normal,
        imageUriLarge = response.imageUris?.large,
        imageUriPng = response.imageUris?.png,
        imageUriArtCrop = response.imageUris?.artCrop,
        imageUriBorderCrop = response.imageUris?.borderCrop,
        priceUsd = response.prices?.usd,
        priceUsdFoil = response.prices?.usdFoil,
        priceEur = response.prices?.eur,
        priceEurFoil = response.prices?.eurFoil,
        flavorText = response.flavorText,
        artist = response.artist,
    )
}
