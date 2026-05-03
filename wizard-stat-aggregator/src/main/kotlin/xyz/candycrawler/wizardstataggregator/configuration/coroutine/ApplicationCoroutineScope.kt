package xyz.candycrawler.wizardstataggregator.configuration.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class ApplicationCoroutineScope :
    CoroutineScope,
    DisposableBean {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext = job + Dispatchers.IO

    override fun destroy() = cancel()
}
