package xyz.candycrawler.mcpserver.tools.schema

import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun stringProp(description: String, enum: List<String>? = null): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
    if (enum != null) {
        put("enum", JsonArray(enum.map { JsonPrimitive(it) }))
    }
}

/** Type-only item schema with no description — matches `buildJsonObject { put("type", "string") }`. */
val stringItem: JsonObject = buildJsonObject { put("type", "string") }

fun integerProp(description: String): JsonObject = buildJsonObject {
    put("type", "integer")
    put("description", description)
}

/** Type-only item schema with no description — matches `buildJsonObject { put("type", "integer") }`. */
val integerItem: JsonObject = buildJsonObject { put("type", "integer") }

fun numberProp(description: String): JsonObject = buildJsonObject {
    put("type", "number")
    put("description", description)
}

fun booleanProp(description: String): JsonObject = buildJsonObject {
    put("type", "boolean")
    put("description", description)
}

fun arrayProp(description: String, items: JsonObject): JsonObject = buildJsonObject {
    put("type", "array")
    put("description", description)
    put("items", items)
}

fun objectProp(
    description: String? = null,
    properties: Map<String, JsonObject>,
    required: List<String> = emptyList(),
): JsonObject = buildJsonObject {
    put("type", "object")
    if (description != null) put("description", description)
    put("properties", buildJsonObject { properties.forEach { (k, v) -> put(k, v) } })
    if (required.isNotEmpty()) {
        put("required", JsonArray(required.map { JsonPrimitive(it) }))
    }
}

fun toolSchema(required: List<String> = emptyList(), props: Map<String, JsonObject>): ToolSchema = ToolSchema(
    properties = buildJsonObject { props.forEach { (k, v) -> put(k, v) } },
    required = required.takeIf { it.isNotEmpty() },
)
