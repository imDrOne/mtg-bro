package xyz.candycrawler.draftsimparser.infrastructure.messaging

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.application.port.ArticleAnalysisPublisher

@Component
class SpringArticleAnalysisPublisher(private val eventPublisher: ApplicationEventPublisher) :
    ArticleAnalysisPublisher {

    override fun publish(articleId: Long) {
        eventPublisher.publishEvent(ArticleAnalysisMessage(articleId))
    }
}
