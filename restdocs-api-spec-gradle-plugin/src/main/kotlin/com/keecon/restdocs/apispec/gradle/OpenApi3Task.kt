package com.keecon.restdocs.apispec.gradle

import com.keecon.restdocs.apispec.generator.OpenApi3Generator
import com.keecon.restdocs.apispec.model.ResourceModel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue

@CacheableTask
abstract class OpenApi3Task : OpenApiBaseTask() {

    @get:Input
    abstract val serializedServers: Property<String>

    fun applyExtension(extension: OpenApi3Extension) {
        super.applyExtension(extension)
        serializedServers.set(extension.serializedServersProperty)
    }

    override fun generateSpecification(resourceModels: List<ResourceModel>): String {
        return OpenApi3Generator.generateAndSerialize(
            resources = resourceModels,
            servers = YAMLMapper.builder()
                .addModule(kotlinModule())
                .build()
                .readValue(serializedServers.get()),
            title = title.get(),
            description = apiDescription.orNull,
            tagDescriptions = tagDescriptions(),
            version = apiVersion.get(),
            oauth2SecuritySchemeDefinition = oauth2SecuritySchemeDefinition(),
            format = format.get()
        )
    }
}
