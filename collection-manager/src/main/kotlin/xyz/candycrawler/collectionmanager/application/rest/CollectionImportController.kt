package xyz.candycrawler.collectionmanager.application.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import xyz.candycrawler.collectionmanager.application.parser.CollectionFileParser
import xyz.candycrawler.collectionmanager.application.parser.MoxfieldFileParser
import xyz.candycrawler.collectionmanager.application.parser.TcgPlayerFileParser
import xyz.candycrawler.collectionmanager.application.rest.dto.response.ImportResultResponse
import xyz.candycrawler.collectionmanager.application.rest.dto.response.NotFoundEntry
import xyz.candycrawler.collectionmanager.application.service.CollectionImportService

@Tag(name = "Collection Import", description = "Endpoints for importing card collections from external sources")
@RestController
@RequestMapping("/api/v1/collection")
class CollectionImportController(
    private val importService: CollectionImportService,
    private val tcgPlayerParser: TcgPlayerFileParser,
    private val moxfieldParser: MoxfieldFileParser,
) {

    @Operation(
        summary = "Import collection from TCG Player export",
        description = """
            Accepts a TCG Player export .txt file. Each line must follow the format:
            `<quantity> <card name> [<set code>] <collector number>`

            Duplicate entries (same set + collector number) are merged by summing quantities.
            Card metadata is enriched via the Scryfall API before persisting.
        """,
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(implementation = TcgPlayerImportRequest::class),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Import completed successfully",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ImportResultResponse::class),
                )],
            ),
        ],
    )
    @PostMapping("/import/tcgplayer", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun importFromTcgPlayer(
        @RequestPart("file") file: MultipartFile,
    ): ImportResultResponse = doImport(file, tcgPlayerParser)

    @Operation(
        summary = "Import collection from Moxfield Haves CSV export",
        description = """
            Accepts a Moxfield "Haves" CSV export file. The file must contain headers:
            `Count`, `Name`, `Edition`, `Collector Number`, `Foil`

            The `Foil` column should contain `foil` for foil copies, or be empty for non-foil.
            Duplicate entries (same set + collector number + foil) are merged by summing quantities.
            Card metadata is enriched via the Scryfall API before persisting.
        """,
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(implementation = MoxfieldImportRequest::class),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Import completed successfully",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ImportResultResponse::class),
                )],
            ),
        ],
    )
    @PostMapping("/import/moxfield", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun importFromMoxfield(
        @RequestPart("file") file: MultipartFile,
    ): ImportResultResponse = doImport(file, moxfieldParser)

    private suspend fun doImport(
        file: MultipartFile,
        parser: CollectionFileParser,
    ): ImportResultResponse {
        val content = file.inputStream.bufferedReader().readText()
        val result = importService.import(parser, content)
        return ImportResultResponse(
            importedCount = result.importedCount,
            notFound = result.notFound.map { NotFoundEntry(it.set, it.collectorNumber) },
        )
    }
}

class MoxfieldImportRequest {
    @Schema(
        description = "Moxfield Haves CSV export file",
        type = "string",
        format = "binary",
        required = true,
    )
    lateinit var file: MultipartFile
}

class TcgPlayerImportRequest {
    @Schema(
        description = "TCG Player export .txt file",
        type = "string",
        format = "binary",
        required = true,
    )
    lateinit var file: MultipartFile
}
