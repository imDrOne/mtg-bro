package xyz.candycrawler.collectionmanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableCaching
@ConfigurationPropertiesScan
class CollectionManagerApplication

fun main(args: Array<String>) {
    runApplication<CollectionManagerApplication>(*args)
}
