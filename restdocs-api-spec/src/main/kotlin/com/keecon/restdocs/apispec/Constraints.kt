package com.keecon.restdocs.apispec

import com.keecon.restdocs.apispec.ResourceDocumentation.parameterWithName
import org.springframework.restdocs.constraints.ValidatorConstraintResolver
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.snippet.AbstractDescriptor
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

/**
 * Constraints can be used to add constraint information to a [FieldDescriptor], [ParameterDescriptorWithType]
 * If these are present in the descriptor they are used to enrich the generated type information (e.g. JsonSchema)
 */
class Constraints private constructor(private val rootType: Class<*>) {
    private val validatorConstraintResolver = ValidatorConstraintResolver()

    /**
     * Create a field description with constraints for bean property with the same name
     * @param path json path of the field
     */
    fun withPath(path: String) = withMappedPath(path, path)

    /**
     * Create a field description with constraints for bean property with a name differing from the path
     * @param path json path of the field
     * @param propsPath name of the property of the bean that is used to get the field constraints
     */
    fun withMappedPath(path: String, propsPath: String): FieldDescriptor =
        addConstraints(fieldWithPath(path), propsPath)

    /**
     * Create a parameter description with constraints for bean property with the same name
     * @param name name of the parameter
     */
    fun withName(name: String) = withMappedName(name, name)

    /**
     * Create a parameter description with constraints for bean property with a name differing from the name
     * @param name name of the parameter
     * @param propsPath name of the property of the bean that is used to get the parameter constraints
     */
    fun withMappedName(name: String, propsPath: String): ParameterDescriptorWithType =
        addConstraints(parameterWithName(name), propsPath)

    private fun <T : AbstractDescriptor<T>> addConstraints(descriptor: T, propsPath: String): T {
        val (propName, objectType) = propertyWithObjectType(propsPath)
        if (objectType == null) {
            return descriptor
        }

        val propType = propertyType(objectType, propName)
        if (isArrayType(propName)) {
            applyArrayAttributes(descriptor, propType)
        } else {
            applyAttributes(descriptor, propType)
        }

        val constraints = this.validatorConstraintResolver.resolveForProperty(actualPropName(propName), objectType)
        return descriptor.attributes(Attributes.constraints(constraints))
    }

    private fun propertyWithObjectType(path: String): Pair<String, Class<*>?> {
        val propNames = path.split(".")
        if (propNames.size == 1) return Pair(path, rootType)

        var objectType: Class<*>? = rootType
        for (name in propNames.filter { actualPropName(it).isNotEmpty() }.dropLast(1)) {
            propertyType(objectType, name)?.let { objectType = it }
        }
        return Pair(propNames.last(), objectType)
    }

    companion object {

        @JvmStatic
        fun model(rootType: Class<*>) = Constraints(rootType)

        @JvmStatic
        fun propertyType(objectType: Class<*>?, name: String): Class<*>? {
            var currentType = objectType
            val propName = actualPropName(name)
            while (currentType != null) {
                val field = currentType.declaredFields.firstOrNull { it.name == propName }
                if (field == null) {
                    currentType = currentType.superclass
                } else {
                    return fieldType(field, name)
                }
            }
            return null
        }

        @JvmStatic
        fun <T : AbstractDescriptor<T>> applyArrayAttributes(descriptor: T, type: Class<*>?) = descriptor.apply {
            when (this) {
                is HeaderDescriptorWithType -> type(DataType.ARRAY)
                is ParameterDescriptorWithType -> type(DataType.ARRAY)
                is FieldDescriptor -> type(DataType.ARRAY)
            }

            when (type) {
                Boolean::class.javaObjectType,
                Boolean::class.javaPrimitiveType -> {
                    attributes(Attributes.items(DataType.BOOLEAN))
                }
                Float::class.javaObjectType,
                Float::class.javaPrimitiveType,
                Double::class.javaObjectType,
                Double::class.javaPrimitiveType -> {
                    attributes(Attributes.items(DataType.NUMBER))
                }
                Int::class.javaObjectType,
                Int::class.javaPrimitiveType -> {
                    attributes(Attributes.items(DataType.INTEGER, DataFormat.INT32))
                }
                Long::class.javaObjectType,
                Long::class.javaPrimitiveType -> {
                    attributes(Attributes.items(DataType.INTEGER, DataFormat.INT64))
                }
                String::class.java -> {
                    attributes(Attributes.items(DataType.STRING))
                }
                else -> if (type?.isEnum == true) {
                    attributes(Attributes.items(DataType.STRING, enums = type.enumConstants.map(Any::toString)))
                }
            }
        }

        @JvmStatic
        fun <T : AbstractDescriptor<T>> applyAttributes(descriptor: T, type: Class<*>?) = descriptor.apply {
            when (type) {
                Boolean::class.javaObjectType,
                Boolean::class.javaPrimitiveType -> when (this) {
                    is HeaderDescriptorWithType -> type(DataType.BOOLEAN)
                    is ParameterDescriptorWithType -> type(DataType.BOOLEAN)
                    is FieldDescriptor -> type(DataType.BOOLEAN)
                }
                Float::class.javaObjectType,
                Float::class.javaPrimitiveType,
                Double::class.javaObjectType,
                Double::class.javaPrimitiveType -> when (this) {
                    is HeaderDescriptorWithType -> type(DataType.NUMBER)
                    is ParameterDescriptorWithType -> type(DataType.NUMBER)
                    is FieldDescriptor -> type(DataType.NUMBER)
                }
                Int::class.javaObjectType,
                Int::class.javaPrimitiveType -> {
                    when (this) {
                        is HeaderDescriptorWithType -> type(DataType.INTEGER)
                        is ParameterDescriptorWithType -> type(DataType.INTEGER)
                        is FieldDescriptor -> type(DataType.INTEGER)
                    }
                    attributes(Attributes.format(DataFormat.INT32))
                }
                Long::class.javaObjectType,
                Long::class.javaPrimitiveType -> {
                    when (this) {
                        is HeaderDescriptorWithType -> type(DataType.INTEGER)
                        is ParameterDescriptorWithType -> type(DataType.INTEGER)
                        is FieldDescriptor -> type(DataType.INTEGER)
                    }
                    attributes(Attributes.format(DataFormat.INT64))
                }
                String::class.java -> when (this) {
                    is HeaderDescriptorWithType -> type(DataType.STRING)
                    is ParameterDescriptorWithType -> type(DataType.STRING)
                    is FieldDescriptor -> type(DataType.STRING)
                }
                else -> if (type?.isEnum == true) {
                    this.attributes(Attributes.enum(type.enumConstants.map(Any::toString)))
                }
            }
        }

        @JvmStatic
        internal fun actualPropName(name: String) = name.substringBefore("[]")

        @JvmStatic
        internal fun isArrayType(name: String) = name.lastIndexOf("[]") != -1

        @JvmStatic
        internal fun fieldType(field: Field, name: String) =
            if (!isArrayType(name)) field.type
            else genericFirstItemType(field)

        @JvmStatic
        internal fun genericFirstItemType(field: Field): Class<*>? {
            val type = field.genericType as? ParameterizedType
            return type?.actualTypeArguments?.first() as? Class<*>
        }
    }
}
