package com.keecon.restdocs.apispec

import org.springframework.restdocs.constraints.Constraint

object Attributes {

    @JvmStatic
    fun key(key: String) = org.springframework.restdocs.snippet.Attributes.key(key)

    @JvmStatic
    fun constraints(constraints: List<Constraint>) = key(CONSTRAINTS_KEY).value(constraints)

    @JvmStatic
    fun enum(values: List<*>) = key(ENUM_VALUES_KEY).value(values)

    @JvmStatic
    fun format(format: DataFormat) = key(FORMAT_KEY).value(format)

    @JvmStatic
    fun items(type: DataType, format: DataFormat? = null, enums: List<*>? = null) =
        key(ITEMS_KEY).value(
            mapOf(
                "type" to type,
                "format" to format,
                ENUM_VALUES_KEY to enums,
            ).filter { it.value != null }
        )

    @JvmStatic
    fun encoding(style: EncodingStyle, explode: Boolean? = null, allowReserved: Boolean? = null) =
        key(ENCODING_KEY).value(
            mapOf(
                "style" to style,
                "explode" to explode,
                "allowReserved" to allowReserved,
            ).filter { it.value != null }
        )

    internal const val CONSTRAINTS_KEY = "validationConstraints"
    internal const val ENUM_VALUES_KEY = "enumValues"
    internal const val FORMAT_KEY = "format"
    internal const val ITEMS_KEY = "items"
    internal const val ENCODING_KEY = "encoding"

    enum class EncodingStyle {
        MATRIX,
        LABEL,
        FORM,
        SIMPLE,
    }
}
