package com.keecon.restdocs.apispec.jsonschema

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class Rfc3339DateTimeFormatValidatorTest {

    private val validator = Rfc3339DateTimeFormatValidator()

    @Test
    fun `should accept supported RFC 3339 date time profile`() {
        listOf(
            "2026-08-30T15:30:00+09:00",
            "2026-08-30T06:30:00Z",
            "2026-08-30T06:30:00.1Z",
            "2026-08-30T06:30:00.123456789Z",
            "2026-08-30t06:30:00z",
            "2026-08-30T06:30:00.12345678901234567890Z",
            "2026-08-30T06:30:00+23:59",
            "2026-08-30T06:30:00-00:00",
        ).forEach { value ->
            then(validator.validate(value)).describedAs(value).isEmpty()
        }
    }

    @Test
    fun `should reject invalid RFC 3339 date times`() {
        listOf(
            "2026-08-30T15:30+09:00",
            "2026-08-30T15:30:00",
            "2026-08-30 15:30:00+09:00",
            "2026-02-30T06:30:00Z",
            "2026-08-30T24:00:00Z",
            "1990-12-31T23:59:60Z",
            "1990-12-31T15:59:60-08:00",
            "1990-07-01T08:59:60+09:00",
            "1991-06-30T23:59:60Z",
            "1990-12-31T23:58:60Z",
            "1990-07-01T00:00:60Z",
            "1990-06-30T23:59:60+09:00",
            "2026-08-30T06:30:00+24:00",
            "2026-08-30T06:30:00+23:60",
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
