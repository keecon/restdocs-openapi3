# Everit 1.14.6 Enum Order Preservation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade Everit JSON Schema to exactly 1.14.6 on `v1.x` and `main` without changing the descriptor-defined order of generated enum arrays.

**Architecture:** Keep Everit's schema construction and validation unchanged. After Everit serializes a schema, parse that JSON with the branch's existing Jackson version, walk the Everit schema graph and JSON tree together, and replace only `enum` nodes with `EnumSchema.possibleValuesAsList`. Implement and validate on `v1.x` first, then forward-port the behavior to `main`.

**Tech Stack:** Gradle, Kotlin, Everit JSON Schema 1.14.6, Jackson 2 on `v1.x`, Jackson 3 on `main`, JUnit 5, AssertJ, JsonPath, Herdr worktrees.

**Spec:** `docs/superpowers/specs/2026-08-28-everit-enum-order-design.md`

## Global Constraints

- [ ] Perform behavior work on `v1.x` first and forward-port it to `main` in a separate Herdr worktree.
- [ ] Immediately before every `herdr` control command, run `test "${HERDR_ENV:-}" = "1"`; stop manipulating Herdr if it fails.
- [ ] Set `com.github.erosb:everit-json-schema` to exactly `1.14.6`; do not use a version range.
- [ ] Preserve enum order for strings, integral numbers, decimal numbers, and booleans in flat, nested-object, array-item, and combined-schema positions.
- [ ] Preserve JSON Schema validation behavior: listed values validate and unlisted values fail.
- [ ] Use only public Everit APIs. Do not use reflection, sorting, a fork, shading, copied upstream internals, or a new dependency.
- [ ] Keep each branch's existing Gradle scope unchanged: `api` on `main`, `implementation` on `v1.x`. Treat any `v1.x` publication-metadata correction as a separate compatibility change.
- [ ] Keep Java 17 bytecode compatibility and verify both branches with installed JDK 17, 21, and 25 runtimes.
- [ ] Do not weaken or reorder the existing exact enum assertion in `OpenApi3GeneratorTest`.
- [ ] Do not commit, push, open/merge PRs, close issues, or post comments without separate explicit user authorization. Commit commands below are gates, not current authorization.
- [ ] Never remove a worktree with `--force`. Remove only clean worktrees created while executing this plan, after their changes are integrated or intentionally retained elsewhere.

For every isolated shell invocation after Task 1, initialize these deterministic paths before running the shown commands:

```bash
v1_worktree_path=/Users/iwaltgen/.herdr/worktrees/restdocs-openapi3/fix-everit-1-14-6-v1
main_worktree_path=/Users/iwaltgen/.herdr/worktrees/restdocs-openapi3/fix-everit-1-14-6-main
```

## Task 1: Create Isolated Implementation Worktrees and Record Baselines

**Files:**

- Inspect: `.git`, `gradle/libs.versions.toml`
- Do not modify repository files in this task.

- [ ] **Step 1: Refresh remote references without changing a branch**

Run from the normal checkout:

```bash
git fetch origin --prune
git rev-parse origin/v1.x origin/main
git status --short --branch
```

Record both base SHAs in the execution log.

- [ ] **Step 2: Create the `v1.x` Herdr worktree**

```bash
v1_worktree_path=/Users/iwaltgen/.herdr/worktrees/restdocs-openapi3/fix-everit-1-14-6-v1
test "${HERDR_ENV:-}" = "1"
herdr worktree create --cwd /Users/iwaltgen/workspace/restdocs-openapi3 --branch fix/everit-1.14.6-v1 --base origin/v1.x --path "$v1_worktree_path" --label "Everit 1.14.6 v1.x" --no-focus
test "${HERDR_ENV:-}" = "1"
v1_workspace_id=$(herdr worktree list --cwd /Users/iwaltgen/workspace/restdocs-openapi3 | jq -r --arg path "$v1_worktree_path" '.result.worktrees[] | select(.path == $path) | .open_workspace_id')
test -n "$v1_worktree_path" && test "$v1_worktree_path" != "null"
test -n "$v1_workspace_id" && test "$v1_workspace_id" != "null"
```

