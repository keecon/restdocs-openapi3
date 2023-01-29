package com.keecon.restdocs.apispec.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.keecon.restdocs.apispec.Attributes
import com.keecon.restdocs.apispec.Constraints
import com.keecon.restdocs.apispec.DataType
import com.keecon.restdocs.apispec.ResourceSnippetParameters
import com.keecon.restdocs.apispec.example.dto.*
import com.keecon.restdocs.apispec.example.model.FileType
import com.keecon.restdocs.apispec.example.model.ProductResult
import com.keecon.restdocs.apispec.example.model.ProductResultAssignObject
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static com.keecon.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import static com.keecon.restdocs.apispec.ResourceDocumentation.resource
import static com.keecon.restdocs.apispec.Schema.schema
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductController.class)
@AutoConfigureRestDocs
class ProductControllerSpec extends Specification {

	@Autowired
	MockMvc mockMvc

	@Autowired
	ObjectMapper objectMapper

	@SpringBean
	ProductService mockProductService = Mock()

	def 'POST /v1/products/1/result (200 OK)'() {
		given:
		def result = ProductResult.builder()
			.code(1)
			.seq(1)
			.score(BigDecimal.TEN)
			.assign(ProductResultAssignObject.builder()
				.code(1L)
				.seq(1)
				.objectId(1L)
				.fileType(FileType.VIDEO)
				.fileUrl("/assets/product/1/1/1/XMHwNo6DDTYfh3wWfC7zsG.mp4")
				.comment("bad quality")
				.build()
			)
			.assign(ProductResultAssignObject.builder()
				.code(1L)
				.seq(2)
				.objectId(2L)
				.fileType(FileType.SOUND)
				.fileUrl("/assets/product/1/2/2/IyzfoVnVlKeHfZRIyR6pVP.mp3")
				.build()
			)
			.build()

		when:
		def resultActions = mockMvc.perform(
			post('/v1/products/{productId}/result', 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(new ProductResultCreateRequestBody(result)))
				.accept(MediaType.APPLICATION_JSON)
		)

		then:
		1 * mockProductService.createResult(1L, _) >> 1L

		def reqModel = Constraints.model(ProductResultCreateRequest.class)
		def reqBodyModel = Constraints.model(ProductResultCreateRequestBody.class)
		def respModel = Constraints.model(ProductResultCreateResponse.class)
		resultActions
			.andExpect(status().isOk())
			.andDo(document('products-id-result-post',
				resource(ResourceSnippetParameters.builder()
					.tag('product')
					.summary('Create a product result')
					.description('''
						|Create a product result
						|
						|### Error details
						|
						|`400` BAD_REQUEST
						|- bad request description
						|
						|`401` UNAUTHORIZED
						|- unauthorized description
						|
						|'''.stripMargin())
					.requestSchema(schema('ProductResultCreateRequest'))
					.pathParameters(
						reqModel.withName('productId').description('product id'),
					)
					.requestFields(
						reqBodyModel.withPath('result').description('product result'),
						reqBodyModel.withPath('result.code').description('product result code'),
						reqBodyModel.withPath('result.seq').description('product result seq'),
						reqBodyModel.withPath('result.score').description('product result score'),
						reqBodyModel.withPath('result.assigns[]').description('result assign object list'),
						reqBodyModel.withPath('result.assigns[].code').description('result assign code'),
						reqBodyModel.withPath('result.assigns[].seq').description('result assign seq'),
						reqBodyModel.withPath('result.assigns[].objectId').description('result assign object id'),
						reqBodyModel.withPath('result.assigns[].fileType').description('result assign file type')
							.optional(),
						reqBodyModel.withPath('result.assigns[].fileUrl').description('result assign file url')
							.optional(),
						reqBodyModel.withPath('result.assigns[].comments[]').description('result assign comment list')
							.type(DataType.ARRAY)
							.attributes(Attributes.items(DataType.STRING, null, null))
							.optional(),
					)
					.responseSchema(schema('ProductResultCreateResponse'))
					.responseFields(
						respModel.withPath('status').description('operation status'),
						respModel.withPath('code').description('product result code')
							.optional(),
					)
					.build())))
	}

