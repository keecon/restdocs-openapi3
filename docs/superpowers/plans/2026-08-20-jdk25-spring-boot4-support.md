# JDK 25 and Spring Boot 4 Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 `com.keecon` 공개 API와 플러그인 ID를 유지하면서, `1.x`는 Spring Boot 3.5 유지보수선으로 고정하고 `main`/`2.x`는 Spring Boot 4.1 및 JDK 25에서 빌드·테스트·사용할 수 있게 한다.

**Architecture:** 현재 동작을 계약 테스트로 고정한 뒤 Gradle/JDK 기반을 현대화하고 Boot 3.5용 `1.1.0` 기준점을 만든다. 이후 `main`에서 Spring Framework 7·REST Docs 4·Jackson 3 전환을 수행하되 Swagger가 요구하는 Jackson 2는 generator/plugin 내부 경계로 격리한다. 모든 산출물은 Java 17 바이트코드를 유지하며 JDK 17·21·25에서 검증한다.

**Tech Stack:** Temurin JDK 17/21/25, Java bytecode 17, Gradle 9.5.0, Kotlin 2.4.10, Spring Boot 3.5.16 (`1.x`), Spring Boot 4.1.0 (`2.x`), Spring REST Docs 3.0.6/4.0.1, Jackson 2.21.4/3.1.4, Swagger Core 2.2.51/2.2.54, Swagger Parser 2.1.44/2.1.47, JUnit 6.0.3, Spock 2.4-groovy-4.0/2.4-groovy-5.0, JaCoCo 0.8.15.

**Spec:** `/Users/iwaltgen/syncthing/handoffs/MAINTENANCE_2026-08.md`와 2026-08-20 사용자 승인 사항. 승인된 핵심 결정은 `1.x=Boot 3.5`, `main/2.x=Boot 4.1`, `com.keecon.*` 호환 유지, Java 17 바이트코드, JDK 25 지원, 초기 배포는 기존 JitPack 유지이다.

## Global Constraints

- 공개 패키지 `com.keecon.restdocs.*`와 플러그인 ID `com.keecon.restdocs-openapi3`를 변경하지 않는다.
- `1.x`와 `2.x`를 하나의 바이너리로 합치지 않는다. Spring Framework/REST Docs/Jackson 세대 차이는 릴리스 선으로 격리한다.
- Java/Kotlin 컴파일 결과는 Java 17이다. JDK 25는 빌드·테스트 런타임 지원 대상이다.
- Spring, Jackson, JUnit 및 연관 라이브러리는 가능한 한 Spring Boot BOM에 맡긴다. 버전 카탈로그에는 BOM 밖의 직접 관리 항목만 남긴다.
- 승인된 신규 직접 의존성 범위는 Spring의 세분화된 web/test 모듈과 Jackson 3 모듈이다. Spring과 Jackson은 Apache-2.0으로 프로젝트 MIT 라이선스와 호환된다. 이 범위 밖의 신규 라이브러리나 배포 플러그인은 별도 승인 대상이다.
- Maven Central 및 Gradle Plugin Portal 배포는 이번 범위가 아니다. JitPack 좌표와 모듈 POM을 먼저 검증한다.
- 테스트 실패 시 구현 결함을 먼저 조사한다. 기존 기대 결과, 공유 픽스처 또는 공개 계약을 변경해야 하면 원인과 영향 범위를 보고하고 승인 후 수정한다.
- `1.1.0`/`2.0.0` 태그, `1.x` 브랜치, 커밋, 푸시, GitHub Release는 실행 직전 사용자 요청 또는 승인이 있어야 한다. 아래 커밋 체크포인트는 승인 전에는 수행하지 않는다.
- 구현 시작 시 `superpowers:using-git-worktrees`로 격리하고 각 태스크는 실패 테스트 → 최소 변경 → 국소 테스트 → 전체 검증 순서로 수행한다.
- 소비자 예제와 릴리스 문서에는 `latest`, commit SHA, 가변 버전을 사용하지 않는다.

## Target Version Matrix

| 구분 | `1.x` 유지보수선 | `main` / `2.x` |
|---|---:|---:|
| Spring Boot | 3.5.16 | 4.1.0 |
| Spring REST Docs | 3.0.6 | 4.0.1 |
| Spring Framework | Boot BOM | 7.0.8 이상, Boot BOM |
| Jackson | 2.21.4, Boot BOM | 3.1.4, Boot BOM; Swagger 경계만 Jackson 2 |
| Gradle / Kotlin | 9.5.0 / 2.4.10 | 9.5.0 / 2.4.10 |
| JUnit / Spock | Boot BOM / 2.4-groovy-4.0 | 6.0.3, Boot BOM / 2.4-groovy-5.0 |
| Swagger Core / Parser | 2.2.51 / 2.1.44 | 2.2.54 / 2.1.47 |
| JaCoCo | 0.8.15 | 0.8.15 |
| 바이트코드 | Java 17 | Java 17 |
| CI JDK | 17, 21, 25 | 17, 21, 25; 26 비차단 canary |
| 첫 릴리스 | 1.1.0 | 2.0.0 |

## Release and Branch Sequence

