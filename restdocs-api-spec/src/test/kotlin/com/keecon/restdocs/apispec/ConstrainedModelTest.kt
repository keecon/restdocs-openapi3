package com.keecon.restdocs.apispec

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.springframework.restdocs.constraints.Constraint
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

internal class ConstrainedModelTest {

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve field constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve one level nested field constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve two level nested field constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nested.nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve parameter constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve one level nested parameter constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve two level nested parameter constraints`() {
        val model = ConstrainedModel(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nested.nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve composite field constraints`() {
        val model = ConstrainedModel(CompositeConstrains::class.java)

        var descriptor = model.withPath("nonBlank")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotBlank::class.java.name)

        descriptor = model.withPath("string.nonEmpty")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("number.nonZero")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(Min::class.java.name)

        descriptor = model.withPath("numberList")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("nonEmptyList")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("string.nested.nonEmpty")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("number.nested.number.nonZero")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(Min::class.java.name)

        descriptor = model.withPath("number.nested.numberList")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("nonEmptyList")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("nonEmptyList[].nonEmpty")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("nonEmptyList[][].nonEmpty")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)

        descriptor = model.withPath("[].nonBlank")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotBlank::class.java.name)

        descriptor = model.withPath("[][].nonBlank")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotBlank::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve enum values, format and encoding style`() {
        val model = ConstrainedModel(EnumConstrains::class.java)

        var descriptor = model.withName("someEnum")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotBlank::class.java.name)
        then((descriptor.attributes[Attributes.ENUM_VALUES_KEY] as? List<String>)).isEqualTo(
            listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE")
        )

        descriptor = model.withName("enumList").attributes(
            Attributes.items(DataType.STRING, enums = listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE")),
        )
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name, Max::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as Map<String, *>))
            .containsKey("type")
            .doesNotContainKey("format")
            .containsKey(Attributes.ENUM_VALUES_KEY)
            .containsValue(DataType.STRING)
            .containsValue(listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE"))

        descriptor = model.withName("enumList[]")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name, Max::class.java.name)
        // TODO(iwaltgen): array items type?
        // then((descriptor.attributes[Attributes.ITEMS_KEY] as Map<String, *>))
        //     .containsKey("type")
        //     .doesNotContainKey("format")
        //     .containsKey(Attributes.ENUM_VALUES_KEY)
        //     .containsValue(DataType.STRING)
        //     .containsValue(listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE"))
    }

    private data class NonEmptyConstraints(
        @field:NotEmpty val nonEmpty: String,
        @field:Valid val nested: NonEmptyConstraints?,
    )

    private data class NonZeroConstrains(
        @field:Min(1) val nonZero: Int,
        val nested: CompositeConstrains?
    )

    private data class CompositeConstrains(
        @field:NotBlank val nonBlank: String,
        @field:Valid val string: NonEmptyConstraints?,
        @field:Valid val number: NonZeroConstrains?,
        @field:NotEmpty val numberList: List<Int>?,
        @field:Valid @field:NotEmpty val nonEmptyList: List<NonEmptyConstraints>?,
        @field:Valid @field:NotEmpty val nonZeroNestedList: List<List<NonZeroConstrains>>?,
    )

    private data class EnumConstrains(
        @field:NotBlank val someEnum: SomeEnum,
        @field:Valid @field:NotEmpty @field:Max(10) val enumList: List<SomeEnum>?,
    )

    private enum class SomeEnum {
        FIRST_VALUE,
        SECOND_VALUE,
        THIRD_VALUE
    }
}
