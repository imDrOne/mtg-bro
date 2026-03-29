package xyz.candycrawler.draftsimparser.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ArticleRecord
import java.util.function.Function

@Component
class ArticleToArticleRecordMapper : Function<Article, ArticleRecord> {

    override fun apply(article: Article): ArticleRecord = ArticleRecord(
        id = article.id,
        externalId = article.externalId,
        title = article.title,
        slug = article.slug,
        url = article.url,
        htmlContent = article.htmlContent,
        textContent = article.textContent,
        analyzedText = article.analyzedText,
        favorite = article.favorite,
        errorMsg = article.errorMsg,
        analyzStartedAt = article.analyzStartedAt,
        analyzEndedAt = article.analyzEndedAt,
        publishedAt = article.publishedAt,
        fetchedAt = article.fetchedAt,
    )
}
