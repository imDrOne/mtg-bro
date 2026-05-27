package xyz.candycrawler.draftsimparser

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
class DraftsimParserApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<DraftsimParserApplication>(*args)
}
