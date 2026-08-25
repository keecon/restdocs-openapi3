# Maintenance Branches and Gradle DSL Design

**Date:** 2026-08-26

**Status:** Approved in conversation; implementation pending

## Context

The review covers every commit from `e71dfe594b430485196bee19f722777b3e5a4fe9`
through `2f180a657596f071ab5156d18620bf02e190eecd`. This range introduced the
Spring Boot 3.5 maintenance baseline, the Spring Boot 4 migration, compatibility
ports, and the 2.1.0 Contact, WebTestClient, and LocalDate features.

The repository currently has inconsistent maintenance branch names:

- `origin/v0` points to `0.19.3` (`b067247`).
- `origin/1.x` points to `1.1.0` (`1b0bc1a`).
- `origin/main` is the active 2.x line (`2f180a6`).

The version policy ties the project major version to the supported Spring Boot
major version. An active release line stays on `main`; a `vN.x` branch is cut
only when that line enters maintenance.

## Goals

- Correct the confirmed schema, OAuth2, and JWT behavior defects with regression
  tests.
- Improve the `openapi3` Gradle DSL additively without breaking existing Groovy
  configuration.
- Establish `v0.x` and `v1.x` as consistently named maintenance branches while
  keeping 2.x on `main`.
- Make CI, release verification, Dependabot targeting, examples, and maintenance
  documentation match the actual branch policy.
- Backport compatible bug fixes to `v1.x` without adding 2.x features.

## Non-goals

- Do not create `v2.x` while 2.x remains the active line on `main`.
- Do not add `schemaName` to 2.x; its ABI-safe design remains unproven.
- Do not restore the 1.x Gradle task ABI in 2.x. The task property migration was
  part of the Spring Boot 4/project 2.0 major transition and will be documented.
- Do not redesign the public DSL around managed Gradle properties in 2.x.
- Do not revive CI, dependency updates, or releases for frozen `v0.x`.
- Do not add dependencies.

## Review Findings

### Confirmed defects

1. `JsonSchemaGenerator.reduceFieldDescriptors` drops a descriptor when path and
   type match. Optional and ignored results therefore depend on input order.
2. Direct assignment to `oauth2SecuritySchemeDefinition` changes only the public
   field and does not refresh the serialized task input.
3. JWT parsing uses the basic Base64 decoder instead of the Base64URL decoder,
   performs decoding outside the guarded parse, and retains blank string scopes.
4. The release workflow installs multiple JDKs but executes the verification on
   one JVM. Tag releases are not gated by the JDK 17, 21, and 25 matrix.
5. The 1.x workflow filters only `main`, so maintenance pushes and pull requests
   do not run CI.

### Compatibility findings

The 2.0 migration changed public task getters from String/Boolean values to
Gradle `Property` and `DirectoryProperty` values. That is a source and binary
break from 1.x, but it occurred at the approved Spring Boot 3 to 4/project 1 to 2
major boundary. The implementation will preserve the current 2.x ABI and add a
migration note instead of reintroducing the old task API.

The existing Groovy extension syntax remains a 2.x compatibility contract:

```groovy
openapi3 {
  server = 'https://api.example.com'
  contact = {
    name = 'API Support'
  }
  oauth2SecuritySchemeDefinition = {
    flows = ['authorizationCode']
  }
}
```

## Code Design

### Deterministic descriptor reduction

Descriptor reduction will merge every repeated path, including identical types.
For identical types it will merge descriptor flags without adding a duplicate
schema builder:

- `optional` is true when either descriptor is optional.
- `ignored` is true only when both descriptors are ignored.
- the existing schema builder is retained for an identical type.

Different types continue to produce the existing combined schema. Public
`JsonSchemaGenerator` tests will pass required/optional and ignored descriptors
in both orders and assert identical schemas.

Constraint and other attribute conflict semantics are not expanded in this
change. Identical-type descriptors are expected to describe the same field
contract; the implementation preserves the existing retained descriptor's
attributes while making optional/ignored behavior deterministic.

### OAuth2 canonical assignment

`oauth2SecuritySchemeDefinition` will use a backing field and a public setter
that always refreshes the scope file input and serialized configuration. The
Closure setter will configure an object and delegate to that same canonical
setter. Direct object assignment and Closure configuration will therefore feed
the same task properties.

The existing mutable `servers` getter cannot observe arbitrary mutations inside
Swagger's mutable `Server` objects without replacing the model or introducing
configuration-cache-hostile callbacks. Existing documented assignment paths
remain supported and tested; direct mutation through `servers[0]` will be
documented as non-canonical instead of being presented as a guaranteed DSL path.

### JWT boundary handling

JWT header and payload segments will be decoded using
`Base64.getUrlDecoder()`. Decode and JSON parse errors will be treated as a
non-JWT/non-scope result rather than failing documentation generation. Scope
claims will accept strings and string lists, discard blank elements, and avoid
unchecked non-string list elements.

