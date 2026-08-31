package com.keecon.restdocs.apispec.gradle

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.BDDAssertions.then
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

class PublishedConsumerTest {

    @Test
    fun `published producer and plugin preserve the RFC 3339 wire contract`(@TempDir projectDir: Path) {
        val repository = requiredSystemProperty("consumerTestRepository")
        val version = requiredSystemProperty("consumerTestVersion")
        val repositoryUri = Path.of(repository).toUri()

        projectDir.resolve("settings.gradle").writeText(
            """
            pluginManagement {
                repositories {
                    maven {
                        url = uri('$repositoryUri')
                        metadataSources {
                            mavenPom()
                            artifact()
                        }
                    }
                    gradlePluginPortal()
                }
            }
            rootProject.name = 'rfc3339-round-trip'
            """.trimIndent()
        )
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'java'
                id 'com.keecon.restdocs-openapi3' version '$version'
            }

            repositories {
                maven {
                    url = uri('$repositoryUri')
                    metadataSources {
                        mavenPom()
                        artifact()
                    }
                }
                mavenCentral()
            }

            dependencies {
                implementation 'com.keecon:restdocs-api-spec-mockmvc:$version'
            }

            def dateTime = providers.gradleProperty('dateTime')
                .orElse('2026-08-30T06:30:00Z')

            tasks.register('generateResourceSnippet', JavaExec) {
                dependsOn classes
                classpath = sourceSets.main.runtimeClasspath
                mainClass = 'example.GenerateRfc3339Snippet'
                inputs.property('dateTime', dateTime)
                doFirst {
                    args = [
                        layout.buildDirectory.dir('generated-snippets').get().asFile.absolutePath,
                        dateTime.get()
                    ]
                }
            }

            tasks.named('openapi3') {
                dependsOn generateResourceSnippet
            }
            """.trimIndent()
        )
        writeRfc3339SnippetProducer(projectDir)

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("openapi3", "--stacktrace")
            .build()

        then(result.task(":openapi3")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        with(JsonPath.parse(projectDir.resolve("build/api-spec/openapi3.json").toFile().readText())) {
            then(read<List<String>>("$..createdAt.format")).contains("date-time")
        }

        val invalidResult = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(
                "openapi3",
                "-PdateTime=2026-08-30T15:30:00+09:00[Asia/Seoul]",
                "--stacktrace",
            )
            .buildAndFail()

        then(invalidResult.output)
            .contains("product-get")
            .contains("RFC 3339 date-time")
    }

    @Test
    fun `published plugin does not expose Kotlin Gradle Plugin as runtime dependency`() {
        val repository = requiredSystemProperty("consumerTestRepository")
        val version = requiredSystemProperty("consumerTestVersion")
        val pluginPom = Path.of(repository)
            .resolve(
                "com/keecon/restdocs-api-spec-gradle-plugin/$version"
            )
            .listDirectoryEntries("restdocs-api-spec-gradle-plugin-*.pom")
            .single()

        then(pluginPom).exists()
        then(pluginPom.toFile().readText()).doesNotContain(
            "<groupId>org.jetbrains.kotlin</groupId>",
            "<artifactId>kotlin-gradle-plugin</artifactId>"
        )
    }

    @Test
    fun `published artifacts expose dependencies required by their public api`(@TempDir projectDir: Path) {
        val repository = requiredSystemProperty("consumerTestRepository")
        val version = requiredSystemProperty("consumerTestVersion")
        val repositoryUri = Path.of(repository).toUri()

        projectDir.resolve("settings.gradle").toFile().writeText(
            """
            pluginManagement {
                repositories {
                    maven {
                        url = uri('$repositoryUri')
                        metadataSources {
                            mavenPom()
                            artifact()
                        }
                    }
                    gradlePluginPortal()
                }
            }
            rootProject.name = 'published-consumer'
            """.trimIndent()
        )
        projectDir.resolve("build.gradle").toFile().writeText(
            """
            plugins {
                id 'java'
                id 'com.keecon.restdocs-openapi3' version '$version'
            }

            repositories {
                maven {
                    url = uri('$repositoryUri')
                    metadataSources {
                        mavenPom()
                        artifact()
                    }
                }
                mavenCentral()
            }

            dependencies {
                testImplementation 'com.keecon:restdocs-api-spec-mockmvc:$version'
                testImplementation 'com.keecon:restdocs-api-spec-webtestclient:$version'
                testImplementation 'com.keecon:restdocs-api-spec-jsonschema:$version'
            }
            """.trimIndent()
        )

        val sourceDirectory = projectDir.resolve("src/test/java/example").toFile().apply { mkdirs() }
        sourceDirectory.resolve("PublishedApiConsumer.java").writeText(
            """
            package example;

            import com.keecon.restdocs.apispec.MockMvcRestDocumentationWrapper;
            import com.keecon.restdocs.apispec.ResourceDocumentation;
            import com.keecon.restdocs.apispec.WebTestClientRestDocumentationWrapper;
            import com.keecon.restdocs.apispec.jsonschema.JsonSchemaGenerator;
            import com.keecon.restdocs.apispec.jsonschema.StringAnyFormatValidator;
            import org.everit.json.schema.FormatValidator;
            import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
            import org.springframework.restdocs.snippet.Snippet;
            import org.springframework.test.web.reactive.server.EntityExchangeResult;

            import java.util.Collections;
            import java.util.function.Consumer;

            class PublishedApiConsumer {
                RestDocumentationResultHandler handler() {
                    return MockMvcRestDocumentationWrapper.document(
                        "operation",
                        MockMvcRestDocumentationWrapper.resourceDetails()
                    );
                }

                Consumer<EntityExchangeResult<byte[]>> webTestClientHandler() {
                    return WebTestClientRestDocumentationWrapper.document(
                        "operation",
                        WebTestClientRestDocumentationWrapper.resourceDetails()
                    );
                }

