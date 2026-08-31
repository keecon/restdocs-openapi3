package com.keecon.restdocs.apispec.generator

import com.keecon.restdocs.apispec.model.RFC3339_DATETIME_FORMAT
import com.keecon.restdocs.apispec.model.Attributes
import com.keecon.restdocs.apispec.model.DataFormat
import com.keecon.restdocs.apispec.model.ParameterDescriptor
import com.keecon.restdocs.apispec.model.TypeDescriptor
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class SchemaExtensionsTest {

    @Test
    fun `should convert a Long number default without losing precision`() {
        val schema = OpenApi3Generator.simpleTypeToSchema(
            descriptor(type = "NUMBER", defaultValue = 9_007_199_254_740_993L)
        ) as NumberSchema

        then(schema.default).isEqualTo(BigDecimal("9007199254740993"))
    }

    @Test
    fun `should convert a Float number default using its decimal representation`() {
        val schema = OpenApi3Generator.simpleTypeToSchema(
            descriptor(type = "NUMBER", defaultValue = 0.1F)
        ) as NumberSchema

        then(schema.default).isEqualTo(BigDecimal("0.1"))
    }

    @Test
    fun `should convert Long and Float number enum values`() {
        val schema = OpenApi3Generator.simpleTypeToSchema(
            descriptor(
                type = "NUMBER",
                attributes = Attributes(enumValues = listOf(9_007_199_254_740_993L, 0.1F))
            )
        ) as NumberSchema

        then(schema.enum).containsExactly(BigDecimal("9007199254740993"), BigDecimal("0.1"))
    }

    @Test
    fun `should convert a Long integer only when it is in Int range`() {
        val schema = OpenApi3Generator.simpleTypeToSchema(
            descriptor(type = "INTEGER", defaultValue = Int.MAX_VALUE.toLong())
        ) as IntegerSchema

        then(schema.default).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `should reject a Long integer outside Int range`() {
        assertThrows<ArithmeticException> {
            OpenApi3Generator.simpleTypeToSchema(
                descriptor(type = "INTEGER", defaultValue = Int.MAX_VALUE.toLong() + 1)
            )
        }
    }

    @Test
    fun `should represent a date as a formatted string`() {
        val schema = OpenApi3Generator.simpleTypeToSchema(
            TypeDescriptor(type = "STRING", attributes = Attributes(format = DataFormat.DATE.lowercase()))
        )

        then(schema?.type).isEqualTo("string")
        then(schema?.format).isEqualTo("date")
    }

    @Test
    fun `should expose RFC 3339 marker as OpenAPI date time`() {
        val schema = OpenApi3Generator.simpleTypeToSchema(
            TypeDescriptor(
                type = "STRING",
                attributes = Attributes(format = RFC3339_DATETIME_FORMAT)
            )
        )

        then(schema?.type).isEqualTo("string")
        then(schema?.format).isEqualTo("date-time")
    }

    @Test
    fun `should reject DATE as a raw descriptor type`() {
        assertThrows<IllegalArgumentException> {
            OpenApi3Generator.simpleTypeToSchema(TypeDescriptor(type = "DATE"))
        }
    }

    private fun descriptor(
        type: String,
        defaultValue: Any? = null,
        attributes: Attributes = Attributes()
    ) = ParameterDescriptor(
        name = "value",
        description = "value",
        type = type,
        defaultValue = defaultValue,
        optional = true,
        ignored = false,
        attributes = attributes
    )
}