### Nullable date validation

The public nullable `FormatValidator.validate` boundary will not throw for a
null subject. A null value is outside a string format assertion and will return
no format error. Valid and invalid ISO LocalDate behavior remains unchanged.

## Gradle DSL Design

Existing Closure and assignment setters remain unchanged. Additive
`server(String)` and Gradle `Action` methods for `Server`, `Contact`, and
`PluginOauth2Configuration` will enable idiomatic Kotlin DSL and Groovy method
syntax:

```kotlin
openapi3 {
    server("https://api.example.com")
    contact {
        name = "API Support"
        email = "support@example.com"
    }
}
```

```groovy
openapi3 {
  server 'https://api.example.com'
  contact {
    name = 'API Support'
  }
}
```

The default output and snippet directories will use conventions derived from
`layout.buildDirectory`, while String setters continue to accept existing
relative paths. A custom Gradle build directory must move both defaults.

The task input will be evaluated for narrowing from the entire snippets
directory to `**/resource.json`. The narrowing is accepted only if Gradle tracks
relative paths correctly, missing snippet directories retain the documented
behavior, and configuration-cache tests pass.

Published-plugin TestKit coverage will compile and execute both Groovy and
Kotlin DSL consumers. Remote `plugins {}` usage through JitPack will not be
documented because Plugin Portal publication and remote marker resolution are
not part of this change.

## Branch and Release Policy

| Branch | Project line | Spring Boot | State | CI and updates |
|---|---:|---:|---|---|
| `v0.x` | 0.x | 2.7.x | Frozen | none |
| `v1.x` | 1.x | 3.5.x | Maintained | JDK 17/21/25 and Dependabot |
| `main` | 2.x | 4.x | Active | JDK 17/21/25 and Dependabot |

When the next Spring Boot major transition begins, the last 2.x release commit
will become `v2.x`, and the next project major will continue on `main`.

### Ref transition

1. Create `v0.x` at the exact `origin/v0` SHA and `v1.x` at the exact
   `origin/1.x` SHA. Do not force-update either new ref.
2. Verify the new remote refs before changing the old refs.
3. Apply 1.x-specific CI/release maintenance and compatible bug backports on
   `v1.x`.
4. Update default-branch documentation and Dependabot target entries.
5. Verify both active and maintained lines.
6. Delete only the obsolete remote `v0` and `1.x` refs after the new refs and
   documentation are available. No open pull requests currently target them.

Frozen `v0.x` receives no source commit as part of the rename. Historical plan
text remains historical; an addendum records the branch naming migration rather
than rewriting completed commands.

### Release gating

Release workflows will run verification on JDK 17, 21, and 25 before creating a
GitHub Release. Supported release workflows will map tag major versions to
release lines:

- `1.*` must be an ancestor of `v1.x`.
- `2.*` must be an ancestor of `main`.

The 0.x line is frozen and does not accept new tags; existing 0.x tags remain
historical records. Unknown or unsupported tag majors fail closed on maintained
workflows. The release publishing job depends on all matrix verification jobs.

## Documentation

A top-level `MAINTENANCE.md` will be the operational policy for branch lifecycle,
support status, allowed backports, forward-port rules, tag ancestry, and EOL.
README will link to it and fix the malformed `dependencies`/`openapi3` example
nesting.

README and example documentation will explain that the example subproject uses
an already published plugin to avoid a self-referential buildscript classpath,
while TestKit local publication verifies the plugin being built. The example's
published plugin version will be aligned with the current documented release.

The 1.x-to-2.x migration section will call out the Spring Boot/REST Docs/Jackson
major change and the managed Gradle task property change. Extension-level
Groovy configuration remains compatible.

## Verification

Main verification:

```bash
./gradlew clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3 --no-daemon
```

Run the command on Temurin JDK 17, 21, and 25 with the Java 17 toolchain. Verify
identical example OpenAPI hashes and Java class major version 61. Run published
Groovy and Kotlin consumer tests, configuration-cache reuse, generated POM
inspection, and `git diff --check`.

`v1.x` verification uses its own Gradle build with the same JDK 17, 21, and 25
matrix. Backported bug tests must pass there without importing Boot 4, Jackson 3,
Contact, or WebTestClient changes.

Remote ref verification compares local and remote SHAs for `main`, `v0.x`, and
`v1.x`, and confirms that `v0` and `1.x` are absent only after successful
transition.

## Rollback and Safety

New refs are created before old refs are removed, so branch tips remain
recoverable. The old SHA values and release tags provide immutable recovery
points. No force push is used. If any verification or push fails, old refs stay
in place and deletion stops.

Only the Herdr worktrees and temporary files created for this change may be
cleaned up. Existing worktrees and local feature branches remain untouched.
