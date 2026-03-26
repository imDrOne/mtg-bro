package xyz.candycrawler.collectionmanager.application.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CollectionOverviewResponse
import xyz.candycrawler.collectionmanager.application.service.CollectionOverviewService

@RestController
@RequestMapping("/api/v1/collection")
class CollectionOverviewController(private val service: CollectionOverviewService) {

    @GetMapping("/overview")
    fun getOverview(): CollectionOverviewResponse = service.getOverview()
}
