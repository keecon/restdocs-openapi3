package com.keecon.restdocs.apispec

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.springframework.restdocs.constraints.Constraint

internal class ConstraintsTest {

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve field constraints`() {
        val model = Constraints.model(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve one level nested field constraints`() {
        val model = Constraints.model(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve two level nested field constraints`() {
        val model = Constraints.model(NonEmptyConstraints::class.java)
        val descriptor = model.withPath("nested.nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve parameter constraints`() {
        val model = Constraints.model(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve one level nested parameter constraints`() {
        val model = Constraints.model(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve two level nested parameter constraints`() {
        val model = Constraints.model(NonEmptyConstraints::class.java)
        val descriptor = model.withName("nested.nested.nonEmpty")

        then(descriptor.attributes).containsKey(Attributes.CONSTRAINTS_KEY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve composite field constraints`() {
        val model = Constraints.model(CompositeConstraints::class.java)

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
    fun `should resolve array parameter constraints, items attributes`() {
        val model = Constraints.model(ArrayConstraints::class.java)

        var descriptor = model.withName("booleanList[]")
        then(descriptor.type).isEqualTo(DataType.ARRAY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as? Map<String, *>)).isEqualTo(
            mapOf(Attributes.TYPE_KEY to DataType.BOOLEAN)
        )

        descriptor = model.withName("intList[]")
        then(descriptor.type).isEqualTo(DataType.ARRAY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as? Map<String, *>)).isEqualTo(
            mapOf(
                Attributes.TYPE_KEY to DataType.INTEGER,
                Attributes.FORMAT_KEY to DataFormat.INT32,
            )
        )

        descriptor = model.withName("longList[]")
        then(descriptor.type).isEqualTo(DataType.ARRAY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as? Map<String, *>)).isEqualTo(
            mapOf(
                Attributes.TYPE_KEY to DataType.INTEGER,
                Attributes.FORMAT_KEY to DataFormat.INT64,
            )
        )

        descriptor = model.withName("floatList[]")
        then(descriptor.type).isEqualTo(DataType.ARRAY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as? Map<String, *>)).isEqualTo(
            mapOf(Attributes.TYPE_KEY to DataType.NUMBER)
        )

        descriptor = model.withName("doubleList[]")
        then(descriptor.type).isEqualTo(DataType.ARRAY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as? Map<String, *>)).isEqualTo(
            mapOf(Attributes.TYPE_KEY to DataType.NUMBER)
        )

        descriptor = model.withName("stringList[]")
        then(descriptor.type).isEqualTo(DataType.ARRAY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as? Map<String, *>)).isEqualTo(
            mapOf(Attributes.TYPE_KEY to DataType.STRING)
        )

        descriptor = model.withName("objectList[]").attributes(Attributes.items(DataType.OBJECT))
        then(descriptor.type).isEqualTo(DataType.ARRAY)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as? Map<String, *>)).isEqualTo(
            mapOf(Attributes.TYPE_KEY to DataType.OBJECT)
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve parameter type and constraints`() {
        val model = Constraints.model(TypeConstraints::class.java)

        var descriptor = model.withName("someBoolean")
        then(descriptor.type).isEqualTo(DataType.BOOLEAN)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotNull::class.java.name)

        descriptor = model.withName("someInt")
        then(descriptor.type).isEqualTo(DataType.INTEGER)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotNull::class.java.name, Min::class.java.name)
        then((descriptor.attributes[Attributes.FORMAT_KEY] as DataFormat)).isEqualTo(DataFormat.INT32)

        descriptor = model.withName("someLong")
        then(descriptor.type).isEqualTo(DataType.INTEGER)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotNull::class.java.name, Min::class.java.name)
        then((descriptor.attributes[Attributes.FORMAT_KEY] as DataFormat)).isEqualTo(DataFormat.INT64)

        descriptor = model.withName("someFloat")
        then(descriptor.type).isEqualTo(DataType.NUMBER)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotNull::class.java.name, Min::class.java.name)

        descriptor = model.withName("someDouble")
        then(descriptor.type).isEqualTo(DataType.NUMBER)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotNull::class.java.name, Min::class.java.name)

        descriptor = model.withName("someString")
        then(descriptor.type).isEqualTo(DataType.STRING)
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotBlank::class.java.name)

        descriptor = model.withName("someEnum")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotBlank::class.java.name)
        then((descriptor.attributes[Attributes.ENUM_VALUES_KEY] as? List<String>)).isEqualTo(
            listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE")
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should resolve attributes enum values, format and encoding style`() {
        val model = Constraints.model(EnumConstraints::class.java)

        var descriptor = model.withName("someEnum")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotBlank::class.java.name)
        then((descriptor.attributes[Attributes.ENUM_VALUES_KEY] as? List<String>)).isEqualTo(
            listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE")
        )

        descriptor = model.withName("enumList[]")
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name, Max::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as Map<String, *>))
            .containsKey(Attributes.TYPE_KEY)
            .doesNotContainKey(Attributes.FORMAT_KEY)
            .containsKey(Attributes.ATTRIBUTES_KEY)
            .containsValue(DataType.STRING)
            .containsValue(
                mapOf(
                    Attributes.ENUM_VALUES_KEY to listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE")
                )
            )

        descriptor = model.withName("enumList[]").attributes(
            Attributes.encoding(Attributes.EncodingStyle.FORM, explode = true)
        )
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name, Max::class.java.name)
        then((descriptor.attributes[Attributes.ITEMS_KEY] as Map<String, *>))
            .containsKey(Attributes.TYPE_KEY)
            .doesNotContainKey(Attributes.FORMAT_KEY)
            .containsKey(Attributes.ATTRIBUTES_KEY)
            .containsValue(DataType.STRING)
            .containsValue(
                mapOf(
                    Attributes.ENUM_VALUES_KEY to listOf("FIRST_VALUE", "SECOND_VALUE", "THIRD_VALUE")
                )
            )
        then((descriptor.attributes[Attributes.ENCODING_KEY] as Map<String, *>))
            .containsKey(Attributes.ENCODING_STYLE_KEY)
            .containsKey(Attributes.ENCODING_EXPLODE_KEY)
            .doesNotContainKey(Attributes.ENCODING_ALLOW_RESERVED_KEY)
            .containsValue(Attributes.EncodingStyle.FORM)
            .containsValue(true)

        descriptor = model.withName("stringList[]").attributes(
            Attributes.encoding(Attributes.EncodingStyle.SIMPLE, explode = false, allowReserved = true)
        )
        then((descriptor.attributes[Attributes.CONSTRAINTS_KEY] as List<Constraint>).map { it.name })
            .containsExactly(NotEmpty::class.java.name, Max::class.java.name)
        then((descriptor.attributes[Attributes.ENCODING_KEY] as Map<String, *>))
            .containsKey(Attributes.ENCODING_STYLE_KEY)
            .containsKey(Attributes.ENCODING_EXPLODE_KEY)
            .containsKey(Attributes.ENCODING_ALLOW_RESERVED_KEY)
            .containsValue(Attributes.EncodingStyle.SIMPLE)
            .containsValues(true, false)
    }

    private data class NonEmptyConstraints(
        @field:NotEmpty val nonEmpty: String,
        @field:Valid val nested: NonEmptyConstraints?,
    )

    private data class NonZeroConstraints(
        @field:Min(1) val nonZero: Int,
        val nested: CompositeConstraints?
    )

    private data class CompositeConstraints(
        @field:NotBlank val nonBlank: String,
        @field:Valid val string: NonEmptyConstraints?,
        @field:Valid val number: NonZeroConstraints?,
        @field:NotEmpty val numberList: List<Int>?,
        @field:Valid @field:NotEmpty val nonEmptyList: List<NonEmptyConstraints>?,
        @field:Valid @field:NotEmpty val nonZeroNestedList: List<List<NonZeroConstraints>>?,
    )

    private data class ArrayConstraints(
        @field:Valid @field:NotEmpty val booleanList: List<Boolean>?,
        @field:Valid @field:NotEmpty val intList: List<Int>?,
        @field:Valid @field:NotEmpty val longList: List<Long>?,
        @field:Valid @field:NotEmpty val floatList: List<Float>?,
        @field:Valid @field:NotEmpty val doubleList: List<Double>?,
        @field:Valid @field:NotEmpty val stringList: List<String>?,
        @field:Valid @field:NotEmpty val objectList: List<NonEmptyConstraints>?,
    )

    private data class TypeConstraints(
        @field:NotNull val someBoolean: Boolean,
        @field:NotNull @field:Min(1) val someInt: Int,
        @field:NotNull @field:Min(1) val someLong: Long,
        @field:NotNull @field:Min(1) val someFloat: Float,
        @field:NotNull @field:Min(1) val someDouble: Double,
        @field:NotBlank val someString: String,
        @field:NotBlank val someEnum: SomeEnum,
    )

    private data class EnumConstraints(
        @field:NotBlank val someEnum: SomeEnum,
        @field:Valid @field:NotEmpty @field:Max(10) val enumList: List<SomeEnum>?,
        @field:Valid @field:NotEmpty @field:Max(10) val stringList: List<String>?,
    )

    private enum class SomeEnum {
        FIRST_VALUE,
        SECOND_VALUE,
        THIRD_VALUE
    }
}
