package xyz.candycrawler.mcpserver.auth

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlContentPolymorphicSerializer
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.InputStream

/**
 * Represents which tools a single role can access.
 * Either a wildcard "*" (all tools) or a specific list of tool names.
 */
@Serializable(with = RoleToolsSerializer::class)
sealed class RoleTools {
    /** All tools are accessible (ADMIN wildcard). */
    object All : RoleTools()

    /** A specific set of named tools. */
    data class Named(val tools: List<String>) : RoleTools()

    fun contains(toolName: String): Boolean = when (this) {
        is All -> true
        is Named -> tools.contains(toolName)
    }
}

object RoleToolsSerializer : YamlContentPolymorphicSerializer<RoleTools>(RoleTools::class) {
    override fun selectDeserializer(node: YamlNode): DeserializationStrategy<RoleTools> =
        when {
            node is YamlScalar && node.content == "*" -> WildcardDeserializer
            else -> NamedDeserializer
        }

    private object WildcardDeserializer : DeserializationStrategy<RoleTools> {
        override val descriptor = String.serializer().descriptor
        override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): RoleTools {
            decoder.decodeString() // consume the "*" scalar
            return RoleTools.All
        }
    }

    private object NamedDeserializer : DeserializationStrategy<RoleTools> {
        private val delegate = ListSerializer(String.serializer())
        override val descriptor = delegate.descriptor
        override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): RoleTools =
            RoleTools.Named(delegate.deserialize(decoder))
    }
}

@Serializable
data class ToolAccessConfigData(
    val roles: Map<String, RoleTools> = emptyMap(),
    @SerialName("default_allowed_tools") val defaultAllowedTools: List<String> = emptyList(),
)

fun ToolAccessConfigData.hasAccess(toolName: String, userRoles: List<String>): Boolean {
    return userRoles.any { role ->
        val allowed = roles[role]
        allowed?.contains(toolName) ?: defaultAllowedTools.contains(toolName)
    }
}

object ToolAccessConfig {
    fun load(stream: InputStream): ToolAccessConfigData {
        val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
        return yaml.decodeFromStream(ToolAccessConfigData.serializer(), stream)
    }

    fun loadFromResources(): ToolAccessConfigData =
        requireNotNull(ToolAccessConfig::class.java.getResourceAsStream("/tool-access.yml")) {
            "Required resource /tool-access.yml not found in classpath"
        }.use { load(it) }
}
