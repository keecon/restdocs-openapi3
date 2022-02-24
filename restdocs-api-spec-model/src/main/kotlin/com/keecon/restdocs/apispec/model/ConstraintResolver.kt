package com.keecon.restdocs.apispec.model

import java.math.BigDecimal

object ConstraintResolver {

    private const val MIN_CONSTRAINT = "javax.validation.constraints.Min"

    private const val MAX_CONSTRAINT = "javax.validation.constraints.Max"

    private const val DECIMAL_MIN_CONSTRAINT = "javax.validation.constraints.DecimalMin"

    private const val DECIMAL_MAX_CONSTRAINT = "javax.validation.constraints.DecimalMax"

    private const val SIZE_CONSTRAINT = "javax.validation.constraints.Size"

    private const val PATTERN_CONSTRAINT = "javax.validation.constraints.Pattern"

    private const val NOT_EMPTY_CONSTRAINT = "javax.validation.constraints.NotEmpty"

    private const val NOT_BLANK_CONSTRAINT = "javax.validation.constraints.NotBlank"

    private val REQUIRED_CONSTRAINTS = setOf(
        "javax.validation.constraints.NotNull",
        NOT_EMPTY_CONSTRAINT,
        NOT_BLANK_CONSTRAINT,
    )

    private fun AbstractDescriptor.constraints() = attributes.validationConstraints

    private fun AbstractDescriptor.maybeConstraint(type: String) = constraints().firstOrNull { type == it.name }

    fun isRequired(descriptor: AbstractDescriptor) =
        descriptor.constraints().any { REQUIRED_CONSTRAINTS.contains(it.name) }

    private fun AbstractDescriptor.maybeSizeConstraint() = maybeConstraint(SIZE_CONSTRAINT)

    private fun AbstractDescriptor.maybePatternConstraint() = maybeConstraint(PATTERN_CONSTRAINT)

    private fun <T : Comparable<T>> AbstractDescriptor.maybeMinConstraint(transform: (Constraint) -> T?) =
        constraints().mapNotNull { transform(it) }.maxOrNull()

    private fun <T : Comparable<T>> AbstractDescriptor.maybeMaxConstraint(transform: (Constraint) -> T?) =
        constraints().mapNotNull { transform(it) }.minOrNull()

    fun maybeMinSize(descriptor: AbstractDescriptor?) = descriptor?.maybeMinConstraint {
        when (it.name) {
            NOT_EMPTY_CONSTRAINT,
            NOT_BLANK_CONSTRAINT -> BigDecimal.ONE
            SIZE_CONSTRAINT -> toBigDecimal(it.configuration["min"])
            else -> null
        }
    }

    fun maybeMaxSize(descriptor: AbstractDescriptor?) =
        descriptor?.maybeSizeConstraint()?.let { toBigDecimal(it.configuration["max"]) }

    fun maybeMinNumber(descriptor: AbstractDescriptor) = descriptor.maybeMinConstraint {
        when (it.name) {
            MIN_CONSTRAINT -> toBigDecimal(it.configuration["value"])
            DECIMAL_MIN_CONSTRAINT -> toBigDecimal(it.configuration["value"])
            else -> null
        }
    }

    fun maybeMaxNumber(descriptor: AbstractDescriptor) = descriptor.maybeMaxConstraint {
        when (it.name) {
            MAX_CONSTRAINT -> toBigDecimal(it.configuration["value"])
            DECIMAL_MAX_CONSTRAINT -> toBigDecimal(it.configuration["value"])
            else -> null
        }
    }

    fun maybePattern(descriptor: AbstractDescriptor?) =
        descriptor?.maybePatternConstraint()?.let { it.configuration["regexp"] as? String }

    private fun toBigDecimal(value: Any?) = when (value) {
        is String -> BigDecimal(value)
        is Int -> value.toBigDecimal()
        is Double -> value.toBigDecimal()
        else -> value as? BigDecimal
    }
}
