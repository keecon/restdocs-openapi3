# Dependabot PR Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Review and integrate every open Dependabot pull request targeting `main` and `v1.x` without weakening the repository's Java, Spring Boot, ABI, or CI guarantees.

**Architecture:** Preserve each Dependabot commit by merging its PR ref into the target branch. Apply dependency updates before workflow updates, run focused tests after each merge, then run the branch's complete verification suite before pushing. Treat already-applied updates as no-op merges so the corresponding PR is closed without changing resolved versions.

**Tech Stack:** Gradle 9.7.x, Kotlin 2.4.10, Java toolchain 17, Temurin JDK 17/21/25, Spring Boot 4.1 on `main`, Spring Boot 3.5 on `v1.x`, GitHub Actions, Dependabot.

**Spec:** GitHub Dependabot pull requests [#217](https://github.com/keecon/restdocs-openapi3/pull/217), [#219](https://github.com/keecon/restdocs-openapi3/pull/219), [#220](https://github.com/keecon/restdocs-openapi3/pull/220), [#221](https://github.com/keecon/restdocs-openapi3/pull/221), [#224](https://github.com/keecon/restdocs-openapi3/pull/224), [#225](https://github.com/keecon/restdocs-openapi3/pull/225), [#226](https://github.com/keecon/restdocs-openapi3/pull/226), [#227](https://github.com/keecon/restdocs-openapi3/pull/227), [#228](https://github.com/keecon/restdocs-openapi3/pull/228), [#229](https://github.com/keecon/restdocs-openapi3/pull/229), [#231](https://github.com/keecon/restdocs-openapi3/pull/231), [#232](https://github.com/keecon/restdocs-openapi3/pull/232), and [#233](https://github.com/keecon/restdocs-openapi3/pull/233).

## Global Constraints

- Keep 2.x on `main` with Spring Boot 4.1 and 1.x on `v1.x` with Spring Boot 3.5.
- Produce Java 17 bytecode and test only LTS JDK 17, 21, and 25.
- Do not introduce new dependencies, source changes, API changes, or unrelated refactors.
- Preserve every Dependabot commit and use one merge commit per PR.
- Verify Gradle wrapper checksums against the official distribution checksum.
- Do not push a target branch until its focused and full local verification passes.
- Stop at the failing PR if a merge conflict, compatibility regression, or test failure occurs.

---

### Task 1: Inventory and compatibility review

**Files:** None

**Interfaces:**
- Consumes: open GitHub pull requests and fetched `refs/pull/*/head` refs.
- Produces: a reviewed set of four `main` PRs and nine `v1.x` PRs.

- [ ] **Step 1: Refresh target branches and PR refs**

```bash
git fetch --prune origin
git status --short --branch
```

Expected: `main` contains only intentional local commits and all 13 PR refs resolve to Dependabot commits.

- [ ] **Step 2: Inspect every changed path and commit author**

```bash
git diff --name-status origin/main...origin/pr/217
git diff --name-status origin/main...origin/pr/219
git diff --name-status origin/main...origin/pr/220
git diff --name-status origin/main...origin/pr/221
git diff --name-status origin/v1.x...origin/pr/224
git diff --name-status origin/v1.x...origin/pr/225
git diff --name-status origin/v1.x...origin/pr/226
git diff --name-status origin/v1.x...origin/pr/227
git diff --name-status origin/v1.x...origin/pr/228
git diff --name-status origin/v1.x...origin/pr/229
git diff --name-status origin/v1.x...origin/pr/231
git diff --name-status origin/v1.x...origin/pr/232
git diff --name-status origin/v1.x...origin/pr/233
```

Expected: dependency PRs modify only `gradle/libs.versions.toml`, wrapper PR #219 modifies only wrapper files, and action PRs modify only `.github/workflows/build.yml`.

- [ ] **Step 3: Review upstream compatibility and CI evidence**

Confirm that Gradle 9.7.1 is a recommended patch, Spring Boot 4.1.1 and Guava 33.7.1 are patch releases, the action majors support GitHub-hosted runners, and JUnit Pioneer `TempDirectory` plus Everit public classes used by this repository remain available. Record any unsupported runner or public API change as a blocker.

### Task 2: Integrate `main` dependency updates

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `gradlew.bat`

**Interfaces:**
- Consumes: PRs #217, #219, #220, and #221.
- Produces: `main` on Axion 1.21.3, Gradle 9.7.1, Spring Boot 4.1.1, and Guava 33.7.1-jre.

- [ ] **Step 1: Merge the already-applied Axion patch PR**

```bash
git merge --no-ff origin/pr/217 -m "Merge pull request #217 from keecon/dependabot/gradle/pl.allegro.tech.build.axion-release-1.21.3"
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew currentVersion --no-daemon
```

Expected: the merge changes no resolved version because `main` already contains 1.21.3; `currentVersion` succeeds.

- [ ] **Step 2: Merge and verify Guava 33.7.1-jre**

```bash
git merge --no-ff origin/pr/221 -m "Merge pull request #221 from keecon/dependabot/gradle/com.google.guava-guava-33.7.1-jre"
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew :restdocs-api-spec:test --no-daemon
```

Expected: core tests pass with Guava 33.7.1-jre.

- [ ] **Step 3: Merge and verify Spring Boot 4.1.1**

```bash
git merge --no-ff origin/pr/220 -m "Merge pull request #220 from keecon/dependabot/gradle/spring-boot-4.1.1"
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew :restdocs-api-spec-example:test :restdocs-api-spec-example:openapi3 --no-daemon
```

Expected: the Boot 4.1 example tests and OpenAPI generation pass.

- [ ] **Step 4: Verify and merge the Gradle 9.7.1 wrapper**

```bash
test "$(curl -fsSL https://services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256)" = "acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a"
git merge --no-ff origin/pr/219 -m "Merge pull request #219 from keecon/dependabot/gradle/gradle-wrapper-9.7.1"
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew --version
```

Expected: the official checksum matches and the wrapper reports Gradle 9.7.1.

### Task 3: Verify and push `main`

**Files:** None beyond Task 2 and the approved README/plan documentation.

**Interfaces:**
- Consumes: integrated `main` from Task 2.
- Produces: verified `origin/main` with PRs #217, #219, #220, and #221 reachable.

- [ ] **Step 1: Run complete verification on Java 17**

```bash
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
```

Expected: all tasks succeed and generated OpenAPI output exists.

- [ ] **Step 2: Push and verify ancestry**

```bash
git push origin main
git fetch origin
test "$(git rev-parse main)" = "$(git rev-parse origin/main)"
```

Expected: `origin/main` contains the four PR commits and the branch workflow starts.

### Task 4: Integrate `v1.x` library and plugin updates

**Files:**
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: PRs #224, #225, #229, #232, and #233.
- Produces: `v1.x` on Axion 1.21.3, Guava 33.7.1-jre, Swagger Parser 2.1.47, Everit JSON Schema 1.14.6, and JUnit Pioneer 0.9.2.

- [ ] **Step 1: Merge the low-risk Axion, Guava, and Swagger Parser updates**

```bash
git switch v1.x
git merge --no-ff origin/pr/233 -m "Merge pull request #233 from keecon/dependabot/gradle/v1.x/pl.allegro.tech.build.axion-release-1.21.3"
git merge --no-ff origin/pr/225 -m "Merge pull request #225 from keecon/dependabot/gradle/v1.x/com.google.guava-guava-33.7.1-jre"
git merge --no-ff origin/pr/229 -m "Merge pull request #229 from keecon/dependabot/gradle/v1.x/io.swagger.parser.v3-swagger-parser-2.1.47"
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew currentVersion :restdocs-api-spec:test :restdocs-api-spec-generator:test --no-daemon
```

Expected: release version calculation, core tests, and parser-backed generator tests pass.

- [ ] **Step 2: Merge and verify Everit JSON Schema 1.14.6**

```bash
git merge --no-ff origin/pr/224 -m "Merge pull request #224 from keecon/dependabot/gradle/v1.x/com.github.erosb-everit-json-schema-1.14.6"
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew :restdocs-api-spec-jsonschema:test :restdocs-api-spec-gradle-plugin:test --no-daemon
```

Expected: schema behavior and the published consumer's Everit-facing public API compile and pass.

- [ ] **Step 3: Merge and verify JUnit Pioneer 0.9.2**

```bash
git merge --no-ff origin/pr/232 -m "Merge pull request #232 from keecon/dependabot/gradle/v1.x/org.junit-pioneer-junit-pioneer-0.9.2"
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew test --rerun-tasks --no-daemon
```

Expected: all `TempDirectory`-based tests compile and pass.

### Task 5: Integrate `v1.x` GitHub Actions updates

**Files:**
- Modify: `.github/workflows/build.yml`

**Interfaces:**
- Consumes: PRs #226, #227, #228, and #231.
- Produces: `v1.x` using checkout v7, setup-java v5, Gradle Actions v6, and Codecov Action v7.

- [ ] **Step 1: Merge each workflow PR**

```bash
git merge --no-ff origin/pr/227 -m "Merge pull request #227 from keecon/dependabot/github_actions/v1.x/actions/checkout-7"
git merge --no-ff origin/pr/231 -m "Merge pull request #231 from keecon/dependabot/github_actions/v1.x/actions/setup-java-5"
git merge --no-ff origin/pr/226 -m "Merge pull request #226 from keecon/dependabot/github_actions/v1.x/gradle/actions-6"
git merge --no-ff origin/pr/228 -m "Merge pull request #228 from keecon/dependabot/github_actions/v1.x/codecov/codecov-action-7"
```

Expected: every merge is conflict-free and only the four `uses:` values change.

- [ ] **Step 2: Validate the combined workflow diff**

```bash
git diff origin/v1.x...v1.x -- .github/workflows/build.yml
git diff --check origin/v1.x...v1.x
```

Expected: JDK 17/21/25, `JAVA_HOME_17_X64`, Codecov inputs, branch filters, and build command remain unchanged.

### Task 6: Verify and push `v1.x`

**Files:** None beyond Tasks 4 and 5.

**Interfaces:**
- Consumes: integrated `v1.x` from Tasks 4 and 5.
- Produces: verified `origin/v1.x` with all nine PR commits reachable.

- [ ] **Step 1: Run complete maintenance-line verification**

```bash
JAVA_HOME=/Users/iwaltgen/.local/share/mise/installs/java/temurin-17.0.18+8 ./gradlew clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
```

Expected: all tasks succeed on the Java 17 toolchain baseline.

- [ ] **Step 2: Push and verify GitHub state**

```bash
git push origin v1.x
git fetch origin
test "$(git rev-parse v1.x)" = "$(git rev-parse origin/v1.x)"
```

Expected: all nine PR commits are reachable from `origin/v1.x`; GitHub closes the PRs and starts JDK 17/21/25 checks.

- [ ] **Step 3: Confirm all PRs and branch checks**

Query the GitHub Pull Requests and Checks APIs until every PR is closed or merged and every target-branch JDK check completes successfully. Report any delayed or failed Codecov upload separately because it is external to the local build result.