1. `main`을 기준으로 격리된 `feature/1.1.0-maintenance` 작업 공간을 만든다. 직접 `main` checkout을 수정하지 않는다.
2. Phase 1에서 Task 1~5만 완료해 Boot 3.5/JDK 25 호환 기준점을 만든다.
3. Task 5 검증 보고 후 작업을 강제로 중단하고 사용자에게 `1.1.0` 반영·릴리스 승인을 요청한다.
4. 승인 후 검증된 변경을 `main`에 반영하고, 그 동일 커밋에 `1.1.0` 태그를 만든다.
5. `1.1.0` 태그와 정확히 같은 커밋에서 `1.x` 유지보수 브랜치를 만든다.
6. 태그·브랜치가 같은 커밋을 가리키는지 확인한 후에만 `main` 기반 Phase 2 작업 공간에서 Task 6~12를 시작한다.
7. `1.x`에는 호환 버그·보안·관리 의존성 패치만 반영하고, 필요한 수정은 `main`으로 forward-port한다.
8. Spring/REST Docs 세대 또는 공개 API 비호환 변경은 major, 호환 기능은 minor, 버그·보안·관리 의존성은 patch로 관리한다.

## Mandatory Phase Gates

### Phase 1 — Spring Boot 3.5 / `1.1.0`

- 실행 범위는 Task 1~5뿐이다.
- 작업 브랜치 후보는 `feature/1.1.0-maintenance`이며 기준 commit은 작업 시작 시점의 `main`이다.
- 완료 조건은 JDK 17·21·25 build, example OpenAPI 생성, Java 17 bytecode, dependency/POM/JitPack 검증이다.
- 완료 조건을 모두 충족해도 커밋·`main` 반영·tag·branch·push·release는 자동 실행하지 않는다.
- 검증 보고 후 사용자 승인을 기다린다. 승인 전에는 Task 6으로 넘어가지 않는다.

### Release Split — `1.1.0` / `1.x`

- 사용자 승인 후 `main`에 반영된 단일 commit을 `1.1.0` 기준점으로 사용한다.
- `1.1.0` tag와 `1.x` branch는 동일 commit을 가리켜야 한다.
- `1.x` 분리 후에는 Boot 3.5 호환 patch release만 수행한다.
- 분리 검증 명령은 `git rev-parse 1.1.0`과 `git rev-parse 1.x`이며 출력이 같아야 한다.

### Phase 2 — Spring Boot 4.1 / `2.0.0`

- `1.1.0`과 `1.x` 분리를 확인한 후에만 Task 6~12를 시작한다.
- Phase 2는 최신 `main`에서 새 격리 작업 공간을 만들어 수행한다.
- Phase 1 worktree나 `1.x` branch에서 Boot 4 변경을 계속하지 않는다.

---

### Task 1: 재현 가능한 기준선과 호환 계약 고정

**Files:**
- Modify: `gradle.properties`
- Verify: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/ResourceSnippetTest.kt`
- Verify: `restdocs-api-spec-generator/src/test/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3GeneratorTest.kt`
- Verify: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecTaskTest.kt`

- [x] **Step 1: 작업 트리와 현재 실패를 기록한다**

```bash
git status --short --branch
java -version
./gradlew --version
./gradlew clean testCodeCoverageReport --no-daemon --stacktrace
```

Observed baseline: JDK 25.0.4에서 Gradle 7.6/Kotlin 2.0.21 조합은 Kotlin compilation 시 `java.lang.IllegalArgumentException: 25.0.4`로 실패한다. Java/Kotlin target도 각각 25/1.8로 불일치한다. Gradle/Kotlin/toolchain 갱신 후 같은 명령이 통과해야 한다.

- [x] **Step 2: 기존 공개 산출물의 최소 계약 테스트를 보강한다**

`OperationRequest`/`OperationResponse`가 `HttpHeaders.readOnlyHttpHeaders(...)`를 반환하고, 기존 `ResourceSnippetTest`가 `request.contentType`, `response.contentType`, media type parameter를 이미 검증하는지 확인한다. 확인되면 중복 테스트를 추가하지 않는다. 해당 assertion이 없을 때만 consumer-visible JSON 값을 검증하는 테스트를 추가한다.

`OpenApi3GeneratorTest.thenOpenApiSpecIsValid()`가 이미 직렬화 결과를 `OpenAPIV3Parser.readContents(...)`로 검증하므로 중복 테스트를 추가하지 않는다.

```kotlin
val result = OpenAPIV3Parser().readContents(
    openApiSpecJsonString,
    null,
    ParseOptions().apply { isResolve = true },
)
then(result.messages).isEmpty()
then(result.openAPI.paths["/products/{id}"]).isNotNull
```

`ApiSpecTaskTest.baseBuildFile()`과 `RestdocsOpenApi3TaskTest`가 이미 `com.keecon.restdocs-openapi3` plugin ID와 Groovy `openapi3 { ... }` DSL을 TestKit에서 실행하므로 중복 테스트를 추가하지 않는다.

- [x] **Step 3: Java 17에서 기존 기준선을 실행한다**

```bash
mise exec java@17 -- ./gradlew clean testCodeCoverageReport --no-daemon
```

Expected: 새 계약 테스트를 포함한 전체 테스트가 통과한다. JDK 17이 없으면 임의 배포판을 설치하지 말고 Temurin 17 사용 승인을 받은 뒤 다시 실행한다.

- [x] **Step 4: Gradle 재현성 설정을 추가한다**

```properties
org.gradle.caching=true
org.gradle.warning.mode=all
```

`org.gradle.configuration-cache=true`는 Gradle plugin을 lazy configuration으로 전환하는 Task 9에서 검증 후 추가한다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add gradle.properties restdocs-api-spec/src/test docs/superpowers/plans
git commit -m "test: lock public compatibility contracts"
```

### Task 2: Gradle 9.5와 Java 17 바이트코드 기반 확립

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`
- Modify: `build.gradle.kts`, `gradle/libs.versions.toml`
- Modify: `restdocs-api-spec-gradle-plugin/build.gradle.kts`

- [x] **Step 1: Wrapper를 표준 명령으로 갱신한다**

