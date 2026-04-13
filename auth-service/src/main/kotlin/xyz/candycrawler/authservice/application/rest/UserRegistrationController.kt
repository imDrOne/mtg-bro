package xyz.candycrawler.authservice.application.rest

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.authservice.application.rest.dto.request.RegisterUserRequest
import xyz.candycrawler.authservice.application.rest.dto.response.RegisterUserResponse
import xyz.candycrawler.authservice.application.service.UserRegistrationService

@RestController
@RequestMapping("/api/v1/users")
class UserRegistrationController(
    private val registrationService: UserRegistrationService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterUserRequest): RegisterUserResponse {
        val user = registrationService.register(
            email = request.email,
            username = request.username,
            rawPassword = request.password,
        )
        return RegisterUserResponse(
            id = user.id!!,
            email = user.email,
            username = user.username,
            createdAt = user.createdAt,
        )
    }
}
