package xyz.candycrawler.authservice.application.rest.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class DynamicClientRegistrationResponse(
    @JsonProperty("client_id")
    val clientId: String,

    @JsonProperty("client_secret")
    val clientSecret: String? = null,

    @JsonProperty("client_id_issued_at")
    val clientIdIssuedAt: Long,

    @JsonProperty("client_secret_expires_at")
    val clientSecretExpiresAt: Long? = null,

    @JsonProperty("redirect_uris")
    val redirectUris: List<String>,

    @JsonProperty("client_name")
    val clientName: String,

    @JsonProperty("token_endpoint_auth_method")
    val tokenEndpointAuthMethod: String,

    @JsonProperty("grant_types")
    val grantTypes: List<String>,

    @JsonProperty("response_types")
    val responseTypes: List<String>,

    @JsonProperty("scope")
    val scope: String,
)
