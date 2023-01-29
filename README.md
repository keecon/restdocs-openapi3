# Spring REST Docs OpenAPI 3 Specification

[![jitpack-badge]](https://jitpack.io/#keecon/restdocs-openapi3)
[![build-badge]](https://github.com/keecon/restdocs-openapi3/actions/workflows/build.yml)
[![codecov-badge]](https://codecov.io/gh/keecon/restdocs-openapi3)
[![sonarcloud-badge]](https://sonarcloud.io/summary/new_code?id=keecon_restdocs-openapi3)
[![license-badge]](https://github.com/keecon/restdocs-openapi3/blob/main/LICENSE)

A modified version of the [restdocs-api-spec] with class field type and constraint inference.
And only support [OpenAPI 3.0.1] specs.

## Getting started

### Build configuration

#### Gradle

1. Add the plugin

    ```groovy
    buildscript() {
      repositories {
        // ...
        maven { url "https://jitpack.io" }
      }
      dependencies {
        // ...
        classpath 'com.github.keecon:restdocs-openapi3:x.x.x'
      }
    }

    plugins {
      // ...
      id 'com.github.keecon:restdocs-openapi3'
    }
    ```

2. Add required dependencies to your tests

    ```groovy
    repositories {
      // ...
      maven { url 'https://jitpack.io' }
    }

    dependencies {
      //..
      testImplementation 'com.github.keecon:restdocs-openapi3:x.x.x'
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

### Usage with Spring REST Docs

```groovy
when:
def resultActions = mockMvc.perform(
  get('/v1/system-uptime')
    .accept(MediaType.APPLICATION_JSON)
)

then:
def respModel = Constraints.model(SystemUptimeResponse.class)
resultActions
  .andExpect(status().isOk())
  .andExpect(jsonPath('$.up', not(emptyOrNullString())))
  .andExpect(jsonPath('$.datetime', not(emptyOrNullString())))
  .andDo(document('system-uptime-get',
    resource(ResourceSnippetParameters.builder()
      .summary('Get a system uptime info')
      .description('''
        |Get a system uptime info
        |- Each server node differs in uptime
        |'''.stripMargin())
      .responseSchema(schema('SystemUptimeResponse'))
      .responseFields(
        respModel.withPath('up').description('uptime (duration)'),
        respModel.withPath('datetime').description('current time (date-time)')
          .attributes(Attributes.format(DataFormat.DATETIME)),
      )
      .build()))
  )
```

```groovy
when:
def resultActions = mockMvc.perform(
  get('/v1/products/{productId}/result?code={code}', 2022, 2012)
    .accept(MediaType.APPLICATION_JSON)
    .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_VALUE)
)

then:
def reqModel = Constraints.model(ProductResultRequest.class)
def respModel = Constraints.model(ProductResultResponse.class)
resultActions
  .andExpect(status().isOk())
  .andDo(document('products-id-result-code-get',
    resource(ResourceSnippetParameters.builder()
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
      .requestParameters(
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
        respModel.withPath('result.assigns[].objectType').description('result assign object type'),
        respModel.withPath('result.assigns[].objectId').description('result assign object id'),
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

[codecov-badge]: https://codecov.io/gh/keecon/restdocs-openapi3/branch/main/graph/badge.svg?token=TRQZ6GOVK4

[sonarcloud-badge]: https://sonarcloud.io/api/project_badges/measure?project=keecon_restdocs-openapi3&metric=alert_status

[license-badge]: https://img.shields.io/github/license/keecon/restdocs-openapi3.svg

[restdocs-api-spec]: https://github.com/ePages-de/restdocs-api-spec

[OpenAPI 3.0.1]: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.1.md
