package xyz.candycrawler.collectionmanager.application.rest.dto.response

data class ImportResultResponse(
    val importedCount: Int,
    val notFound: List<NotFoundEntry>,
)

data class NotFoundEntry(
    val setCode: String,
    val collectorNumber: String,
)
