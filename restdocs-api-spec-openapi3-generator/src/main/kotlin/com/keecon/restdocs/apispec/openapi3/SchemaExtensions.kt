package com.keecon.restdocs.apispec.openapi3

import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.AbstractParameterDescriptor
import com.keecon.restdocs.apispec.model.DataFormat
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import java.math.BigDecimal

internal object SchemaExtensions {

    internal fun Schema<*>.applyProperties(descriptor: AbstractDescriptor) = apply {
        when (descriptor.attributes.format) {
            DataFormat.DATETIME.lowercase() -> format("date-time")
            is String -> format(descriptor.attributes.format)
            else -> Unit
        }
    }

    internal fun BooleanSchema.applyDefaultValue(descriptor: AbstractParameterDescriptor?) = apply {
        descriptor?.defaultValue?.let { _default(it as Boolean) }
    }

    internal fun StringSchema.applyDefaultValue(descriptor: AbstractParameterDescriptor?) = apply {
        descriptor?.defaultValue?.let { _default(it as String) }
    }

    internal fun NumberSchema.applyDefaultValue(descriptor: AbstractParameterDescriptor?) = apply {
        descriptor?.defaultValue?.let { _default(toBigDecimal(it)) }
    }

    internal fun IntegerSchema.applyDefaultValue(descriptor: AbstractParameterDescriptor?) = apply {
        descriptor?.defaultValue?.let { _default(it as Int) }
    }

    internal fun BooleanSchema.applyEnumValues(descriptor: AbstractDescriptor) = apply {
        descriptor.attributes.enumValues
            .map { it as Boolean }
            .forEach { addEnumItem(it) }
    }

    internal fun StringSchema.applyEnumValues(descriptor: AbstractDescriptor) = apply {
        descriptor.attributes.enumValues
            .map { it as String }
            .forEach { addEnumItem(it) }
    }

    internal fun NumberSchema.applyEnumValues(descriptor: AbstractDescriptor) = apply {
        descriptor.attributes.enumValues
            .map { toBigDecimal(it) }
            .forEach { addEnumItem(it) }
    }

    internal fun IntegerSchema.applyEnumValues(descriptor: AbstractDescriptor) = apply {
        descriptor.attributes.enumValues
            .map { it as Int }
            .forEach { addEnumItem(it) }
    }

    internal fun ArraySchema.applyItems(descriptor: AbstractDescriptor) = apply {
        descriptor.attributes.items?.let { items(OpenApi3Generator.simpleTypeToSchema(it)) }
    }

    private fun toBigDecimal(value: Any) = when (value) {
        is String -> BigDecimal(value)
        is Int -> value.toBigDecimal()
        is Double -> value.toBigDecimal()
        else -> value as BigDecimal
    }
}
