package com.keecon.restdocs.apispec.generator

import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.ConstraintResolver
import com.keecon.restdocs.apispec.model.EncodingStyle
import io.swagger.v3.oas.models.parameters.Parameter

internal object ParameterExtensions {

    internal fun Parameter.applyProperties(descriptor: AbstractDescriptor) = apply {
        when (descriptor.attributes.encoding?.style?.lowercase()) {
            EncodingStyle.MATRIX.lowercase() -> style = Parameter.StyleEnum.MATRIX
            EncodingStyle.LABEL.lowercase() -> style = Parameter.StyleEnum.LABEL
            EncodingStyle.FORM.lowercase() -> style = Parameter.StyleEnum.FORM
            EncodingStyle.SIMPLE.lowercase() -> style = Parameter.StyleEnum.SIMPLE
            else -> Unit
        }
        descriptor.attributes.encoding?.explode?.let { explode = it }
        descriptor.attributes.encoding?.allowReserved?.let { allowReserved = it }

        if (ConstraintResolver.isRequired(descriptor)) required = true
    }
}
