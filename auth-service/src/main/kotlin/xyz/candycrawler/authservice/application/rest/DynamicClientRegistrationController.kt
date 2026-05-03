package xyz.candycrawler.authservice.application.rest

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.authservice.application.rest.dto.request.DynamicClientRegistrationRequest
import xyz.candycrawler.authservice.application.rest.dto.response.DynamicClientRegistrationResponse
import xyz.candycrawler.authservice.application.service.DynamicClientRegistrationService

@RestController
class DynamicClientRegistrationController(private val registrationService: DynamicClientRegistrationService) {

    @PostMapping("/connect/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: DynamicClientRegistrationRequest): DynamicClientRegistrationResponse =
        registrationService.register(request)
}
