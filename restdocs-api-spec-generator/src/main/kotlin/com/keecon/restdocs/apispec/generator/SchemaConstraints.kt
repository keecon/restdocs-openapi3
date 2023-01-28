package com.keecon.restdocs.apispec.generator

import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.ConstraintResolver
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.StringSchema

internal object SchemaConstraints {

    internal fun ArraySchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinSize(descriptor)?.let { minItems = it.toInt() }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxItems = it.toInt() }
    }

    internal fun StringSchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinSize(descriptor)?.let { minLength = it.toInt() }
        ConstraintResolver.maybeMaxSize(descriptor)?.let { maxLength = it.toInt() }
        ConstraintResolver.maybePattern(descriptor)?.let { pattern = it }
    }

    internal fun IntegerSchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinNumber(descriptor)?.let { minimum = it }
        ConstraintResolver.maybeMaxNumber(descriptor)?.let { maximum = it }
    }

    internal fun NumberSchema.applyConstraints(descriptor: AbstractDescriptor) = apply {
        ConstraintResolver.maybeMinNumber(descriptor)?.let { minimum = it }
        ConstraintResolver.maybeMaxNumber(descriptor)?.let { maximum = it }
    }
}
