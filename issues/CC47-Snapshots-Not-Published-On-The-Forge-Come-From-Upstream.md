# CC47 — Snapshots Not Published On The Forge Come From Upstream

**Status:** Review.
**Size:** S - `settings.xml`, the workflows' variables and one test of this repository; the variable's name is agreed
with the host's forge role.

**What must be true.** On a forge whose own registry holds only what its organization publishes, a Build of
`porelo-claude-code` still resolves every other Porelo snapshot (the parent, the BOM, the agent API, the release
tooling): from a second, anonymous snapshot repository that the forge's organization variable
`MAVEN_UPSTREAM_SNAPSHOTS_URL` names. The forge's read-only token reaches only the forge's own registry, and a tag's
build still reads no snapshot repository at all.

## Plan

- **`settings.xml`** gets a third profile, `upstream`, active where `MAVEN_UPSTREAM_SNAPSHOTS_URL` is set and not
  empty. It names the URL as the repository and the plugin repository `porelo-upstream-snapshots`, snapshots only.
  No `<server>` carries that id, so Maven sends it no credential. Beside `forge`, Maven reads the forge's registry
  and this one; where the variable is unset, as on the central forge, the build is unchanged.
- **The workflows** (`build.yml`, `shared-rules.yml`, `update.yml`, `workflows.yml`) pass
  `vars.MAVEN_UPSTREAM_SNAPSHOTS_URL` next to `MAVEN_SNAPSHOTS_URL`. `build.yml` sets `MAVEN_ARGS` to
  `-P!standard,!forge,!upstream` on a tag.
- **`doc/decisions.md`**: *A release is built from releases only* names the three profiles, and an accepted risk
  records that, for the same snapshot version in both repositories, Maven takes the newer timestamp.
- **Red first:** `UpstreamSnapshotsTest`, self-contained like `SnapshotTokenTest`. It fails on the unchanged tree
  and guards two harms:
  - the forge's token going to a repository other than the forge's, read as: no repository or plugin repository of
    a profile shares an id with a `<server>`, except `porelo-forge`;
  - a tag built from snapshots: every profile of `settings.xml` that enables snapshots is deactivated in the tag's
    `MAVEN_ARGS`.
- **The reading against `security-audit`, `test-quality`, `java-code-review` and `clean-code` (2026.906.3)** found five
  things. Each now has a case in the test's own fixtures that was seen red with its check taken out.
  - A repository reads snapshots by Maven's default when its `snapshots` or `enabled` is left out.
  - An id is read from the element's own child, never from a nested repository's; a profile or server without one
    is Maven's `default`.
  - A mirror in `settings.xml`, a repository in `pom.xml`, a profile in `activeProfiles` and a repository under
    another server's id are each refused.
  - The tag's `MAVEN_ARGS` is read from its own uncommented line, must be one `-P` list and must list exactly the
    snapshot profiles.
  - The comment in `settings.xml` names the three profiles.

## Acceptance

- `UpstreamSnapshotsTest` was seen red with the upstream repository under the id `porelo-forge`, and again with
  `!upstream` taken out of the tag's `MAVEN_ARGS`.
- From an empty local repository, the build resolves `porelo-parent` and the Porelo snapshots with only
  `MAVEN_SNAPSHOTS_URL` (a registry without them) and `MAVEN_UPSTREAM_SNAPSHOTS_URL` set, and fails without the
  second. Each run is measured once.
- With `-P!standard,!forge,!upstream` and both variables set, the snapshot parent does not resolve.

## Measured

- From an empty local repository, `-Pci-tools dependency:resolve dependency:resolve-plugins` with
  `MAVEN_SNAPSHOTS_URL` naming an empty registry and `MAVEN_UPSTREAM_SNAPSHOTS_URL` naming Central's snapshots passes
  in 41 s. The parent, the BOM, `porelo-agent-api`, `porelo-wire`, `porelo-release`, `porelo-machines` and
  `porelo-json` were downloaded from `porelo-upstream-snapshots`. Without the second variable, the parent does not
  resolve from `porelo-forge`. With both variables and `-P!standard,!forge,!upstream`, it does not resolve at all.
- `help:active-profiles` lists `forge` alone with `MAVEN_UPSTREAM_SNAPSHOTS_URL` empty, the way an unset
  organization variable reaches a workflow, and `forge` and `upstream` with it set.
