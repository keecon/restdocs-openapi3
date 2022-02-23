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
        ConstraintResolver.maybeMinSize(descriptor)?.let { minItems(it) }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxItems(it) }
    }

    internal fun StringSchema.Builder.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinSize(descriptor)?.let { minLength(it) }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxLength(it) }
        ConstraintResolver.maybePattern(descriptor)?.let { pattern(it) }
    }

    internal fun NumberSchema.Builder.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinInt(descriptor)?.let { minimum(it) }
        ConstraintResolver.maybeMaxInt(descriptor)?.let { maximum(it) }
    }

    internal fun StringSchema.Builder.applyFormat(descriptor: AbstractDescriptor) = apply {
        formatValidator(
            descriptor.format?.let { FormatValidator.forFormat(it) } ?: FormatValidator.NONE
        )
    }

    internal fun NumberSchema.Builder.applyFormat(descriptor: AbstractDescriptor) = apply {
        when (descriptor.format) {
            DataFormat.INT32.lowercase(),
            DataFormat.INT64.lowercase() -> requiresInteger(true)
            else -> Unit
        }
    }
}
