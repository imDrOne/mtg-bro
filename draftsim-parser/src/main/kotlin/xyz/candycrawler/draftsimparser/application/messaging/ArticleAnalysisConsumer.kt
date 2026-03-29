package xyz.candycrawler.draftsimparser.application.messaging

interface ArticleAnalysisConsumer {
    suspend fun consume(message: ArticleAnalysisMessage)
}
