package xyz.candycrawler.authservice.application.rest.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class DynamicClientRegistrationRequest(
    @JsonProperty("redirect_uris")
    val redirectUris: List<String>,

    @JsonProperty("client_name")
    val clientName: String? = null,

    @JsonProperty("token_endpoint_auth_method")
    val tokenEndpointAuthMethod: String? = null,

    @JsonProperty("grant_types")
    val grantTypes: List<String>? = null,

    @JsonProperty("response_types")
    val responseTypes: List<String>? = null,

    @JsonProperty("scope")
    val scope: String? = null,
)
