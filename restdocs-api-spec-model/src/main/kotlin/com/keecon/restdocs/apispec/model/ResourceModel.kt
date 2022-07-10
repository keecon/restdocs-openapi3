package com.keecon.restdocs.apispec.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ResourceModel(
    val operationId: String,
    val summary: String? = null,
    val description: String? = null,
    val privateResource: Boolean,
    val deprecated: Boolean,
    val tags: Set<String> = emptySet(),
    val request: RequestModel,
    val response: ResponseModel
)

fun List<ResourceModel>.groupByPath(): Map<String, List<ResourceModel>> {
    return this.sortedWith(
        // by first path segment, then path length, then path
        Comparator.comparing<ResourceModel, String> {
            it.request.path.split("/").firstOrNull { s -> s.isNotEmpty() }.orEmpty()
        }
            .thenComparing(Comparator.comparingInt { it.request.path.count { c -> c == '/' } })
            .thenComparing(Comparator.comparing { it.request.path })
    )
        .groupBy { it.request.path }
}

data class Schema(
    val name: String
)

data class RequestModel(
    val path: String,
    val method: HTTPMethod,
    val contentType: String? = null,
    val securityRequirements: SecurityRequirements?,
    val headers: List<HeaderDescriptor>,
    val pathParameters: List<ParameterDescriptor>,
    val requestParameters: List<ParameterDescriptor>,
    val requestParts: List<PartDescriptor>,
    val requestFields: List<FieldDescriptor>,
    val example: String? = null,
    val schema: Schema? = null
)

data class ResponseModel(
    val status: Int,
    val contentType: String?,
    val headers: List<HeaderDescriptor>,
    val responseFields: List<FieldDescriptor>,
    val example: String? = null,
    val schema: Schema? = null
)

// https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.1.md#data-types
enum class DataType {
    STRING,
    INTEGER,
    NUMBER,
    BOOLEAN,
    ARRAY,
    OBJECT,
    ;

    fun lowercase(): String = name.lowercase()
}

// https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.1.md#data-types
enum class DataFormat {
    INT32,
    INT64,
    FLOAT,
    DOUBLE,
    BYTE,
    BINARY,
    PASSWORD,
    DATE,
    DATETIME,
    EMAIL,
    UUID,
    URI,
    HOSTNAME,
    IPV4,
    IPV6,
    ;

    fun lowercase(): String = name.lowercase()
}

// https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.1.md#schemaObject
interface AbstractDescriptor {
    val description: String
    val type: String
    val optional: Boolean
    val attributes: Attributes
}

interface AbstractParameterDescriptor : AbstractDescriptor {
    val name: String
    val defaultValue: Any?
}

data class HeaderDescriptor(
    override val name: String,
    override val description: String,
    override val type: String,
    @JsonProperty("default") override val defaultValue: Any? = null,
    override val optional: Boolean,
    val example: String? = null,
    override val attributes: Attributes = Attributes()
) : AbstractParameterDescriptor

open class FieldDescriptor(
    val path: String,
    override val description: String,
    override val type: String,
    override val optional: Boolean = false,
    val ignored: Boolean = false,
    override val attributes: Attributes = Attributes()
) : AbstractDescriptor

data class ParameterDescriptor(
    override val name: String,
    override val description: String,
    override val type: String,
    @JsonProperty("default") override val defaultValue: Any? = null,
    override val optional: Boolean,
    val ignored: Boolean,
    override val attributes: Attributes = Attributes()
) : AbstractParameterDescriptor

data class PartDescriptor(
    override val name: String,
    override val description: String,
    override val type: String,
    @JsonProperty("default") override val defaultValue: Any? = null,
    override val optional: Boolean,
    val ignored: Boolean,
    override val attributes: Attributes = Attributes()
) : AbstractParameterDescriptor

data class TypeDescriptor(
    override val type: String,
    override val description: String = "",
    override val optional: Boolean = false,
    override val attributes: Attributes = Attributes()
) : AbstractDescriptor

data class Attributes(
    val validationConstraints: List<Constraint> = emptyList(),
    val enumValues: List<Any> = emptyList(),
    val format: String? = null,
    val items: TypeDescriptor? = null,
    val encoding: Encoding? = null,
)

data class Constraint(
    val name: String,
    val configuration: Map<String, Any>
)

// https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.1.md#style-examples
enum class EncodingStyle {
    MATRIX,
    LABEL,
    FORM,
    SIMPLE,
    ;

    fun lowercase(): String = name.lowercase()
}

// https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.1.md#encoding-object
data class Encoding(
    val style: String,
    val explode: Boolean? = null,
    val allowReserved: Boolean? = null,
)

data class SecurityRequirements(
    val type: SecurityType,
    val requiredScopes: List<String>? = null
)

enum class SecurityType {
    OAUTH2,
    BASIC,
    API_KEY,
    JWT_BEARER
}

enum class HTTPMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS
}
