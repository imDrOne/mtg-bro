package xyz.candycrawler.draftsimparser.application.service

import org.springframework.beans.factory.DisposableBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.context.ApplicationEventPublisher
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTaskStatus
import xyz.candycrawler.draftsimparser.domain.parsetask.repository.ParseTaskRepository
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.DraftsimWpApiClient
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.dto.WpPostResponse
import java.time.LocalDateTime
import java.util.UUID

@Service
class DraftsimParseService(
    private val parseTaskRepository: ParseTaskRepository,
    private val articleRepository: ArticleRepository,
    private val wpApiClient: DraftsimWpApiClient,
    private val eventPublisher: ApplicationEventPublisher,
    private val parseAlertService: ParseAlertService,
    private val articleKeywordExtractor: ArticleKeywordExtractor,
    @Value("\${infrastructure.analysis.auto-publish}") private val autoPublish: Boolean,
) : DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startParsing(keyword: String): UUID {
        val now = LocalDateTime.now()
        val task = parseTaskRepository.save(
            ParseTask(
                id = null,
                keyword = keyword,
                status = ParseTaskStatus.PENDING,
                totalArticles = null,
                processedArticles = 0,
                errorMessage = null,
                createdAt = now,
                updatedAt = now,
            )
        )
        val taskId = task.id!!

        scope.launch {
            try {
                parseAlertService.parsingStarted(taskId, keyword)
                runParseTask(taskId, keyword)
            } catch (e: Exception) {
                log.error("Parse task {} failed", taskId, e)
                runCatching {
                    parseTaskRepository.update(taskId) {
                        it.copy(
                            status = ParseTaskStatus.FAILED,
                            errorMessage = e.message?.take(1000),
                            updatedAt = LocalDateTime.now(),
                        )
                    }
                }
                parseAlertService.parseTaskFailed(taskId, keyword, e)
            }
        }

        return taskId
    }

    private suspend fun runParseTask(taskId: UUID, keyword: String) {
        parseTaskRepository.update(taskId) {
            it.copy(status = ParseTaskStatus.SEARCHING, updatedAt = LocalDateTime.now())
        }

        val firstResult = wpApiClient.searchPosts(keyword, page = 1)
        val allPosts = firstResult.posts.toMutableList()

        for (page in 2..firstResult.totalPages) {
            allPosts.addAll(wpApiClient.searchPosts(keyword, page).posts)
        }

        log.info("Task {}: found {} posts for keyword '{}'", taskId, allPosts.size, keyword)

        parseTaskRepository.update(taskId) {
            it.copy(
                status = ParseTaskStatus.FETCHING_ARTICLES,
                totalArticles = allPosts.size,
                updatedAt = LocalDateTime.now(),
            )
        }

        val semaphore = Semaphore(MAX_PARALLEL_ARTICLES)
        val savedArticles = mutableListOf<Article>()
        coroutineScope {
            allPosts.chunked(CHUNK_SIZE).forEach { chunk ->
                val chunkResults = chunk.map { post ->
                    async {
                        semaphore.withPermit {
                            runCatching {
                                processPost(taskId, post)
                            }.onFailure {
                                log.error("Task {}: article post id={} failed", taskId, post.id, it)
                                parseAlertService.articleParsingFailed(taskId, keyword, post.id, post.link, it)
                            }.getOrNull()
                        }
                    }
                }.awaitAll()
                savedArticles.addAll(chunkResults.filterNotNull())
                parseTaskRepository.incrementProcessedArticles(taskId, chunk.size)
            }
        }

        log.info("Task {}: fetched {} articles, queuing analysis", taskId, savedArticles.size)

        if (autoPublish) {
            savedArticles.forEach { article ->
                eventPublisher.publishEvent(ArticleAnalysisMessage(article.id!!))
            }
        }

        parseTaskRepository.update(taskId) {
            it.copy(status = ParseTaskStatus.COMPLETED, updatedAt = LocalDateTime.now())
        }
        log.info("Task {}: completed, processed {} articles", taskId, savedArticles.size)
    }

    private fun processPost(taskId: UUID, post: WpPostResponse): Article {
        val doc = Jsoup.parse(post.content.rendered)
        val textContent = doc.select("p")
            .map { it.text() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        val article = Article(
            id = null,
            externalId = post.id,
            title = post.title.rendered,
            slug = post.slug,
            url = post.link,
            htmlContent = post.content.rendered,
            textContent = textContent,
            analyzedText = null,
            keywords = emptyList(),
            favorite = false,
            errorMsg = null,
            analyzStartedAt = null,
            analyzEndedAt = null,
            publishedAt = post.date,
            fetchedAt = LocalDateTime.now(),
        )
        val saved = articleRepository.save(article)
        val savedArticleId = saved.id!!
        articleRepository.saveTaskArticleLink(taskId, savedArticleId)
        val keywords = articleKeywordExtractor.extract(saved.textContent)
        return articleRepository.update(savedArticleId) { it.copy(keywords = keywords) }
    }

    override fun destroy() {
        scope.cancel()
    }

    companion object {
        private const val MAX_PARALLEL_ARTICLES = 5
        private const val CHUNK_SIZE = 5
    }
}
