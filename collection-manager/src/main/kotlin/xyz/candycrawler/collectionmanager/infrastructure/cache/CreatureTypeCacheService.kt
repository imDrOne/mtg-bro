package xyz.candycrawler.collectionmanager.infrastructure.cache

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.ScryfallApiClient

@Service
class CreatureTypeCacheService(private val scryfallApiClient: ScryfallApiClient) {

    @Cacheable("creature-types")
    fun getCreatureTypes(): Set<String> = scryfallApiClient.getCreatureTypes().data.toSet()
}
