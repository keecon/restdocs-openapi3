package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.jsonschema.JsonFieldPath.Companion.compile
import com.keecon.restdocs.apispec.model.Attributes
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.util.Collections.emptyList

class JsonFieldPathTest {

    @Test
    fun should_get_remaining_segments() {
        with(
            compile(
                FieldDescriptorWithSchema(
                    "a.b.c", "", "string", false, false,
                    Attributes()
                )
            )
        ) {
            then(remainingSegments(listOf("a"))).contains("b", "c")
            then(remainingSegments(listOf("a", "b"))).contains("c")
            then(remainingSegments(listOf("a", "b", "c"))).isEmpty()
            then(remainingSegments(listOf("d", "e", "c"))).contains("a", "b", "c")
            then(remainingSegments(emptyList())).contains("a", "b", "c")
        }
    }
}
