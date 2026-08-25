# Maintenance Review Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the confirmed `e71dfe5..2f180a6` review defects, improve the compatible `openapi3` DSL, and transition maintenance refs to `v0.x` and `v1.x` while 2.x stays active on `main`.

**Architecture:** Keep main/2.x and Boot 3.5 maintenance changes in separate Herdr worktrees. Fix behavior on main with public regression tests, backport only compatible schema/JWT fixes to `v1.x`, and keep `v0.x` as a frozen ref. Preserve current 2.x ABI and existing Groovy DSL while adding Action-based Kotlin/Groovy entry points.

**Tech Stack:** Gradle 9.7.0, Kotlin 2.4.10, Java toolchain 17, Temurin JDK 17/21/25, Spring Boot 4.1 on main, Spring Boot 3.5 on v1.x, JUnit, Gradle TestKit, GitHub Actions, Dependabot.

**Spec:** `docs/superpowers/specs/2026-08-26-maintenance-branches-and-gradle-dsl-design.md`

## Global Constraints

- Project major versions change only with the supported Spring Boot major.
- Keep 2.x on `main`; do not create `v2.x`.
- Never force-push maintenance refs.
- Keep `v0.x` frozen at `b0672470f411c0039addd3f3b11d8b50d8f3b83d`.
- Keep `v1.x` on Boot 3.5; backport only compatible fixes and maintenance automation.
- Preserve current 2.x task ABI and every existing Groovy assignment/Closure entry point.
- Do not add `schemaName`, dependencies, Plugin Portal publication, or unrelated refactors.
- Produce Java 17 bytecode and test only LTS JDK 17, 21, and 25.
- Verify `HERDR_ENV=1` before each Herdr command.
- Delete old refs only after replacement refs, CI, documentation, and pushes are verified.

---

### Task 1: Create recoverable maintenance refs

**Files:** None

**Interfaces:**
- Consumes: `origin/v0`, `origin/1.x`, `0.19.3`, and `1.1.0`.
- Produces: `origin/v0.x` and `origin/v1.x` at the exact old branch tips.

- [ ] **Step 1: Refresh and assert source refs**

```bash
git fetch origin --prune
test "$(git rev-parse origin/v0)" = "b0672470f411c0039addd3f3b11d8b50d8f3b83d"
test "$(git rev-parse origin/1.x)" = "1b0bc1af7f1ac7f4c0eac35356da1ad5d9e34dd7"
test "$(git rev-parse 0.19.3)" = "$(git rev-parse origin/v0)"
test "$(git rev-parse 1.1.0)" = "$(git rev-parse origin/1.x)"
```

Expected: all assertions pass. Stop if an old branch moved.

- [ ] **Step 2: Assert replacement refs are absent**

```bash
test -z "$(git ls-remote --heads origin refs/heads/v0.x)"
test -z "$(git ls-remote --heads origin refs/heads/v1.x)"
```

- [ ] **Step 3: Create new refs before deleting anything**

```bash
git push origin \
  b0672470f411c0039addd3f3b11d8b50d8f3b83d:refs/heads/v0.x \
  1b0bc1af7f1ac7f4c0eac35356da1ad5d9e34dd7:refs/heads/v1.x
git fetch origin --prune
test "$(git rev-parse origin/v0.x)" = "$(git rev-parse origin/v0)"
test "$(git rev-parse origin/v1.x)" = "$(git rev-parse origin/1.x)"
```

Expected: new and old names coexist at identical tips.

- [ ] **Step 4: Normalize local tracking names**

```bash
git branch --track v0.x origin/v0.x
git branch -m 1.x v1.x
git branch --set-upstream-to=origin/v1.x v1.x
```

Expected: local `v0.x` and `v1.x` track the new refs without moving commits.

### Task 2: Make JSON Schema duplicate reduction deterministic

**Files:**
- Modify: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGenerator.kt`
- Modify: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/FieldDescriptorWithSchema.kt`
- Test: `restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGeneratorTest.kt`
- Test: `restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/FieldDescriptorWithSchemaTest.kt`

**Interfaces:**
- Consumes: repeated `FieldDescriptor` values.
- Produces: order-independent optional/ignored behavior without duplicate same-type schema builders.

- [ ] **Step 1: Add public-generator order regression tests**

