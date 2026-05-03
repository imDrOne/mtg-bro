package xyz.candycrawler.draftsimparser.application.rest

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.draftsimparser.application.rest.dto.request.StartParseRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ParseTaskResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toResponse
import xyz.candycrawler.draftsimparser.application.service.DraftsimParseService
import xyz.candycrawler.draftsimparser.domain.parsetask.repository.ParseTaskRepository
import java.util.UUID

@RestController
@RequestMapping("/api/v1/parse")
class ParseController(
    private val parseService: DraftsimParseService,
    private val parseTaskRepository: ParseTaskRepository,
) {

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun startParse(@RequestBody request: StartParseRequest): Map<String, UUID> {
        val taskId = parseService.startParsing(request.keyword)
        return mapOf("taskId" to taskId)
    }

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @GetMapping("/{taskId}")
    fun getTask(@PathVariable taskId: UUID): ParseTaskResponse = parseTaskRepository.findById(taskId).toResponse()
}
