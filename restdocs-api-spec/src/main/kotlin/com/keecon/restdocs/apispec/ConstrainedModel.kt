package com.keecon.restdocs.apispec

import org.springframework.restdocs.constraints.ValidatorConstraintResolver
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.snippet.AbstractDescriptor
import org.springframework.restdocs.snippet.Attributes
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

/**
 * ConstrainedModel can be used to add constraint information to a [FieldDescriptor], [ParameterDescriptor]
 * If these are present in the descriptor they are used to enrich the generated type information (e.g. JsonSchema)
 */
class ConstrainedModel(private val rootType: Class<*>) {
    private val validatorConstraintResolver = ValidatorConstraintResolver()

    /**
     * Create a field description with constraints for bean property with the same name
     * @param path json path of the field
     */
    fun withPath(path: String): FieldDescriptor = withMappedPath(path, path)

    /**
     * Create a parameter description with constraints for bean property with the same name
     * @param name name of the parameter
     */
    fun withName(name: String): ParameterDescriptor = withMappedName(name, name)

    /**
     * Create a field description with constraints for bean property with a name differing from the path
     * @param path json path of the field
     * @param propsPath name of the property of the bean that is used to get the field constraints
     */
    fun withMappedPath(path: String, propsPath: String): FieldDescriptor =
        addConstraints(fieldWithPath(path), propsPath)

    /**
     * Create a parameter description with constraints for bean property with a name differing from the name
     * @param name name of the parameter
     * @param propsPath name of the property of the bean that is used to get the parameter constraints
     */
    fun withMappedName(name: String, propsPath: String): ParameterDescriptor =
        addConstraints(parameterWithName(name), propsPath)

    private fun <T : AbstractDescriptor<T>> addConstraints(descriptor: T, propsPath: String): T {
        val (propName, objectType) = propertyWithObjectType(propsPath)
        if (objectType == null) {
            return descriptor
        }

        val propType = propertyType(objectType, propName)
        if (propType?.isEnum == true) {
            // TODO(iwaltgen): array type?
            if (descriptor is FieldDescriptor) {
                descriptor.type(ENUM_TYPE)
            }
            descriptor.attributes(
                Attributes.key(ENUM_VALUES_KEY).value(propType.enumConstants.map(Any::toString))
            )
        }

        return descriptor.attributes(
            Attributes.key(CONSTRAINTS_KEY)
                .value(this.validatorConstraintResolver.resolveForProperty(actualPropName(propName), objectType))
        )
    }

    private fun propertyWithObjectType(path: String): Pair<String, Class<*>?> {
        val propNames = path.split(DOT_NOTATION_DELIMITER)
        if (propNames.size == 1) return Pair(path, rootType)

        var objectType: Class<*>? = rootType
        for (name in propNames.filter { actualPropName(it).isNotEmpty() }.dropLast(1)) {
            propertyType(objectType, name)?.let { objectType = it }
        }
        return Pair(propNames.last(), objectType)
    }

    private fun propertyType(objectType: Class<*>?, name: String): Class<*>? {
        var currentType = objectType
        val propName = actualPropName(name)
        while (currentType != null) {
            val field = currentType.declaredFields.firstOrNull { it.name == propName }
            if (field == null) currentType = currentType.superclass
            else {
                return fieldType(field, name)
            }
        }
        return null
    }

    private fun actualPropName(name: String) = name.substringBefore(ARRAY_SYMBOL)

    private fun isArrayType(name: String) = name.lastIndexOf(ARRAY_SYMBOL) != -1

    private fun fieldType(field: Field, name: String) =
        if (!isArrayType(name)) field.type
        else genericFirstItemType(field)

    private fun genericFirstItemType(field: Field): Class<*>? {
        val type = field.genericType as? ParameterizedType
        return type?.actualTypeArguments?.first() as? Class<*>
    }

    companion object {
        private const val CONSTRAINTS_KEY = "validationConstraints"
        private const val ENUM_VALUES_KEY = "enumValues"
        // private const val ITEMS_KEY = "items"

        private const val ENUM_TYPE = "enum"
        private const val DOT_NOTATION_DELIMITER = "."
        private const val ARRAY_SYMBOL = "[]"
    }
}
