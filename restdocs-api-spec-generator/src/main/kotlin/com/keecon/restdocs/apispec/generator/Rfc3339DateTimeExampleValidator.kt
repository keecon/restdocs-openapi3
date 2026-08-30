package com.keecon.restdocs.apispec.generator

import com.keecon.restdocs.apispec.jsonschema.JsonSchemaGenerator
import com.keecon.restdocs.apispec.model.Attributes
import com.keecon.restdocs.apispec.model.DataFormat
import com.keecon.restdocs.apispec.model.FieldDescriptor
import com.keecon.restdocs.apispec.model.ResourceModel
import com.keecon.restdocs.apispec.model.TypeDescriptor
import org.everit.json.schema.ValidationException

internal object Rfc3339DateTimeExampleValidator {

    private val jsonSchemaGenerator = JsonSchemaGenerator()

    fun validate(resources: List<ResourceModel>) {
        resources.forEach { resource ->
            validateExample(
                operationId = resource.operationId,
                direction = "request",
                contentType = resource.request.contentType,
                example = resource.request.example,
                fieldDescriptors = resource.request.requestFields,
            )
            validateExample(
                operationId = resource.operationId,
                direction = "response",
                contentType = resource.response.contentType,
                example = resource.response.example,
                fieldDescriptors = resource.response.responseFields,
            )
        }
    }

    private fun validateExample(
        operationId: String,
        direction: String,
        contentType: String?,
        example: String?,
        fieldDescriptors: List<FieldDescriptor>,
    ) {
        if (contentType?.contains("json") != true || example == null) return

        val dateTimeDescriptors = fieldDescriptors
            .filter(::isDateTime)
            .map(::toValidationDescriptor)
        if (dateTimeDescriptors.isEmpty()) return

        try {
            jsonSchemaGenerator.validate(dateTimeDescriptors, example)
        } catch (exception: ValidationException) {
            throw IllegalArgumentException(
                "Operation '$operationId' $direction example is not a strict RFC 3339 date-time " +
                    "at ${exception.pointerToViolation}: ${exception.errorMessage}",
                exception,
            )
        }
    }

    private fun isDateTime(descriptor: FieldDescriptor): Boolean =
        descriptor.attributes.format.isDateTime() || descriptor.attributes.items?.attributes?.format.isDateTime()

    private fun String?.isDateTime() = equals(DataFormat.DATETIME.lowercase(), ignoreCase = true)

    private fun toValidationDescriptor(descriptor: FieldDescriptor) = FieldDescriptor(
        path = descriptor.path,
        description = descriptor.description,
        type = descriptor.type,
        optional = true,
        ignored = false,
        attributes = validationAttributes(descriptor.attributes),
    )

    private fun validationAttributes(attributes: Attributes) = Attributes(
        format = attributes.format,
        items = attributes.items?.let { item ->
            TypeDescriptor(
                type = item.type,
                description = item.description,
                optional = true,
                attributes = Attributes(format = item.attributes.format),
            )
        },
    )
}
