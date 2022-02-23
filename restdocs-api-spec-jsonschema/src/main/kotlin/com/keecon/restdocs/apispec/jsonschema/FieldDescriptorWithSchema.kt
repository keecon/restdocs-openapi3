package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.jsonschema.JsonSchemaConstraints.applyConstraints
import com.keecon.restdocs.apispec.jsonschema.JsonSchemaConstraints.applyFormat
import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.Attributes
import com.keecon.restdocs.apispec.model.FieldDescriptor
import org.everit.json.schema.ArraySchema
import org.everit.json.schema.BooleanSchema
import org.everit.json.schema.CombinedSchema
import org.everit.json.schema.EmptySchema
import org.everit.json.schema.EnumSchema
import org.everit.json.schema.NullSchema
import org.everit.json.schema.NumberSchema
import org.everit.json.schema.ObjectSchema
import org.everit.json.schema.Schema
import org.everit.json.schema.StringSchema

internal class FieldDescriptorWithSchema(
    path: String,
    description: String,
    type: String,
    optional: Boolean,
    ignored: Boolean,
    attributes: Attributes,
    private val schemaBuilders: Set<Schema.Builder<*>> = setOf(
        toSchemaBuilder(
            jsonSchemaType(type),
            FieldDescriptor(path, description, type, optional, ignored, attributes)
        ),
    )
) : FieldDescriptor(path, description, type, optional, ignored, attributes) {

    fun jsonSchemaType(): Schema {
        val builder = if (schemaBuilders.size == 1) schemaBuilders.first()
        else CombinedSchema.oneOf(schemaBuilders.map { it.build() })
        return builder.description(description).build()
    }

    fun merge(other: FieldDescriptor): FieldDescriptorWithSchema {
        if (this.path != other.path)
            throw IllegalArgumentException("path of fieldDescriptor is not equal to ${this.path}")

        return FieldDescriptorWithSchema(
            path = this.path,
            description = this.description,
            type = this.type,
            optional = this.optional || other.optional, // optional if one it optional
            ignored = this.ignored && other.optional, // ignored if both are optional
            attributes = this.attributes,
            schemaBuilders = this.schemaBuilders + toSchemaBuilder(jsonSchemaType(other.type), other)
        )
    }

    fun equalsOnPathAndType(f: FieldDescriptorWithSchema): Boolean =
        (this.path == f.path && this.type == f.type)

    companion object {
        fun fromFieldDescriptor(fieldDescriptor: FieldDescriptor) =
            FieldDescriptorWithSchema(
                path = fieldDescriptor.path,
                description = fieldDescriptor.description,
                type = fieldDescriptor.type,
                optional = fieldDescriptor.optional,
                ignored = fieldDescriptor.ignored,
                attributes = fieldDescriptor.attributes
            )

        private fun toSchemaBuilder(type: String, descriptor: AbstractDescriptor): Schema.Builder<*> =
            when (type) {
                "null" -> NullSchema.builder()
                "empty" -> EmptySchema.builder()
                "object" -> ObjectSchema.builder()
                "array" -> ArraySchema.builder()
                    .applyConstraints(descriptor)
                    .allItemSchema(arrayItemsSchema(descriptor))
                "boolean" -> BooleanSchema.builder()
                "number" -> NumberSchema.builder()
                    .applyConstraints(descriptor)
                    .applyFormat(descriptor)
                "string" -> StringSchema.builder()
                    .applyConstraints(descriptor)
                    .applyFormat(descriptor)
                "enum" -> CombinedSchema.oneOf(
                    listOf(
                        StringSchema.builder().build(),
                        EnumSchema.builder().possibleValues(descriptor.attributes.enumValues).build()
                    )
                ).isSynthetic(true)
                else -> throw IllegalArgumentException("unknown field type $type")
            }

        private fun arrayItemsSchema(descriptor: AbstractDescriptor): Schema {
            return descriptor.attributes.items
                ?.let { toSchemaBuilder(jsonSchemaType(it.type.lowercase()), it).build() }
                ?: CombinedSchema.oneOf(
                    listOf(
                        ObjectSchema.builder().build(),
                        BooleanSchema.builder().build(),
                        StringSchema.builder().build(),
                        NumberSchema.builder().build()
                    )
                ).build()
        }

        private fun jsonSchemaType(descriptorType: String) =
            // varies are used by spring rest docs if the type is ambiguous - in json schema we want to represent as empty
            descriptorType.lowercase().let { if (it == "varies") "empty" else it }
    }
}
