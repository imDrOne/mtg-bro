package xyz.candycrawler.collectionmanager.application.rest

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import xyz.candycrawler.collectionmanager.application.rest.dto.response.ImportResultResponse
import xyz.candycrawler.collectionmanager.application.rest.dto.response.NotFoundEntry
import xyz.candycrawler.collectionmanager.application.service.TcgPlayerImportService

@RestController
@RequestMapping("/api/v1/collection")
class CollectionImportController(
    private val tcgPlayerImportService: TcgPlayerImportService,
) {

    @PostMapping("/import/tcgplayer", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importFromTcgPlayer(@RequestParam("file") file: MultipartFile): ImportResultResponse {
        val content = file.inputStream.bufferedReader().readText()
        val result = tcgPlayerImportService.import(content)

        return ImportResultResponse(
            importedCount = result.importedCount,
            notFound = result.notFound.map { NotFoundEntry(it.set, it.collectorNumber) },
        )
    }
}