```bash
mise exec java@17 -- ./gradlew wrapper --gradle-version 9.5.0 --distribution-type bin
mise exec java@17 -- ./gradlew wrapper
```

Expected: `distributionUrl`은 `gradle-9.5.0-bin.zip`이고 wrapper JAR/scripts도 갱신된다. Gradle 공식 SHA-256을 `distributionSha256Sum`에 고정하고 wrapper validation으로 확인한다.

- [x] **Step 2: 기반 플러그인 버전을 올린다**

```toml
kotlin = "2.4.10"
ktlint = "14.2.0"
axion-release = "1.21.2"
jacoco = "0.8.15"
```

`plugin-publish = "0.21.0"`과 plugin-publish 설정은 제거한다. 현재 Portal 게시가 없고 설정도 구식이므로 Portal 도입 승인 전에는 죽은 배포 경로를 유지하지 않는다.

- [x] **Step 3: 모든 프로젝트에 Java 17 toolchain/release를 적용한다**

```kotlin
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
jacoco {
    toolVersion = libs.versions.jacoco.get()
}
```

Java/Kotlin 컴파일은 Java 17 toolchain을 사용하되, `Test` task의 launcher는 Gradle을 실행한 현재 JDK로 명시한다. 따라서 CI의 17·21·25 job은 단순히 Gradle 호환성만 확인하지 않고 실제 테스트 JVM도 각 matrix JDK를 사용한다.

기존 subproject 전용 `compileKotlin` 설정은 제거해 test compilation에도 같은 target을 적용한다. TestKit agent `0.8.2`도 catalog의 JaCoCo 0.8.15 runtime artifact로 바꾼다.

- [x] **Step 4: JDK별 wrapper와 바이트코드를 검증한다**

```bash
mise exec java@17 -- ./gradlew testCodeCoverageReport --no-daemon
mise exec java@21 -- ./gradlew testCodeCoverageReport --no-daemon
mise exec java@25 -- ./gradlew testCodeCoverageReport --no-daemon
javap -verbose restdocs-api-spec/build/classes/kotlin/main/com/keecon/restdocs/apispec/ResourceSnippet.class | rg "major version: 61"
```

Expected: 세 JDK에서 통과하고 `javap`은 Java 17 major version 61을 출력한다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add gradle build.gradle.kts gradlew gradlew.bat restdocs-api-spec-gradle-plugin/build.gradle.kts
git commit -m "build: support JDK 25 with Gradle 9.5"
```

### Task 3: `1.x`를 Spring Boot 3.5 기준으로 갱신

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`
- Modify: `restdocs-api-spec/build.gradle.kts`
- Modify: `restdocs-api-spec-mockmvc/build.gradle.kts`
- Modify: `restdocs-api-spec-generator/build.gradle.kts`
- Modify: `restdocs-api-spec-jsonschema/build.gradle.kts`
- Modify: `restdocs-api-spec-model/build.gradle.kts`
- Modify: `restdocs-api-spec-gradle-plugin/build.gradle.kts`

- [x] **Step 1: `1.x` 버전 카탈로그를 정리한다**

```toml
[versions]
spring-boot = "3.5.16"
swagger = "2.2.51"
swagger-parser = "2.1.44"
spock = "2.4-groovy-4.0"

[libraries]
spring-boot-dependencies = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
```

Spring REST Docs, Jackson, JUnit, AssertJ, Hibernate Validator 등 Boot BOM 관리 대상의 explicit version은 제거한다. BOM 밖 라이브러리만 별도 버전을 유지한다. Spock은 Boot BOM 관리 대상이 아니므로 Boot 3.5의 Groovy 4와 일치하는 `2.4-groovy-4.0`을 명시한다. Swagger Core 2.2.52/Parser 2.1.45는 Jackson 2.22를 요구하므로, Boot 3.5의 Jackson 2.21.4 정렬을 유지하는 최신 조합인 Core 2.2.51/Parser 2.1.44를 `1.x`에 사용한다.

- [x] **Step 2: Boot BOM을 라이브러리 모듈에 적용한다**

```kotlin
if (!isExampleProject()) {
    dependencies {
        add("implementation", platform(libs.spring.boot.dependencies))
        add("testImplementation", platform(libs.spring.boot.dependencies))
    }
}
```

라이브러리 모듈은 Spring Boot plugin을 적용하지 않고 BOM만 소비한다. 예제만 Boot plugin을 유지한다.

- [x] **Step 3: 공개 POM의 전이 의존성을 축소한다**

```bash
./gradlew generatePomFileForRestdocs-api-specPublication
./gradlew generatePomFileForRestdocs-api-spec-generatorPublication
rg "spring-boot-starter|jackson|spring-restdocs" */build/publications/*/pom-default.xml
```

Expected: 공개 API에 필요한 의존성만 `api`, 내부 구현은 `implementation`이며 불필요한 Boot starter가 소비자 runtime으로 전이되지 않는다. 가능한 경우 `spring-web`, `spring-restdocs-core`, Jackson 세부 모듈을 사용한다.

- [x] **Step 4: 전체 `1.x` 기준선을 검증한다**

```bash
./gradlew clean check testCodeCoverageReport --no-daemon
./gradlew :restdocs-api-spec:dependencyInsight --dependency spring-core --configuration runtimeClasspath
./gradlew :restdocs-api-spec-generator:dependencyInsight --dependency jackson-databind --configuration runtimeClasspath
```

