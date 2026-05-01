package xyz.candycrawler.draftsimparser.application.port

interface ArticleAnalysisPublisher {
    fun publish(articleId: Long)
}
