package com.keecon.restdocs.apispec.jsonschema

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class Rfc3339DateTimeFormatValidatorTest {

    private val validator = Rfc3339DateTimeFormatValidator()

    @Test
    fun `should accept canonical Java compatible RFC 3339 date times`() {
        listOf(
            "2026-08-30T15:30:00+09:00",
            "2026-08-30T06:30:00Z",
            "2026-08-30T06:30:00.1Z",
            "2026-08-30T06:30:00.123456789Z",
            "2026-08-30T06:30:00+18:00",
        ).forEach { value ->
            then(validator.validate(value)).describedAs(value).isEmpty()
        }
    }

    @Test
    fun `should reject non canonical or invalid RFC 3339 date times`() {
        listOf(
            "2026-08-30T15:30+09:00",
            "2026-08-30T15:30:00",
            "2026-08-30 15:30:00+09:00",
            "2026-08-30t06:30:00z",
            "2026-08-30T06:30:00.1234567890Z",
            "2026-02-30T06:30:00Z",
            "2026-08-30T24:00:00Z",
            "2026-08-30T06:30:60Z",
            "2026-08-30T06:30:00+18:01",
            "2026-08-30T15:30:00+09:00[Asia/Seoul]",
        ).forEach { value ->
            then(validator.validate(value)).describedAs(value).isPresent()
        }
    }

    @Test
    fun `should ignore null subjects`() {
        then(validator.validate(null)).isEmpty()
    }
}
