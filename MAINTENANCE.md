# Maintenance Policy

| Branch | Spring Boot | Status |
|---|---|---|
| `v0.x` | 2.7.x | Frozen; no fixes or releases |
| `v1.x` | 3.5.x | Frozen; no fixes or releases |
| `main` | 4.x | Active 2.x development |

The active release line stays on `main`. A maintenance branch named `vN.x` is
created only when the next Spring Boot and project major transition starts. In
particular, there is no `v2.x` branch while project 2.x remains active on
`main`.

## Fix and backport policy

Fixes and dependency updates are made only on `main`. The frozen `v0.x` and
`v1.x` lines receive neither fixes nor dependency updates, including security
fixes; users of those lines should migrate to the 2.x line.

## Releases and Java support

- Tags matching `2.*` must be created from a commit on `main`.
- The 0.x and 1.x lines are frozen. Existing 0.x and 1.x tags remain historical
  records, but no new release is made from those lines.
- All supported artifacts target Java 17 bytecode. CI tests only the supported
  LTS JDKs 17, 21, and 25.

When a release line is no longer listed as active or maintained in this file,
it is end-of-life and receives no further fixes, updates, or releases.
