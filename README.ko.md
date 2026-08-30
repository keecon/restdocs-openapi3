# Spring REST Docs OpenAPI 3 Specification

[English](README.md) | 한국어

[![jitpack-badge]](https://jitpack.io/#keecon/restdocs-openapi3)
[![build-badge]](https://github.com/keecon/restdocs-openapi3/actions/workflows/build.yml)
[![codecov-badge]](https://codecov.io/gh/keecon/restdocs-openapi3)
[![sonarcloud-badge]](https://sonarcloud.io/summary/new_code?id=keecon_restdocs-openapi3)
[![license-badge]](https://github.com/keecon/restdocs-openapi3/blob/main/LICENSE)

이 프로젝트는 [ePages-de/restdocs-api-spec]을 수정하여 클래스 필드 타입과 제약 조건 추론을
지원하는 버전입니다. [OpenAPI 3.0.1] 명세만 지원합니다.

## 빌드 구성

### 버전

| 아티팩트 버전대 | Spring Boot | Spring REST Docs | Java 바이트코드 | 테스트 JDK | 상태 |
|---|---|---|---|---|---|
| [2.x (`main`)](https://github.com/keecon/restdocs-openapi3/tree/main) | 4.1.x | 4.0.x | 17 | 17, 21, 25 | 활성(최신 릴리스: 2.1.3) |
| [1.x (`v1.x`)](https://github.com/keecon/restdocs-openapi3/tree/v1.x) | 3.5.x | 3.0.x | 17 | 17, 21, 25 | 유지보수 중 |
| [0.x (`v0.x`)](https://github.com/keecon/restdocs-openapi3/tree/v0.x) | 2.7.x | 2.0.x | — | — | 동결, 지원 종료 |

모든 릴리스 버전대에서 공개 패키지는 `com.keecon.restdocs.*`로 유지되며 Gradle 플러그인 ID는
`com.keecon.restdocs-openapi3`으로 유지됩니다.

2.x 버전대는 Java 17 이상이 필요합니다. CI에서는 LTS JDK 릴리스 17, 21, 25를 검증합니다.
Spring Boot 3.5 애플리케이션에서는 1.1.2를 사용하세요. 버전 2.1.3은 JitPack에서 제공되며
Plugin Portal 배포는 전제하지 않습니다.

브랜치 수명 주기, 백포트 및 릴리스 정책은 [MAINTENANCE.md](MAINTENANCE.md)를 참고하세요.

### Gradle

1. 플러그인을 추가합니다.

    ```groovy
    buildscript {
      repositories {
        // ...
        maven { url = uri('https://jitpack.io') }
      }
      dependencies {
        // ...
        classpath 'com.github.keecon.restdocs-openapi3:restdocs-api-spec-gradle-plugin:2.1.3'
      }
    }

    apply plugin: 'com.keecon.restdocs-openapi3'
    ```

2. 테스트에 필요한 의존성을 추가합니다.

    ```groovy
    repositories {
      // ...
      maven { url 'https://jitpack.io' }
    }

    dependencies {
      //..
      testImplementation 'com.github.keecon.restdocs-openapi3:restdocs-api-spec:2.1.3'
      testImplementation 'com.github.keecon.restdocs-openapi3:restdocs-api-spec-mockmvc:2.1.3'
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

#### 2.1.1 DSL 추가 기능

현재 `main` 브랜치에서 `openapi3` 태스크는 프로젝트의 Gradle `layout.buildDirectory`를 기준으로
기본 입력 및 출력 디렉터리를 결정합니다. 표준 `build` 디렉터리를 사용하는 경우
`build/generated-snippets`에서 스니펫을 읽고 `format`에 따라
`build/api-spec/openapi3.json` 또는 `build/api-spec/openapi3.yaml`에 결과를 작성합니다.

이 빌드 디렉터리 연동 동작과 아래의 추가 메서드 및 Gradle `Action` 구문은 2.1.1부터
사용할 수 있습니다. 2.1.0에서는 위에 표시된 대입 구문을 사용하세요.

Groovy 빌드에서는 메서드 구문을 사용할 수 있습니다.

```groovy
openapi3 {
  server 'https://api.example.com'
  contact {
    name = 'API Support'
    email = 'support@example.com'
  }
}
```

동일한 JitPack 빌드스크립트 classpath를 추가하고 플러그인을 적용한 뒤, Kotlin 빌드에서는
이름이 지정된 확장을 명시적으로 구성할 수 있습니다.

```kotlin
import com.keecon.restdocs.apispec.gradle.OpenApi3Extension

extensions.configure<OpenApi3Extension>("openapi3") {
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
```

문서화된 JWT scope가 OAuth2 보안 요구사항을 생성하는 경우 OAuth2 flow를 하나 이상 구성해야 합니다.
이에 대응하는 OAuth2 보안 스킴 정의가 없으면 명세 생성이 실패합니다.

### 1.x에서 2.x로 마이그레이션

2.x 버전대는 Spring Boot 4, Spring REST Docs 4, Jackson 3으로 전환되었습니다. 확장 수준의
Groovy 구성은 계속 호환됩니다. `openapi3` 태스크를 직접 구성하는 코드는 관리형 Gradle
프로퍼티에 맞게 변경해야 합니다. 스칼라 태스크 getter는 이제 `Property<T>`를 반환하고
디렉터리 getter는 `DirectoryProperty`를 반환하므로, 1.x의 `String`/`Boolean` 태스크
프로퍼티처럼 대입하지 말고 `.set(...)`으로 값을 지정하세요.

### 2.1.0 기능

Gradle DSL에서 OpenAPI 연락처 메타데이터를 구성할 수 있습니다.

```groovy
openapi3 {
  contact = {
    name = 'API Support'
    email = 'support@example.com'
    url = 'https://example.com/support'
  }
}
```

`java.time.LocalDate`로 선언된 필드는 OpenAPI `string` 값과 `date` 형식으로 추론됩니다.

WebTestClient 통합은 `restdocs-api-spec-webtestclient` 모듈에서 제공합니다.

```groovy
dependencies {
  testImplementation 'com.github.keecon.restdocs-openapi3:restdocs-api-spec-webtestclient:2.1.3'
}
```

Spring REST Docs의 WebTestClient `document` consumer를 wrapper로 교체하세요. 일반 REST Docs
스니펫은 그대로 유지하면서 해당 descriptor에서 `resource.json`을 추가로 생성합니다.

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

## Spring REST Docs와 함께 사용하기

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
