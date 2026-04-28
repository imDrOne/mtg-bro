package xyz.candycrawler.draftsimparser.application.service

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
import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository

@Service
class ArticleKeywordService(
    private val queryArticleRepository: QueryArticleRepository,
    private val articleRepository: ArticleRepository,
    private val keywordExtractor: ArticleKeywordExtractor,
) : DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(MAX_CONCURRENT_ARTICLES)

    fun collectAsync(ids: List<Long>) {
        val uniqueIds = ids.distinct()
        if (uniqueIds.isEmpty()) return

        scope.launch {
            coroutineScope {
                uniqueIds.map { id ->
                    async {
                        semaphore.withPermit {
                            runCatching { collect(id) }
                                .onFailure { log.error("Article id={}: keyword extraction failed", id, it) }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    fun collect(id: Long): Article {
        val article = queryArticleRepository.findById(id)
        val keywords = keywordExtractor.extract(article.textContent)
        return articleRepository.update(id) { it.copy(keywords = keywords) }
    }

    override fun destroy() {
        scope.cancel()
    }

    companion object {
        private const val MAX_CONCURRENT_ARTICLES = 5
    }
}
