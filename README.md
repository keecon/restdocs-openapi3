# Spring REST Docs OpenAPI 3 Specification

[![jitpack-badge]](https://jitpack.io/#keecon/restdocs-openapi3)
[![build-badge]](https://github.com/keecon/restdocs-openapi3/actions/workflows/build.yml)
[![codecov-badge]](https://codecov.io/gh/keecon/restdocs-openapi3)
[![sonarcloud-badge]](https://sonarcloud.io/summary/new_code?id=keecon_restdocs-openapi3)
[![license-badge]](https://github.com/keecon/restdocs-openapi3/blob/main/LICENSE)

A modified version of the [ePages-de/restdocs-api-spec] with class field type and constraint inference.
And only support [OpenAPI 3.0.1] specs.

## Build configuration

### Versions

| Artifact line | Spring Boot | Spring REST Docs | Java bytecode | Tested JDKs | Status |
|---|---|---|---|---|---|
| 1.x | 3.5.x | 3.0.x | 17 | 17, 21, 25 | Maintained |
| 2.x | 4.1.x | 4.0.x | 17 | 17, 21, 25 | Released (2.1.0) |
| 0.x | 2.7.x | 2.0.x | — | — | Frozen, unsupported |

The public packages remain under `com.keecon.restdocs.*`, and the Gradle plugin ID remains
`com.keecon.restdocs-openapi3` across release lines.

The 2.x line requires Java 17 or newer. CI verifies the LTS JDK releases 17, 21, and 25.
Use 1.1.0 for Spring Boot 3.5 applications. Version 2.1.0 is available from JitPack; no Plugin
Portal publication is assumed.

### Gradle

1. Add the plugin

    ```groovy
    buildscript {
      repositories {
        // ...
        maven { url = uri('https://jitpack.io') }
      }
      dependencies {
        // ...
        classpath 'com.github.keecon.restdocs-openapi3:restdocs-api-spec-gradle-plugin:2.1.0'
      }
    }

    apply plugin: 'com.keecon.restdocs-openapi3'
    ```

2. Add required dependencies to your tests

    ```groovy
    repositories {
      // ...
      maven { url 'https://jitpack.io' }
    }

    dependencies {
      //..
      testImplementation 'com.github.keecon.restdocs-openapi3:restdocs-api-spec:2.1.0'
      testImplementation 'com.github.keecon.restdocs-openapi3:restdocs-api-spec-mockmvc:2.1.0'
    }

    openapi3 {
      server = 'http://localhost:8080'
      title = 'My API'
      description = 'My API description'
      tagDescriptionsPropertiesFile = 'src/test/resources/openapi-tags.yml'
      version = '0.1.0'
      format = 'yaml'
    }
    ```

### 2.1.0 features

OpenAPI contact metadata can be configured in the Gradle DSL.

```groovy
openapi3 {
  contact = {
    name = 'API Support'
    email = 'support@example.com'
    url = 'https://example.com/support'
  }
}
```

Fields declared as `java.time.LocalDate` are inferred as OpenAPI `string` values with
`format: date`.

The WebTestClient integration is available from the `restdocs-api-spec-webtestclient` module.

```groovy
dependencies {
  testImplementation 'com.github.keecon.restdocs-openapi3:restdocs-api-spec-webtestclient:2.1.0'
}
```

Replace Spring REST Docs' WebTestClient `document` consumer with the wrapper. It keeps the normal
REST Docs snippets and adds `resource.json` from their descriptors.

```groovy
webTestClient.get()
  .uri('/v1/products/{id}', 1)
  .exchange()
  .expectStatus().isOk()
  .expectBody()
  .consumeWith(WebTestClientRestDocumentationWrapper.document(
    'products-id-get',
    responseFields(fieldWithPath('id').description('product id'))
  ))
```

## Usage with Spring REST Docs

```groovy
when:
def resultActions = mockMvc.perform(
  post('/v1/products/{productId}/result', 1)
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsBytes(new ProductResultCreateRequestBody(result)))
    .accept(MediaType.APPLICATION_JSON)
)

then:
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
```

```groovy
when:
def resultActions = mockMvc.perform(
  get('/v1/products/{productId}/result?code={code}', 1, 1)
    .accept(MediaType.APPLICATION_JSON)
)

then:
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
```

[jitpack-badge]: https://jitpack.io/v/keecon/restdocs-openapi3.svg

[build-badge]: https://github.com/keecon/restdocs-openapi3/actions/workflows/build.yml/badge.svg

[codecov-badge]: https://codecov.io/gh/keecon/restdocs-openapi3/branch/main/graph/badge.svg

[sonarcloud-badge]: https://sonarcloud.io/api/project_badges/measure?project=keecon_restdocs-openapi3&metric=alert_status

[license-badge]: https://img.shields.io/github/license/keecon/restdocs-openapi3.svg

[ePages-de/restdocs-api-spec]: https://github.com/ePages-de/restdocs-api-spec

[OpenAPI 3.0.1]: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.1.md
