package com.keecon.restdocs.apispec.model

import java.math.BigDecimal

object ConstraintResolver {

    private val MIN_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.Min",
        "javax.validation.constraints.Min",
    )

    private val MAX_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.Max",
        "javax.validation.constraints.Max",
    )

    private val DECIMAL_MIN_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.DecimalMin",
        "javax.validation.constraints.DecimalMin",
    )

    private val DECIMAL_MAX_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.DecimalMax",
        "javax.validation.constraints.DecimalMax",
    )

    private val SIZE_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.Size",
        "javax.validation.constraints.Size",
    )

    private const val LENGTH_CONSTRAINT = "org.hibernate.validator.constraints.Length"

    private val PATTERN_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.Pattern",
        "javax.validation.constraints.Pattern",
    )

    private val NOT_EMPTY_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.NotEmpty",
        "javax.validation.constraints.NotEmpty",
    )

    private val NOT_BLANK_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.NotBlank",
        "javax.validation.constraints.NotBlank",
    )

    private val REQUIRED_CONSTRAINTS = setOf(
        "jakarta.validation.constraints.NotNull",
        "javax.validation.constraints.NotNull",
    ) + NOT_EMPTY_CONSTRAINTS + NOT_BLANK_CONSTRAINTS

    private fun AbstractDescriptor.constraints() = attributes.validationConstraints

    private fun AbstractDescriptor.maybeConstraint(types: Set<String>) = constraints().firstOrNull { it.name in types }

    fun isRequired(descriptor: AbstractDescriptor) =
        descriptor.constraints().any { REQUIRED_CONSTRAINTS.contains(it.name) } || !descriptor.optional

    private fun AbstractDescriptor.maybePatternConstraint() = maybeConstraint(PATTERN_CONSTRAINTS)

    private fun Constraint.isSizeConstraint() = name in SIZE_CONSTRAINTS || name == LENGTH_CONSTRAINT

    private fun <T : Comparable<T>> AbstractDescriptor.maybeMinConstraint(transform: (Constraint) -> T?) =
        constraints().mapNotNull { transform(it) }.maxOrNull()

    private fun <T : Comparable<T>> AbstractDescriptor.maybeMaxConstraint(transform: (Constraint) -> T?) =
        constraints().mapNotNull { transform(it) }.minOrNull()

    fun maybeMinSize(descriptor: AbstractDescriptor?) = descriptor?.maybeMinConstraint {
        when (it.name) {
            in NOT_EMPTY_CONSTRAINTS, in NOT_BLANK_CONSTRAINTS -> BigDecimal.ONE
            else -> if (it.isSizeConstraint()) toBigDecimal(it.configuration["min"]) else null
        }
    }

    fun maybeMaxSize(descriptor: AbstractDescriptor?) =
        descriptor?.maybeMaxConstraint {
            if (it.isSizeConstraint()) toBigDecimal(it.configuration["max"]) else null
        }

    fun maybeMinNumber(descriptor: AbstractDescriptor) = descriptor.maybeMinConstraint {
        if (it.name in MIN_CONSTRAINTS || it.name in DECIMAL_MIN_CONSTRAINTS)
            toBigDecimal(it.configuration["value"])
        else null
    }

    fun maybeMaxNumber(descriptor: AbstractDescriptor) = descriptor.maybeMaxConstraint {
        if (it.name in MAX_CONSTRAINTS || it.name in DECIMAL_MAX_CONSTRAINTS)
            toBigDecimal(it.configuration["value"])
        else null
    }

    fun maybePattern(descriptor: AbstractDescriptor?) =
        descriptor?.maybePatternConstraint()?.let { it.configuration["regexp"] as? String }

    private fun toBigDecimal(value: Any?) = when (value) {
        is String -> BigDecimal(value)
        is Int -> value.toBigDecimal()
        is Long -> value.toBigDecimal()
        is Float -> BigDecimal(value.toString())
        is Double -> value.toBigDecimal()
        else -> value as? BigDecimal
    }
}
