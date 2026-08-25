package com.keecon.restdocs.apispec.gradle

import com.keecon.restdocs.apispec.model.Oauth2Configuration
import groovy.lang.Closure
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.gradle.api.Action
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
        set(value) {
            field = value
            oauth2ScopeDescriptionsFile.unset()
            value?.scopeDescriptionsPropertiesFile?.let {
                oauth2ScopeDescriptionsFile.set(project.layout.projectDirectory.file(it))
            }
            if (value == null) serializedOauth2ConfigurationProperty.unset()
            else serializedOauth2ConfigurationProperty.set(objectMapper.writeValueAsString(value))
        }

    fun setOauth2SecuritySchemeDefinition(closure: Closure<PluginOauth2Configuration>) {
        oauth2SecuritySchemeDefinition =
            project.configure(PluginOauth2Configuration(), closure) as PluginOauth2Configuration
    }

    fun oauth2SecuritySchemeDefinition(action: Action<in PluginOauth2Configuration>) {
        oauth2SecuritySchemeDefinition = PluginOauth2Configuration().also(action::execute)
    }

    init {
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
    internal val serializedContactProperty = project.objects.property(String::class.java)

    val servers
        get() = _servers

    fun server(serverUrl: String) = setServer(serverUrl)

    fun server(serverAction: Closure<Server>) = setServer(serverAction)

    fun server(action: Action<in Server>) {
        _servers = listOf(Server().also(action::execute))
        updateSerializedServers()
    }

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

    fun setContact(contactAction: Closure<Contact>) = contact(contactAction)

    fun contact(contactAction: Closure<Contact>) =
        updateSerializedContact(project.configure(Contact(), contactAction) as Contact)

    fun contact(action: Action<in Contact>) =
        updateSerializedContact(Contact().also(action::execute))

    private fun updateSerializedContact(contact: Contact) {
        serializedContactProperty.set(objectMapper.writeValueAsString(contact))
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