- [ ] **Step 3: Create the `main` Herdr worktree**

```bash
main_worktree_path=/Users/iwaltgen/.herdr/worktrees/restdocs-openapi3/fix-everit-1-14-6-main
test "${HERDR_ENV:-}" = "1"
herdr worktree create --cwd /Users/iwaltgen/workspace/restdocs-openapi3 --branch fix/everit-1.14.6-main --base origin/main --path "$main_worktree_path" --label "Everit 1.14.6 main" --no-focus
test "${HERDR_ENV:-}" = "1"
main_workspace_id=$(herdr worktree list --cwd /Users/iwaltgen/workspace/restdocs-openapi3 | jq -r --arg path "$main_worktree_path" '.result.worktrees[] | select(.path == $path) | .open_workspace_id')
test -n "$main_worktree_path" && test "$main_worktree_path" != "null"
test -n "$main_workspace_id" && test "$main_workspace_id" != "null"
```

- [ ] **Step 4: Confirm isolation and clean starting state**

```bash
test "${HERDR_ENV:-}" = "1"
herdr worktree list --cwd /Users/iwaltgen/workspace/restdocs-openapi3
git -C "$v1_worktree_path" status --short --branch
git -C "$main_worktree_path" status --short --branch
```

Expected: each new worktree is clean and points at the recorded branch base. Existing P2 and planning worktrees remain untouched.

- [ ] **Step 5: Run a JDK 17 baseline on each branch**

```bash
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
JAVA_HOME_17_X64=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
  "$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
  -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64 \
  clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 \
  --no-daemon --console=plain

JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
JAVA_HOME_17_X64=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
  "$main_worktree_path/gradlew" -p "$main_worktree_path" \
  -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64 \
  clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 \
  --no-daemon --console=plain
```

Expected: both baselines pass before source changes.

## Task 2: Lock the Enum Contract with Tests on `v1.x`

**Files:**

- Modify: `restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGeneratorTest.kt`
- Verify unchanged: `restdocs-api-spec-generator/src/test/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3GeneratorTest.kt`

- [ ] **Step 1: Add a scalar-type ordering test**

Add a test named `should preserve scalar enum value order` that generates one object containing:

```kotlin
FieldDescriptor("stringEnum", "", "string", attributes = Attributes(enumValues = listOf("THIRD", "FIRST", "SECOND")))
FieldDescriptor("integerEnum", "", "integer", attributes = Attributes(enumValues = listOf(3, 1, 2)))
FieldDescriptor("numberEnum", "", "number", attributes = Attributes(enumValues = listOf(0.3, 0.1, 0.2)))
FieldDescriptor("booleanEnum", "", "boolean", attributes = Attributes(enumValues = listOf(true, false)))
```

Read the generated JSON with JsonPath and assert exact list equality, including element order, for all four `enum` arrays. Use order-sensitive AssertJ assertions.

- [ ] **Step 2: Add a generated-shape and validation test**

Add a test named `should preserve enum value order across generated schema shapes`. Use `listOf("THIRD", "FIRST", "SECOND")` for:

- a flat string property;
- `nested.value` inside an object;
- an array whose `Attributes.items` is `TypeDescriptor("string", attributes = Attributes(enumValues = enumValues))`.

Assert exact JsonPath list equality for:

```text
$.properties.flat.enum
$.properties.nested.properties.value.enum
$.properties.values.items.enum
```

Load the generated schema with `SchemaLoader`, validate a document containing listed values, and assert `ValidationException` for a document containing `"UNKNOWN"`.

- [ ] **Step 3: Add a non-synthetic combined-schema test**

Add a test named `should preserve enum order inside non synthetic combined schema`. Generate a serialized `oneOf` from these two descriptors for the same path:

```kotlin
FieldDescriptor(
    "mixed",
    "",
    "string",
    attributes = Attributes(enumValues = listOf("THIRD", "FIRST", "SECOND")),
)
FieldDescriptor("mixed", "", "boolean")
```

Select the branch containing `enum` from `$.properties.mixed.oneOf`, assert exactly one such branch, and assert `listOf("THIRD", "FIRST", "SECOND")` in exact order.

- [ ] **Step 4: Prove the tests describe the existing 1.11.0 contract**

```bash
"$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
  :restdocs-api-spec-jsonschema:test \
  --tests 'com.keecon.restdocs.apispec.jsonschema.JsonSchemaGeneratorTest' \
  :restdocs-api-spec-generator:test \
  --tests 'com.keecon.restdocs.apispec.generator.OpenApi3GeneratorTest' \
  --no-daemon --console=plain
```

Expected: all new and existing tests pass with Everit 1.11.0. If not, investigate the implementation before changing test expectations.

- [ ] **Step 5: Review the test-only diff**

```bash
git -C "$v1_worktree_path" diff --check
git -C "$v1_worktree_path" diff -- restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGeneratorTest.kt
```

Confirm the assertions cover values, order, schema positions, and validation without depending on incidental pretty-print whitespace.

- [ ] **Step 6: Optional commit gate**

Only after explicit authorization:

```bash
git -C "$v1_worktree_path" add restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGeneratorTest.kt
git -C "$v1_worktree_path" commit -m "test: cover deterministic enum schema ordering"
```

## Task 3: Upgrade `v1.x` and Reproduce the Regression

**Files:**

- Modify: `gradle/libs.versions.toml:12`

- [ ] **Step 1: Change only the Everit version**

```toml
everit-json-schema = "1.14.6"
```

- [ ] **Step 2: Inspect the resolved runtime graph**

```bash
"$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
  :restdocs-api-spec-jsonschema:dependencyInsight \
  --dependency com.github.erosb:everit-json-schema \
  --configuration runtimeClasspath --no-daemon --console=plain
"$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
  :restdocs-api-spec-jsonschema:dependencies \
  --configuration runtimeClasspath --no-daemon --console=plain
```

Expected: Everit 1.14.6 resolves with its current transitive graph, including `org.json:json:20250107`, `commons-validator:commons-validator:1.9.0`, `com.damnhandy:handy-uri-templates:2.1.8`, and `com.google.re2j:re2j:1.8`. Confirm the Everit path no longer brings in `commons-beanutils:commons-beanutils:1.9.3`.

- [ ] **Step 3: Re-run the focused contract tests and capture RED**

```bash
"$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
  :restdocs-api-spec-jsonschema:test \
  --tests 'com.keecon.restdocs.apispec.jsonschema.JsonSchemaGeneratorTest' \
  :restdocs-api-spec-generator:test \
  --tests 'com.keecon.restdocs.apispec.generator.OpenApi3GeneratorTest' \
  --no-daemon --console=plain
```

Expected: order-sensitive assertions fail while validation semantics remain intact. Record the actual/expected enum arrays. Do not change the assertions to accept arbitrary order.

## Task 4: Implement the Jackson 2 Formatter on `v1.x`

**Files:**

- Create: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/EveritSchemaJsonFormatter.kt`
- Modify: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGenerator.kt`

- [ ] **Step 1: Add the internal formatter**

Implement this exact internal surface:

```kotlin
internal class EveritSchemaJsonFormatter {
    private val objectMapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    fun format(schema: Schema): String

    private fun restoreEnumOrder(schema: Schema, node: JsonNode)
    private fun restoreEnumOrder(schema: EnumSchema, node: JsonNode)
    private fun restoreEnumOrder(schema: ObjectSchema, node: JsonNode)
    private fun restoreEnumOrder(schema: ArraySchema, node: JsonNode)
    private fun restoreEnumOrder(schema: CombinedSchema, node: JsonNode)
}
```

