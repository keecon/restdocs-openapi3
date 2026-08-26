package com.keecon.restdocs.apispec.gradle

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.BDDAssertions.then
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.Boolean.FALSE

class RestdocsOpenApi3TaskTest : RestdocsOpenApiTaskTestBase() {

    override val taskName = "openapi3"

    override var outputFileNamePrefix = "openapi3"

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

    @Test
    fun `should include contact configured by groovy dsl`() {
        givenBuildFileWithContact()
        givenResourceSnippet()

        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        with(outputFileContext()) {
            then(read<String>("info.contact.name")).isEqualTo("API Support")
            then(read<String>("info.contact.email")).isEqualTo("support@example.com")
            then(read<String>("info.contact.url")).isEqualTo("https://example.com/support")
        }
    }

    @Test
    fun `should include oauth2 configured with direct object`() {
        buildFile.writeText(
            "import com.keecon.restdocs.apispec.gradle.PluginOauth2Configuration\n\n" +
                baseBuildFile() +
                """
                openapi3 {
                    server = 'http://some.api'
                    oauth2SecuritySchemeDefinition = new PluginOauth2Configuration().tap {
                        flows = ['authorizationCode']
                        tokenUrl = 'https://example.com/token'
                        authorizationUrl = 'https://example.com/authorize'
                    }
                }
                """.trimIndent()
        )
        givenResourceSnippet()

        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        with(outputFileContext()) {
            then(read<String>("components.securitySchemes.oauth2.type")).isEqualTo("oauth2")
            then(read<String>("components.securitySchemes.oauth2.flows.authorizationCode.tokenUrl"))
                .isEqualTo("https://example.com/token")
            then(read<String>("components.securitySchemes.oauth2.flows.authorizationCode.authorizationUrl"))
                .isEqualTo("https://example.com/authorize")
        }
    }

    @Test
    fun `should reuse configuration cache`() {
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        givenResourceSnippet()

        whenPluginExecuted()
        thenApiSpecTaskSuccessful()
        whenPluginExecuted()

        then(result.output).contains("Reusing configuration cache")
    }

    @Test
    fun `should respect custom build directory for default paths`() {
        buildFile.writeText(
            baseBuildFile() + """
            layout.buildDirectory = layout.projectDirectory.dir('custom-build')
            """.trimIndent()
        )
        snippetsFolder = testProjectDir.resolve("custom-build/generated-snippets").toFile().apply { mkdirs() }
        outputFolder = testProjectDir.resolve("custom-build/api-spec").toFile()
        givenResourceSnippet()

        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        then(outputFolder.resolve("openapi3.json")).exists()
    }

    @Test
    fun `should ignore irrelevant snippet files for task inputs`() {
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        givenResourceSnippet()

        whenPluginExecuted()
        thenApiSpecTaskSuccessful()
        File(snippetsFolder.resolve("some-operation"), "http-request.adoc").writeText("irrelevant")
        whenPluginExecuted()

        then(result.task(":$taskName")!!.outcome).isIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `should remove public specification when separate public api is disabled`() {
        separatePublicApi = true
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        givenResourceSnippet()

        whenPluginExecuted()
        thenOutputFileForPublicResourceSpecificationFound()

        separatePublicApi = false
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        thenOutputFileForPublicResourceSpecificationNotFound()
    }

    @Test
    fun `should remove specification in previous format`() {
        format = "yaml"
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        givenResourceSnippet()

        whenPluginExecuted()
        then(outputFolder.resolve("$outputFileNamePrefix.yaml")).exists()

        format = "json"
        givenBuildFileWithOpenApiClosureWithSingleServerString()
        whenPluginExecuted()

        thenApiSpecTaskSuccessful()
        then(outputFolder.resolve("$outputFileNamePrefix.yaml")).doesNotExist()
        thenOutputFileFound()
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

    private fun givenBuildFileWithContact() {
        buildFile.writeText(
            baseBuildFile() + """
            openapi3 {
                server = 'http://some.api'
                contact = {
                    name = 'API Support'
                    email = 'support@example.com'
                    url = 'https://example.com/support'
                }
                title = '$title'
                version = '$version'
                format = '$format'
                outputFileNamePrefix = '$outputFileNamePrefix'
            }
            """.trimIndent()
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
