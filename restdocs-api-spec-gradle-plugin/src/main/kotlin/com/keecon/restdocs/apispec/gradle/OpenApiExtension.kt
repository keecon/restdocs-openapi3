package com.keecon.restdocs.apispec.gradle

import com.keecon.restdocs.apispec.model.Oauth2Configuration
import groovy.lang.Closure
import io.swagger.v3.oas.models.servers.Server
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

abstract class OpenApiBaseExtension(project: Project) : ApiSpecExtension(project) {
    protected val objectMapper = YAMLMapper.builder().addModule(kotlinModule()).build()

    internal val titleProperty = project.objects.property(String::class.java)
    internal val versionProperty = project.objects.property(String::class.java)
    internal val descriptionProperty = project.objects.property(String::class.java)
    internal val formatProperty = project.objects.property(String::class.java)
    internal val serializedOauth2ConfigurationProperty = project.objects.property(String::class.java)

    var title: String
        get() = titleProperty.get()
        set(value) = titleProperty.set(value)

    var version: String
        get() = versionProperty.get()
        set(value) = versionProperty.set(value)

    var description: String?
        get() = descriptionProperty.orNull
        set(value) = descriptionProperty.set(value)
    internal val tagDescriptionsFile: RegularFileProperty = project.objects.fileProperty()
    internal val oauth2ScopeDescriptionsFile: RegularFileProperty = project.objects.fileProperty()

    var tagDescriptionsPropertiesFile: String?
        get() = tagDescriptionsFile.orNull?.asFile?.path
        set(value) {
            if (value == null) tagDescriptionsFile.unset()
            else tagDescriptionsFile.set(project.layout.projectDirectory.file(value))
        }

    var format: String
        get() = formatProperty.get()
        set(value) = formatProperty.set(value)

    var oauth2SecuritySchemeDefinition: PluginOauth2Configuration? = null

    fun setOauth2SecuritySchemeDefinition(closure: Closure<PluginOauth2Configuration>) {
        oauth2SecuritySchemeDefinition =
            project.configure(PluginOauth2Configuration(), closure) as PluginOauth2Configuration
        oauth2SecuritySchemeDefinition?.scopeDescriptionsPropertiesFile?.let {
            oauth2ScopeDescriptionsFile.set(project.layout.projectDirectory.file(it))
        }
        serializedOauth2ConfigurationProperty.set(
            objectMapper.writeValueAsString(oauth2SecuritySchemeDefinition)
        )
    }

    init {
        outputDirectory = "build/api-spec"
        title = "API documentation"
        version = (project.version as? String)?.let { if (it == "unspecified") null else it } ?: "1.0.0"
        format = "json"
    }
}

class PluginOauth2Configuration(
    var scopeDescriptionsPropertiesFile: String? = null
) : Oauth2Configuration()

open class OpenApi3Extension(project: Project) : OpenApiBaseExtension(project) {

    private var _servers: List<Server> = mutableListOf(Server().apply { url = "http://localhost" })
    internal val serializedServersProperty = project.objects.property(String::class.java)

    val servers
        get() = _servers

    fun setServer(serverAction: Closure<Server>) {
        _servers = listOf(project.configure(Server(), serverAction) as Server)
        updateSerializedServers()
    }

    fun setServer(serverUrl: String) {
        _servers = listOf(Server().apply { url = serverUrl })
        updateSerializedServers()
    }

    fun setServers(serversActions: List<Closure<Server>>) {
        _servers = serversActions.map { project.configure(Server(), it) as Server }
        updateSerializedServers()
    }

    private fun updateSerializedServers() {
        serializedServersProperty.set(objectMapper.writeValueAsString(_servers))
    }

    companion object {
        const val name = "openapi3"
    }

    init {
        outputFileNamePrefix = "openapi3"
        updateSerializedServers()
    }
}