	def 'GET /v1/products/1/result (200 OK)'() {
		given:
		def result = ProductResult.builder()
			.code(1)
			.seq(1)
			.score(BigDecimal.TEN)
			.assign(ProductResultAssignObject.builder()
				.code(1L)
				.seq(1)
				.objectId(1L)
				.fileType(FileType.VIDEO)
				.fileUrl("/assets/product/1/1/1/XMHwNo6DDTYfh3wWfC7zsG.mp4")
				.comment("bad quality")
				.build()
			)
			.assign(ProductResultAssignObject.builder()
				.code(1L)
				.seq(2)
				.objectId(2L)
				.fileType(FileType.SOUND)
				.fileUrl("/assets/product/1/2/2/IyzfoVnVlKeHfZRIyR6pVP.mp3")
				.build()
			)
			.build()

		when:
		def resultActions = mockMvc.perform(
			get('/v1/products/{productId}/result?code={code}', 1, 1)
				.accept(MediaType.APPLICATION_JSON)
		)

		then:
		1 * mockProductService.getResult(1L, 1, _) >> result

		def reqModel = Constraints.model(ProductResultRequest.class)
		def respModel = Constraints.model(ProductResultResponse.class)
		resultActions
			.andExpect(status().isOk())
			.andDo(document('products-id-result-code-get',
				resource(ResourceSnippetParameters.builder()
					.tag('product')
					.summary('Get a product result info')
					.description('''
						|Get a product result info
						|
						|### Error details
						|
						|`400` BAD_REQUEST
						|- bad request description
						|
						|`401` UNAUTHORIZED
						|- unauthorized description
						|
						|`404` NOT_FOUND
						|- not found description
						|
						|'''.stripMargin())
					.requestSchema(schema('ProductResultRequest'))
					.pathParameters(
						reqModel.withName('productId').description('product id'),
					)
					.queryParameters(
						reqModel.withName('code').description('product result code'),
						reqModel.withName('seq').description('product result seq')
							.defaultValue(ProductResultRequest.DEFAULT_RESULT_SEQ)
							.optional(),
					)
					.responseSchema(schema('ProductResultResponse'))
					.responseFields(
						respModel.withPath('result').description('product result'),
						respModel.withPath('result.code').description('product result code'),
						respModel.withPath('result.seq').description('product result seq'),
						respModel.withPath('result.score').description('product result score'),
						respModel.withPath('result.assigns[]').description('result assign object list'),
						respModel.withPath('result.assigns[].code').description('result assign code'),
						respModel.withPath('result.assigns[].seq').description('result assign seq'),
						respModel.withPath('result.assigns[].objectId').description('result assign object id'),
						respModel.withPath('result.assigns[].fileType').description('result assign file type')
							.optional(),
						respModel.withPath('result.assigns[].fileUrl').description('result assign file url')
							.optional(),
						respModel.withPath('result.assigns[].comments[]').description('result assign comment list')
							.type(DataType.ARRAY)
							.attributes(Attributes.items(DataType.STRING, null, null))
							.optional(),
					)
					.build())))
	}

	def 'PUT /v1/products/1/result/files (200 OK)'() {
		when:
		def resultActions = mockMvc.perform(
			put('/v1/products/{productId}/result/files', 1)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("fileType", FileType.IMAGE.name())
				.param("fileUrl", "4Dd1kOJFCijSxpxUjlCNJC.png")
				.accept(MediaType.APPLICATION_JSON)
		)

		then:
		1 * mockProductService.updateResult(1L, _) >> 1L

		def reqModel = Constraints.model(ProductResultUpdateRequest.class)
		def respModel = Constraints.model(ProductResultUpdateResponse.class)
		resultActions
			.andExpect(status().isOk())
			.andDo(document('products-id-result-files-post',
				resource(ResourceSnippetParameters.builder()
					.tag('product')
					.summary('Append a file to the product result')
					.description('''
						|Append a file to the product result
						|
						|### Error details
						|
						|`400` BAD_REQUEST
						|- bad request description
						|
						|`401` UNAUTHORIZED
						|- unauthorized description
						|
						|'''.stripMargin())
					.requestSchema(schema('ProductResultUpdateRequest'))
					.pathParameters(
						reqModel.withName('productId').description('product id'),
					)
					.formParameters(
						reqModel.withName('fileType').description('file type'),
						reqModel.withName('fileUrl').description('file url'),
					)
					.responseSchema(schema('ProductResultUpdateResponse'))
					.responseFields(
						respModel.withPath('status').description('operation status'),
					)
					.build())))
	}
}