Expected: Spring은 Boot 3.5.16 BOM, Jackson 2는 Boot BOM의 2.21.4로 정렬된다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add gradle/libs.versions.toml build.gradle.kts */build.gradle.kts
git commit -m "build: align maintenance line with Spring Boot 3.5"
```

### Task 4: `1.x` 예제 통합과 JDK CI 매트릭스 복구

**Files:**
- Modify: `.github/workflows/build.yml`
- Modify: `restdocs-api-spec-example/build.gradle`
- Modify: `restdocs-api-spec-example/src/test/groovy/com/keecon/restdocs/apispec/example/ProductControllerSpec.groovy`
- Modify: `settings.gradle`

- [x] **Step 1: 예제가 현재 로컬 플러그인을 소비하게 한다**

오래된 JitPack classpath `1.0.2`를 같은 build의 project dependency로 교체하고 기존 plugin ID 적용을 유지한다.

```groovy
buildscript {
    dependencies {
        classpath project(':restdocs-api-spec-gradle-plugin')
    }
}

apply plugin: 'com.keecon.restdocs-openapi3'
```

Gradle 9가 buildscript의 project dependency를 거부하면 TestKit과 중복되는 별도 plugin build를 만들지 말고 실패 근거를 보고한다. 그 경우 예제 실행은 `restdocs-api-spec-gradle-plugin` TestKit consumer fixture로 대체할지 사용자 승인을 받는다.

실행 결과: Gradle 9.5.0이 `Project dependencies cannot be declared here`로 sibling project classpath를 거부했다. 예제의 원격 classpath는 복구했으며, 2026-08-21 사용자 승인에 따라 로컬 플러그인 통합 검증은 기존 TestKit consumer fixture로 대체한다. 예제는 JitPack `1.0.2` 소비자 호환 흐름을 검증한다.

- [x] **Step 2: 예제 전체 흐름을 실행한다**

```bash
./gradlew :restdocs-api-spec-example:test :restdocs-api-spec-example:openapi3 --no-daemon --stacktrace
```

Expected: 테스트와 OpenAPI 생성이 성공하고 `restdocs-api-spec-example/build/api-spec/openapi3.yaml`이 생긴다.

- [x] **Step 3: JDK 17·21·25 blocking matrix를 추가한다**

```yaml
strategy:
  fail-fast: false
  matrix:
    java: ['17', '21', '25']
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with:
      distribution: temurin
      java-version: |
        17
        ${{ matrix.java }}
      cache: gradle
  - uses: gradle/actions/wrapper-validation@v4
  - run: ./gradlew -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64 clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
```

Codecov는 JDK 17 job에서만 업로드한다.

- [x] **Step 4: 로컬 CI 등가 검증을 실행한다**

```bash
jdk17_path=$(mise where java@temurin-17.0.18+8)
for java_version in temurin-17.0.18+8 temurin-21.0.10+7.0.LTS temurin-25.0.4+7.0.LTS; do
  mise exec java@${java_version} -- ./gradlew \
    -Dorg.gradle.java.installations.paths="$jdk17_path" \
    clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
done
```

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add .github/workflows/build.yml settings.gradle restdocs-api-spec-example
git commit -m "ci: verify maintenance line on JDK 17 through 25"
```

### Task 5: `1.1.0` 유지보수 기준점 확정

**Files:**
- Modify: `README.md`, `jitpack.yml`
- Verify: all published module POMs

- [x] **Step 1: README에 지원 정책과 좌표를 기록한다**

```markdown
| Artifact line | Spring Boot | Spring REST Docs | Java bytecode | Tested JDKs |
|---|---|---|---|---|
| 1.x | 3.5.x | 3.0.x | 17 | 17, 21, 25 |
| 2.x | 4.1.x | 4.0.x | 17 | 17, 21, 25 |
| 0.x | 2.7.x | 2.0.x | frozen | unsupported |
```

소비자 예시는 `com.github.keecon.restdocs-openapi3:restdocs-api-spec:1.1.0`처럼 불변 버전을 사용하고, 패키지와 plugin ID가 계속 `com.keecon`임을 명시한다.

- [x] **Step 2: JitPack JDK를 25로 갱신한다**

Temurin/OpenJDK 25.0.4를 선택하되 Java 17 bytecode임을 주석으로 남긴다. JitPack이 exact patch를 제공하지 않으면 `25` channel을 사용하고 빌드 로그의 실제 patch를 릴리스 기록에 남긴다.

- [x] **Step 3: local Maven metadata를 검증한다**

```bash
./gradlew clean publishToMavenLocal --no-daemon
find */build/publications -name 'pom-default.xml' -print
rg "spring-boot-starter|jackson" */build/publications/*/pom-default.xml
```

Expected: 예제를 제외한 공개 모듈 POM이 생성되고 불필요한 starter가 새지 않는다. 이 단계의 POM 구조 검증은 현재 axion 산출 버전으로 수행하며, `1.1.0` 버전 자체는 승인된 tag 후보에서 `./gradlew currentVersion`으로 별도 확인한다.

- [x] **Step 4: 릴리스 승인 게이트**

전체 테스트, JDK matrix, 생성 POM, example OpenAPI, dependencyInsight와 경고를 보고한다. 이 단계에서 실행을 중단한다. 승인 후에만 변경을 `main`에 반영하고 그 동일 commit에 `1.1.0` 태그와 `1.x` 브랜치를 만든다. 태그·브랜치·푸시·GitHub Release는 각각 승인 범위 안에서만 실행한다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add README.md jitpack.yml
git commit -m "docs: define 1.x maintenance compatibility"
```

---

### Task 6: Boot 4 BOM과 모듈 의존성 전환

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`
- Modify: `restdocs-api-spec/build.gradle.kts`
- Modify: `restdocs-api-spec-mockmvc/build.gradle.kts`
- Modify: `restdocs-api-spec-generator/build.gradle.kts`
- Modify: `restdocs-api-spec-jsonschema/build.gradle.kts`
- Modify: `restdocs-api-spec-model/build.gradle.kts`
- Modify: `restdocs-api-spec-gradle-plugin/build.gradle.kts`
- Modify: `restdocs-api-spec-example/build.gradle`

