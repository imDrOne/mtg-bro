package xyz.candycrawler.draftsimparser.application.rest

import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.application.rest.dto.request.AnalyzeArticlesRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.CollectArticleKeywordsRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.GetArticlesByIdsRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.UpdateArticleFavoriteRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleAnalysisResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticlePageResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleSummaryResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toAnalysisResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toSummaryResponse
import xyz.candycrawler.draftsimparser.application.service.ArticleKeywordService
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository

@RestController
@RequestMapping("/api/v1/articles")
class ArticleController(
    private val queryArticleRepository: QueryArticleRepository,
    private val articleRepository: ArticleRepository,
    private val articleKeywordService: ArticleKeywordService,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @PreAuthorize("hasAuthority('PERM_api:articles:read')")
    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestParam(required = false) favorite: Boolean?,
    ): ArticlePageResponse =
        queryArticleRepository.search(q, page, pageSize, favorite).toResponse()

    @PreAuthorize("hasAuthority('PERM_api:articles:read')")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ArticleResponse =
        queryArticleRepository.findById(id).toResponse()

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PatchMapping("/{id}/favorite")
    fun updateFavorite(
        @PathVariable id: Long,
        @RequestBody request: UpdateArticleFavoriteRequest,
    ): ArticleResponse =
        articleRepository.update(id) { it.copy(favorite = request.favorite) }.toResponse()

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun analyze(@RequestBody request: AnalyzeArticlesRequest): List<ArticleSummaryResponse> {
        return request.ids.map { id ->
            val article = queryArticleRepository.findById(id)
            eventPublisher.publishEvent(ArticleAnalysisMessage(id))
            article.toSummaryResponse()
        }
    }

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PostMapping("/keywords")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun collectKeywords(@RequestBody request: CollectArticleKeywordsRequest): List<ArticleSummaryResponse> {
        val articles = request.ids.map { id -> queryArticleRepository.findById(id) }
        articleKeywordService.collectAsync(request.ids)
        return articles.map { it.toSummaryResponse() }
    }

    @PreAuthorize("hasAuthority('PERM_api:articles:read')")
    @PostMapping("/by-ids")
    fun getByIds(@RequestBody request: GetArticlesByIdsRequest): List<ArticleAnalysisResponse> =
        request.ids.map { id -> queryArticleRepository.findById(id).toAnalysisResponse() }
}