Use Jackson 2 imports from `com.fasterxml.jackson.databind` and `com.fasterxml.jackson.module.kotlin.jacksonObjectMapper`. In `format`:

1. serialize the Everit `Schema` using `JSONPrinter` and `StringWriter`;
2. parse the JSON to a Jackson `JsonNode`;
3. recursively restore enum arrays;
4. return `objectMapper.writeValueAsString(root)`.

Traversal rules:

```kotlin
when (schema) {
    is EnumSchema -> restoreEnumOrder(schema, node)
    is ObjectSchema -> schema.propertySchemas.forEach { (name, propertySchema) ->
        node.path("properties").get(name)?.let { restoreEnumOrder(propertySchema, it) }
    }
    is ArraySchema -> schema.allItemSchema?.let { itemSchema ->
        node.get("items")?.let { restoreEnumOrder(itemSchema, it) }
    }
    is CombinedSchema -> {
        val subschemas = schema.subschemas.toList()
        val criterionNode = node.get(schema.criterion.toString())
        if (criterionNode is ArrayNode && criterionNode.size() == subschemas.size) {
            subschemas.zip(criterionNode).forEach { (subschema, subnode) -> restoreEnumOrder(subschema, subnode) }
        } else {
            subschemas.forEach { restoreEnumOrder(it, node) }
        }
    }
}
```

Use `schema.criterion.toString()` for the criterion lookup. Everit 1.14.6's public `ALL_CRITERION`, `ANY_CRITERION`, and `ONE_CRITERION` implementations return `allOf`, `anyOf`, and `oneOf`, matching `ToStringVisitor`. Do not inspect package-private `isSynthetic`.

For `EnumSchema`, require an `ObjectNode`, create a Jackson array with `objectMapper.valueToTree<JsonNode>(schema.possibleValuesAsList)`, and replace only its `enum` field.

- [ ] **Step 2: Wire the formatter into the generator**

In `JsonSchemaGenerator`:

```kotlin
private val schemaFormatter = EveritSchemaJsonFormatter()
```

Replace the return path with:

```kotlin
return schemaFormatter.format(unWrapRootArray(jsonFieldPaths, schema))
```

Delete the superseded `toFormattedString` method and only the imports made unused by that deletion.

- [ ] **Step 3: Run focused tests for GREEN**

```bash
"$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
  :restdocs-api-spec-jsonschema:test \
  --tests 'com.keecon.restdocs.apispec.jsonschema.JsonSchemaGeneratorTest' \
  :restdocs-api-spec-generator:test \
  --tests 'com.keecon.restdocs.apispec.generator.OpenApi3GeneratorTest' \
  --no-daemon --console=plain
```

Expected: scalar, shape, combined-schema, validation, and existing OpenAPI exact-order tests all pass.

- [ ] **Step 4: Run the complete affected module tests**

```bash
"$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
  :restdocs-api-spec-jsonschema:check \
  :restdocs-api-spec-generator:check \
  --no-daemon --console=plain
```

- [ ] **Step 5: Review the behavior diff**

```bash
git -C "$v1_worktree_path" diff --check
git -C "$v1_worktree_path" diff --stat
git -C "$v1_worktree_path" diff -- gradle/libs.versions.toml restdocs-api-spec-jsonschema
```

Confirm the formatter is `internal`, only enum arrays are replaced, and no public signature or dependency scope changes.

- [ ] **Step 6: Optional commit gate**

Only after explicit authorization:

```bash
git -C "$v1_worktree_path" add gradle/libs.versions.toml restdocs-api-spec-jsonschema
git -C "$v1_worktree_path" commit -m "fix: preserve enum order with Everit 1.14.6"
```

## Task 5: Verify the Complete `v1.x` Candidate

**Files:**

- Verify: all tracked files in the `v1.x` worktree

- [ ] **Step 1: Run the full build under every supported installed JDK**