- [x] **Step 1: `1.1.0` 이후 `main` 기반인지 확인한다**

```bash
git branch --show-current
git describe --tags --always
git log -1 --oneline
```

Expected: Boot 4 변경은 `1.x`가 아니라 `main` 기반 격리 작업 트리에서 시작한다. 기준점과 다르면 브랜치를 임의 조정하지 않고 보고한다.

추가 gate:

```bash
test "$(git rev-parse 1.1.0)" = "$(git rev-parse 1.x)"
```

Expected: exit code 0. tag 또는 branch가 없거나 commit이 다르면 Task 6을 시작하지 않는다.

- [ ] **Step 2: Boot 4 버전으로 카탈로그를 바꾼다**

```toml
spring-boot = "4.1.0"
spring-restdocs = "4.0.1"
swagger = "2.2.54"
swagger-parser = "2.1.47"
spock = "2.4-groovy-5.0"
```

JUnit 6.0.3, Jackson 3.1.4, Spring Framework 7.0.8 이상은 Boot BOM에서 선택되게 하고 별도 버전을 선언하지 않는다.

- [ ] **Step 3: starter를 최소 Spring/Jackson 모듈로 교체한다**

```toml
spring-web = { module = "org.springframework:spring-web" }
spring-webmvc = { module = "org.springframework:spring-webmvc" }
spring-restdocs-core = { module = "org.springframework.restdocs:spring-restdocs-core" }
spring-restdocs-mockmvc = { module = "org.springframework.restdocs:spring-restdocs-mockmvc" }
jackson3-databind = { module = "tools.jackson.core:jackson-databind" }
jackson3-module-kotlin = { module = "tools.jackson.module:jackson-module-kotlin" }
jackson2-annotations = { module = "com.fasterxml.jackson.core:jackson-annotations" }
```

Swagger Core가 Jackson 2를 요구하는 generator/plugin에는 Swagger 전이 Jackson 2와 필요한 `com.fasterxml.jackson` 모듈만 둔다. Jackson 2/3 `ObjectMapper`를 같은 소스 파일에서 섞지 않는다.

- [ ] **Step 4: 컴파일 실패를 분류한다**

```bash
./gradlew compileKotlin compileTestKotlin --no-daemon --stacktrace
```

Expected: 실패를 Spring/REST Docs 4 API, Jackson 3 package, Boot 4 test slice, Gradle 9 API로 분류한다. 무차별 import 치환은 하지 않는다.

- [ ] **Step 5: 의존성 그래프의 Jackson 경계를 확인한다**

```bash
./gradlew :restdocs-api-spec:dependencies --configuration runtimeClasspath
./gradlew :restdocs-api-spec-generator:dependencies --configuration runtimeClasspath
./gradlew :restdocs-api-spec-gradle-plugin:dependencies --configuration runtimeClasspath
```

Expected: Jackson 2 databind는 Swagger 연동이 있는 generator/plugin을 넘어 불필요하게 전이되지 않는다.

- [ ] **Step 6: 승인 시 체크포인트 커밋**

```bash
git add gradle/libs.versions.toml build.gradle.kts */build.gradle.kts
git commit -m "build: align main with Spring Boot 4.1"
```

### Task 7: Spring REST Docs 4 및 Spring Framework 7 호환 복구

**Files:**
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/ResourceSnippet.kt`
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/DescriptorExtractor.kt`
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/FieldDescriptors.kt`
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/ResourceDocumentation.kt`
- Modify: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/ResourceSnippetTest.kt`
- Modify: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/FieldDescriptorsTest.kt`
- Modify: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/SecurityRequirementsHandlerTest.kt`
- Modify: `restdocs-api-spec-mockmvc/src/test/kotlin/com/keecon/restdocs/apispec/DoSomethingIntegrationTest.kt`
- Modify: `restdocs-api-spec-mockmvc/src/test/kotlin/com/keecon/restdocs/apispec/ListSomethingIntegrationTest.kt`
- Modify: `restdocs-api-spec-mockmvc/src/test/kotlin/com/keecon/restdocs/apispec/ResourceSnippetIntegrationTest.kt`

- [ ] **Step 1: read-only headers 회귀를 재현한다**

```bash
./gradlew :restdocs-api-spec:test --tests '*ResourceSnippetTest*' --stacktrace
```

Expected before fix: Task 1의 read-only headers 계약이 class cast 또는 content type 처리 실패를 드러낸다. 통과하면 프로덕션 코드를 바꾸지 않고 Framework 7의 실제 Operation headers 타입이 fixture에 반영됐는지 확인한다.

- [ ] **Step 2: headers를 공개 API로만 읽는다**

```kotlin
private fun getContentTypeOrDefault(headers: HttpHeaders): String =
    headers.getFirst(HttpHeaders.CONTENT_TYPE)
        ?.let(MediaType::parseMediaType)
        ?.toString()
        ?: APPLICATION_JSON.toString()
