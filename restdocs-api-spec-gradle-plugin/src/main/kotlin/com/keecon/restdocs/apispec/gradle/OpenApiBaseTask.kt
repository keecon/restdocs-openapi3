package com.keecon.restdocs.apispec.gradle

import com.keecon.restdocs.apispec.model.Oauth2Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue

@CacheableTask
abstract class OpenApiBaseTask : ApiSpecTask() {
    @get:Input
    abstract val title: Property<String>

    @get:Input
    @get:Optional
    abstract val apiDescription: Property<String>

    @get:Input
    abstract val apiVersion: Property<String>

    @get:Input
    abstract val format: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tagDescriptionsFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val serializedOauth2Configuration: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val oauth2ScopeDescriptionsFile: RegularFileProperty

    override fun outputFileExtension() = format.get()

    protected fun tagDescriptions(): Map<String, String> =
        tagDescriptionsFile.orNull?.asFile?.let { file -> yamlMapper().readValue(file) } ?: emptyMap()

    protected fun oauth2SecuritySchemeDefinition(): Oauth2Configuration? =
        serializedOauth2Configuration.orNull?.let {
            yamlMapper().readValue<Oauth2Configuration>(it).apply {
                scopes = oauth2ScopeDescriptionsFile.orNull?.asFile
                    ?.let { file -> yamlMapper().readValue(file) }
                    ?: emptyMap()
            }
        }

    private fun yamlMapper() = YAMLMapper.builder().addModule(kotlinModule()).build()

    fun applyExtension(extension: OpenApiBaseExtension) {
        super.applyExtension(extension)
        format.set(extension.formatProperty)
        serializedOauth2Configuration.set(extension.serializedOauth2ConfigurationProperty)
        title.set(extension.titleProperty)
        apiDescription.set(extension.descriptionProperty)
        tagDescriptionsFile.set(extension.tagDescriptionsFile)
        oauth2ScopeDescriptionsFile.set(extension.oauth2ScopeDescriptionsFile)
        apiVersion.set(extension.versionProperty)
    }
}
