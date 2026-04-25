package xyz.candycrawler.mcpserver.auth

import io.ktor.util.AttributeKey
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

val UserTokenKey = AttributeKey<String>("UserToken")

class UserTokenElement(val token: String) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<UserTokenElement>
}

suspend fun currentUserToken(): String? =
    currentCoroutineContext()[UserTokenElement]?.token
