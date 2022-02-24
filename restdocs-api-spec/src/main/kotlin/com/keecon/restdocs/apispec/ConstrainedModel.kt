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
class ConstrainedModel(private val rootClass: Class<*>) {
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
     * @param propertyPath name of the property of the bean that is used to get the field constraints
     */
    fun withMappedPath(path: String, propertyPath: String): FieldDescriptor =
        addConstraints(fieldWithPath(path), propertyPath)

    /**
     * Create a parameter description with constraints for bean property with a name differing from the name
     * @param name name of the parameter
     * @param propertyPath name of the property of the bean that is used to get the parameter constraints
     */
    fun withMappedName(name: String, propertyPath: String): ParameterDescriptor =
        addConstraints(parameterWithName(name), propertyPath)

    private fun <T : AbstractDescriptor<T>> addConstraints(descriptor: T, propertyPath: String): T {
        val (propertyName, targetClass) = propertyNameWithClass(propertyPath)
        if (targetClass == null) return descriptor

        return descriptor.attributes(
            Attributes.key(CONSTRAINTS_KEY)
                .value(this.validatorConstraintResolver.resolveForProperty(propertyName, targetClass))
        )
    }

    private fun propertyNameWithClass(path: String): Pair<String, Class<*>?> {
        val hierarchyPropertyNames = path.split(DOT_NOTATION_DELIMITER)
        if (hierarchyPropertyNames.size == 1) return Pair(removeArraySymbol(path), rootClass)

        var targetClass: Class<*>? = rootClass
        for (name in hierarchyPropertyNames.filter { removeArraySymbol(it).isNotEmpty() }.dropLast(1)) {
            var hierarchyClass = targetClass
            val propertyName = removeArraySymbol(name)
            while (hierarchyClass != null) {
                val field = hierarchyClass.declaredFields.firstOrNull { it.name == propertyName }
                if (field == null) hierarchyClass = hierarchyClass.superclass
                else {
                    targetClass = getFieldType(name, field)
                    break
                }
            }
        }
        return Pair(removeArraySymbol(hierarchyPropertyNames.last()), targetClass)
    }

    private fun removeArraySymbol(name: String) = name.substringBefore(ARRAY_SYMBOL)

    private fun getFieldType(name: String, field: Field) =
        if (name.lastIndexOf(ARRAY_SYMBOL) == -1) field.type
        else getArrayItemType(field)

    private fun getArrayItemType(field: Field): Class<*>? {
        val type = field.genericType as? ParameterizedType
        return type?.actualTypeArguments?.first() as? Class<*>
    }

    companion object {
        private const val CONSTRAINTS_KEY = "validationConstraints"
        private const val DOT_NOTATION_DELIMITER = "."
        private const val ARRAY_SYMBOL = "[]"
    }
}