```kotlin
@Test
fun `should merge optional state for duplicate path and type regardless of order`() {
    val required = FieldDescriptor("value", "required", "STRING", optional = false)
    val optional = FieldDescriptor("value", "optional", "STRING", optional = true)

    listOf(listOf(required, optional), listOf(optional, required)).forEach { descriptors ->
        val generated = SchemaLoader.load(JSONObject(generator.generateSchema(descriptors))) as ObjectSchema
        then(generated.requiredProperties).doesNotContain("value")
        then(generated.definesProperty("value")).isTrue()
    }
}

@Test
fun `should merge ignored state for duplicate path and type regardless of order`() {
    val visible = FieldDescriptor("value", "visible", "STRING", ignored = false)
    val ignored = FieldDescriptor("value", "ignored", "STRING", ignored = true)

    listOf(listOf(visible, ignored), listOf(ignored, visible)).forEach { descriptors ->
        val generated = SchemaLoader.load(JSONObject(generator.generateSchema(descriptors))) as ObjectSchema
        then(generated.definesProperty("value")).isTrue()
    }
}
```

- [ ] **Step 2: Verify RED**

```bash
./gradlew :restdocs-api-spec-jsonschema:test --tests '*JsonSchemaGeneratorTest*duplicate*' --no-daemon
```

Expected: at least one input order fails.

- [ ] **Step 3: Merge flags while retaining a same-type builder**

In `FieldDescriptorWithSchema.merge`:

```kotlin
val mergedSchemaBuilders = if (type == other.type) {
    schemaBuilders
} else {
    schemaBuilders + toSchemaBuilder(jsonSchemaType(other.type), other)
}

return FieldDescriptorWithSchema(
    path = path,
    description = description,
    type = type,
    optional = optional || other.optional,
    ignored = ignored && other.ignored,
    attributes = attributes,
    schemaBuilders = mergedSchemaBuilders
)
```

In `reduceFieldDescriptors`, replace same-type omission with:

```kotlin
groups.firstOrNull { it.equalsOnPathAndType(fieldDescriptor) }
    ?.let { groups - it + it.merge(fieldDescriptor) }
```

- [ ] **Step 4: Verify GREEN and commit**

```bash
./gradlew :restdocs-api-spec-jsonschema:test --no-daemon
git add restdocs-api-spec-jsonschema
git commit -m "fix: merge duplicate schema descriptors deterministically"
```

### Task 3: Harden JWT and nullable date boundaries

**Files:**
- Modify: `restdocs-api-spec/src/main/kotlin/com/keecon/restdocs/apispec/JwtSecurityHandler.kt`
- Test: `restdocs-api-spec/src/test/kotlin/com/keecon/restdocs/apispec/JwtSecurityHandlerTest.kt`
- Modify: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/StringAnyFormatValidator.kt`
- Test: `restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/StringAnyFormatValidatorTest.kt`

**Interfaces:**
- Consumes: Bearer tokens and nullable format subjects.
- Produces: safe Base64URL parsing, nonblank string scopes, and null-safe date validation.

- [ ] **Step 1: Add failing boundary tests**

Add malformed `Bearer e.e`, Base64URL tokens built with `Base64.getUrlEncoder().withoutPadding()`, blank scope, mixed non-string scope lists, and:

```kotlin
@Test
fun `should ignore null subjects`() {
    then(StringAnyFormatValidator(DataFormat.DATE.lowercase()).validate(null)).isEmpty()
}
```

Assert malformed tokens return no security requirement, valid URL tokens return exact scopes, blank scopes produce `JWTBearer`, and non-string list members are ignored.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :restdocs-api-spec:test --tests '*JwtSecurityHandlerTest*' \
  :restdocs-api-spec-jsonschema:test --tests '*StringAnyFormatValidatorTest*' --no-daemon
```

- [ ] **Step 3: Implement safe decoding and filtering**

```kotlin
private fun decodeJwtPart(value: String): String? = try {
    String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
} catch (@Suppress("SwallowedException") exception: IllegalArgumentException) {
    null
}
```

Decode before parsing and treat decode/JSON errors as no result. Replace unchecked scope handling with:

```kotlin
return when (scope) {
    is List<*> -> scope.filterIsInstance<String>().filter(String::isNotBlank)
    is String -> scope.trim().split("\\s+".toRegex()).filter(String::isNotBlank)
    else -> emptyList()
}
```

