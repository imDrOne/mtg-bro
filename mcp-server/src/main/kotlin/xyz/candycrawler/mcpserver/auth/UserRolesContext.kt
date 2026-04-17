package xyz.candycrawler.mcpserver.auth

import io.ktor.util.AttributeKey
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

val UserRolesKey = AttributeKey<List<String>>("UserRoles")

class UserRolesElement(val roles: List<String>) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<UserRolesElement>
}

suspend fun currentUserRoles(): List<String> =
    coroutineContext[UserRolesElement]?.roles ?: emptyList()

suspend fun isAuthEnabled(): Boolean =
    coroutineContext[UserRolesElement] != null
