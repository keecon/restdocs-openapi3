package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.model.DataFormat
import org.everit.json.schema.FormatValidator
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Optional

class StringAnyFormatValidator(val format: String) : FormatValidator {

    override fun validate(subject: String?): Optional<String> {
        if (format != DataFormat.DATE.lowercase()) return Optional.empty()
        if (subject == null) return Optional.empty()

        return try {
            LocalDate.parse(subject)
            Optional.empty()
        } catch (@Suppress("SwallowedException") exception: DateTimeParseException) {
            Optional.of("'$subject' is not a valid ISO-8601 date (expected format: yyyy-MM-dd)")
        }
    }

    override fun formatName(): String {
        return format
    }
}
