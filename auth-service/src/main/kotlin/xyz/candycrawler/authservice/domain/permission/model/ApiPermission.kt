package xyz.candycrawler.authservice.domain.permission.model

data class ApiPermission(
    val id: Long,
    val name: String,
    val description: String,
) {
    init {
        require(name.isNotBlank()) { "ApiPermission name must not be blank" }
        require(description.isNotBlank()) { "ApiPermission description must not be blank" }
    }
}
