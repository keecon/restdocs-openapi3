package com.keecon.restdocs.apispec

import org.springframework.restdocs.constraints.Constraint
import org.springframework.restdocs.snippet.Attributes.Attribute
import org.springframework.restdocs.snippet.Attributes.AttributeBuilder

object Attributes {

    @JvmStatic
    fun key(key: String): AttributeBuilder = org.springframework.restdocs.snippet.Attributes.key(key)

    @JvmStatic
    fun constraints(constraints: List<Constraint>): Attribute = key(CONSTRAINTS_KEY).value(constraints)

    @JvmStatic
    fun enum(values: List<*>): Attribute = key(ENUM_VALUES_KEY).value(values)

    @JvmStatic
    fun format(format: DataFormat): Attribute = key(FORMAT_KEY).value(format)

    @JvmStatic
    fun items(type: DataType, format: DataFormat? = null, enums: List<*>? = null): Attribute =
        key(ITEMS_KEY).value(
            mapOf(
                TYPE_KEY to type,
                FORMAT_KEY to format,
                ENUM_VALUES_KEY to enums,
            ).filter { it.value != null }
        )

    @JvmStatic
    fun encoding(style: EncodingStyle, explode: Boolean? = null, allowReserved: Boolean? = null): Attribute =
        key(ENCODING_KEY).value(
            mapOf(
                ENCODING_STYLE_KEY to style,
                ENCODING_EXPLODE_KEY to explode,
                ENCODING_ALLOW_RESERVED_KEY to allowReserved,
            ).filter { it.value != null }
        )

    internal const val CONSTRAINTS_KEY = "validationConstraints"
    internal const val ENUM_VALUES_KEY = "enumValues"
    internal const val ITEMS_KEY = "items"
    internal const val TYPE_KEY = "type"
    internal const val FORMAT_KEY = "format"
    internal const val ENCODING_KEY = "encoding"
    internal const val ENCODING_STYLE_KEY = "style"
    internal const val ENCODING_EXPLODE_KEY = "explode"
    internal const val ENCODING_ALLOW_RESERVED_KEY = "allowReserved"

    enum class EncodingStyle {
        MATRIX,
        LABEL,
        FORM,
        SIMPLE,
    }
}
