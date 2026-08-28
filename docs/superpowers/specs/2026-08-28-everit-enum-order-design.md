# Everit 1.14.6 Enum Order Preservation Design

## Context

Everit JSON Schema 1.14.x keeps enum values in insertion order internally, but `ToStringVisitor` serializes `EnumSchema#getPossibleValues()`, which returns an unordered `Set`. Updating from 1.11.0 therefore changes generated OpenAPI enum arrays even though JSON Schema validation semantics are unchanged.

Keeping 1.11.0 indefinitely also retains old transitive dependencies and misses upstream correctness and maintenance fixes. The selected outcome is to update both maintained branches to Everit 1.14.6 while preserving the existing generated-output contract.

## Decision

Add an internal `EveritSchemaJsonFormatter` in the JSON Schema module. It serializes the Everit schema normally, parses the result into a Jackson tree, walks the Everit schema graph and JSON tree together, and replaces every serialized `enum` array with `EnumSchema#getPossibleValuesAsList()`.

The traversal supports only schema shapes produced by `JsonSchemaGenerator`:

- `ObjectSchema.propertySchemas` map to the JSON `properties` object.
- `ArraySchema.allItemSchema` maps to JSON `items`.
- A non-synthetic `CombinedSchema` maps its ordered subschemas to the array named by its criterion, such as `oneOf`.
- A flattened synthetic `CombinedSchema` has no criterion array; all subschemas map to the same JSON object.
- `EnumSchema` replaces only the `enum` node.

Synthetic status is inferred from the serialized criterion array rather than reflection because `CombinedSchema#isSynthetic()` is package-private. No Everit internals are reflected, forked, shaded, or copied.

## Branch Strategy

Implement on `v1.x` first, then forward-port the behavior to `main`, following the maintenance policy. Keep separate Herdr worktrees because `v1.x` uses Jackson 2 (`com.fasterxml.jackson`) while `main` uses Jackson 3 (`tools.jackson`). The formatter algorithm and tests remain logically identical; only Jackson imports and mapper construction differ.

## Output Contract

- Preserve descriptor-provided order for string, numeric, and boolean enum values.
- Preserve order in flat fields, nested object fields, array item schemas, and enum schemas nested inside non-synthetic `oneOf` structures.
- Preserve enum validation: allowed values pass and unlisted values fail.
- Do not alphabetically sort or weaken existing `containsExactly` assertions.
- Do not expose the formatter as public API.

## Dependency Policy

- Set `com.github.erosb:everit-json-schema` to exactly `1.14.6` on `v1.x` and `main`.
- Keep each branch's existing dependency scope unchanged: `api` on `main` and `implementation` on `v1.x`. Correcting the legacy `v1.x` publication metadata is a separate compatibility decision.
- Remove the `v1.x` Dependabot ignore for Everit only after both branches pass the required verification.
- Do not add another library or a patched Everit fork.

## Verification

- Reproduce the 1.14.6 failure before implementing the formatter.
- Run JSON Schema and OpenAPI generator regression tests after the formatter.
- Confirm resolved Everit, `org.json`, Commons Validator, URI template, and RE2/J versions and confirm old Commons BeanUtils is no longer resolved through Everit.
- Run `clean check testCodeCoverageReport :restdocs-api-spec-example:openapi3` on JDK 17, 21, and 25 for both branches.
- Require clean diffs and no unrelated source or generated-file changes.

## Out of Scope

- Migrating from maintenance-mode Everit to json-sKema or another schema library.
- Changing JSON Schema draft support.
- Changing enum semantics, values, naming, or consumer-visible ordering.
- Upstreaming the one-line Everit fix; that can proceed independently after the local compatibility fix.
