package com.keecon.restdocs.apispec.generator

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class ContentTypeExtensionsTest {

    @Test
    fun `should recognize JSON media types case insensitively`() {
        listOf(
            "application/json",
            "Application/JSON",
            "application/problem+json",
            "application/hal+JSON; charset=UTF-8",
        ).forEach { contentType ->
            then(contentType.isJsonContentType()).describedAs(contentType).isTrue()
        }
    }

    @Test
    fun `should reject non JSON media types`() {
        listOf<String?>(
            null,
            "",
            "text/plain",
            "application/notjson",
            "application/json-seq",
        ).forEach { contentType ->
            then(contentType.isJsonContentType()).describedAs(contentType).isFalse()
        }
    }
}
