package com.keecon.restdocs.apispec.jsonschema

import org.everit.json.schema.FormatValidator
import java.util.Optional

class StringAnyFormatValidator(val format: String) : FormatValidator {

    override fun validate(subject: String?): Optional<String> {
        return Optional.empty()
    }

    override fun formatName(): String {
        return format
    }
}
