package com.keecon.restdocs.apispec.gradle

import com.keecon.restdocs.apispec.model.ResourceModel
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import java.io.File

@CacheableTask
abstract class ApiSpecTask : DefaultTask() {

    @get:Input
    abstract val separatePublicApi: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val snippetsDirectory: DirectoryProperty

    @get:Input
    abstract val outputFileNamePrefix: Property<String>

    private val outputDirectoryFile
        get() = outputDirectory.get().asFile

    private val snippetsDirectoryFile
        get() = snippetsDirectory.get().asFile

    open fun applyExtension(extension: ApiSpecExtension) {
        outputDirectory.set(extension.outputDirectoryProperty)
        snippetsDirectory.set(extension.snippetsDirectoryProperty)
        outputFileNamePrefix.set(extension.outputFileNamePrefixProperty)
        separatePublicApi.set(extension.separatePublicApiProperty)
    }

    @TaskAction
    fun aggregateResourceModels() {
        val objectMapper = jacksonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val resourceModels = snippetsDirectoryFile.walkTopDown()
            .filter { it.name == "resource.json" }
            .map { objectMapper.readValue<ResourceModel>(it.readText()) }
            .toList()

        writeSpecificationFile(outputFileNamePrefix.get(), generateSpecification(resourceModels))

        if (separatePublicApi.get()) {
            val content = generateSpecification(resourceModels.filterNot { it.privateResource })
            writeSpecificationFile("${outputFileNamePrefix.get()}-public", content)
        }
    }

    private fun writeSpecificationFile(outputFilenamePrefix: String, content: String) {
        outputDirectoryFile.mkdirs()
        File(outputDirectoryFile, "$outputFilenamePrefix.${outputFileExtension()}").writeText(content)
    }

    protected abstract fun outputFileExtension(): String

    protected abstract fun generateSpecification(resourceModels: List<ResourceModel>): String
}