```bash
for java_home in \
  /Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
  /Users/iwaltgen/.local/share/mise/installs/java/temurin-21.0.11+10.0.LTS \
  /Users/iwaltgen/.local/share/mise/installs/java/temurin-25.0.4+7.0.LTS
do
  JAVA_HOME="$java_home" \
  JAVA_HOME_17_X64=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
    "$v1_worktree_path/gradlew" -p "$v1_worktree_path" \
    -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64 \
    clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 \
    --no-daemon --console=plain || exit 1
done
```

- [ ] **Step 2: Confirm bytecode and dependency intent**

```bash
javap -verbose \
  -classpath "$v1_worktree_path/restdocs-api-spec-jsonschema/build/classes/kotlin/main" \
  com.keecon.restdocs.apispec.jsonschema.JsonSchemaGenerator | rg 'major version: 61'
git -C "$v1_worktree_path" diff --check
git -C "$v1_worktree_path" status --short
```

Expected: all matrices pass, Java 17 remains the target, and only planned files are modified.

## Task 6: Forward-Port the Verified Behavior to `main`

**Files:**

- Modify: `gradle/libs.versions.toml:12`
- Create: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/EveritSchemaJsonFormatter.kt`
- Modify: `restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGenerator.kt`
- Modify: `restdocs-api-spec-jsonschema/src/test/kotlin/com/keecon/restdocs/apispec/jsonschema/JsonSchemaGeneratorTest.kt`
- Modify: `.github/dependabot.yml:42`
- Verify unchanged: `restdocs-api-spec-generator/src/test/kotlin/com/keecon/restdocs/apispec/generator/OpenApi3GeneratorTest.kt`

- [ ] **Step 1: Add the full enum contract tests while main still uses Everit 1.11.0**

Add three order-sensitive tests to `JsonSchemaGeneratorTest`:

1. `should preserve scalar enum value order`, covering string `THIRD/FIRST/SECOND`, integer `3/1/2`, decimal `0.3/0.1/0.2`, and boolean `true/false` arrays.
2. `should preserve enum value order across generated schema shapes`, covering flat, `nested.value`, and array `items`, plus allowed/unknown value validation.
3. `should preserve enum order inside non synthetic combined schema`, covering a `oneOf` string enum branch beside a boolean branch.

Use the existing Jackson 3-compatible test imports and exact JsonPath list assertions. Run:

```bash
"$main_worktree_path/gradlew" -p "$main_worktree_path" \
  :restdocs-api-spec-jsonschema:test \
  --tests 'com.keecon.restdocs.apispec.jsonschema.JsonSchemaGeneratorTest' \
  :restdocs-api-spec-generator:test \
  --tests 'com.keecon.restdocs.apispec.generator.OpenApi3GeneratorTest' \
  --no-daemon --console=plain
```

Expected: the new contract is green on 1.11.0.

- [ ] **Step 2: Set Everit to 1.14.6 and capture the same RED evidence**

Set:

```toml
everit-json-schema = "1.14.6"
```

Re-run the focused command and record the order failures without relaxing assertions.

- [ ] **Step 3: Implement the Jackson 3 formatter**

Create the internal `EveritSchemaJsonFormatter` with these exact signatures:

```kotlin
internal class EveritSchemaJsonFormatter {
    private val objectMapper = jacksonMapperBuilder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build()

    fun format(schema: Schema): String

