package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.ConstraintResolver
import com.keecon.restdocs.apispec.model.DataFormat
import org.everit.json.schema.ArraySchema
import org.everit.json.schema.FormatValidator
import org.everit.json.schema.NumberSchema
import org.everit.json.schema.StringSchema

internal object JsonSchemaConstraints {

    internal fun ArraySchema.Builder.applyConstraints(descriptor: AbstractDescriptor?) = apply {
        ConstraintResolver.maybeMinSize(descriptor)?.let { minItems(it.toInt()) }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxItems(it.toInt()) }
    }

    internal fun StringSchema.Builder.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinSize(descriptor)?.let { minLength(it.toInt()) }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxLength(it.toInt()) }
        ConstraintResolver.maybePattern(descriptor)?.let { pattern(it) }
    }

    internal fun NumberSchema.Builder.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinNumber(descriptor)?.let { minimum(it) }
        ConstraintResolver.maybeMaxNumber(descriptor)?.let { maximum(it) }
    }

    internal fun StringSchema.Builder.applyFormat(descriptor: AbstractDescriptor) = apply {
        formatValidator(
            descriptor.attributes.format?.let {
                when (val format = it.lowercase()) {
                    DataFormat.DATETIME.lowercase() -> {
                        FormatValidator.forFormat("date-time")
                    }

                    DataFormat.EMAIL.lowercase(),
                    DataFormat.URI.lowercase(),
                    DataFormat.HOSTNAME.lowercase(),
                    DataFormat.IPV4.lowercase(),
                    DataFormat.IPV6.lowercase() -> {
                        FormatValidator.forFormat(format)
                    }

                    DataFormat.BINARY.lowercase() -> {
                        StringBinaryValidator()
                    }

                    else -> FormatValidator.NONE
                }
            } ?: FormatValidator.NONE
        )
    }

    internal fun NumberSchema.Builder.applyFormat(descriptor: AbstractDescriptor) = apply {
        when (descriptor.attributes.format?.lowercase()) {
            DataFormat.INT32.lowercase(), DataFormat.INT64.lowercase() -> requiresInteger(true)
            else -> Unit
        }
    }
}
