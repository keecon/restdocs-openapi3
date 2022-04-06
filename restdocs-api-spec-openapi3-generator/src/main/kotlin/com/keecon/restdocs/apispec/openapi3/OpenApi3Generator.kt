package com.keecon.restdocs.apispec.openapi3

import com.fasterxml.jackson.module.kotlin.readValue
import com.keecon.restdocs.apispec.jsonschema.JsonSchemaGenerator
import com.keecon.restdocs.apispec.model.AbstractDescriptor
import com.keecon.restdocs.apispec.model.AbstractParameterDescriptor
import com.keecon.restdocs.apispec.model.DataType
import com.keecon.restdocs.apispec.model.FieldDescriptor
import com.keecon.restdocs.apispec.model.HTTPMethod
import com.keecon.restdocs.apispec.model.HeaderDescriptor
import com.keecon.restdocs.apispec.model.Oauth2Configuration
import com.keecon.restdocs.apispec.model.ParameterDescriptor
import com.keecon.restdocs.apispec.model.RequestModel
import com.keecon.restdocs.apispec.model.ResourceModel
import com.keecon.restdocs.apispec.model.ResponseModel
import com.keecon.restdocs.apispec.model.groupByPath
import com.keecon.restdocs.apispec.openapi3.ParameterExtensions.applyProperties
import com.keecon.restdocs.apispec.openapi3.SchemaConstraints.applyConstraints
import com.keecon.restdocs.apispec.openapi3.SchemaExtensions.applyDefaultValue
import com.keecon.restdocs.apispec.openapi3.SchemaExtensions.applyEnumValues
import com.keecon.restdocs.apispec.openapi3.SchemaExtensions.applyItems
import com.keecon.restdocs.apispec.openapi3.SchemaExtensions.applyProperties
import com.keecon.restdocs.apispec.openapi3.SecuritySchemeGenerator.addSecurityDefinitions
import com.keecon.restdocs.apispec.openapi3.SecuritySchemeGenerator.addSecurityItemFromSecurityRequirements
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.http.HttpStatus

object OpenApi3Generator {

    private val PATH_PARAMETER_PATTERN = """\{([^/}]+)}""".toRegex()
    private fun generate(
        resources: List<ResourceModel>,
        servers: List<Server>,
        title: String = "API",
        description: String? = null,
        tagDescriptions: Map<String, String> = emptyMap(),
        version: String = "1.0.0",
        oauth2SecuritySchemeDefinition: Oauth2Configuration? = null
    ): OpenAPI {
        return OpenAPI().apply {

            this.servers = servers
            info = Info().apply {
                this.title = title
                this.description = description
                this.version = version
            }
            this.tags(
                tagDescriptions.map {
                    Tag().apply {
                        this.name = it.key
                        this.description = it.value
                    }
                }
            )
            paths = generatePaths(
                resources,
                oauth2SecuritySchemeDefinition
            )
            extractDefinitions()
            addSecurityDefinitions(oauth2SecuritySchemeDefinition)
        }
    }

    fun generateAndSerialize(
        resources: List<ResourceModel>,
        servers: List<Server>,
        title: String = "API",
        description: String? = null,
        tagDescriptions: Map<String, String> = emptyMap(),
        version: String = "1.0.0",
        oauth2SecuritySchemeDefinition: Oauth2Configuration? = null,
        format: String
    ) =
        ApiSpecificationWriter.serialize(
            format,
            generate(
                resources = resources,
                servers = servers,
                title = title,
                description = description,
                tagDescriptions = tagDescriptions,
                version = version,
                oauth2SecuritySchemeDefinition = oauth2SecuritySchemeDefinition
            )
        )

    private fun OpenAPI.extractDefinitions() {
        val schemasToKeys = HashMap<Schema<Any>, String>()
        val operationToPathKey = HashMap<Operation, String>()

        paths.map { it.key to it.value.readOperations() }
            .forEach { (path, operations) ->
                operations.forEach { operation ->
                    operationToPathKey[operation] = path
                }
            }

        operationToPathKey.keys.forEach { operation ->
            val path = operationToPathKey[operation]!!

            operation.requestBody?.content?.mapNotNull { it.value }
                ?.extractSchemas(schemasToKeys, path)

            operation.responses.values.mapNotNull { it.content }.flatMap { it.values }
                .extractSchemas(schemasToKeys, path)
        }

        this.components = Components().apply {
            schemas = schemasToKeys.keys.map {
                schemasToKeys.getValue(it) to it
            }.toMap()
        }
    }

