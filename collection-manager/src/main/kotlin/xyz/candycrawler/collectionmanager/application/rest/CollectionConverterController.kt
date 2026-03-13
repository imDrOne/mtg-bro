package xyz.candycrawler.collectionmanager.application.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import xyz.candycrawler.collectionmanager.application.service.TcgPlayerToMoxfieldConverterService

@Tag(name = "Collection Converter", description = "Utility endpoints for converting collection files between formats")
@RestController
@RequestMapping("/api/v1/collection/convert")
class CollectionConverterController(
    private val converterService: TcgPlayerToMoxfieldConverterService,
) {

    @Operation(
        summary = "Convert TCG Player export to Moxfield CSV",
        description = """
            Accepts a TCG Player export .txt file and returns a Moxfield-compatible "Haves" CSV.

            Input line format: `<quantity> <card name> [<set code>] <collector number>`
            
            Duplicate entries (same set + collector number) are merged by summing quantities.
            Foil column is left empty — TCG Player exports do not carry foil information.
            The resulting file can be imported directly via Moxfield → Collection → Import Haves.
        """,
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(implementation = TcgPlayerConvertRequest::class),
            )],
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Converted CSV file",
                content = [Content(mediaType = "text/csv")],
            ),
        ],
    )
    @PostMapping("/tcgplayer-to-moxfield", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun convertTcgPlayerToMoxfield(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<ByteArray> {
        val content = file.inputStream.bufferedReader().readText()
        val csv = converterService.convert(content)
        val filename = file.originalFilename
            ?.removeSuffix(".txt")
            ?.plus("_moxfield.csv")
            ?: "moxfield_haves.csv"

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType("text/csv;charset=UTF-8")
            contentDisposition = ContentDisposition.attachment().filename(filename, Charsets.UTF_8).build()
        }

        return ResponseEntity.ok().headers(headers).body(csv.toByteArray(Charsets.UTF_8))
    }
}

class TcgPlayerConvertRequest {
    @Schema(
        description = "TCG Player export .txt file",
        type = "string",
        format = "binary",
        required = true,
    )
    lateinit var file: MultipartFile
}
