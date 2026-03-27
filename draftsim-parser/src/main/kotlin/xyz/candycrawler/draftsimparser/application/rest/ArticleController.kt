package xyz.candycrawler.draftsimparser.application.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticlePageResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.ArticleResponse
import xyz.candycrawler.draftsimparser.application.rest.dto.response.toResponse
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository

@RestController
@RequestMapping("/api/v1/articles")
class ArticleController(private val queryArticleRepository: QueryArticleRepository) {

    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
    ): ArticlePageResponse =
        queryArticleRepository.search(q, page, pageSize).toResponse()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ArticleResponse =
        queryArticleRepository.findById(id).toResponse()
}
