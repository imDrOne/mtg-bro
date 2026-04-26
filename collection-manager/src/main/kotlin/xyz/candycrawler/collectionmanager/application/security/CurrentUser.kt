package xyz.candycrawler.collectionmanager.application.security

import org.springframework.security.oauth2.jwt.Jwt

fun Jwt.userId(): Long =
    claims["user_id"]?.toString()?.toLongOrNull()
        ?: throw IllegalStateException("user_id claim missing in JWT")
