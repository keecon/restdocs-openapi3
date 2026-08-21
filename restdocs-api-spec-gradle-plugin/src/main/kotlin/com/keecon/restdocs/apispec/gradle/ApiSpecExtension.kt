package com.keecon.restdocs.apispec.gradle

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class ApiSpecExtension(protected val project: Project) {

    internal val outputDirectoryProperty: DirectoryProperty = project.objects.directoryProperty()
    internal val snippetsDirectoryProperty: DirectoryProperty = project.objects.directoryProperty()
    internal val outputFileNamePrefixProperty: Property<String> = project.objects.property(String::class.java)
    internal val separatePublicApiProperty: Property<Boolean> = project.objects.property(Boolean::class.java)

    open var outputDirectory: String
        get() = outputDirectoryProperty.get().asFile.path
        set(value) = outputDirectoryProperty.set(project.layout.projectDirectory.dir(value))

    var snippetsDirectory: String
        get() = snippetsDirectoryProperty.get().asFile.path
        set(value) = snippetsDirectoryProperty.set(project.layout.projectDirectory.dir(value))

    open var outputFileNamePrefix: String
        get() = outputFileNamePrefixProperty.get()
        set(value) = outputFileNamePrefixProperty.set(value)

    var separatePublicApi: Boolean
        get() = separatePublicApiProperty.get()
        set(value) = separatePublicApiProperty.set(value)

    init {
        snippetsDirectory = "build/generated-snippets"
        separatePublicApi = false
    }
}
