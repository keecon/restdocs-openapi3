package com.keecon.restdocs.apispec.generator

import com.keecon.restdocs.apispec.jsonschema.JsonSchemaGenerator
import com.keecon.restdocs.apispec.model.Attributes
import com.keecon.restdocs.apispec.model.FieldDescriptor
import com.keecon.restdocs.apispec.model.RFC3339_DATETIME_FORMAT
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
        if (!contentType.isJsonContentType() || example == null) return

        val dateTimeDescriptors = fieldDescriptors
            .filterNot { it.ignored }
            .filter(::isRfc3339DateTime)
            .map(::toValidationDescriptor)
        if (dateTimeDescriptors.isEmpty()) return
        val dateTimePointers = dateTimeDescriptors.map(::dateTimePointer)

        try {
            jsonSchemaGenerator.validate(dateTimeDescriptors, example)
        } catch (exception: ValidationException) {
            val violation = exception.leafViolations()
                .firstOrNull { candidate ->
                    dateTimePointers.any { pointer -> pointer.matches(candidate.pointerToViolation) }
                }
                ?: return
            throw IllegalArgumentException(
                "Operation '$operationId' $direction example does not match " +
                    "the supported RFC 3339 date-time profile " +
                    "at ${violation.pointerToViolation}: ${violation.errorMessage}",
                exception,
            )
        }
    }

    private fun isRfc3339DateTime(descriptor: FieldDescriptor): Boolean =
        descriptor.attributes.format.isRfc3339DateTime() ||
            descriptor.attributes.items?.attributes?.format.isRfc3339DateTime()

    private fun String?.isRfc3339DateTime() =
        equals(RFC3339_DATETIME_FORMAT, ignoreCase = true)

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

    private fun ValidationException.leafViolations(): Sequence<ValidationException> =
        if (causingExceptions.isEmpty()) sequenceOf(this)
        else causingExceptions.asSequence().flatMap { it.leafViolations() }

    private fun dateTimePointer(descriptor: FieldDescriptor): Regex {
        val validatesArrayItems = descriptor.attributes.items?.attributes?.format.isRfc3339DateTime()
        val path = if (validatesArrayItems && !descriptor.path.endsWithArraySegment()) {
            "${descriptor.path}[]"
        } else {
            descriptor.path
        }
        val pointerSegments = pathSegments(path).joinToString(separator = "") { segment ->
            val arraySegment = ARRAY_SEGMENT.matchEntire(segment)
            val pointerSegment = if (arraySegment == null) {
                Regex.escape(segment.toJsonPointerToken())
            } else {
                "\\d+"
            }
            "/$pointerSegment"
        }
        return Regex("^#$pointerSegments$")
    }

    private fun String.endsWithArraySegment(): Boolean =
        ARRAY_SEGMENT.findAll(this).lastOrNull()?.range?.last == lastIndex

    private fun pathSegments(path: String): List<String> {
        val segments = mutableListOf<String>()
        var previous = 0
        BRACKETS_AND_ARRAY.findAll(path).forEach { match ->
            if (previous != match.range.first) {
                segments += dotSeparatedSegments(path.substring(previous, match.range.first))
            }
            segments += match.groups[1]?.value ?: match.value
            previous = match.range.last + 1
        }
        if (previous < path.length) {
            segments += dotSeparatedSegments(path.substring(previous))
        }
        return segments
    }

    private fun dotSeparatedSegments(path: String) = path.split('.').filter(String::isNotEmpty)

    private fun String.toJsonPointerToken() = replace("~", "~0").replace("/", "~1")

    private val BRACKETS_AND_ARRAY = Regex("""\['(.+?)']|\[([0-9]+|\*)?]""")
    private val ARRAY_SEGMENT = Regex("""\[([0-9]+|\*)?]""")
}
