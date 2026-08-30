# Audit Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 확인된 문서 생성 결함과 로컬 파일 안전 문제를 수정하고, 공개 Gradle 플러그인 및 CI 공급망을 최소 변경으로 강화한다.

**Architecture:** 모델과 공개 DSL은 유지하고 기존 generator/handler/task 경계 안에서 입력 검증과 파싱만 수정한다. 각 동작 변경은 해당 모듈의 회귀 테스트로 먼저 고정하며, 공급망 변경은 공개 POM과 strict dependency verification으로 검증한다.

**Tech Stack:** Gradle 9.7.1, Kotlin 2.4.10, Java toolchain 17, Spring Boot 4.1.1, Spring REST Docs 4.0.1, Jackson 3.1.5, JUnit 5, Gradle TestKit.

**Spec:** `docs/superpowers/specs/2026-08-30-audit-remediation-scope.md`

## Global Constraints

- 새 의존성을 추가하지 않는다.
- 공개 패키지, Gradle 플러그인 ID 및 기존 DSL ABI를 유지한다.
- Java 17 바이트코드와 JDK 17/21/25 테스트 범위를 유지한다.
- 구현 결함이 확인된 테스트는 기대값을 바꾸지 않고 제품 코드를 수정한다.
- 영어/한국어 README의 동작 설명과 코드 예제를 동기화한다.
- lockfile, Configuration Cache 기본 활성화, Isolated Projects 및 광범위한 빌드 리팩터링은 제외한다.
- 사용자 요청 전에는 커밋하거나 외부 시스템을 변경하지 않는다. 각 task 끝에서 diff와 테스트 결과만 검토한다.

---

### Task 1: 출력 파일 경로를 출력 디렉터리 안으로 제한

**Files:**
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecTask.kt`
- Test support: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecTaskTest.kt`
- Test: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/RestdocsOpenApi3TaskTest.kt`

**Interfaces:**
- Consumes: `outputFileNamePrefix: Property<String>`, `outputDirectory: DirectoryProperty`
- Produces: 출력 디렉터리의 직접 자식인 `.json`/`.yaml` 파일만 반환하는 `specificationFile(String): File`

- [x] **Step 1: 디렉터리 탈출 회귀 테스트 추가**

먼저 `ApiSpecTaskTest`에 기존 `whenPluginExecuted()`와 같은 인자를 사용하고 마지막 호출만
`buildAndFail()`로 바꾼 `whenPluginExecutionFails()` helper를 추가한다. `RestdocsOpenApi3TaskTest`에서
`outputFileNamePrefix`를 `../escaped`와 `..\\escaped`로 각각 구성하고 실패 결과가
`outputFileNamePrefix must be a simple file name`을 포함하는지 확인한다. 테스트 임시 프로젝트의 output
directory 부모에 `escaped.json`과 `escaped.yaml`이 생성되지 않았음도 검증한다.

```kotlin
protected fun whenPluginExecutionFails() {
    result = GradleRunner.create()
        .withProjectDir(testProjectDir.toFile())
        .withArguments(
            "--configuration-cache",
            "--configuration-cache-problems=fail",
            "--info",
            "--stacktrace",
            taskName
        )
        .withPluginClasspath()
        .buildAndFail()
}
```

- [x] **Step 2: RED 확인**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test \
  --tests '*RestdocsOpenApi3TaskTest*reject*output*prefix*' --no-daemon
```

Expected: 현재 구현이 prefix를 허용해 새 assertion이 실패한다.

- [x] **Step 3: 단순 파일명 및 canonical parent 검증 구현**

```kotlin
private fun specificationFile(outputFilenamePrefix: String): File {
    require(
        outputFilenamePrefix.isNotBlank() &&
            outputFilenamePrefix != "." &&
            outputFilenamePrefix != ".." &&
            '/' !in outputFilenamePrefix &&
            '\\' !in outputFilenamePrefix
    ) { "outputFileNamePrefix must be a simple file name" }

    val outputDirectory = outputDirectoryFile.canonicalFile
    val outputFile = File(outputDirectory, "$outputFilenamePrefix.${outputFileExtension()}").canonicalFile
    require(outputFile.parentFile == outputDirectory) {
        "Specification file must stay inside outputDirectory"
    }
    return outputFile
}
```