Return `Optional.empty()` before `LocalDate.parse` when `subject == null`.

- [ ] **Step 4: Verify GREEN and commit**

```bash
./gradlew :restdocs-api-spec:test :restdocs-api-spec-jsonschema:test --no-daemon
git add restdocs-api-spec restdocs-api-spec-jsonschema
git commit -m "fix: harden token and date validation boundaries"
```

### Task 4: Canonicalize OAuth2 configuration and add compatible DSL methods

**Files:**
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/OpenApiExtension.kt`
- Test: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/RestdocsOpenApi3TaskTest.kt`
- Test: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/PublishedConsumerTest.kt`

**Interfaces:**
- Consumes: existing Groovy setters/Closures and new Gradle `Action` methods.
- Produces: identical task inputs for object, Closure, and Action configuration plus executable Kotlin DSL coverage.

- [ ] **Step 1: Add a failing direct-object OAuth2 TestKit test**

Generate this Groovy configuration and assert `components.securitySchemes.oauth2`:

```groovy
import com.keecon.restdocs.apispec.gradle.PluginOauth2Configuration

openapi3 {
  server = 'http://some.api'
  oauth2SecuritySchemeDefinition = new PluginOauth2Configuration().tap {
    flows = ['authorizationCode']
    tokenUrl = 'https://example.com/token'
    authorizationUrl = 'https://example.com/authorize'
  }
}
```

- [ ] **Step 2: Verify RED**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test \
  --tests '*RestdocsOpenApi3TaskTest*direct*object*' --no-daemon
```

Expected: direct assignment produces no OAuth2 scheme.

- [ ] **Step 3: Route all OAuth2 assignment through one setter**

```kotlin
var oauth2SecuritySchemeDefinition: PluginOauth2Configuration? = null
    set(value) {
        field = value
        oauth2ScopeDescriptionsFile.unset()
        value?.scopeDescriptionsPropertiesFile?.let {
            oauth2ScopeDescriptionsFile.set(project.layout.projectDirectory.file(it))
        }
        if (value == null) serializedOauth2ConfigurationProperty.unset()
        else serializedOauth2ConfigurationProperty.set(objectMapper.writeValueAsString(value))
    }
```

Make the existing Closure setter configure one value and assign this property.

- [ ] **Step 4: Add forwarding methods without removing setters**

Add `server(String)`, `server(Closure<Server>)`, `server(Action<in Server>)`, `contact(Closure<Contact>)`, `contact(Action<in Contact>)`, and `oauth2SecuritySchemeDefinition(Action<in PluginOauth2Configuration>)`. Closure and Action contact methods call one `updateSerializedContact(Contact)` helper.

Representative Kotlin implementation:

```kotlin
fun server(serverUrl: String) = setServer(serverUrl)

fun server(action: Action<in Server>) {
    _servers = listOf(Server().also(action::execute))
    updateSerializedServers()
}

fun contact(action: Action<in Contact>) =
    updateSerializedContact(Contact().also(action::execute))
```

- [ ] **Step 5: Add a published Kotlin DSL execution test**

Create `settings.gradle.kts` and `build.gradle.kts` in a second temporary consumer. Resolve the locally published plugin marker and execute:

```kotlin
openapi3 {
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

Write one resource snippet, run `openapi3`, and assert server, contact, and OAuth2 output. Keep the existing POM-only Java consumer test unchanged.

- [ ] **Step 6: Verify and commit**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test --no-daemon
git add restdocs-api-spec-gradle-plugin
git commit -m "feat: improve compatible openapi3 configuration"
```

### Task 5: Respect Gradle build layout and narrow cache inputs

**Files:**
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecExtension.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/OpenApiExtension.kt`
- Modify: `restdocs-api-spec-gradle-plugin/src/main/kotlin/com/keecon/restdocs/apispec/gradle/ApiSpecTask.kt`
- Test: `restdocs-api-spec-gradle-plugin/src/test/kotlin/com/keecon/restdocs/apispec/gradle/RestdocsOpenApi3TaskTest.kt`

**Interfaces:**
- Consumes: `project.layout.buildDirectory` and resource snippet files.
- Produces: relocatable default paths and cache keys based only on `resource.json`.

- [ ] **Step 1: Add failing custom-build-dir and irrelevant-snippet tests**

Set:

```groovy
layout.buildDirectory = layout.projectDirectory.dir('custom-build')
```

Assert default output at `custom-build/api-spec/openapi3.json`. In a second test, run `openapi3`, add only `http-request.adoc`, rerun, and require `UP_TO_DATE` or `FROM_CACHE`.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test \
  --tests '*RestdocsOpenApi3TaskTest*build*directory*' \
  --tests '*RestdocsOpenApi3TaskTest*irrelevant*snippet*' --no-daemon
```

