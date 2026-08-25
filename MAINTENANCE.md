# Maintenance Policy

| Branch | Spring Boot | Status |
|---|---|---|
| `v0.x` | 2.7.x | Frozen; no fixes or releases |
| `v1.x` | 3.5.x | Security, compatibility, and managed dependency fixes |
| `main` | 4.x | Active 2.x development |

The active release line stays on `main`. A maintenance branch named `vN.x` is
created only when the next Spring Boot and project major transition starts. In
particular, there is no `v2.x` branch while project 2.x remains active on
`main`.

## Fix and backport policy

Fixes specific to the maintained 1.x line are made on `v1.x` first. Every such
fix must then be forward-ported to `main` so the active line does not regress.
For defects shared by both lines, validate the fix on `main` first and backport
only the compatible parts to `v1.x`. The frozen `v0.x` line receives neither
fixes nor dependency updates.

## Releases and Java support

- Tags matching `1.*` must be created from a commit on `v1.x`.
- Tags matching `2.*` must be created from a commit on `main`.
- The 0.x line is frozen. Existing 0.x tags remain historical records, but no
  new 0.x release is made.
- All supported artifacts target Java 17 bytecode. CI tests only the supported
  LTS JDKs 17, 21, and 25.

When a release line is no longer listed as active or maintained in this file,
it is end-of-life and receives no further fixes, updates, or releases.