```

Expected: mutable/read-only headers가 같은 결과를 내고 media type parameters를 보존한다.

- [ ] **Step 3: REST Docs reflection 실패를 명시적으로 처리한다**

`DescriptorExtractor`의 반복 reflection을 단일 helper로 모으고 `printStackTrace()` 후 빈 목록을 반환하는 동작을 제거한다.

```kotlin
private fun <T> invokeDescriptorAccessor(target: Any, owner: Class<*>, name: String): T {
    val method = owner.getDeclaredMethod(name).apply { trySetAccessible() }
    return try {
        @Suppress("UNCHECKED_CAST")
        method.invoke(target) as T
    } catch (exception: ReflectiveOperationException) {
        throw IllegalStateException(
            "Spring REST Docs descriptor API changed: ${owner.name}#$name",
            exception,
        )
    }
}
```

REST Docs 4에 안정적인 public accessor가 있으면 reflection 대신 그 API를 사용한다. 필수 descriptor를 조용히 유실하는 fallback은 두지 않는다.

- [ ] **Step 4: descriptor 종류별 계약을 실행한다**

```bash
./gradlew :restdocs-api-spec:test --tests '*FieldDescriptorsTest' --tests '*ResourceSnippetTest' --tests '*SecurityRequirementsHandlerTest' --stacktrace
./gradlew :restdocs-api-spec-mockmvc:test --stacktrace
```

Expected: fields, headers, links, path/query/form parameters, request parts, security metadata가 누락 없이 resource snippet에 기록된다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add restdocs-api-spec/src restdocs-api-spec-mockmvc/src/test
git commit -m "fix: support Spring REST Docs 4 operations"
```

### Task 8: Jackson 3 전환과 Swagger Jackson 2 경계 격리

**Files:**
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/ResourceSnippet.kt`
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/JwtSecurityHandler.kt`
- Modify: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGenerator.kt`
- Modify: `restdocs-api-spec-generator/src/main/kotlin/com/keecon/restdocs/apispec/generator/ApiSpecificationWriter.kt`
- Modify: `restdocs-api-spec-generator/src/main/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3Generator.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/OpenApiExtension.kt`
- Modify: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/ResourceSnippetTest.kt`
- Modify: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/JwtSecurityHandlerTest.kt`
- Modify: `restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGeneratorTest.kt`
- Modify: `restdocs-api-spec-generator/src/test/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3GeneratorTest.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecTaskTest.kt`

- [ ] **Step 1: import inventory를 기준으로 경계를 정한다**

```bash
rg -n '^import (com\.fasterxml\.jackson|tools\.jackson)' --glob '*.kt'
```

Expected: `com.fasterxml.jackson.annotation`은 공개 model annotation/Swagger 경계에 남을 수 있고 일반 JSON 읽기/쓰기는 `tools.jackson` 대상이다.

- [ ] **Step 2: core와 jsonschema를 Jackson 3로 옮긴다**

일반 serializer/deserializer는 Jackson 3 Kotlin module을 사용한다. 실제 3.1 API의 builder/feature signature에 맞추되 JSON 필드와 공개 model 계약은 변경하지 않는다.

- [ ] **Step 3: Swagger 직렬화를 전용 adapter에 격리한다**

`ApiSpecificationWriter`만 `io.swagger.v3.core.util.Json`/`Yaml`과 Jackson 2를 알도록 한다.

```kotlin
internal object ApiSpecificationWriter {
    fun serialize(format: String, openApi: OpenAPI): String = when (format.lowercase()) {
        "json" -> Json.pretty(openApi)
        "yaml", "yml" -> Yaml.pretty(openApi)
        else -> throw IllegalArgumentException("Unsupported OpenAPI format: $format")
    }
}
```

공개 model에는 Jackson databind 타입을 추가하지 않는다.

- [ ] **Step 4: Jackson 2/3 상호운용 회귀를 실행한다**

```bash
./gradlew :restdocs-api-spec:test :restdocs-api-spec-jsonschema:test :restdocs-api-spec-generator:test --stacktrace
./gradlew :restdocs-api-spec-generator:dependencyInsight --dependency jackson-databind --configuration runtimeClasspath
```

Expected: resource JSON을 Jackson 3로 기록/읽고 Swagger는 JSON/YAML OpenAPI를 만들며 parser 검증이 통과한다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add restdocs-api-spec/src restdocs-api-spec-jsonschema/src restdocs-api-spec-generator/src restdocs-api-spec-gradle-plugin/src
git commit -m "refactor: isolate Jackson 2 from Jackson 3 runtime"
```

### Task 9: Gradle 플러그인을 Gradle 9 lazy configuration으로 현대화

**Files:**
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecExtension.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/OpenApiExtension.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecTask.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/OpenApiBaseTask.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/OpenApi3Task.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/RestdocsOpenApi3Plugin.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecTaskTest.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/RestdocsOpenApi3TaskTest.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/RestdocsOpenApiTaskTestBase.kt`
- Modify: `gradle.properties`

- [ ] **Step 1: configuration cache 실패를 먼저 재현한다**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test --configuration-cache --configuration-cache-problems=fail --stacktrace
```

Expected before fix: eager `afterEvaluate`, mutable task input, task action의 `project.file` 접근 중 하나 이상이 Gradle 9 validation 문제로 드러난다.

- [ ] **Step 2: task inputs/outputs를 Provider API로 바꾼다**

```kotlin
abstract class ApiSpecTask : DefaultTask() {
    @get:Input
    abstract val separatePublicApi: Property<Boolean>
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val snippetsDirectory: DirectoryProperty
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @get:Input
    abstract val outputFileNamePrefix: Property<String>
}
```

`@TaskAction`은 `.get().asFile`만 사용하고 `project`를 참조하지 않는다. 출력 파일별 annotation 대신 public/private 변형을 포함하는 output directory를 선언한다.

- [ ] **Step 3: extension을 Provider 기반으로 만들되 Groovy DSL을 유지한다**

```kotlin
abstract class ApiSpecExtension @Inject constructor(objects: ObjectFactory) {
    val outputDirectory: DirectoryProperty = objects.directoryProperty()
    val snippetsDirectory: DirectoryProperty = objects.directoryProperty()
    val outputFileNamePrefix: Property<String> = objects.property(String::class.java)
    val separatePublicApi: Property<Boolean> = objects.property(Boolean::class.java)
}
```