    private fun List<MediaType>.extractSchemas(
        schemasToKeys: MutableMap<Schema<Any>, String>,
        path: String
    ) {
        this.filter { it.schema != null }
            .forEach {
                it.schema(
                    extractOrFindSchema(
                        schemasToKeys,
                        it.schema,
                        generateSchemaName(path)
                    )
                )
            }
    }

    private fun extractOrFindSchema(
        schemasToKeys: MutableMap<Schema<Any>, String>,
        schema: Schema<Any>,
        schemaNameGenerator: (Schema<Any>) -> String
    ): Schema<Any> {
        val schemaKey = if (schemasToKeys.containsKey(schema)) {
            schemasToKeys[schema]!!
        } else {
            val name = schema.name ?: schemaNameGenerator(schema)
            schemasToKeys[schema] = name
            name
        }
        return Schema<Any>().apply { `$ref`("#/components/schemas/$schemaKey") }
    }

    private fun generateSchemaName(path: String): (Schema<Any>) -> String {
        return { schema ->
            path
                .removePrefix("/")
                .replace("/", "-")
                .replace(Regex.fromLiteral("{"), "")
                .replace(Regex.fromLiteral("}"), "")
                .plus(schema.hashCode())
        }
    }

    private fun generatePaths(
        resources: List<ResourceModel>,
        oauth2SecuritySchemeDefinition: Oauth2Configuration?
    ): Paths {
        return resources.groupByPath().entries
            .map {
                it.key to resourceModels2PathItem(
                    it.value,
                    oauth2SecuritySchemeDefinition
                )
            }
            .let { pathAndPathItem ->
                Paths().apply { pathAndPathItem.forEach { addPathItem(it.first, it.second) } }
            }
    }

    private fun groupByHttpMethod(resources: List<ResourceModel>): Map<HTTPMethod, List<ResourceModel>> {
        return resources.groupBy { it.request.method }
    }

    private fun resourceModels2PathItem(
        modelsWithSamePath: List<ResourceModel>,
        oauth2SecuritySchemeDefinition: Oauth2Configuration?
    ): PathItem {
        val path = PathItem()
        groupByHttpMethod(modelsWithSamePath)
            .entries
            .forEach {
                addOperation(
                    method = it.key,
                    pathItem = path,
                    operation = resourceModels2Operation(
                        it.value,
                        oauth2SecuritySchemeDefinition
                    )
                )
            }

        return path
    }

    private fun addOperation(method: HTTPMethod, pathItem: PathItem, operation: Operation) =
        when (method) {
            HTTPMethod.GET -> pathItem.get(operation)
            HTTPMethod.POST -> pathItem.post(operation)
            HTTPMethod.PUT -> pathItem.put(operation)
            HTTPMethod.DELETE -> pathItem.delete(operation)
            HTTPMethod.PATCH -> pathItem.patch(operation)
            HTTPMethod.HEAD -> pathItem.head(operation)
            HTTPMethod.OPTIONS -> pathItem.options(operation)
        }

    private fun resourceModels2Operation(
        modelsWithSamePathAndMethod: List<ResourceModel>,
        oauth2SecuritySchemeDefinition: Oauth2Configuration?
    ): Operation {
        val firstModelForPathAndMethod = modelsWithSamePathAndMethod.first()
        val operationIds = modelsWithSamePathAndMethod.map { model -> model.operationId }
        return Operation().apply {
            operationId = operationId(operationIds)
            summary = modelsWithSamePathAndMethod.map { it.summary }.find { !it.isNullOrBlank() }
            description = modelsWithSamePathAndMethod.map { it.description }.find { !it.isNullOrBlank() }
            tags = modelsWithSamePathAndMethod.flatMap { it.tags }.distinct().ifEmpty { null }
            if (modelsWithSamePathAndMethod.all { it.deprecated }) deprecated = true
            parameters =
                extractPathParameters(
                    firstModelForPathAndMethod
                ).plus(
                    modelsWithSamePathAndMethod
                        .filter { it.request.contentType != "application/x-www-form-urlencoded" }
                        .flatMap { it.request.requestParameters }
                        .distinctBy { it.name }
                        .map { requestParameterDescriptor2Parameter(it) }
                ).plus(
                    modelsWithSamePathAndMethod
                        .flatMap { it.request.headers }
                        .distinctBy { it.name }
                        .map { header2Parameter(it) }
                ).ifEmpty { null }
            requestBody = resourceModelsToRequestBody(
                modelsWithSamePathAndMethod.map {
                    RequestModelWithOperationId(
                        it.operationId,
                        it.request
                    )
                }
            )
            responses = resourceModelsToApiResponses(
                modelsWithSamePathAndMethod.map {
                    ResponseModelWithOperationId(
                        it.operationId,
                        it.response
                    )
                }
            )
        }.apply {
            addSecurityItemFromSecurityRequirements(
                firstModelForPathAndMethod.request.securityRequirements,
                oauth2SecuritySchemeDefinition
            )
        }
    }