- [x] **Step 4: GREEN 확인**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test --no-daemon
git diff --check
```

Expected: 새 경로 거부 테스트와 기존 stale specification 삭제 테스트가 모두 통과한다.

### Task 2: 정의되지 않은 OAuth2 Security Requirement 차단

**Files:**
- Modify: `restdocs-api-spec-generator/src/main/kotlin/com/keecon/restdocs/apispec/generator/SecuritySchemeGenerator.kt`
- Test: `restdocs-api-spec-generator/src/test/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3GeneratorTest.kt`
- Modify: `README.md`
- Modify: `README.ko.md`

**Interfaces:**
- Consumes: operation의 `oauth2` security requirement와 `Oauth2Configuration?`
- Produces: OAuth2 flow가 없을 때 `IllegalArgumentException`, 있을 때 완전한 `components.securitySchemes.oauth2`

- [x] **Step 1: 구성 누락 회귀 테스트 추가**

`getProductRequest(::getOAuth2SecurityRequirement)`를 사용하는 resource를 만들고 다음을 검증한다.

```kotlin
val exception = assertThrows<IllegalArgumentException> {
    whenOpenApiObjectGeneratedWithoutOAuth2()
}
then(exception).hasMessage(
    "OAuth2 security requirements require oauth2SecuritySchemeDefinition with at least one flow"
)
```

- [x] **Step 2: RED 확인**

```bash
./gradlew :restdocs-api-spec-generator:test \
  --tests '*OpenApi3GeneratorTest*OAuth2*without*configuration*' --no-daemon
```

Expected: 예외가 발생하지 않아 새 테스트가 실패한다.

- [x] **Step 3: 스킴 생성 전에 fail-fast 검증 추가**

```kotlin
val hasOAuth2Requirement = hasAnyOperationWithSecurityName(this, OAUTH2_SECURITY_NAME)
require(!hasOAuth2Requirement || oauth2SecuritySchemeDefinition?.flows?.isNotEmpty() == true) {
    "OAuth2 security requirements require oauth2SecuritySchemeDefinition with at least one flow"
}
```

기존 flow 생성 로직과 Basic/API Key/JWT Bearer 정의 생성은 변경하지 않는다.

- [x] **Step 4: README 동기화 및 GREEN 확인**

OAuth2 DSL 예제 다음에 flow가 하나 이상 필요하며 OAuth2 scope를 문서화하면서 설정을 생략하면 생성이
실패한다는 문장을 양쪽 README에 추가한다.

```bash
./gradlew :restdocs-api-spec-generator:test --no-daemon
diff -u \
  <(perl -ne 'if (/^\s*```/) {$in = !$in; print; next} print if $in' README.md) \
  <(perl -ne 'if (/^\s*```/) {$in = !$in; print; next} print if $in' README.ko.md)
```

Expected: 테스트가 통과하고 두 README의 코드 블록 diff가 비어 있다.

### Task 3: JSON 예제를 표준 JVM 값으로 파싱

**Files:**
- Modify: `restdocs-api-spec-generator/src/main/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3Generator.kt`
- Test: `restdocs-api-spec-generator/src/test/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3GeneratorTest.kt`

**Interfaces:**
- Consumes: JSON media type의 `RequestModel.example` 및 `ResponseModel.example`
- Produces: 객체, 배열, 앞쪽 공백 및 모든 JSON 스칼라를 표현하는 `Map`, `List`, primitive 또는 null

- [x] **Step 1: 배열 공백과 스칼라 예제 회귀 테스트 추가**

기존 resource의 response를 `copy(example = "  [1, 2]")`와 `copy(example = "true")`로 교체해 생성하고
아래 값을 검증한다. `"text"`, `1`, `null`도 parameterized input으로 직렬화 실패가 없는지 확인한다.

```kotlin
then(openApiJsonPathContext.read<List<Int>>(
    "paths./products/{id}.get.responses.200.content.application/json.examples.test.value"
)).containsExactly(1, 2)

then(openApiJsonPathContext.read<Boolean>(
    "paths./products/{id}.get.responses.200.content.application/json.examples.test.value"
)).isTrue()
```

- [x] **Step 2: RED 확인**

```bash
./gradlew :restdocs-api-spec-generator:test \
  --tests '*OpenApi3GeneratorTest*JSON example*' --no-daemon
