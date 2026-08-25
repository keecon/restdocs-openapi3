package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.model.DataFormat
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class StringAnyFormatValidatorTest {

    @Test
    fun `should accept an ISO local date`() {
        val result = StringAnyFormatValidator(DataFormat.DATE.lowercase()).validate("2024-02-29")

        then(result).isEmpty()
    }

    @Test
    fun `should reject an invalid ISO local date`() {
        val result = StringAnyFormatValidator(DataFormat.DATE.lowercase()).validate("2024-02-30")

        then(result).isPresent()
    }

    @Test
    fun `should continue accepting arbitrary values for custom formats`() {
        val result = StringAnyFormatValidator("custom").validate("any value")

        then(result).isEmpty()
    }
}