    private fun restoreEnumOrder(schema: Schema, node: JsonNode)
    private fun restoreEnumOrder(schema: EnumSchema, node: JsonNode)
    private fun restoreEnumOrder(schema: ObjectSchema, node: JsonNode)
    private fun restoreEnumOrder(schema: ArraySchema, node: JsonNode)
    private fun restoreEnumOrder(schema: CombinedSchema, node: JsonNode)
}
```

Use Jackson 3 imports under `tools.jackson.databind`, `tools.jackson.databind.node`, and `tools.jackson.module.kotlin.jacksonMapperBuilder`. The implementation must:

- serialize with Everit `JSONPrinter` and `StringWriter`;
- parse to `JsonNode`;
- replace an `EnumSchema` node's `enum` with `possibleValuesAsList`;
- traverse object `properties` and array `items`;
- zip a combined schema's ordered subschemas with an equally sized serialized criterion array;
- traverse every subschema against the same JSON node when the criterion array is absent, which handles flattened synthetic schemas;
- avoid private/package-private Everit state.

Wire a private formatter field into `JsonSchemaGenerator`, return `schemaFormatter.format(...)`, and delete the old formatting helper and now-unused imports.

- [ ] **Step 4: Restore Dependabot coverage**

Delete only this v1.x ignore block from `.github/dependabot.yml`:

```yaml
      # Everit 1.14.6 changes generated enum ordering. Keep the v1.x output contract stable.
      - dependency-name: "com.github.erosb:everit-json-schema"
        versions:
          - "1.14.6"
```

Keep the repository's general semver-major ignore policy unchanged.

- [ ] **Step 5: Verify dependency resolution and focused behavior**

```bash
"$main_worktree_path/gradlew" -p "$main_worktree_path" \
  :restdocs-api-spec-jsonschema:dependencyInsight \
  --dependency com.github.erosb:everit-json-schema \
  --configuration runtimeClasspath --no-daemon --console=plain
"$main_worktree_path/gradlew" -p "$main_worktree_path" \
  :restdocs-api-spec-jsonschema:test \
  --tests 'com.keecon.restdocs.apispec.jsonschema.JsonSchemaGeneratorTest' \
  :restdocs-api-spec-generator:test \
  --tests 'com.keecon.restdocs.apispec.generator.OpenApi3GeneratorTest' \
  --no-daemon --console=plain
```

Expected: Everit 1.14.6 is selected and all focused tests pass.

- [ ] **Step 6: Keep behavior and automation commits separable**

After explicit commit authorization only:

```bash
git -C "$main_worktree_path" add gradle/libs.versions.toml restdocs-api-spec-jsonschema
git -C "$main_worktree_path" commit -m "fix: preserve enum order with Everit 1.14.6"
git -C "$main_worktree_path" add .github/dependabot.yml
git -C "$main_worktree_path" commit -m "build: re-enable Everit dependency updates"
```

## Task 7: Verify the Complete `main` Candidate and Cross-Branch Parity

**Files:**

- Verify: all tracked files in both implementation worktrees

- [ ] **Step 1: Run the full `main` build under every installed JDK**

```bash
for java_home in \
  /Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
  /Users/iwaltgen/.local/share/mise/installs/java/temurin-21.0.11+10.0.LTS \
  /Users/iwaltgen/.local/share/mise/installs/java/temurin-25.0.4+7.0.LTS
do
  JAVA_HOME="$java_home" \
  JAVA_HOME_17_X64=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.19+10 \
    "$main_worktree_path/gradlew" -p "$main_worktree_path" \
    -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64 \
    clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 \
    --no-daemon --console=plain || exit 1
done
```

- [ ] **Step 2: Compare formatter behavior across branches**

```bash
git diff --no-index \
  "$v1_worktree_path/restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/EveritSchemaJsonFormatter.kt" \
  "$main_worktree_path/restdocs-api-spec-jsonschema/src/main/kotlin/com/keecon/restdocs/apispec/jsonschema/EveritSchemaJsonFormatter.kt" || true
