package com.keecon.restdocs.apispec.model

object ConstraintResolver {

    private const val MIN_CONSTRAINT = "javax.validation.constraints.Min"

    private const val MAX_CONSTRAINT = "javax.validation.constraints.Max"

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

    fun maybeMinSize(descriptor: AbstractDescriptor?) =
        descriptor?.constraints()
            ?.mapNotNull {
                when (it.name) {
                    NOT_EMPTY_CONSTRAINT,
                    NOT_BLANK_CONSTRAINT -> 1
                    SIZE_CONSTRAINT -> it.configuration["min"] as? Int
                    else -> null
                }
            }
            ?.maxOrNull()

    fun maybeMaxSize(descriptor: AbstractDescriptor?) =
        descriptor?.maybeSizeConstraint()?.let { it.configuration["max"] as? Int }

    fun maybePattern(descriptor: AbstractDescriptor?) =
        descriptor?.maybePatternConstraint()?.let { it.configuration["pattern"] as? String }

    fun maybeMinInt(descriptor: AbstractDescriptor) =
        descriptor.maybeConstraint(MIN_CONSTRAINT)?.let { it.configuration["value"] as? Int }

    fun maybeMaxInt(descriptor: AbstractDescriptor) =
        descriptor.maybeConstraint(MAX_CONSTRAINT)?.let { it.configuration["value"] as? Int }
}
