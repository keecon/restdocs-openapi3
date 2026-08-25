package com.keecon.restdocs.apispec.jsonschema

import com.keecon.restdocs.apispec.model.FieldDescriptor
import org.assertj.core.api.BDDAssertions.then
import org.everit.json.schema.StringSchema
import org.junit.jupiter.api.Test

class FieldDescriptorWithSchemaTest {

    @Test
    fun `should merge optional state when either descriptor is optional`() {
        val required = FieldDescriptorWithSchema.fromFieldDescriptor(descriptor(optional = false, ignored = false))
        val optional = FieldDescriptorWithSchema.fromFieldDescriptor(descriptor(optional = true, ignored = false))

        then(required.merge(optional).optional).isTrue()
        then(optional.merge(required).optional).isTrue()
    }

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

    @Test
    fun `should retain one schema builder when duplicate descriptors have the same type`() {
        val first = FieldDescriptorWithSchema.fromFieldDescriptor(descriptor(ignored = false))

        val merged = first.merge(descriptor(ignored = false))

        then(merged.jsonSchemaType()).isInstanceOf(StringSchema::class.java)
    }

    private fun descriptor(optional: Boolean = false, ignored: Boolean) = FieldDescriptor(
        path = "field",
        description = "field",
        type = "STRING",
        optional = optional,
        ignored = ignored
    )
}
