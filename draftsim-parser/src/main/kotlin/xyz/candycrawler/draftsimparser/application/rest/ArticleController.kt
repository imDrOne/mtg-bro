package xyz.candycrawler.draftsimparser.application.rest

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
import xyz.candycrawler.draftsimparser.application.rest.dto.request.AnalyzeArticlesRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.CollectArticleKeywordsRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.GetArticlesByIdsRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.ReindexArticleVectorsRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.SemanticArticleSearchRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.request.UpdateArticleFavoriteRequest
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleAnalysisResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticlePageResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleSummaryResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.SemanticArticleSearchResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toAnalysisResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toSemanticSearchResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toSummaryResponse
import xyz.candycrawler.draftsimparser.application.service.ArticleSemanticSearchService
import xyz.candycrawler.draftsimparser.application.service.ArticleService
import xyz.candycrawler.draftsimparser.domain.article.model.ArticleSearchFilter
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.model.ArticleSortField
import xyz.candycrawler.draftsimparser.domain.article.model.SortDirection
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/articles")
class ArticleController(
    private val articleService: ArticleService,
    private val queryArticleRepository: QueryArticleRepository,
    private val articleSemanticSearchService: ArticleSemanticSearchService,
) {

    @PreAuthorize("hasAuthority('PERM_api:articles:read')")
    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestParam(required = false) favorite: Boolean?,
        @RequestParam(required = false) setCode: String?,
        @RequestParam(required = false) publishedFrom: LocalDate?,
        @RequestParam(required = false) publishedTo: LocalDate?,
        @RequestParam(defaultValue = "PUBLISHED_AT") sortBy: ArticleSortField,
        @RequestParam(defaultValue = "DESC") sortDirection: SortDirection,
    ): ArticlePageResponse = queryArticleRepository.search(
        filter = ArticleSearchFilter(
            query = q,
            favoriteOnly = favorite,
            setCode = setCode,
            publishedFrom = publishedFrom,
            publishedTo = publishedTo,
            sortBy = sortBy,
            sortDirection = sortDirection,
        ),
        page = page,
        pageSize = pageSize,
    ).toResponse()

    @PreAuthorize("hasAuthority('PERM_api:articles:read')")
    @PostMapping("/search/semantic")
    fun semanticSearch(@RequestBody request: SemanticArticleSearchRequest): SemanticArticleSearchResponse =
        articleSemanticSearchService.search(
            query = request.query,
            topK = request.topK,
            similarityThreshold = request.similarityThreshold,
            favoriteOnly = request.favorite,
        ).toSemanticSearchResponse()

    @PreAuthorize("hasAuthority('PERM_api:articles:read')")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ArticleResponse = articleService.findById(id).toResponse()

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PatchMapping("/{id}/favorite")
    fun updateFavorite(@PathVariable id: Long, @RequestBody request: UpdateArticleFavoriteRequest): ArticleResponse =
        articleService.updateFavorite(id, request.favorite).toResponse()

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun analyze(@RequestBody request: AnalyzeArticlesRequest): List<ArticleSummaryResponse> =
        articleService.analyze(request.ids).map { it.toSummaryResponse() }

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PostMapping("/keywords")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun collectKeywords(@RequestBody request: CollectArticleKeywordsRequest): List<ArticleSummaryResponse> =
        articleService.collectKeywords(request.ids).map { it.toSummaryResponse() }

    @PreAuthorize("hasAuthority('PERM_api:articles:parse')")
    @PostMapping("/vector-index")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun reindexVectors(@RequestBody request: ReindexArticleVectorsRequest): List<ArticleSummaryResponse> =
        articleService.reindexVectors(request.ids).map { it.toSummaryResponse() }

    @PreAuthorize("hasAuthority('PERM_api:articles:read')")
    @PostMapping("/by-ids")
    fun getByIds(@RequestBody request: GetArticlesByIdsRequest): List<ArticleAnalysisResponse> =
        articleService.findByIds(request.ids).map { it.toAnalysisResponse() }
}
