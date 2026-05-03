package xyz.candycrawler.mcpserver.auth

class McpAuthConfig {
    lateinit var issuerUri: String
    lateinit var jwksUri: String
    lateinit var resourceMetadataUrl: String
    var scopes: String = "openid profile decks:read"
}