- [ ] **Step 3: Replace eager paths with conventions**

In `ApiSpecExtension.init`:

```kotlin
outputDirectoryProperty.convention(project.layout.buildDirectory.dir("api-spec"))
snippetsDirectoryProperty.convention(project.layout.buildDirectory.dir("generated-snippets"))
```

Remove the `"build/api-spec"` subclass assignment. Keep public String setters.

- [ ] **Step 4: Track only resource snippets**

```kotlin
@get:Internal
abstract val snippetsDirectory: DirectoryProperty

@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
val resourceSnippetFiles: FileTree
    get() = snippetsDirectory.asFileTree.matching { include("**/resource.json") }
```

Read `resourceSnippetFiles.files` sorted by path relative to `snippetsDirectory` so output ordering is deterministic.

- [ ] **Step 5: Verify configuration cache and commit**

```bash
./gradlew :restdocs-api-spec-gradle-plugin:test \
  :restdocs-api-spec-example:openapi3 --configuration-cache \
  --configuration-cache-problems=fail --no-daemon
git add restdocs-api-spec-gradle-plugin
git commit -m "build: align openapi3 inputs with Gradle layout"
```

Expected: custom layout, cache reuse, and example generation pass.

### Task 6: Align main maintenance, release, and usage documentation

**Files:**
- Create: `MAINTENANCE.md`
- Modify: `README.md`
- Modify: `restdocs-api-spec-example/build.gradle`
- Modify: `.github/dependabot.yml`
- Modify: `.github/workflows/release.yml`
- Modify: `docs/superpowers/plans/2026-08-20-jdk25-spring-boot4-support.md`

**Interfaces:**
- Consumes: verified DSL and branch policy.
- Produces: operational policy, accurate examples, v1.x updates, and matrix-gated releases.

- [ ] **Step 1: Write `MAINTENANCE.md`**

Document:

```markdown
| Branch | Spring Boot | Status |
| `v0.x` | 2.7.x | Frozen; no fixes or releases |
| `v1.x` | 3.5.x | Security, compatibility, and managed dependency fixes |
| `main` | 4.x | Active 2.x development |
```

Include backport-first for v1.x defects, mandatory forward-port to main, tag ancestry (`1.*` from `v1.x`, `2.*` from `main`), Java 17 bytecode, JDK 17/21/25, and creation of `vN.x` only at the next Boot/project major transition.

- [ ] **Step 2: Correct README and example usage**

Close `dependencies {}` before `openapi3 {}`. Link the support table to `v0.x`, `v1.x`, and `main`. Add compatible Groovy method and Kotlin `extensions.configure<OpenApi3Extension>("openapi3")` examples without claiming Plugin Portal resolution. Add the 1.x-to-2.x task property migration note.

Change the example plugin coordinate to `2.1.0` and add:

```groovy
// The example consumes the latest released plugin; the current source plugin is verified by TestKit.
```

- [ ] **Step 3: Add v1.x Dependabot targets**

Append Gradle and GitHub Actions entries with `target-branch: "v1.x"`, the existing weekly schedule, and existing labels. Do not target `v0.x`.

- [ ] **Step 4: Matrix-gate main releases**

Split `.github/workflows/release.yml` into `verify` and `release`. `verify` uses JDK matrix `['17', '21', '25']`, checks a `2.*` tag is an ancestor of `origin/main`, and runs the full build. `release` has `needs: verify`, `contents: write`, and generated notes.

```bash
test "${GITHUB_REF_NAME%%.*}" = "2"
git fetch origin main
git merge-base --is-ancestor "$GITHUB_SHA" origin/main
```

- [ ] **Step 5: Add the historical-plan naming addendum**

Append a dated note recording `v0→v0.x`, `1.x→v1.x`, 2.x remaining on main, and the decision to retain completed historical commands unchanged.

