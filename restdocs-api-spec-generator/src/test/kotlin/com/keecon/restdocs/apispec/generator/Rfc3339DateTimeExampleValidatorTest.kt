package com.keecon.restdocs.apispec.generator

import com.keecon.restdocs.apispec.model.Attributes
import com.keecon.restdocs.apispec.model.Constraint
import com.keecon.restdocs.apispec.model.FieldDescriptor
import com.keecon.restdocs.apispec.model.HTTPMethod
import com.keecon.restdocs.apispec.model.RequestModel
import com.keecon.restdocs.apispec.model.ResourceModel
import com.keecon.restdocs.apispec.model.ResponseModel
import com.keecon.restdocs.apispec.model.TypeDescriptor
import org.assertj.core.api.BDDAssertions.thenCode
import org.assertj.core.api.BDDAssertions.thenThrownBy
import org.everit.json.schema.ValidationException
import org.junit.jupiter.api.Test

class Rfc3339DateTimeExampleValidatorTest {

    @Test
    fun `should accept canonical request and response date times`() {
        val resource = resource(
            requestExample = """{"createdAt":"2026-08-30T15:30:00+09:00"}""",
            responseExample =
                """{"createdAt":"2026-08-30T06:30:00Z","history":["2026-08-30T06:30:00.123456789Z"]}""",
            requestFields = listOf(dateTimeField("createdAt")),
            responseFields = listOf(dateTimeField("createdAt"), dateTimeArray("history[]")),
        )

        thenCode { Rfc3339DateTimeExampleValidator.validate(listOf(resource)) }.doesNotThrowAnyException()
    }

    @Test
    fun `should report request operation direction and pointer for invalid date time`() {
        val resource = resource(
            requestExample = """{"createdAt":"2026-08-30T15:30:00"}""",
            requestFields = listOf(dateTimeField("createdAt")),
        )

        thenThrownBy { Rfc3339DateTimeExampleValidator.validate(listOf(resource)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("dates-create")
            .hasMessageContaining("request")
            .hasMessageContaining("#/createdAt")
            .hasMessageContaining("RFC 3339 date-time")
            .hasCauseInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `should report response direction for leaked zone id`() {
        val resource = resource(
            responseExample = """{"createdAt":"2026-08-30T15:30:00+09:00[Asia/Seoul]"}""",
            responseFields = listOf(dateTimeField("createdAt")),
        )

        thenThrownBy { Rfc3339DateTimeExampleValidator.validate(listOf(resource)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("dates-create")
            .hasMessageContaining("response")
            .hasCauseInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `should validate nested and array date times`() {
        val resource = resource(
            requestExample =
                """{"nested":{"createdAt":"2026-02-30T06:30:00Z"},"history":["2026-08-30T06:30:00Z"]}""",
            requestFields = listOf(dateTimeField("nested.createdAt"), dateTimeArray("history[]")),
        )

        thenThrownBy { Rfc3339DateTimeExampleValidator.validate(listOf(resource)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("#/nested/createdAt")
    }

    @Test
    fun `should reject explicit null date time`() {
        val resource = resource(
            requestExample = """{"createdAt":null}""",
            requestFields = listOf(dateTimeField("createdAt", optional = true)),
        )

        thenThrownBy { Rfc3339DateTimeExampleValidator.validate(listOf(resource)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasCauseInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `should ignore absent optional fields unrelated constraints and non JSON bodies`() {
        val impossiblePattern = Constraint(
            "jakarta.validation.constraints.Pattern",
            mapOf("regexp" to "never")
        )
        val resources = listOf(
            resource(
                requestExample = "{}",
                requestFields = listOf(dateTimeField("createdAt", optional = true)),
            ),
            resource(
                requestExample = """{"createdAt":"2026-08-30T06:30:00Z"}""",
                requestFields = listOf(
                    FieldDescriptor(
                        path = "createdAt",
                        description = "",
                        type = "string",
                        attributes = Attributes(
                            validationConstraints = listOf(impossiblePattern),
                            format = "datetime",
                        ),
                    )
                ),
            ),
            resource(
                requestExample = """{"count":"not-an-integer"}""",
                requestFields = listOf(FieldDescriptor("count", "", "integer")),
            ),
            resource(
                requestExample = "createdAt=2026-08-30T15:30:00",
                requestContentType = "application/x-www-form-urlencoded",
                requestFields = listOf(dateTimeField("createdAt")),
            ),
        )

        thenCode { Rfc3339DateTimeExampleValidator.validate(resources) }.doesNotThrowAnyException()
    }

    private fun resource(
        requestExample: String? = null,
        responseExample: String? = null,
        requestContentType: String? = "application/json",
        responseContentType: String? = "application/json",
        requestFields: List<FieldDescriptor> = emptyList(),
        responseFields: List<FieldDescriptor> = emptyList(),
    ) = ResourceModel(
        operationId = "dates-create",
        privateResource = false,
        deprecated = false,
        request = RequestModel(
            path = "/dates",
            method = HTTPMethod.POST,
            contentType = requestContentType,
            securityRequirements = null,
            headers = emptyList(),
            pathParameters = emptyList(),
            queryParameters = emptyList(),
            formParameters = emptyList(),
            requestParts = emptyList(),
            requestFields = requestFields,
            example = requestExample,
        ),
        response = ResponseModel(
            status = 200,
            contentType = responseContentType,
            headers = emptyList(),
            responseFields = responseFields,
            example = responseExample,
        )
    )

    private fun dateTimeField(path: String, optional: Boolean = false) = FieldDescriptor(
        path = path,
        description = "",
        type = "string",
        optional = optional,
        attributes = Attributes(format = "datetime"),
    )

    private fun dateTimeArray(path: String) = FieldDescriptor(
        path = path,
        description = "",
        type = "array",
        attributes = Attributes(
            items = TypeDescriptor(
                type = "string",
                attributes = Attributes(format = "datetime"),
            )
        ),
    )
}