    private fun operationId(operationIds: List<String>): String {
        var prefix = operationIds.first()
        for (operationId in operationIds) {
            prefix = prefix.commonPrefixWith(operationId)
        }

        if (prefix.isEmpty()) {
            prefix = operationIds.sorted().joinToString(separator = "")
        }

        return prefix
    }

    private fun resourceModelsToRequestBody(
        requestModelsWithOperationId: List<RequestModelWithOperationId>
    ): RequestBody? {
        val requestByContentType = requestModelsWithOperationId
            .filter { it.request.contentType != null }
            .groupBy { it.request.contentType!! }

        if (requestByContentType.isEmpty())
            return null

        return requestByContentType
            .map { (contentType, requests) ->
                toMediaType(
                    requestFields = requests.flatMap { it ->
                        if (it.request.contentType == "application/x-www-form-urlencoded") {
                            it.request.requestParameters.map { parameterDescriptor2FieldDescriptor(it) }
                        } else {
                            it.request.requestFields
                        }
                    },
                    examplesWithOperationId = requests.filter { it.request.example != null }
                        .map { it.operationId to it.request.example!! }.toMap(),
                    contentType = contentType,
                    schemaName = requests.first().request.schema?.name
                )
            }.toMap()
            .let { contentTypeToMediaType ->
                if (contentTypeToMediaType.isEmpty()) null
                else RequestBody()
                    .apply {
                        content = Content().apply { contentTypeToMediaType.forEach { addMediaType(it.key, it.value) } }
                    }
            }
    }

    private fun resourceModelsToApiResponses(
        responseModelsWithOperationId: List<ResponseModelWithOperationId>
    ): ApiResponses? {
        val responsesByStatus = responseModelsWithOperationId
            .groupBy { it.response.status }

        if (responsesByStatus.isEmpty())
            return null

        return responsesByStatus
            .mapValues { (_, responses) ->
                responsesWithSameStatusToApiResponse(
                    responses
                )
            }
            .let {
                ApiResponses().apply {
                    it.forEach { (status, apiResponse) -> addApiResponse(status.toString(), apiResponse) }
                }
            }
    }

    private fun responsesWithSameStatusToApiResponse(
        responseModelsSameStatus: List<ResponseModelWithOperationId>
    ): ApiResponse {
        val responsesByContentType = responseModelsSameStatus
            .filter { it.response.contentType != null }
            .groupBy { it.response.contentType!! }

        val apiResponse = ApiResponse().apply {
            description = HttpStatus.valueOf(responseModelsSameStatus.first().response.status).reasonPhrase
            headers = responseModelsSameStatus
                .flatMap { it.response.headers }
                .associate {
                    it.name to Header().apply {
                        description(it.description)
                        schema = simpleTypeToSchema(it)
                    }
                }
                .ifEmpty { null }
        }
        return responsesByContentType
            .map { (contentType, requests) ->
                toMediaType(
                    requestFields = requests.flatMap { it.response.responseFields },
                    examplesWithOperationId = requests.associate { it.operationId to it.response.example!! },
                    contentType = contentType,
                    schemaName = requests.first().response.schema?.name
                )
            }.toMap()
            .let { contentTypeToMediaType ->
                apiResponse
                    .apply {
                        content =
                            if (contentTypeToMediaType.isEmpty()) null
                            else Content().apply { contentTypeToMediaType.forEach { addMediaType(it.key, it.value) } }
                    }
            }
    }

