package com.keecon.restdocs.apispec

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.springframework.restdocs.constraints.Constraint
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

internal class ConstrainedModelTest {

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve field constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nonEmpty")

        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve one level nested field constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nested.nonEmpty")

        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve two level nested field constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nested.nested.nonEmpty")

        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve parameter constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nonEmpty")

        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve one level nested parameter constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nested.nonEmpty")

        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve two level nested parameter constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nested.nested.nonEmpty")

        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve composite field constraints`() {
        val model = ConstrainedModel(CompositeConstrains::class.java)

        var descriptor = model.withName("string.nonEmpty")
        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withName("number.nonZero")
        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(Min::class.java.name)

        descriptor = model.withName("list")
        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name, Size::class.java.name)

        descriptor = model.withName("string.nested.nonEmpty")
        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withName("number.nested.number.nonZero")
        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(Min::class.java.name)

        descriptor = model.withName("number.nested.list")
        then(descriptor.attributes).containsKey("validationConstraints")
        then((descriptor.attributes["validationConstraints"] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name, Size::class.java.name)
    }

    private data class CompositeConstrains(
        @field:Valid val string: NonEmptyConstraints?,

        @field:Valid val number: NonZeroConstrains?,

        @field:NotEmpty
        @field:Size(min = 1, max = 10)
        val list: List<Int>?,
    )

    private data class NonEmptyConstraints(
        @field:NotEmpty val nonEmpty: String,
        @field:Valid val nested: NonEmptyConstraints?,
    )

    private data class NonZeroConstrains(
        @field:Min(1) val nonZero: Int,
        val nested: CompositeConstrains?
    )
}
