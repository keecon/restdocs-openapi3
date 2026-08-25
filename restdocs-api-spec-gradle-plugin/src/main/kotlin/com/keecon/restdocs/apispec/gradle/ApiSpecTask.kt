package com.keecon.restdocs.apispec.gradle

import com.keecon.restdocs.apispec.model.ResourceModel
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
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

    @get:Internal
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

    @get:OutputFiles
    val specificationFiles: List<File>
        get() = buildList {
            add(specificationFile(outputFileNamePrefix.get()))
            if (separatePublicApi.get()) {
                add(specificationFile("${outputFileNamePrefix.get()}-public"))
            }
        }

    open fun applyExtension(extension: ApiSpecExtension) {
        outputDirectory.set(extension.outputDirectoryProperty)
        snippetsDirectory.set(extension.snippetsDirectoryProperty)
        outputFileNamePrefix.set(extension.outputFileNamePrefixProperty)
        separatePublicApi.set(extension.separatePublicApiProperty)
    }

    @TaskAction
    fun aggregateResourceModels() {
        removeStaleSpecificationFiles()

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
        specificationFile(outputFilenamePrefix).writeText(content)
    }

    private fun specificationFile(outputFilenamePrefix: String) =
        File(outputDirectoryFile, "$outputFilenamePrefix.${outputFileExtension()}")

    private fun removeStaleSpecificationFiles() {
        val expectedFiles = specificationFiles.toSet()
        val filenamePrefixes = listOf(
            outputFileNamePrefix.get(),
            "${outputFileNamePrefix.get()}-public"
        )

        filenamePrefixes
            .flatMap { prefix -> SUPPORTED_OUTPUT_FORMATS.map { format -> File(outputDirectoryFile, "$prefix.$format") } }
            .filterNot(expectedFiles::contains)
            .forEach(File::delete)
    }

    protected abstract fun outputFileExtension(): String

    protected abstract fun generateSpecification(resourceModels: List<ResourceModel>): String

    private companion object {
        val SUPPORTED_OUTPUT_FORMATS = setOf("json", "yaml", "yml")
    }
}