convention은 기존 값(`build/api-spec`, `build/generated-snippets`, `openapi3`, `false`)과 동일하게 둔다. 기존 Groovy assignment가 깨지면 adapter setter를 유지한다.

- [ ] **Step 4: `afterEvaluate`와 `tasks.create`를 제거한다**

```kotlin
val extension = extensions.create<OpenApi3Extension>(OpenApi3Extension.name)
tasks.register<OpenApi3Task>("openapi3") {
    group = "documentation"
    description = "Aggregate resource fragments into an OpenAPI 3 specification"
    dependsOn(tasks.named("check"))
    outputDirectory.set(extension.outputDirectory)
    snippetsDirectory.set(extension.snippetsDirectory)
    outputFileNamePrefix.set(extension.outputFileNamePrefix)
    separatePublicApi.set(extension.separatePublicApi)
}
```

OpenAPI 전용 title/version/format/server는 provider 또는 immutable serializable value로 연결한다.

- [ ] **Step 5: TestKit 두 번째 실행에서 cache 재사용을 검증한다**

테스트의 `withDebug(true)`를 제거하고 같은 fixture를 `--configuration-cache`로 두 번 실행한다.

```kotlin
then(first.task(":openapi3")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
then(second.output).contains("Reusing configuration cache")
```

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test --configuration-cache --configuration-cache-problems=fail
```

릴리스 전 example은 공개된 구버전 플러그인을 외부 소비하므로 저장소 전체에 configuration cache를 강제하지 않는다. 현재 2.0.0 플러그인은 TestKit의 두 번째 실행과 `help --configuration-cache --configuration-cache-problems=fail`로 명시 검증한다.

- [ ] **Step 6: 승인 시 체크포인트 커밋**

```bash
git add restdocs-api-spec-gradle-plugin
git commit -m "refactor: make Gradle plugin compatible with Gradle 9"
```

### Task 10: Boot 4 예제와 엔드투엔드 OpenAPI 생성 검증

**Files:**
- Modify: `restdocs-api-spec-example/build.gradle`
- Modify: `restdocs-api-spec-example/src/test/groovy/com/keecon/restdocs/apispec/example/ProductControllerSpec.groovy`
- Modify: `restdocs-api-spec-mockmvc/src/test/kotlin/com/keecon/restdocs/apispec/DoSomethingIntegrationTest.kt`
- Modify: `restdocs-api-spec-mockmvc/src/test/kotlin/com/keecon/restdocs/apispec/ListSomethingIntegrationTest.kt`
- Modify: `restdocs-api-spec-mockmvc/src/test/kotlin/com/keecon/restdocs/apispec/ResourceSnippetIntegrationTest.kt`
- Modify: `README.md`

- [ ] **Step 1: Boot 4 test slice import 실패를 재현한다**

```bash
./gradlew :restdocs-api-spec-example:compileTestGroovy :restdocs-api-spec-mockmvc:compileTestKotlin --stacktrace
```

- [ ] **Step 2: Boot 4 공식 test starter/import로 최소 전환한다**

`WebMvcTest`가 필요하면 다음 패키지를 사용한다.

```kotlin
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
```

`spring-boot-starter-test`만으로 부족하면 Boot 4의 `spring-boot-starter-webmvc-test`를 testImplementation으로 추가한다. 이는 승인된 Spring 세분화 의존성 범위 안이다.

- [ ] **Step 3: example 전체 흐름을 실행한다**

```bash
./gradlew clean :restdocs-api-spec-example:test :restdocs-api-spec-example:openapi3 --no-daemon
test -f restdocs-api-spec-example/build/api-spec/openapi3.yaml
rg '^openapi:|^paths:|/products' restdocs-api-spec-example/build/api-spec/openapi3.yaml
```

Expected: 테스트가 resource snippets를 만들고 `openapi3` task가 parser로 읽을 수 있는 YAML을 생성한다.

- [ ] **Step 4: README Boot 4 사용 예시를 갱신한다**

`2.0.0` dependency/plugin 예, Java 17 minimum, JDK 17/21/25 tested, `1.x` 정책을 기록한다. Plugin Portal 배포 전에는 Portal에서 plugins DSL version lookup이 된다고 쓰지 않고 JitPack 좌표를 안내한다. 같은 multi-project build의 sibling plugin은 Gradle 9 buildscript classpath에 직접 넣을 수 없으므로 example은 외부 소비자 흐름을 유지하고, 현재 로컬 plugin 코드는 TestKit과 Task 12의 독립 소비자 local publication으로 검증한다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add restdocs-api-spec-example restdocs-api-spec-mockmvc/src/test README.md
git commit -m "test: verify Spring Boot 4 consumer workflow"
```

### Task 11: CI, dependency automation, release workflow 정비

**Files:**
- Modify: `.github/workflows/build.yml`, `.github/workflows/release.yml`, `jitpack.yml`
- Delete: `.github/dependabot.yml.old`
- Create: `.github/dependabot.yml`
- Modify or Delete: `.github/workflows/merge-deps.yml`

- [ ] **Step 1: JDK 26 non-blocking canary를 분리한다**

```yaml
jdk-26-canary:
  continue-on-error: true
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: |
          17
          26
    - uses: gradle/actions/wrapper-validation@v4
    - run: ./gradlew -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64 clean check --no-daemon
```

