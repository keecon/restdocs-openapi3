package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.model.FieldDescriptor
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class FieldDescriptorWithSchemaTest {

    @Test
    fun `should remain ignored when both descriptors are ignored`() {
        val first = FieldDescriptorWithSchema.fromFieldDescriptor(descriptor(ignored = true))

        val merged = first.merge(descriptor(ignored = true))

        then(merged.ignored).isTrue()
    }

    @Test
    fun `should not be ignored when only one descriptor is ignored`() {
        val first = FieldDescriptorWithSchema.fromFieldDescriptor(descriptor(ignored = true))

        val merged = first.merge(descriptor(optional = true, ignored = false))

        then(merged.ignored).isFalse()
    }

    private fun descriptor(optional: Boolean = false, ignored: Boolean) = FieldDescriptor(
        path = "field",
        description = "field",
        type = "STRING",
        optional = optional,
        ignored = ignored
    )
}
