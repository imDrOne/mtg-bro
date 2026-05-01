package xyz.candycrawler.draftsimparser.application.port

interface AlertPublisher {
    fun send(message: String)
}