- [ ] **Step 6: Validate and commit**

```bash
git diff --check
rg -n 'v0\.x|v1\.x|main' MAINTENANCE.md README.md .github/dependabot.yml
rg -n 'matrix|needs: verify|merge-base' .github/workflows/release.yml
./gradlew :restdocs-api-spec-gradle-plugin:test :restdocs-api-spec-example:openapi3 --no-daemon
git add MAINTENANCE.md README.md restdocs-api-spec-example/build.gradle \
  .github/dependabot.yml .github/workflows/release.yml docs/superpowers/plans
git commit -m "docs: align maintenance and release policy"
```

### Task 7: Verify and integrate main

**Files:** All files changed by Tasks 2-6.

**Interfaces:**
- Consumes: review branch based on `2f180a6`.
- Produces: verified and pushed `main` without a release tag.

- [ ] **Step 1: Compare 2.1.0 and candidate JVM APIs**

Use `javap -p -s` for `ApiSpecTask`, `OpenApiBaseTask`, `OpenApi3Extension`, and `OpenApi3Generator`. Download the immutable JitPack 2.1.0 jars into a `mktemp -d` directory and compare them with candidate classes; do not create another Git worktree. Existing descriptors must remain and only designed additive methods may appear. Move the temporary directory to Trash after comparison.

- [ ] **Step 2: Run the exact candidate on every LTS JDK**

```bash
toolchain17=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8
for java_version in temurin-17.0.18+8 temurin-21.0.10+7.0.LTS temurin-25.0.4+7.0.LTS; do
  mise exec java@${java_version} -- ./gradlew \
    -Dorg.gradle.java.installations.paths="$toolchain17" \
    clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 \
    --no-daemon --console=plain
  shasum -a 256 restdocs-api-spec-example/build/api-spec/openapi3.yaml
done
```

Expected: all builds pass, hashes match, and public bytecode is major version 61.

- [ ] **Step 3: Request independent review**

Review `2f180a6..HEAD` against the spec. Fix every Critical and Important finding and rerun affected tests. Record any intentionally deferred Minor finding.

- [ ] **Step 4: Fast-forward main and reverify JDK 17**

```bash
git fetch origin --prune
test "$(git rev-parse origin/main)" = "2f180a657596f071ab5156d18620bf02e190eecd"
git -C /Users/iwaltgen/workspace/restdocs-openapi3 merge --ff-only review/e71dfe-maintenance
```

Run the full build once in the main checkout on JDK 17.

- [ ] **Step 5: Push main without a tag**

```bash
git -C /Users/iwaltgen/workspace/restdocs-openapi3 push origin main
git ls-remote --heads origin refs/heads/main
```

Expected: origin/main equals candidate HEAD; no tag is created.

### Task 8: Update and verify v1.x

**Files in a separate Herdr worktree:**
- Modify: `.github/workflows/build.yml`
- Modify: `.github/workflows/release.yml`
- Modify: `README.md`
- Modify: `docs/superpowers/plans/2026-08-20-jdk25-spring-boot4-support.md`
- Backport: JSON Schema sources/tests from Task 2.
- Backport: JWT sources/tests from Task 3, excluding date/Jackson 3 code.

**Interfaces:**
- Consumes: `origin/v1.x` at `1b0bc1a`.
- Produces: maintained Boot 3.5 line with compatible fixes and correct automation.

- [ ] **Step 1: Create a dedicated Herdr worktree**

```bash
test "${HERDR_ENV:-}" = "1"
herdr worktree create --cwd /Users/iwaltgen/workspace/restdocs-openapi3 \
  --branch maintenance/v1.x-review --base origin/v1.x \
  --label v1x-maintenance-review --no-focus
```

Parse the returned checkout path and use it for all v1.x work.

- [ ] **Step 2: Backport tests and verify RED**

Adapt the duplicate same-type schema tests and malformed/Base64URL/blank-scope JWT tests to v1.x imports. Do not copy LocalDate, Contact, WebTestClient, Jackson 3, or Boot 4 changes.

```bash
./gradlew :restdocs-api-spec-jsonschema:test --tests '*JsonSchemaGeneratorTest*duplicate*' \
  :restdocs-api-spec:test --tests '*JwtSecurityHandlerTest*' --no-daemon
```

Expected: descriptor order and JWT boundary tests fail.

