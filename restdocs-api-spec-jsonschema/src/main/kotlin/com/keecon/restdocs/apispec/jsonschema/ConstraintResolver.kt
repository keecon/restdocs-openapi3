package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.Constraint

internal object ConstraintResolver {

    // since validation-api 2.0 NotEmpty moved to javax.validation - we support both
    private val NOT_EMPTY_CONSTRAINTS = setOf(
        "org.hibernate.validator.constraints.NotEmpty",
        "javax.validation.constraints.NotEmpty"
    )

    private val NOT_BLANK_CONSTRAINTS = setOf(
        "javax.validation.constraints.NotBlank",
        "org.hibernate.validator.constraints.NotBlank"
    )

    private val REQUIRED_CONSTRAINTS = setOf("javax.validation.constraints.NotNull")
        .plus(NOT_EMPTY_CONSTRAINTS)
        .plus(NOT_BLANK_CONSTRAINTS)

    private const val LENGTH_CONSTRAINT = "org.hibernate.validator.constraints.Length"

    private const val SIZE_CONSTRAINT = "javax.validation.constraints.Size"

    private const val PATTERN_CONSTRAINT = "javax.validation.constraints.Pattern"

    private const val MIN_CONSTRAINT = "javax.validation.constraints.Min"

    private const val MAX_CONSTRAINT = "javax.validation.constraints.Max"

    internal fun maybeMinSizeArray(fieldDescriptor: AbstractDescriptor?) =
        fieldDescriptor?.maybeSizeConstraint()?.let { it.configuration["min"] as? Int }

    internal fun maybeMaxSizeArray(fieldDescriptor: AbstractDescriptor?) =
        fieldDescriptor?.maybeSizeConstraint()?.let { it.configuration["max"] as? Int }

    private fun AbstractDescriptor.maybeSizeConstraint() =
        findConstraints(this).firstOrNull { SIZE_CONSTRAINT == it.name }

    internal fun maybePattern(fieldDescriptor: AbstractDescriptor?) =
        fieldDescriptor?.maybePatternConstraint()?.let { it.configuration["pattern"] as? String }

    private fun AbstractDescriptor.maybePatternConstraint() =
        findConstraints(this).firstOrNull { PATTERN_CONSTRAINT == it.name }

    internal fun minLengthString(fieldDescriptor: AbstractDescriptor): Int? {
        return findConstraints(fieldDescriptor)
            .firstOrNull { constraint ->
                (
                    NOT_EMPTY_CONSTRAINTS.contains(constraint.name) ||
                        NOT_BLANK_CONSTRAINTS.contains(constraint.name) ||
                        LENGTH_CONSTRAINT == constraint.name
                    )
            }
            ?.let { constraint ->
                if (LENGTH_CONSTRAINT == constraint.name) constraint.configuration["min"] as Int
                else 1
            }
    }

    internal fun maxLengthString(fieldDescriptor: AbstractDescriptor): Int? {
        return findConstraints(fieldDescriptor)
            .firstOrNull { LENGTH_CONSTRAINT == it.name }
            ?.let { it.configuration["max"] as Int }
    }

    internal fun minNumber(fieldDescriptor: AbstractDescriptor): Int? {
        return findConstraints(fieldDescriptor)
            .firstOrNull { MIN_CONSTRAINT == it.name }
            ?.let { it.configuration["value"] as? Int }
    }

    internal fun maxNumber(fieldDescriptor: AbstractDescriptor): Int? {
        return findConstraints(fieldDescriptor)
            .firstOrNull { MAX_CONSTRAINT == it.name }
            ?.let { it.configuration["value"] as? Int }
    }

    internal fun isRequired(fieldDescriptor: AbstractDescriptor): Boolean =
        findConstraints(fieldDescriptor)
            .any { constraint -> REQUIRED_CONSTRAINTS.contains(constraint.name) }

    private fun findConstraints(fieldDescriptor: AbstractDescriptor): List<Constraint> =
        fieldDescriptor.attributes.validationConstraints
}
