package com.keecon.restdocs.apispec

import com.keecon.restdocs.apispec.ResourceDocumentation.parameterWithName
import com.keecon.restdocs.apispec.ResourceDocumentation.resource
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.mvc.BasicLinkBuilder
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ExtendWith(SpringExtension::class)
@WebMvcTest
@AutoConfigureRestDocs
open class ResourceSnippetIntegrationTest {

    lateinit var operationName: String
    lateinit var resultActions: ResultActions

    @SpringBootApplication
    open class TestApplication {
        lateinit var applicationContext: ConfigurableApplicationContext
        fun main(args: Array<String>) {
            applicationContext = SpringApplication.run(TestApplication::class.java, *args)
        }

        @RestController
        internal open class TestController {

            @PostMapping(path = ["/some/{someId}/other/{otherId}"])
            fun doSomething(
                @PathVariable someId: String,
                @PathVariable otherId: Int?,
                @RequestHeader("X-Custom-Header") customHeader: String,
                @Valid @RequestBody testDataHolder: TestDataHolder
            ): ResponseEntity<EntityModel<TestDataHolder>> {
                val resource = EntityModel.of(testDataHolder.copy(id = UUID.randomUUID().toString()))
                val link = BasicLinkBuilder.linkToCurrentMapping()
                    .slash("some").slash(someId)
                    .slash("other").slash(otherId)
                    .toUri().toString()
                resource.add(Link.of(link).withSelfRel())
                resource.add(Link.of(link, "multiple"))
                resource.add(Link.of(link, "multiple"))

                return ResponseEntity.ok()
                    .header("X-Custom-Header", customHeader)
                    .body(resource)
            }

            @GetMapping(path = ["/some/{someId}/other/{otherId}"])
            fun listSomething(
                @PathVariable someId: String,
                @PathVariable otherId: Int?,
                @RequestHeader("X-Custom-Header") customHeader: String,
                @Valid testDataHolder: TestDataHolder
            ): ResponseEntity<EntityModel<TestDataHolder>> {
                val resource = EntityModel.of(testDataHolder.copy(id = UUID.randomUUID().toString()))
                val link = BasicLinkBuilder.linkToCurrentMapping()
                    .slash("some").slash(someId)
                    .slash("other").slash(otherId)
                    .toUri().toString()
                resource.add(Link.of(link).withSelfRel())
                resource.add(Link.of(link, "multiple"))
                resource.add(Link.of(link, "multiple"))

                return ResponseEntity.ok()
                    .header("X-Custom-Header", customHeader)
                    .body(resource)
            }

            @GetMapping(path = ["/some/select"])
            fun listSelectSomething(@Valid idsHolder: TestSelect): ResponseEntity<*> {
                val resource = EntityModel.of(idsHolder.copy(id = UUID.randomUUID().toString()))
                return ResponseEntity.ok().body(resource)
            }
        }
    }

    internal data class TestDataHolder(
        @field:NotEmpty
        @field:Size(min = 1, max = 255)
        val comment: String?,

        @field:NotNull
        val flag: Boolean,

        @field:NotNull @field:Min(1)
        val count: Int,

        val id: String?
    )

    internal data class TestSelect(
        @field:NotEmpty
        @field:Size(min = 1, max = 16)
        val code: List<Int>,

        val id: String?
    )

    fun ResultActions.doPrintAndDocument(snippet: ResourceSnippetParameters) =
        andDo(print()).andDo(document(operationName, resource(snippet)))

    fun fieldDescriptors(): FieldDescriptors {
        val model = Constraints.model(TestDataHolder::class.java)
        return ResourceDocumentation.fields(
            model.withPath("comment").description("the comment").optional(),
            model.withPath("flag").description("the flag"),
            model.withMappedPath("count", "count").description("the count")
        )
    }

    fun buildResourceSnippetWithRequestFields(): ResourceSnippetParameters {
        val model = Constraints.model(TestDataHolder::class.java)
        return ResourceSnippetParameters.builder()
            .description("description")
            .summary("summary")
            .deprecated(true)
            .privateResource(true)
            .requestFields(fieldDescriptors())
            .responseFields(fieldDescriptors().and(model.withPath("id").description("the id")))
            .requestHeaders(
                headerWithName("X-Custom-Header").description("A custom header"),
                headerWithName(ACCEPT).description("Accept")
            )
            .responseHeaders(
                headerWithName("X-Custom-Header").description("A custom header"),
                headerWithName(CONTENT_TYPE).description("ContentType")
            )
            .pathParameters(
                parameterWithName("someId").description("some id"),
                parameterWithName("otherId").description("otherId id")
            )
            .links(
                linkWithRel("self").description("some"),
                linkWithRel("multiple").description("multiple")
            )
            .build()
    }

    fun buildResourceSnippetWithRequestParameters(): ResourceSnippetParameters {
        val model = Constraints.model(TestDataHolder::class.java)
        return ResourceSnippetParameters.builder()
            .description("description")
            .summary("summary")
            .deprecated(true)
            .privateResource(true)
            .requestParameters(
                model.withName("comment").description("the comment").optional(),
                model.withName("flag").description("the flag").type(DataType.BOOLEAN),
                model.withMappedName("count", "count").description("the count")
            )
            .responseFields(fieldDescriptors().and(model.withPath("id").description("the id")))
            .requestHeaders(
                headerWithName("X-Custom-Header").description("A custom header"),
                headerWithName(ACCEPT).description("Accept")
            )
            .responseHeaders(
                headerWithName("X-Custom-Header").description("A custom header"),
                headerWithName(CONTENT_TYPE).description("ContentType")
            )
            .pathParameters(
                parameterWithName("someId").description("some id"),
                parameterWithName("otherId").description("otherId id")
            )
            .links(
                linkWithRel("self").description("some"),
                linkWithRel("multiple").description("multiple")
            )
            .build()
    }
}
