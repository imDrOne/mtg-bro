package xyz.candycrawler.authservice.application.rest

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.authservice.application.rest.dto.request.LoginRequest
import xyz.candycrawler.authservice.application.rest.dto.response.LoginResponse
import xyz.candycrawler.authservice.application.service.AuthSession
import xyz.candycrawler.authservice.application.service.AuthenticationService
import xyz.candycrawler.authservice.domain.refreshtoken.exception.RefreshTokenInvalidException
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authenticationService: AuthenticationService,
    @Value("\${auth.refresh-cookie.secure:true}") private val refreshCookieSecure: Boolean,
) {

    companion object {
        const val REFRESH_COOKIE_NAME = "refresh_token"
        const val REFRESH_COOKIE_PATH = "/api/v1/auth"
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, response: HttpServletResponse): ResponseEntity<Any> = try {
        val session = authenticationService.login(request.email, request.password)
        writeRefreshCookie(response, session.refreshToken, session.refreshTokenExpiresInSeconds.toInt())
        ResponseEntity.ok(LoginResponse(session.accessToken, expiresIn = session.accessTokenExpiresInSeconds))
    } catch (_: UserInvalidException) {
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
    }

    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Any> {
        val cookie = request.cookies?.firstOrNull { it.name == REFRESH_COOKIE_NAME }
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Missing refresh token"))
        return try {
            val session = authenticationService.refresh(cookie.value)
            writeRefreshCookie(response, session.refreshToken, session.refreshTokenExpiresInSeconds.toInt())
            ResponseEntity.ok(LoginResponse(session.accessToken, expiresIn = session.accessTokenExpiresInSeconds))
        } catch (_: RefreshTokenInvalidException) {
            clearRefreshCookie(response)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid refresh token"))
        } catch (_: UserInvalidException) {
            clearRefreshCookie(response)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid refresh token"))
        }
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Void> {
        request.cookies?.firstOrNull { it.name == REFRESH_COOKIE_NAME }?.let {
            authenticationService.logout(it.value)
        }
        clearRefreshCookie(response)
        return ResponseEntity.noContent().build()
    }

    private fun writeRefreshCookie(response: HttpServletResponse, value: String, maxAgeSeconds: Int) {
        val cookie = Cookie(REFRESH_COOKIE_NAME, value).apply {
            isHttpOnly = true
            secure = refreshCookieSecure
            path = REFRESH_COOKIE_PATH
            maxAge = maxAgeSeconds
            setAttribute("SameSite", "Strict")
        }
        response.addCookie(cookie)
    }

    private fun clearRefreshCookie(response: HttpServletResponse) {
        val cookie = Cookie(REFRESH_COOKIE_NAME, "").apply {
            isHttpOnly = true
            secure = refreshCookieSecure
            path = REFRESH_COOKIE_PATH
            maxAge = 0
            setAttribute("SameSite", "Strict")
        }
        response.addCookie(cookie)
    }
}