                Snippet snippet() {
                    return ResourceDocumentation.resource();
                }

                String schema() {
                    return new JsonSchemaGenerator().generateSchema(Collections.emptyList(), null);
                }

                FormatValidator formatValidator() {
                    return new StringAnyFormatValidator("custom");
                }
            }
            """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("compileTestJava", "--stacktrace")
            .build()
    }

    @Test
    fun `published plugin executes kotlin dsl configuration`(@TempDir projectDir: Path) {
        val repository = requiredSystemProperty("consumerTestRepository")
        val version = requiredSystemProperty("consumerTestVersion")
        val repositoryUri = Path.of(repository).toUri()

        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven {
                        url = uri("$repositoryUri")
                        metadataSources {
                            mavenPom()
                            artifact()
                        }
                    }
                    gradlePluginPortal()
                }
            }
            rootProject.name = "published-kotlin-consumer"
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                id("com.keecon.restdocs-openapi3") version "$version"
            }

            repositories {
                maven {
                    url = uri("$repositoryUri")
                    metadataSources {
                        mavenPom()
                        artifact()
                    }
                }
                mavenCentral()
            }

            openapi3 {
                server("https://api.example.com")
                contact {
                    name = "API Support"
                    email = "support@example.com"
                }
                oauth2SecuritySchemeDefinition {
                    flows = arrayOf("authorizationCode")
                    tokenUrl = "https://example.com/token"
                    authorizationUrl = "https://example.com/authorize"
                }
            }
            """.trimIndent()
        )
        writeResourceSnippet(projectDir)

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("openapi3", "--stacktrace")
            .build()

        then(result.task(":openapi3")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        with(JsonPath.parse(projectDir.resolve("build/api-spec/openapi3.json").toFile().readText())) {
            then(read<String>("servers[0].url")).isEqualTo("https://api.example.com")
            then(read<String>("info.contact.name")).isEqualTo("API Support")
            then(read<String>("info.contact.email")).isEqualTo("support@example.com")
            then(read<String>("components.securitySchemes.oauth2.type")).isEqualTo("oauth2")
            then(read<String>("components.securitySchemes.oauth2.flows.authorizationCode.tokenUrl"))
                .isEqualTo("https://example.com/token")
            then(read<String>("components.securitySchemes.oauth2.flows.authorizationCode.authorizationUrl"))
                .isEqualTo("https://example.com/authorize")
        }
    }

    private fun writeResourceSnippet(projectDir: Path) {
        val operationDirectory = projectDir.resolve("build/generated-snippets/some-operation")
        operationDirectory.createDirectories()
        operationDirectory.resolve("resource.json").writeText(
            """
            {
              "operationId": "product-get",
              "privateResource": false,
              "deprecated": false,
              "request": {
                "path": "/products/{id}",
                "method": "GET",
                "securityRequirements": {
                  "type": "OAUTH2",
                  "requiredScopes": []
                },
                "headers": [],
                "pathParameters": [],
                "queryParameters": [],
                "formParameters": [],
                "requestParts": [],
                "requestFields": []
              },
              "response": {
                "status": 200,
                "contentType": "application/json",
                "headers": [],
                "responseFields": [],
                "example": "{}"
              }
            }
            """.trimIndent()
        )
    }

    private fun writeRfc3339SnippetProducer(projectDir: Path) {
        val sourceDirectory = projectDir.resolve("src/main/java/example").apply { createDirectories() }
        sourceDirectory.resolve("GenerateRfc3339Snippet.java").writeText(
            """
            package example;

            import com.keecon.restdocs.apispec.Constraints;
            import com.keecon.restdocs.apispec.MockMvcRestDocumentationWrapper;
            import com.keecon.restdocs.apispec.ResourceSnippetParameters;
            import org.springframework.http.MediaType;
            import org.springframework.restdocs.ManualRestDocumentation;
            import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
            import org.springframework.restdocs.payload.PayloadDocumentation;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.test.web.servlet.setup.MockMvcBuilders;

            import java.time.Instant;

            import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;

            public final class GenerateRfc3339Snippet {
                private GenerateRfc3339Snippet() {
                }

                public static void main(String[] args) throws Exception {
                    var outputDirectory = args[0];
                    var dateTime = args[1];
                    var documentation = new ManualRestDocumentation(outputDirectory);
                    documentation.beforeTest(GenerateRfc3339Snippet.class, "generate");
                    var descriptor = Constraints.model(Rfc3339Response.class)
                        .withPath("createdAt")
                        .description("creation time");
                    var mockMvc = MockMvcBuilders.standaloneSetup(new TimeController(dateTime))
                        .apply(MockMvcRestDocumentation.documentationConfiguration(documentation))
                        .build();

                    mockMvc.perform(get("/times").accept(MediaType.APPLICATION_JSON))
                        .andDo(MockMvcRestDocumentationWrapper.document(
                            "product-get",
                            ResourceSnippetParameters.builder(),
                            PayloadDocumentation.responseFields(descriptor)
                        ));
                    documentation.afterTest();
                }

                @RestController
                private static final class TimeController {
                    private final String dateTime;

                    private TimeController(String dateTime) {
                        this.dateTime = dateTime;
                    }

                    @GetMapping(value = "/times", produces = MediaType.APPLICATION_JSON_VALUE)
                    String getTime() {
                        return "{\"createdAt\":\"" + dateTime + "\"}";
                    }
                }

                private static final class Rfc3339Response {
                    private Instant createdAt;
                }
            }
            """.trimIndent()
        )
    }

    private fun requiredSystemProperty(name: String): String =
        requireNotNull(System.getProperty(name)) { "Required system property '$name' is missing" }
}
