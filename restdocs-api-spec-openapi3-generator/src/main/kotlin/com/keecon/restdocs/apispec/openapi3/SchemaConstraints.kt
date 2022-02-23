package com.keecon.restdocs.apispec.openapi3

import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.ConstraintResolver
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.StringSchema

internal object SchemaConstraints {

    internal fun ArraySchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinSize(descriptor)?.let { minItems = it }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxItems = it }
    }

    internal fun StringSchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinSize(descriptor)?.let { minLength = it }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxLength = it }
        ConstraintResolver.maybePattern(descriptor)?.let { pattern = it }
    }

    internal fun IntegerSchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinInt(descriptor)?.let { minimum = it.toBigDecimal() }
        ConstraintResolver.maybeMaxInt(descriptor)?.let { maximum = it.toBigDecimal() }
    }

    internal fun NumberSchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinInt(descriptor)?.let { minimum = it.toBigDecimal() }
        ConstraintResolver.maybeMaxInt(descriptor)?.let { maximum = it.toBigDecimal() }
    }
}