    private fun toMediaType(
        requestFields: List<FieldDescriptor>,
        examplesWithOperationId: Map<String, String>,
        contentType: String,
        schemaName: String? = null
    ): Pair<String, MediaType> {
        val schema = JsonSchemaGenerator().generateSchema(requestFields, schemaName)
            .let { Json.mapper().readValue<Schema<Any>>(it) }

        if (schemaName != null) schema.name = schemaName

        return contentType to MediaType()
            .schema(schema)
            .examples(
                examplesWithOperationId
                    .map { it.key to Example().apply { value(it.value) } }
                    .toMap()
                    .ifEmpty { null }
            )
    }

    private fun extractPathParameters(resourceModel: ResourceModel): List<PathParameter> {
        val pathParameterNames = PATH_PARAMETER_PATTERN.findAll(resourceModel.request.path)
            .map { matchResult -> matchResult.groupValues[1] }
            .toList()

        return pathParameterNames.map { parameterName ->
            resourceModel.request.pathParameters
                .firstOrNull { it.name == parameterName }
                ?.let { pathParameterDescriptor2Parameter(it) }
                ?: parameterName2PathParameter(parameterName)
        }
    }

    private fun parameterDescriptor2FieldDescriptor(parameterDescriptor: ParameterDescriptor): FieldDescriptor {
        return FieldDescriptor(
            // It's safe to map name to path, as in application/x-www-form-urlencoded
            // we should have a flat structure.
            path = parameterDescriptor.name,
            description = parameterDescriptor.description,
            type = parameterDescriptor.type,
            optional = parameterDescriptor.optional,
            ignored = parameterDescriptor.ignored,
            attributes = parameterDescriptor.attributes
        )
    }

    private fun pathParameterDescriptor2Parameter(parameterDescriptor: ParameterDescriptor): PathParameter {
        return PathParameter().apply {
            name = parameterDescriptor.name
            description = parameterDescriptor.description
            schema = simpleTypeToSchema(parameterDescriptor)
            applyProperties(parameterDescriptor)
        }
    }

    private fun parameterName2PathParameter(parameterName: String): PathParameter {
        return PathParameter().apply {
            name = parameterName
            description = ""
            schema = StringSchema()
        }
    }

    private fun requestParameterDescriptor2Parameter(parameterDescriptor: ParameterDescriptor): QueryParameter {
        return QueryParameter().apply {
            name = parameterDescriptor.name
            description = parameterDescriptor.description
            required = parameterDescriptor.optional.not()
            schema = simpleTypeToSchema(parameterDescriptor)
            applyProperties(parameterDescriptor)
        }
    }

    private fun header2Parameter(headerDescriptor: HeaderDescriptor): HeaderParameter {
        return HeaderParameter().apply {
            name = headerDescriptor.name
            description = headerDescriptor.description
            required = headerDescriptor.optional.not()
            schema = simpleTypeToSchema(headerDescriptor)
            example = headerDescriptor.example
            applyProperties(headerDescriptor)
        }
    }

    internal fun simpleTypeToSchema(descriptor: AbstractDescriptor): Schema<*>? {
        return when (descriptor.type.lowercase()) {
            DataType.BOOLEAN.lowercase() -> BooleanSchema().apply {
                applyProperties(descriptor)
                applyDefaultValue(descriptor as? AbstractParameterDescriptor)
                applyEnumValues(descriptor)
            }
            DataType.STRING.lowercase() -> StringSchema().apply {
                applyProperties(descriptor)
                applyDefaultValue(descriptor as? AbstractParameterDescriptor)
                applyEnumValues(descriptor)
                applyConstraints(descriptor)
            }
            DataType.NUMBER.lowercase() -> NumberSchema().apply {
                applyProperties(descriptor)
                applyDefaultValue(descriptor as? AbstractParameterDescriptor)
                applyEnumValues(descriptor)
                applyConstraints(descriptor)
            }
            DataType.INTEGER.lowercase() -> IntegerSchema().apply {
                applyProperties(descriptor)
                applyDefaultValue(descriptor as? AbstractParameterDescriptor)
                applyEnumValues(descriptor)
                applyConstraints(descriptor)
            }
            DataType.ARRAY.lowercase() -> ArraySchema().apply {
                applyProperties(descriptor)
                applyItems(descriptor)
                applyConstraints(descriptor)
            }
            else -> throw IllegalArgumentException("Unknown type '${descriptor.type}'")
        }
    }

    private data class RequestModelWithOperationId(
        val operationId: String,
        val request: RequestModel
    )

    private data class ResponseModelWithOperationId(
        val operationId: String,
        val response: ResponseModel
    )
}