```

Expected: 앞쪽 공백 배열 또는 스칼라 역직렬화가 실패한다.

- [x] **Step 3: 첫 문자 분기를 범용 값 파싱으로 교체**

```kotlin
if (!contentType.contains("json")) {
    value(it.value)
} else {
    value(objectMapper.readValue(it.value, Any::class.java))
}
```

더 이상 사용하지 않는 Kotlin `readValue` import도 제거한다.

- [x] **Step 4: GREEN 확인**

```bash
./gradlew :restdocs-api-spec-generator:test --no-daemon
git diff --check
```

Expected: 새 JSON 경계 테스트와 기존 example aggregation 테스트가 모두 통과한다.

### Task 4: HTTP 인증 스킴을 대소문자 무관하게 처리

**Files:**
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/SecurityRequirementsHandler.kt`
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/JwtSecurityHandler.kt`
- Test: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/SecurityRequirementsHandlerTest.kt`
- Test: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/JwtSecurityHandlerTest.kt`

**Interfaces:**
- Consumes: `Authorization` header의 인증 스킴과 credentials
- Produces: `basic`, `BASIC`, `bearer`, `BEARER`에도 기존과 같은 `Basic`, `Oauth2`, `JWTBearer`

- [x] **Step 1: 혼합 대소문자 회귀 테스트 추가**

기존 credential과 JWT를 그대로 사용하면서 header의 스킴만 `bAsIc` 및 `bEaReR`로 바꾼 테스트를 추가한다.

- [x] **Step 2: RED 확인**

```bash
./gradlew :restdocs-api-spec:test \
  --tests '*SecurityRequirementsHandlerTest*mixed*case*' \
  --tests '*JwtSecurityHandlerTest*mixed*case*' --no-daemon
```

Expected: 두 결과가 `null`이어서 실패한다.

- [x] **Step 3: 스킴 prefix만 대소문자 무시 비교**

```kotlin
.any { it.regionMatches(0, "Basic ", 0, "Basic ".length, ignoreCase = true) }
```

Bearer는 동일한 `regionMatches`로 filter한 뒤 prefix 길이만큼 제거한다.

```kotlin
.filter { it.regionMatches(0, "Bearer ", 0, "Bearer ".length, ignoreCase = true) }
.map { it.substring("Bearer ".length) }
```

- [x] **Step 4: GREEN 확인**

```bash
./gradlew :restdocs-api-spec:test --no-daemon
```

Expected: malformed JWT와 scope 파싱을 포함한 기존 테스트도 모두 통과한다.

### Task 5: 공개 플러그인에서 불필요한 Kotlin runtime 의존성 제거

**Files:**
- Modify: `restdocs-api-spec-gradle-plugin/build.gradle.kts`
- Test: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/PublishedConsumerTest.kt`

**Interfaces:**
- Consumes: consumer repository의 `restdocs-api-spec-gradle-plugin` POM
- Produces: Kotlin Gradle Plugin runtime 의존성이 없고 기존 Groovy/Kotlin DSL consumer가 동작하는 플러그인

- [x] **Step 1: 공개 POM 비노출 테스트 추가**

```kotlin
val pluginPom = Path.of(repository)
    .resolve("com/keecon/restdocs-api-spec-gradle-plugin/$version/restdocs-api-spec-gradle-plugin-$version.pom")
then(pluginPom).exists()
then(pluginPom.toFile().readText()).doesNotContain("org.jetbrains.kotlin", "kotlin-gradle-plugin")
```

- [x] **Step 2: RED 확인**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test \
  --tests '*PublishedConsumerTest*runtime*dependency*' --no-daemon
```

Expected: 현재 POM에 `kotlin-gradle-plugin`이 있어 실패한다.

- [x] **Step 3: runtime 선언만 제거**

`restdocs-api-spec-gradle-plugin/build.gradle.kts`에서 다음 줄을 제거하고
`compileOnly(kotlin("gradle-plugin"))`은 유지한다.

```kotlin
implementation(libs.kotlin.gradle.plugin)
```

- [x] **Step 4: 공개 소비자와 POM 검증**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test --no-daemon
./gradlew :restdocs-api-spec-gradle-plugin:generatePomFileForPluginMavenPublication --no-daemon
! rg 'kotlin-gradle-plugin' \
  restdocs-api-spec-gradle-plugin/build/publications/pluginMaven/pom-default.xml
```

