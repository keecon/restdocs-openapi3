package com.keecon.restdocs.apispec

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

@ExtendWith(SpringExtension::class)
@WebMvcTest
class ListSomethingIntegrationTest(
    @Autowired private val mockMvc: MockMvc
) : ResourceSnippetIntegrationTest() {

    @Test
    fun should_document_both_restdocs_and_resource() {
        givenEndpointInvoked()

        whenDocumentedWithRestdocsAndResource()

        thenSnippetFileExists()
    }

    @Test
    fun should_document_request_with_parameters() {
        givenEndpointInvoked()

        whenResourceSnippetDocumentedWithRequestParametersAndResponseFields()

        thenSnippetFileExists()
    }

    @Test
    fun should_document_request_with_array_parameters() {
        givenEndpointListSelectInvoked(listOf(1, 2, 3))

        whenResourceSnippetDocumentedWithArrayRequestParameter()

        thenSnippetFileExists()
    }

    private fun givenEndpointInvoked(flagValue: Boolean? = true) {
        operationName = "list-something-${System.currentTimeMillis()}"
        resultActions = mockMvc.perform(
            get("/some/{someId}/other/{otherId}", "id", 1)
                .contentType(APPLICATION_JSON)
                .accept(HAL_JSON)
                .header("X-Custom-Header", "test")
                .queryParam("comment", "some")
                .queryParam("flag", flagValue?.toString())
                .queryParam("count", "1")
        ).andExpect(status().isOk)
    }

    private fun givenEndpointListSelectInvoked(params: List<Int>) {
        operationName = "list-select-something-${System.currentTimeMillis()}"
        val builder =
            get("/some/select")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .queryParam("code", *params.map(Int::toString).toTypedArray())
        resultActions = mockMvc.perform(builder).andExpect(status().isOk)
    }

    private fun thenSnippetFileExists() {
        with(generatedSnippetFile()) {
            then(this).exists()
            val contents = readText()
            then(contents).isNotEmpty
        }
    }

    private fun generatedSnippetFile() = File("build/generated-snippets", "$operationName/resource.json")

    @Throws(Exception::class)
    private fun whenDocumentedWithRestdocsAndResource() {
        val model = Constraints.model(TestDataHolder::class.java)
        resultActions
            .andDo(print())
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = operationName,
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("someId").description("someId"),
                            parameterWithName("otherId").description("otherId")
                        ),
                        requestHeaders(
                            headerWithName("X-Custom-Header").description("some custom header")
                        ),
                        queryParameters(
                            parameterWithName("comment").description("the comment").optional(),
                            parameterWithName("flag").description("the flag"),
                            parameterWithName("count").description("the count")
                        ),
                        responseFields(
                            model.withPath("comment").description("the comment").optional(),
                            model.withPath("flag").description("the flag"),
                            model.withMappedPath("count", "count").description("the count"),
                            model.withPath("id").description("the id"),
                            subsectionWithPath("_links").ignored()
                        ),
                        responseHeaders(
                            headerWithName("X-Custom-Header").description("some custom header")
                        ),
                        links(
                            linkWithRel("self").description("some"),
                            linkWithRel("multiple").description("multiple")
                        )
                    )
                )
            )
    }

    private fun whenResourceSnippetDocumentedWithRequestParametersAndResponseFields() {
        resultActions
            .doPrintAndDocument(buildResourceSnippetWithRequestParameters())
    }

    @Throws(Exception::class)
    private fun whenResourceSnippetDocumentedWithArrayRequestParameter() {
        val model = Constraints.model(TestSelect::class.java)
        resultActions.doPrintAndDocument(
            ResourceSnippetParameters.builder()
                .description("description")
                .summary("summary")
                .queryParameters(
                    model.withMappedName("code", "code[]").description("the code list")
                )
                .responseFields(
                    model.withPath("code[]").description("the code list"),
                    model.withPath("id").description("the id"),
                )
                .build()
        )
    }
}
