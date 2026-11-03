package com.keecon.restdocs.apispec.gradle

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.BDDAssertions.then
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.lang.Boolean.FALSE

class RestdocsOpenApi3TaskTest : RestdocsOpenApiTaskTestBase() {

    override val taskName = "openapi3"

    override var outputFileNamePrefix = "openapi3"

    @Test
    fun `should generate specification without execution time project access`() {
        givenBuildFileWithoutApiSpecClosure()
        givenResourceSnippet()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withGradleVersion("8.14.6")
            .withArguments("openapi3", "--warning-mode", "all", "--stacktrace")
            .withPluginClasspath()
            .build()

        then(result.output).doesNotContain("Invocation of Task.project at execution time")
        thenOutputFileFound()
    }

    @Test
    fun `should resolve configured relative paths`() {
        buildFile.writeText(baseBuildFile() + """
            openapi3 {
                snippetsDirectory = 'custom-snippets'
                outputDirectory = 'custom-output'
                format = 'yaml'
            }
        """.trimIndent())
        snippetsFolder = testProjectDir.resolve("custom-snippets").toFile().apply { mkdirs() }
        givenResourceSnippet()

        GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withGradleVersion("8.14.6")
            .withArguments("openapi3", "--warning-mode", "all")
            .withPluginClasspath()
            .build()

        then(testProjectDir.resolve("custom-output/openapi3.yaml").toFile()).exists()
    }

    @Test
    fun `should resolve absolute paths and reuse configuration cache`() {
        testProjectDir.resolve("gradle.properties").toFile().writeText("")
        snippetsFolder = testProjectDir.resolve("absolute-snippets").toFile().apply { mkdirs() }
        val output = testProjectDir.resolve("absolute-output/openapi3.yaml").toFile()
        buildFile.writeText(baseBuildFile() + """
            openapi3 {
                snippetsDirectory = '${snippetsFolder.absolutePath}'
                outputDirectory = '${output.parentFile.absolutePath}'
                format = 'yaml'
            }
        """.trimIndent())
        givenResourceSnippet()

        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withGradleVersion("8.14.6")
            .withArguments("openapi3", "--configuration-cache", "--configuration-cache-problems=fail", "--warning-mode", "all")
            .withPluginClasspath()

        val first = runner.build()
        then(first.output).contains("Configuration cache entry stored")
        val firstOutput = output.readText()
        val second = runner.build()
        then(second.output).contains("Reusing configuration cache")
        then(second.output).doesNotContain("Invocation of Task.project at execution time")
        then(output.readText()).isEqualTo(firstOutput).contains("/products/{id}")
    }

    @Test
    override fun `should run openapi task`() {
        super.`should run openapi task`()

        with(outputFileContext()) {
            then(read<List<String>>("servers[*].url")).hasSize(2)
            then(read<String>("servers[0].url")).isEqualTo("http://some.api/api/{id}")
            then(read<String>("servers[0].variables.id.default")).isEqualTo("some")
            then(read<List<String>>("servers[0].variables.id.enum")).containsOnly("some", "other")
        }
    }

    @Test
    fun `should run openapi task with single server`() {
        givenBuildFileWithOpenApiClosureWithSingleServer()
        givenResourceSnippet()

        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        thenOutputFileFound()
        thenSingleServerContainedInOutput()
    }

    @Test
    fun `should run openapi task with single server string`() {
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        givenResourceSnippet()

        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        thenOutputFileFound()
        thenSingleServerContainedInOutput()
    }

    @Test
    fun `should run openapi task with default values in headers`() {
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        givenResourceSnippetWithDefaultHeader()

        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        thenOutputFileFound()
        thenHeaderWithDefaultValuesContainedInOutput()
    }

    private fun thenSingleServerContainedInOutput() {
        with(outputFileContext()) {
            then(read<List<String>>("servers[*].url")).containsOnly("http://some.api")
        }
    }

    private fun thenHeaderWithDefaultValuesContainedInOutput() {
        with(outputFileContext()) {
            then(read<String>("paths./products/{id}.get.parameters[1].name")).isEqualTo("one")
            then(read<String>("paths./products/{id}.get.parameters[1].description"))
                .isEqualTo("Override request header param")
            then(read<Boolean>("paths./products/{id}.get.parameters[1].required")).isEqualTo(FALSE)
            then(read<String>("paths./products/{id}.get.parameters[1].schema.type")).isEqualTo("string")
            then(read<String>("paths./products/{id}.get.parameters[1].schema.default"))
                .isEqualTo("a default value")
            then(read<String>("paths./products/{id}.get.parameters[1].example")).isEqualTo("one")
        }
    }

    fun givenBuildFileWithOpenApiClosureWithSingleServerString() {
        givenBuildFileWithOpenApiClosure("server", """ 'http://some.api' """)
    }

    fun givenBuildFileWithOpenApiClosureWithSingleServer() {
        givenBuildFileWithOpenApiClosure(
            "server",
            """{ url = 'http://some.api' }"""
        )
    }

    override fun givenBuildFileWithOpenApiClosure() {
        givenBuildFileWithOpenApiClosure(
            "servers",
            """[ {
                url = 'http://some.api/api/{id}'
                variables = [
                    id: [
                        default: 'some',
                        description: 'some',
                        enum: ['some', 'other']
                    ]
                ]
            },
            {
                url = 'http://{host}.api/api/{id}'
                variables = [
                    id: [
                        default: 'some',
                        description: 'some',
                        enum: ['some', 'other']
                    ],
                    host: [
                        default: 'host',
                    ]
                ]
            }
            ]""".trimMargin()
        )
    }

    private fun givenBuildFileWithOpenApiClosure(serverConfigurationFieldName: String, serversSection: String) {
        buildFile.writeText(
            baseBuildFile() + """
            openapi3 {
                $serverConfigurationFieldName = $serversSection
                title = '$title'
                version = '$version'
                format = '$format'
                separatePublicApi = $separatePublicApi
                outputFileNamePrefix = '$outputFileNamePrefix'
            }
            """.trimIndent()
        )
    }

    override fun givenBuildFileWithOpenApiClosureAndSecurityDefinitions() {
        buildFile.writeText(
            baseBuildFile() + """
            openapi3 {
                servers = [ { url = "http://some.api" } ]
                title = '$title'
                description = '$description'
                tagDescriptionsPropertiesFile = "tagDescriptions.yaml"
                version = '$version'
                format = '$format'
                separatePublicApi = $separatePublicApi
                outputFileNamePrefix = '$outputFileNamePrefix'
                oauth2SecuritySchemeDefinition = {
                    flows = ['authorizationCode']
                    tokenUrl = 'http://example.com/token'
                    authorizationUrl = 'http://example.com/authorize'
                    scopeDescriptionsPropertiesFile = "scopeDescriptions.yaml"
                }
            }
            """.trimIndent()
        )
    }

    override fun thenSecurityDefinitionsFoundInOutputFile() {
        with(JsonPath.parse(outputFolder.resolve("$outputFileNamePrefix.$format").readText())) {
            then(read<String>("components.securitySchemes.oauth2.type")).isEqualTo("oauth2")
            then(read<String>("components.securitySchemes.oauth2.flows.authorizationCode.scopes.prod:r"))
                .isEqualTo("Some text")
            then(read<String>("components.securitySchemes.oauth2.flows.authorizationCode.tokenUrl")).isNotEmpty
            then(read<String>("components.securitySchemes.oauth2.flows.authorizationCode.authorizationUrl"))
                .isNotEmpty
        }
    }
}