- [ ] **Step 3: Apply minimal compatible fixes and verify GREEN**

Port only descriptor merge/reduction and JWT decoder/scope handling.

```bash
./gradlew :restdocs-api-spec-jsonschema:test :restdocs-api-spec:test --no-daemon
```

- [ ] **Step 4: Modernize v1.x workflows**

Change build push/PR filters from `main` to `v1.x`. Retain JDK 17/21/25. Replace the old release job with the main matrix pattern, but require tag major `1` and ancestry in `origin/v1.x`. Remove `@latest` refs and nonexistent `build/dist/*` assets.

- [ ] **Step 5: Update v1.x documentation**

Use `v1.x` in current support prose and add the naming addendum without rewriting historical commands. Add a concise maintenance policy to README so the branch does not link to a file absent from its own history.

- [ ] **Step 6: Commit structure and behavior separately**

```bash
git add .github README.md docs
git commit -m "ci: align v1.x maintenance workflows"
git add restdocs-api-spec restdocs-api-spec-jsonschema
git commit -m "fix: backport descriptor and token handling"
```

- [ ] **Step 7: Run the v1.x LTS matrix**

Run `clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3` on Temurin 17, 21, and 25 with the Java 17 toolchain. Require identical OpenAPI hashes and bytecode major 61.

- [ ] **Step 8: Review and push v1.x**

Request independent review of `1b0bc1a..maintenance/v1.x-review`, resolve all Critical/Important findings, then:

```bash
git branch -f v1.x maintenance/v1.x-review
git push origin v1.x
git ls-remote --heads origin refs/heads/v1.x
```

Expected: `v1.x` fast-forwards; `v0.x` remains at `b067247`.

### Task 9: Complete the remote branch transition

**Files:** None

**Interfaces:**
- Consumes: verified remote `main`, `v0.x`, and `v1.x`.
- Produces: removal of obsolete remote `v0` and `1.x` with recovery through new refs/tags.

- [ ] **Step 1: Audit replacement refs and remote docs**

```bash
git fetch origin --prune
test "$(git rev-parse origin/v0.x)" = "$(git rev-parse 0.19.3)"
git merge-base --is-ancestor 1.1.0 origin/v1.x
git merge-base --is-ancestor 2.1.0 origin/main
git show origin/main:MAINTENANCE.md | rg 'v0.x|v1.x|main'
git show origin/v1.x:.github/workflows/build.yml | rg 'v1.x'
```

- [ ] **Step 2: Confirm old refs did not move**

```bash
test "$(git rev-parse origin/v0)" = "b0672470f411c0039addd3f3b11d8b50d8f3b83d"
test "$(git rev-parse origin/1.x)" = "1b0bc1af7f1ac7f4c0eac35356da1ad5d9e34dd7"
```

Stop if either old ref moved.

- [ ] **Step 3: Delete only obsolete remote refs**

```bash
git push origin --delete v0 1.x
git fetch origin --prune
test -z "$(git ls-remote --heads origin refs/heads/v0)"
test -z "$(git ls-remote --heads origin refs/heads/1.x)"
```

Expected: `v0.x`, `v1.x`, and `main` remain; old names are absent.

- [ ] **Step 4: Audit and clean only plan-owned Herdr resources**

```bash
git status --short --branch
git -C /Users/iwaltgen/workspace/restdocs-openapi3 status --short --branch
test "${HERDR_ENV:-}" = "1"
herdr worktree list --cwd /Users/iwaltgen/workspace/restdocs-openapi3
```

Remove only workspaces created by this plan after every change is committed and pushed. Never use `--force`; stop if removal reports uncommitted files.

## Definition of Done

- [ ] Schema, OAuth2, JWT, and nullable date defects have public regression tests on main.
- [ ] Existing Groovy DSL and new Action-based Groovy/Kotlin usage pass TestKit.
- [ ] Main and v1.x pass JDK 17/21/25 with Java 17 bytecode.
- [ ] `MAINTENANCE.md`, README, example, Dependabot, build, and release automation match policy.
- [ ] `v0.x` equals `0.19.3`; v1.x contains `1.1.0`; main contains `2.1.0`.
- [ ] Old remote refs are removed only after replacements are verified.
- [ ] No `v2.x`, dependency, release tag, force push, or unrelated cleanup is introduced.
