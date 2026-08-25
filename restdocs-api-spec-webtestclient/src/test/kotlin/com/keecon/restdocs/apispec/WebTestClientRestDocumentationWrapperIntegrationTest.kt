package com.keecon.restdocs.apispec

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.File

@ExtendWith(RestDocumentationExtension::class)
class WebTestClientRestDocumentationWrapperIntegrationTest {

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        webTestClient = WebTestClient.bindToController(TestController())
            .configureClient()
            .filter(documentationConfiguration(restDocumentation))
            .build()
    }

    @Test
    fun `should document restdocs snippets and resource snippet`() {
        val identifier = "webtestclient-wrapper"

        webTestClient.get()
            .uri("/greetings/{id}", "42")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                WebTestClientRestDocumentationWrapper.document(
                    identifier = identifier,
                    summary = "Get greeting",
                    snippets = arrayOf(
                        pathParameters(parameterWithName("id").description("greeting id")),
                        responseFields(fieldWithPath("message").description("greeting message"))
                    )
                )
            )

        val resource = File("build/generated-snippets/$identifier/resource.json")
        then(resource).exists()
        with(JsonPath.parse(resource.readText())) {
            then(read<String>("summary")).isEqualTo("Get greeting")
            then(read<String>("request.pathParameters[0].name")).isEqualTo("id")
            then(read<String>("response.responseFields[0].path")).isEqualTo("message")
        }
    }

    @RestController
    private class TestController {
        @GetMapping("/greetings/{id}")
        fun greeting(@PathVariable id: String) = mapOf("message" to "Hello $id")
    }
}
