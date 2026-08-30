package com.keecon.restdocs.apispec.gradle

import com.keecon.restdocs.apispec.model.ResourceModel
import org.assertj.core.api.BDDAssertions.then
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ApiSpecTaskUnitTest {

    @Test
    fun `should resolve specification file directly below output directory`(@TempDir projectDir: Path) {
        val task = givenApiSpecTask(projectDir, "openapi3")

        then(task.specificationFiles).containsExactly(
            projectDir.resolve("build/api-spec/openapi3.json").toFile().canonicalFile
        )
    }

    @Test
    fun `should reject output prefix that is not a simple file name`(@TempDir projectDir: Path) {
        listOf("", " ", ".", "..", "../escaped", "..\\escaped").forEachIndexed { index, prefix ->
            val task = givenApiSpecTask(projectDir, prefix, index)

            val exception = assertThrows<IllegalArgumentException> {
                task.specificationFiles
            }

            then(exception).hasMessage("outputFileNamePrefix must be a simple file name")
        }
    }

    private fun givenApiSpecTask(
        projectDir: Path,
        outputFileNamePrefix: String,
        index: Int = 0
    ): TestApiSpecTask {
        val project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
        return project.tasks.register("apiSpec$index", TestApiSpecTask::class.java).get().apply {
            outputDirectory.set(project.layout.buildDirectory.dir("api-spec"))
            separatePublicApi.set(false)
            this.outputFileNamePrefix.set(outputFileNamePrefix)
        }
    }
}

abstract class TestApiSpecTask : ApiSpecTask() {
    override fun outputFileExtension() = "json"

    override fun generateSpecification(resourceModels: List<ResourceModel>) = "{}"
}
