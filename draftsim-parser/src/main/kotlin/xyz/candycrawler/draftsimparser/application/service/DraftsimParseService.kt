package xyz.candycrawler.draftsimparser.application.service

import kotlinx.coroutines.CoroutineDispatcher
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSearchResult
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSource
import xyz.candycrawler.draftsimparser.application.port.DraftsimSourceArticle
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTask
import xyz.candycrawler.draftsimparser.domain.parsetask.model.ParseTaskStatus
import xyz.candycrawler.draftsimparser.domain.parsetask.repository.ParseTaskRepository
import xyz.candycrawler.draftsimparser.domain.set.model.ActiveSet
import java.time.LocalDateTime
import java.util.UUID

private typealias ArticleFetchStrategy = suspend (page: Int) -> DraftsimArticleSearchResult

@Suppress("LongParameterList")
@Service
class DraftsimParseService(
    private val parseTaskRepository: ParseTaskRepository,
    private val articleRepository: ArticleRepository,
    private val articleSource: DraftsimArticleSource,
    private val articleAnalysisPublisher: ArticleAnalysisPublisher,
    private val parseAlertService: ParseAlertService,
    private val articleKeywordExtractor: ArticleKeywordExtractor,
    @Value("\${infrastructure.analysis.auto-publish}") private val autoPublish: Boolean,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun startParsing(keyword: String): UUID =
        startParseTask(keyword) { page -> articleSource.searchArticles(keyword, page) }

    fun startScheduledParse(set: ActiveSet): UUID? {
        val tagIds = articleSource.findTagIdsBySetName(set.name)
        if (tagIds.isEmpty()) {
            log.info("No tags found for set '{}' ({}), skipping", set.name, set.code)
            return null
        }
        val keyword = "set:${set.code}"
        return startParseTask(keyword) { page -> articleSource.searchArticlesByTagIds(tagIds, page) }
    }

    private fun startParseTask(keyword: String, fetchPage: ArticleFetchStrategy): UUID {
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
            ),
        )
        val taskId = requireNotNull(task.id)

        scope.launch {
            runCatching {
                parseAlertService.parsingStarted(taskId, keyword)
                runParseTask(taskId, keyword, fetchPage)
            }.onFailure { e ->
                log.error("Parse task {} failed", taskId, e)
                runCatching {
                    parseTaskRepository.update(taskId) {
                        it.copy(
                            status = ParseTaskStatus.FAILED,
                            errorMessage = e.message?.take(ERROR_MESSAGE_MAX_LENGTH),
                            updatedAt = LocalDateTime.now(),
                        )
                    }
                }
                parseAlertService.parseTaskFailed(taskId, keyword, e)
            }
        }

        return taskId
    }

    private suspend fun runParseTask(taskId: UUID, keyword: String, fetchPage: ArticleFetchStrategy) {
        parseTaskRepository.update(taskId) {
            it.copy(status = ParseTaskStatus.SEARCHING, updatedAt = LocalDateTime.now())
        }

        val firstResult = fetchPage(1)
        val allArticles = firstResult.articles.toMutableList()

        for (page in 2..firstResult.totalPages) {
            allArticles.addAll(fetchPage(page).articles)
        }

        log.info("Task {}: found {} articles for keyword '{}'", taskId, allArticles.size, keyword)

        parseTaskRepository.update(taskId) {
            it.copy(
                status = ParseTaskStatus.FETCHING_ARTICLES,
                totalArticles = allArticles.size,
                updatedAt = LocalDateTime.now(),
            )
        }

        val semaphore = Semaphore(MAX_PARALLEL_ARTICLES)
        val savedArticles = mutableListOf<Article>()
        coroutineScope {
            allArticles.chunked(CHUNK_SIZE).forEach { chunk ->
                val chunkResults = chunk.map { sourceArticle ->
                    async {
                        semaphore.withPermit {
                            runCatching {
                                processArticle(taskId, sourceArticle)
                            }.onFailure {
                                log.error(
                                    "Task {}: article external id={} failed",
                                    taskId,
                                    sourceArticle.externalId,
                                    it,
                                )
                                parseAlertService.articleParsingFailed(
                                    taskId,
                                    keyword,
                                    sourceArticle.externalId,
                                    sourceArticle.url,
                                    it,
                                )
                            }.getOrNull()
                        }
                    }
                }.awaitAll()
                savedArticles.addAll(chunkResults.filterNotNull())
                parseTaskRepository.incrementProcessedArticles(taskId, chunk.size)
            }
        }

        log.info("Task {}: fetched {} articles, queuing analysis", taskId, savedArticles.size)

        val toAnalyze = if (autoPublish) savedArticles.filter { it.analyzedText == null } else emptyList()
        toAnalyze.forEach { article -> articleAnalysisPublisher.publish(requireNotNull(article.id)) }

        parseTaskRepository.update(taskId) {
            it.copy(status = ParseTaskStatus.COMPLETED, updatedAt = LocalDateTime.now())
        }
        log.info("Task {}: completed, processed {} articles", taskId, savedArticles.size)

        parseAlertService.parsingFinished(
            taskId = taskId,
            keyword = keyword,
            totalArticles = allArticles.size,
            savedArticles = savedArticles.size,
            queuedForAnalysis = toAnalyze.size,
        )
    }

    private fun processArticle(taskId: UUID, sourceArticle: DraftsimSourceArticle): Article {
        val article = Article(
            id = null,
            externalId = sourceArticle.externalId,
            title = sourceArticle.title,
            slug = sourceArticle.slug,
            url = sourceArticle.url,
            htmlContent = sourceArticle.htmlContent,
            textContent = sourceArticle.textContent,
            analyzedText = null,
            keywords = emptyList(),
            favorite = false,
            errorMsg = null,
            analyzStartedAt = null,
            analyzEndedAt = null,
            publishedAt = sourceArticle.publishedAt,
            fetchedAt = LocalDateTime.now(),
        )
        val saved = articleRepository.save(article)
        val savedArticleId = requireNotNull(saved.id)
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
        private const val ERROR_MESSAGE_MAX_LENGTH = 1000
    }
}