- [ ] **Step 2: Dependabot을 공식 설정으로 복원한다**

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: /
    schedule:
      interval: weekly
    open-pull-requests-limit: 5
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
```

`dependabot.yml.old`는 제거한다. `akheron/dependabot-cron-action@v1` 기반 자동 병합은 새 승인이 없으면 제거하고 PR 생성까지만 자동화한다.

- [ ] **Step 3: release workflow를 결정적으로 만든다**

`git-chglog@latest`와 존재하지 않는 `build/dist/*` 업로드를 제거한다. 새 changelog 도구 없이 GitHub generated notes를 사용한다.

```yaml
- name: Verify release
  run: ./gradlew clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
- name: Publish release
  uses: softprops/action-gh-release@v2
  with:
    generate_release_notes: true
```

- [ ] **Step 4: workflow와 wrapper를 정적 검증한다**

```bash
./gradlew help --configuration-cache --configuration-cache-problems=fail
git diff --check
rg -n '@latest|build/dist/\*|fix example module test' .github jitpack.yml
```

Expected: 검색 결과가 없고 configuration cache 검증이 통과한다.

- [ ] **Step 5: 승인 시 체크포인트 커밋**

```bash
git add .github jitpack.yml
git commit -m "ci: verify and maintain Boot 4 release line"
```

### Task 12: `2.0.0` 배포 전 통합 및 소비자 스모크 검증

**Files:**
- Verify: all source/build/workflow files
- Verify: generated POMs under `*/build/publications/`
- Verify: example output under `restdocs-api-spec-example/build/api-spec/`
- Modify only with evidence: `README.md`, module build files

- [x] **Step 1: 세 JDK에서 clean build를 실행한다**

```bash
for java_version in 17 21 25; do
  mise exec java@${java_version} -- ./gradlew clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
done
```

Expected: 세 JDK 모두 단위/통합/TestKit/example 테스트와 OpenAPI 생성을 통과한다.

- [x] **Step 2: bytecode와 dependency 경계를 검증한다**

```bash
javap -verbose restdocs-api-spec/build/classes/kotlin/main/com/keecon/restdocs/apispec/ResourceSnippet.class | rg "major version: 61"
./gradlew :restdocs-api-spec:dependencies --configuration runtimeClasspath
./gradlew :restdocs-api-spec-generator:dependencies --configuration runtimeClasspath
./gradlew :restdocs-api-spec-gradle-plugin:dependencies --configuration runtimeClasspath
```

Expected: Java 17 bytecode이며 Boot BOM이 Spring 7/Jackson 3/JUnit 6을 정렬한다. Jackson 2 databind는 Swagger 경계 밖으로 불필요하게 확산되지 않는다.

- [x] **Step 3: local publish와 독립 소비자 스모크 테스트를 수행한다**

```bash
./gradlew publishToMavenLocal -Prelease.forceVersion=2.0.0 --no-daemon
./gradlew :restdocs-api-spec-gradle-plugin:validatePlugins --no-daemon
./gradlew :restdocs-api-spec-gradle-plugin:test --tests '*RestdocsOpenApi3TaskTest*' --no-daemon
```

이후 작업 트리 밖 `mktemp -d` 경로에 소비자 build를 만들고 local Maven의 `com.keecon:restdocs-api-spec:2.0.0-SNAPSHOT`, `com.keecon:restdocs-api-spec-mockmvc:2.0.0-SNAPSHOT` 및 같은 버전의 plugin marker로 compile/test/openapi3를 실행한다. `release.forceVersion`은 저장소나 원격에 marker tag를 만들지 않는 검증 전용 override이며 실제 `2.0.0` 버전은 승인된 release tag에서 다시 확인한다. 완료 후 현재 작업이 만든 임시 경로만 정리한다.

Expected: 저장소 project dependency 없이 게시 metadata만으로 플러그인 적용, snippet 생성, OpenAPI 생성이 성공한다.

- [x] **Step 4: 공개 API/패키지 drift를 확인한다**

```bash
rg -n '^package ' --glob '*.kt' | rg -v 'com\.keecon\.restdocs'
rg -n 'com\.keecon\.restdocs-openapi3' README.md restdocs-api-spec-gradle-plugin restdocs-api-spec-example
git diff --check
git status --short
```

Expected: 의도하지 않은 package 변경이 없고 plugin ID가 source/test/docs에서 일치한다.

- [x] **Step 5: 최종 보고와 릴리스 승인 요청**

보고 항목은 JDK 17/21/25 결과, JDK 26 canary, resolved dependency 버전, class major 61, POM, 독립 소비자 결과, 공개 API/Groovy DSL 호환성, 알려진 제한이다. 검증 보고 후 명시 승인된 경우에만 최종 커밋, `2.0.0` 태그, 푸시와 GitHub Release를 실행한다.

## Definition of Done

- [x] `1.1.0` 기준점이 Boot 3.5.16/REST Docs 3.0.6에서 JDK 17·21·25를 통과하고 `1.x`로 보존될 준비가 되었다.
- [x] `main`이 Boot 4.1.0/REST Docs 4.0.1에서 JDK 17·21·25를 통과한다.
- [x] 모든 공개 모듈이 Java 17 bytecode를 생성한다.
- [x] `ReadOnlyHttpHeaders`, REST Docs descriptor, Jackson 2/3, Gradle configuration cache 회귀 테스트가 있다.
- [x] example 외부 소비자 흐름과 local plugin TestKit/독립 소비자 흐름이 test → resource snippets → OpenAPI 생성까지 완료한다.
- [x] 생성 POM과 독립 소비자 테스트로 JitPack 배포 전 metadata를 검증했다.
- [x] README, CI, Dependabot, release workflow가 실제 지원/배포 정책과 일치한다.
- [x] 사용자 승인 없이 커밋, 태그, 브랜치 푸시, PR 또는 GitHub Release를 만들지 않았다.
