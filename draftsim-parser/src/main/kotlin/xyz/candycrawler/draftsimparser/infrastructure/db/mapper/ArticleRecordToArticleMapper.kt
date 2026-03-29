package xyz.candycrawler.draftsimparser.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.infrastructure.db.entity.ArticleRecord
import java.util.function.Function

@Component
class ArticleRecordToArticleMapper : Function<ArticleRecord, Article> {

    override fun apply(record: ArticleRecord): Article = Article(
        id = record.id,
        externalId = record.externalId,
        title = record.title,
        slug = record.slug,
        url = record.url,
        htmlContent = record.htmlContent,
        textContent = record.textContent,
        analyzedText = record.analyzedText,
        publishedAt = record.publishedAt,
        fetchedAt = record.fetchedAt,
    )
}
