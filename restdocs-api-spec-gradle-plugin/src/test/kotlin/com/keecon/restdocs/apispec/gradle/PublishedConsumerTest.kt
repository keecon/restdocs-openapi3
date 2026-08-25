package com.keecon.restdocs.apispec.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PublishedConsumerTest {

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

    private fun requiredSystemProperty(name: String): String =
        requireNotNull(System.getProperty(name)) { "Required system property '$name' is missing" }
}
