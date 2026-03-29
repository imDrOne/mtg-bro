package xyz.candycrawler.draftsimparser.application.rest

import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.application.rest.dto.request.AnalyzeArticlesRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticlePageResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleSummaryResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toSummaryResponse
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository

@RestController
@RequestMapping("/api/v1/articles")
class ArticleController(
    private val queryArticleRepository: QueryArticleRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestParam(required = false) favorite: Boolean?,
    ): ArticlePageResponse =
        queryArticleRepository.search(q, page, pageSize, favorite).toResponse()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ArticleResponse =
        queryArticleRepository.findById(id).toResponse()

    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun analyze(@RequestBody request: AnalyzeArticlesRequest): List<ArticleSummaryResponse> {
        return request.ids.map { id ->
            val article = queryArticleRepository.findById(id)
            eventPublisher.publishEvent(ArticleAnalysisMessage(id))
            article.toSummaryResponse()
        }
    }
}