```

Expected: differences are restricted to Jackson package names and mapper construction. Traversal and enum replacement logic are behaviorally identical.

- [ ] **Step 3: Audit final diffs and dependency graphs**

```bash
git -C "$v1_worktree_path" diff --check
git -C "$v1_worktree_path" diff --stat
git -C "$v1_worktree_path" status --short
git -C "$main_worktree_path" diff --check
git -C "$main_worktree_path" diff --stat
git -C "$main_worktree_path" status --short
```

Confirm:

- both version catalogs resolve Everit 1.14.6;
- both formatters preserve the same order contract;
- the main Dependabot file no longer suppresses 1.14.6;
- no unrelated source, lockfile, generated output, or P2 worktree change is present.

## Task 8: Integration Approval and Safe Herdr Cleanup

**Files:**

- No repository modifications required.

- [ ] **Step 1: Present the completion evidence before external actions**

Report:

- recorded base SHAs and final branch tips;
- focused RED/GREEN evidence;
- JDK 17/21/25 build matrix for each branch;
- resolved dependency graph changes;
- exact changed-file lists and cross-branch formatter differences;
- remaining risks, if any.

Ask separately for authorization to commit, push, open/merge PRs, close the historical Everit PR/issue, or post GitHub comments. Repository-level Dependabot ignore changes outside `.github/dependabot.yml` are also separate external state changes and require explicit authorization.

- [ ] **Step 2: Integrate in maintenance order when authorized**

Integrate the `v1.x` candidate first. Integrate the `main` forward-port only after the `v1.x` result and parity evidence are confirmed. Re-run the branch-required checks if either target branch moved after the recorded base SHA.

- [ ] **Step 3: Inspect every Herdr worktree before cleanup**

```bash
test "${HERDR_ENV:-}" = "1"
herdr worktree list --cwd /Users/iwaltgen/workspace/restdocs-openapi3
v1_worktree_path=/Users/iwaltgen/.herdr/worktrees/restdocs-openapi3/fix-everit-1-14-6-v1
main_worktree_path=/Users/iwaltgen/.herdr/worktrees/restdocs-openapi3/fix-everit-1-14-6-main
git -C "$v1_worktree_path" status --short
git -C "$main_worktree_path" status --short
```

- [ ] **Step 4: Remove only clean implementation worktrees created in Task 1**

After integration or verified preservation of all changes:

```bash
test "${HERDR_ENV:-}" = "1"
v1_workspace_id=$(herdr worktree list --cwd /Users/iwaltgen/workspace/restdocs-openapi3 | jq -r --arg path "$v1_worktree_path" '.result.worktrees[] | select(.path == $path) | .open_workspace_id')
test -n "$v1_workspace_id" && test "$v1_workspace_id" != "null"
test "${HERDR_ENV:-}" = "1"
herdr worktree remove --workspace "$v1_workspace_id"
test "${HERDR_ENV:-}" = "1"
main_workspace_id=$(herdr worktree list --cwd /Users/iwaltgen/workspace/restdocs-openapi3 | jq -r --arg path "$main_worktree_path" '.result.worktrees[] | select(.path == $path) | .open_workspace_id')
test -n "$main_workspace_id" && test "$main_workspace_id" != "null"
test "${HERDR_ENV:-}" = "1"
herdr worktree remove --workspace "$main_workspace_id"
```

Do not remove the existing P2 workspaces or the planning worktree. If either implementation worktree is dirty, stop and report it; do not use `--force`.

## Definition of Done

- [ ] `v1.x` and `main` both use exactly Everit JSON Schema 1.14.6.
- [ ] Exact enum order is covered for string, integer, decimal, and boolean values.
- [ ] Exact enum order is covered in flat, nested-object, array-item, and non-synthetic combined-schema output.
- [ ] Allowed values validate and an unlisted value raises `ValidationException`.
- [ ] Existing OpenAPI enum order assertions remain unchanged and pass.
- [ ] The solution uses no reflection, sorting, fork, new library, or public API expansion.
- [ ] Both branches pass the full JDK 17/21/25 build matrix.
- [ ] Resolved dependencies show Everit 1.14.6 and no obsolete Everit-sourced BeanUtils 1.9.3 path.
- [ ] The main branch's Everit 1.14.6 Dependabot ignore is removed only after both candidates are green.
- [ ] Diffs contain no unrelated changes, and no external action occurred without explicit approval.
