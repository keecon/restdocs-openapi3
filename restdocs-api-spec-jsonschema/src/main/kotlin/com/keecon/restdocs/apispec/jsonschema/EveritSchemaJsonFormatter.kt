package com.keecon.restdocs.apispec.jsonschema

import org.everit.json.schema.ArraySchema
import org.everit.json.schema.CombinedSchema
import org.everit.json.schema.EnumSchema
import org.everit.json.schema.ObjectSchema
import org.everit.json.schema.Schema
import org.everit.json.schema.internal.JSONPrinter
import java.io.StringWriter
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonMapperBuilder

internal class EveritSchemaJsonFormatter {
    private val objectMapper = jacksonMapperBuilder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build()

    fun format(schema: Schema): String {
        val schemaJson = StringWriter().use { writer ->
            schema.describeTo(JSONPrinter(writer))
            writer.toString()
        }
        val root = objectMapper.readTree(schemaJson)

        restoreEnumOrder(schema, root)

        return objectMapper.writeValueAsString(root)
    }

    private fun restoreEnumOrder(schema: Schema, node: JsonNode) {
        when (schema) {
            is EnumSchema -> restoreEnumOrder(schema, node)
            is ObjectSchema -> restoreEnumOrder(schema, node)
            is ArraySchema -> restoreEnumOrder(schema, node)
            is CombinedSchema -> restoreEnumOrder(schema, node)
        }
    }

    private fun restoreEnumOrder(schema: EnumSchema, node: JsonNode) {
        if (node is ObjectNode && node.has("enum")) {
            node.replace("enum", objectMapper.valueToTree(schema.possibleValuesAsList))
        }
    }

    private fun restoreEnumOrder(schema: ObjectSchema, node: JsonNode) {
        schema.propertySchemas.forEach { (name, propertySchema) ->
            node.path("properties").get(name)?.let { restoreEnumOrder(propertySchema, it) }
        }
    }

    private fun restoreEnumOrder(schema: ArraySchema, node: JsonNode) {
        schema.allItemSchema?.let { itemSchema ->
            node.get("items")?.let { restoreEnumOrder(itemSchema, it) }
        }
    }

    private fun restoreEnumOrder(schema: CombinedSchema, node: JsonNode) {
        val subschemas = schema.subschemas.toList()
        val criterionNode = node.get(schema.criterion.toString())

        if (criterionNode is ArrayNode && criterionNode.size() == subschemas.size) {
            subschemas.zip(criterionNode).forEach { (subschema, subnode) ->
                restoreEnumOrder(subschema, subnode)
            }
        } else {
            subschemas.forEach { restoreEnumOrder(it, node) }
        }
    }
}
