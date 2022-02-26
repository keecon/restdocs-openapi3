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
    fun items(type: DataType, format: DataFormat? = null, enums: List<*>? = null): Attribute {
        val items = mutableMapOf<String, Any>(TYPE_KEY to type)
        if (format != null) items[FORMAT_KEY] = format
        if (enums != null) items[ATTRIBUTES_KEY] = mapOf(ENUM_VALUES_KEY to enums)
        return key(ITEMS_KEY).value(items)
    }

    @JvmStatic
    fun encoding(style: EncodingStyle, explode: Boolean? = null, allowReserved: Boolean? = null): Attribute {
        val items = mutableMapOf<String, Any>(ENCODING_STYLE_KEY to style)
        if (explode != null) items[ENCODING_EXPLODE_KEY] = explode
        if (allowReserved != null) items[ENCODING_ALLOW_RESERVED_KEY] = allowReserved
        return key(ENCODING_KEY).value(items)
    }

    internal const val ATTRIBUTES_KEY = "attributes"
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
