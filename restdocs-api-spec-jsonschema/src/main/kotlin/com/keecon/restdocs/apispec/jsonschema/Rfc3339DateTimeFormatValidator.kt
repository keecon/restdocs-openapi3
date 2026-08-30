package com.keecon.restdocs.apispec.jsonschema

import org.everit.json.schema.FormatValidator
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Optional

internal class Rfc3339DateTimeFormatValidator : FormatValidator {

    override fun validate(subject: String?): Optional<String> {
        if (subject == null) return Optional.empty()
        if (!WIRE_SHAPE.matches(subject)) return invalid(subject)

        return try {
            OffsetDateTime.parse(subject, PARSER)
            Optional.empty()
        } catch (@Suppress("SwallowedException") exception: DateTimeParseException) {
            invalid(subject)
        }
    }

    override fun formatName() = "date-time"

    private fun invalid(subject: String) = Optional.of(
        "'$subject' is not a strict RFC 3339 date-time " +
            "(expected yyyy-MM-dd'T'HH:mm:ss[.fraction](Z|+/-HH:mm))"
    )

    private companion object {
        val WIRE_SHAPE = Regex(
            """^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})$"""
        )
        val PARSER: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withResolverStyle(ResolverStyle.STRICT)
    }
}
