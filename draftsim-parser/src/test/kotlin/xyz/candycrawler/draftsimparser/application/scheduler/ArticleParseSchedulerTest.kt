package xyz.candycrawler.draftsimparser.application.scheduler

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import xyz.candycrawler.draftsimparser.application.service.ArticleParseRunner

class ArticleParseSchedulerTest {

    private val runner = mock<ArticleParseRunner>()
    private val scheduler = ArticleParseScheduler(runner)

    @Test
    fun `run delegates to runner tryRunScheduled`() {
        scheduler.run()
        verify(runner).tryRunScheduled()
    }
}
