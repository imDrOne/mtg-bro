package xyz.candycrawler.draftsimparser

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
class DraftsimParserApplication

fun main(args: Array<String>) {
    runApplication<DraftsimParserApplication>(*args)
}