Expected: consumer 및 Kotlin DSL 테스트가 성공하고 공개 POM에 해당 의존성이 없다.

### Task 6: Gradle 및 GitHub Actions 공급망 강화

**Files:**
- Create: `gradle/verification-metadata.xml`
- Modify: `.github/workflows/build.yml`
- Modify: `.github/workflows/release.yml`

**Interfaces:**
- Consumes: 전체 빌드가 해석하는 플러그인 및 Maven 아티팩트
- Produces: strict SHA-256 dependency verification, 최소 workflow 권한, 불변 Action 참조

- [x] **Step 1: SHA-256 검증 메타데이터 생성 및 검토**

```bash
./gradlew --write-verification-metadata sha256 \
  clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
```

생성 파일에 `verify-metadata="true"`, `verify-signatures="false"`가 있고 `trusted-artifacts`,
verification 비활성화, 비밀정보나 로컬 경로가 없는지 직접 검토한다.

- [x] **Step 2: strict 모드 검증**

```bash
./gradlew --dependency-verification=strict \
  clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
```

Expected: 검증 실패나 누락 artifact 없이 성공한다. 누락 시 해당 태스크를 실제로 해석한 뒤 SHA-256만
추가하고 metadata diff를 다시 검토한다.

- [x] **Step 3: Actions를 현재 검증한 전체 SHA로 고정**

```text
actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7
actions/setup-java@dd06d9cba3e5552c54d9f8ea23572deb30010f7c # v6
gradle/actions/wrapper-validation@9c971963bec38e04b3d30dcc455b5382be2fdbfb # v6
codecov/codecov-action@fb8b3582c8e4def4969c97caa2f19720cb33a72f # v7
softprops/action-gh-release@efb35369e0ad2afab669f228072c1b0d510eae64 # v3
```

`.github/dependabot.yml`의 `github-actions` 항목은 그대로 유지한다.

- [x] **Step 4: workflow 최소 권한 명시**

두 workflow 최상위에 다음을 추가하고 release job의 기존 `contents: write` override는 유지한다.

```yaml
permissions:
  contents: read
```

- [x] **Step 5: workflow 및 wrapper 검증**

```bash
git diff --check
shasum -a 256 gradle/wrapper/gradle-wrapper.jar
! rg -n 'uses: [^ ]+@(v[0-9]+|main|master)$' .github/workflows
```

Expected: wrapper JAR 해시는
`7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`이고 mutable Action ref가 없다.

### Task 7: 전체 매트릭스와 산출물 최종 검증

**Files:**
- Verify: `README.md`, `README.ko.md`
- Verify: all files modified by Tasks 1-6

**Interfaces:**
- Consumes: Task 1~6의 변경 전체
- Produces: JDK 독립적인 OpenAPI 결과, Java 17 ABI, clean final diff

- [x] **Step 1: JDK 17에서 전체 검증**

```bash
./gradlew -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64 \
  --dependency-verification=strict \
  clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
shasum -a 256 restdocs-api-spec-example/build/api-spec/openapi3.yaml
javap -verbose \
  restdocs-api-spec-generator/build/classes/kotlin/main/com/keecon/restdocs/apispec/generator/OpenApi3Generator.class \
  | rg 'major version: 61'
```

- [x] **Step 2: 동일 명령을 JDK 21과 JDK 25에서 실행**

각 실행에서 Gradle runtime `JAVA_HOME`만 바꾸고 Java 17 toolchain은 `JAVA_HOME_17_X64`로 유지한다.
세 OpenAPI SHA-256 값이 정확히 같아야 한다.

- [x] **Step 3: 문서와 변경 범위 검증**

```bash
git diff --check
diff -u \
  <(perl -ne 'if (/^\s*```/) {$in = !$in; print; next} print if $in' README.md) \
  <(perl -ne 'if (/^\s*```/) {$in = !$in; print; next} print if $in' README.ko.md)
git status --short
git diff --stat
```

Expected: 공백 오류와 코드 블록 차이가 없고, status에는 계획에 열거한 파일과 기존 승인된 README 변경만
나타난다. 커밋·푸시는 수행하지 않고 결과를 사용자에게 보고한다.
