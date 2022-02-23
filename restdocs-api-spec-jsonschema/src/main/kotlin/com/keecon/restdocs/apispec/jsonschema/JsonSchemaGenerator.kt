package com.keecon.restdocs.apispec.jsonschema

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.keecon.restdocs.apispec.jsonschema.JsonSchemaConstraints.applyConstraints
import com.keecon.restdocs.apispec.model.ConstraintResolver.isRequired
import com.keecon.restdocs.apispec.model.FieldDescriptor
import org.everit.json.schema.ArraySchema
import org.everit.json.schema.ObjectSchema
import org.everit.json.schema.Schema
import org.everit.json.schema.internal.JSONPrinter
import java.io.StringWriter
import java.util.Collections.emptyList
import java.util.function.Predicate

class JsonSchemaGenerator {

    fun generateSchema(fieldDescriptors: List<FieldDescriptor>, title: String? = null): String {
        val jsonFieldPaths = reduceFieldDescriptors(fieldDescriptors)
            .map { JsonFieldPath.compile(it) }

        val schema = traverse(emptyList(), jsonFieldPaths, ObjectSchema.builder().title(title) as ObjectSchema.Builder)

        return toFormattedString(unWrapRootArray(jsonFieldPaths, schema))
    }

    /**
     * Reduce the list of field descriptors so that the path of each list item is unique.
     *
     * The implementation will
     */
    private fun reduceFieldDescriptors(fieldDescriptors: List<FieldDescriptor>): List<FieldDescriptorWithSchema> {
        return fieldDescriptors
            .map { FieldDescriptorWithSchema.fromFieldDescriptor(it) }
            .foldRight(listOf()) { fieldDescriptor, groups ->
                groups.firstOrNull { it.equalsOnPathAndType(fieldDescriptor) }
                    ?.let { groups } // omit the descriptor it is considered equal and can be omitted
                    ?: groups.firstOrNull { it.path == fieldDescriptor.path }
                        ?.let { groups - it + it.merge(fieldDescriptor) } // merge the type with the descriptor with the same name
                    ?: (groups + fieldDescriptor) // it is new just add it
            }
    }

    private fun unWrapRootArray(jsonFieldPaths: List<JsonFieldPath>, schema: Schema): Schema {
        if (schema is ObjectSchema) {
            val groups = groupFieldsByFirstRemainingPathSegment(emptyList(), jsonFieldPaths)
            if (groups.keys.size == 1 && groups.keys.contains("[]")) {
                // In case of root array without additional fields, return the propertySchemas as it has already been properly defined in [typeToSchema]
                // In other cases wrap it in ArraySchema
                val rootDescriptor = jsonFieldPaths.find { it.fieldDescriptor.path == "[]" }
                return takeIf {
                    rootDescriptor?.remainingSegments(emptyList())?.size == 1 && jsonFieldPaths.size == 1
                }?.let { schema.propertySchemas["[]"] }
                    ?: ArraySchema.builder().allItemSchema(schema.propertySchemas["[]"])
                        .applyConstraints(rootDescriptor?.fieldDescriptor)
                        .description(rootDescriptor?.fieldDescriptor?.description)
                        .title(schema.title).build()
            }
        }
        return schema
    }

    private fun toFormattedString(schema: Schema): String {
        val objectMapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        return StringWriter().use {
            schema.describeTo(JSONPrinter(it))
            objectMapper.writeValueAsString(objectMapper.readTree(it.toString()))
        }
    }

    private fun traverse(
        traversedSegments: List<String>,
        jsonFieldPaths: List<JsonFieldPath>,
        builder: ObjectSchema.Builder
    ): Schema {
        val groupedFields = groupFieldsByFirstRemainingPathSegment(traversedSegments, jsonFieldPaths)
        groupedFields.forEach { (propertyName, fieldList) ->
            val newTraversedSegments = (traversedSegments + propertyName).toMutableList()
            fieldList.stream()
                .filter(isDirectMatch(newTraversedSegments))
                .findFirst()
                .map { directMatch ->
                    if (fieldList.size == 1) {
                        handleEndOfPath(builder, propertyName, directMatch.fieldDescriptor)
                    } else {
                        val newFields = ArrayList(fieldList)
                        newFields.remove(directMatch)
                        processRemainingSegments(
                            builder,
                            propertyName,
                            newTraversedSegments,
                            newFields,
                            directMatch
                        )
                    }
                    true
                }.orElseGet {
                    processRemainingSegments(builder, propertyName, newTraversedSegments, fieldList, null)
                    true
                }
        }
        return builder.build()
    }

    private fun isDirectMatch(traversedSegments: List<String>): Predicate<JsonFieldPath> {
        // we have a direct match when there are no remaining segments or when the only following element is an array
        return Predicate { jsonFieldPath ->
            val remainingSegments = jsonFieldPath.remainingSegments(traversedSegments)
            remainingSegments.isEmpty() || remainingSegments.size == 1 && JsonFieldPath.isArraySegment(
                remainingSegments[0]
            )
        }
    }

    private fun groupFieldsByFirstRemainingPathSegment(
        traversedSegments: List<String>,
        jsonFieldPaths: List<JsonFieldPath>
    ): Map<String, List<JsonFieldPath>> {
        return jsonFieldPaths.groupBy { it.remainingSegments(traversedSegments)[0] }
    }

    private fun processRemainingSegments(
        builder: ObjectSchema.Builder,
        propertyName: String,
        traversedSegments: MutableList<String>,
        fields: List<JsonFieldPath>,
        propertyField: JsonFieldPath? = null
    ) {
        val remainingSegments = fields[0].remainingSegments(traversedSegments)
        if (propertyField?.fieldDescriptor?.let { isRequired(it) } == true) {
            builder.addRequiredProperty(propertyName)
        }
        if (remainingSegments.isNotEmpty() && JsonFieldPath.isArraySegment(remainingSegments[0])) {
            traversedSegments.add(remainingSegments[0])
            builder.addPropertySchema(
                propertyName,
                ArraySchema.builder()
                    .allItemSchema(traverse(traversedSegments, fields, ObjectSchema.builder()))
                    .applyConstraints(propertyField?.fieldDescriptor)
                    .description(propertyField?.fieldDescriptor?.description)
                    .build()
            )
        } else {
            builder.addPropertySchema(
                propertyName,
                traverse(
                    traversedSegments, fields,
                    ObjectSchema.builder()
                        .description(propertyField?.fieldDescriptor?.description) as ObjectSchema.Builder
                )
            )
        }
    }

    private fun handleEndOfPath(
        builder: ObjectSchema.Builder,
        propertyName: String,
        fieldDescriptor: FieldDescriptorWithSchema
    ) {
        if (!fieldDescriptor.ignored) {
            if (isRequired(fieldDescriptor)) {
                builder.addRequiredProperty(propertyName)
            }
            builder.addPropertySchema(propertyName, fieldDescriptor.jsonSchemaType())
        }
    }
}
