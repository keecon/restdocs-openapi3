package com.keecon.restdocs.apispec.generator

import com.keecon.restdocs.apispec.model.Attributes
import com.keecon.restdocs.apispec.model.Constraint
import com.keecon.restdocs.apispec.model.ConstraintResolver
import com.keecon.restdocs.apispec.model.ParameterDescriptor
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ConstraintCompatibilityTest {

    @Test
    fun `should apply Hibernate Length to a string schema`() {
        val schema = stringSchema(
            Constraint("org.hibernate.validator.constraints.Length", mapOf("min" to 2, "max" to 50))
        )

        then(schema.minLength).isEqualTo(2)
        then(schema.maxLength).isEqualTo(50)
    }

    @Test
    fun `should apply legacy javax Size and Pattern to a string schema`() {
        val schema = stringSchema(
            Constraint("javax.validation.constraints.Size", mapOf("min" to 3, "max" to 10)),
            Constraint("javax.validation.constraints.Pattern", mapOf("regexp" to "[a-z]+"))
        )

        then(schema.minLength).isEqualTo(3)
        then(schema.maxLength).isEqualTo(10)
        then(schema.pattern).isEqualTo("[a-z]+")
    }

    @Test
    fun `should apply legacy javax numeric constraints with Long values`() {
        val descriptor = descriptor(
            type = "NUMBER",
            constraints = listOf(
                Constraint("javax.validation.constraints.Min", mapOf("value" to 1L)),
                Constraint("javax.validation.constraints.Max", mapOf("value" to 9_007_199_254_740_993L))
            )
        )

        val schema = OpenApi3Generator.simpleTypeToSchema(descriptor) as NumberSchema

        then(schema.minimum).isEqualTo(BigDecimal("1"))
        then(schema.maximum).isEqualTo(BigDecimal("9007199254740993"))
    }

    @Test
    fun `should treat legacy javax NotNull as required without changing optional`() {
        val descriptor = descriptor(
            type = "STRING",
            constraints = listOf(Constraint("javax.validation.constraints.NotNull", emptyMap()))
        )

        then(descriptor.optional).isTrue()
        then(ConstraintResolver.isRequired(descriptor)).isTrue()
    }

    private fun stringSchema(vararg constraints: Constraint) =
        OpenApi3Generator.simpleTypeToSchema(
            descriptor(type = "STRING", constraints = constraints.toList())
        ) as StringSchema

    private fun descriptor(type: String, constraints: List<Constraint>) = ParameterDescriptor(
        name = "value",
        description = "value",
        type = type,
        optional = true,
        ignored = false,
        attributes = Attributes(validationConstraints = constraints)
    )
}
