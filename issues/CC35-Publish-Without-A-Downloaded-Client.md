# CC35 — Publish Without A Downloaded Client

**Status:** Analyzed.
**Size:** S - the two Publish steps of `build.yml` and the smoke workflow move from the JFrog CLI to porelo's
`artifactory.sh`; the packages, their channels and the index read-back stay as they are.

**What must be true.** The Build publishes this repository's packages with no client downloaded beside the
Artifactory token: no `jfrog/setup-jfrog-cli` and no `jf` command is left under `.github/`, and every package is
still found in its index after a Publish on `main`.

## Why

[B49](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b49) moved porelo's Publish to
`.github/scripts/artifactory.sh`: curl, with the token in a curl config file of mode 600 that neither `set -x` nor
`/proc/<pid>/cmdline` shows. Its first Publish through it was green on 2026-10-18. This repository's `build.yml` still
installs the JFrog CLI in both Publish steps and calls `jf rt` ten times: the uploads of the .deb, the .rpm and the
bill, and the index read-back. A downloaded client is one more thing that runs beside the token.

## Plan

- **`.github/scripts/artifactory.sh`**, taken from porelo byte for byte (`33014f05`), sourced, not run.
- **Both Publish steps** (the snapshot's and the release's):
  - `jfrog/setup-jfrog-cli` goes, and `art_login` reads `JF_ACCESS_TOKEN` as before;
  - each `jf rt upload` becomes `art_put`, the .deb with `deb.distribution`, `deb.component` and
    `deb.architecture`, so it is indexed;
  - the read-back of the Debian index and the RPM metadata goes through `art_api GET`;
  - the refusal of a release already in the index stays.
- **`artifactory-smoke.yml`**, dispatched by hand, runs the same checks through the script: ping, both
  repositories and their types, and write, annotate and delete, as porelo's smoke workflow does.
- **Red first**, against a stand-in HTTP server that records each PUT and checks its bearer header and both
  checksum headers, the step blocks taken out of `build.yml` and run as the runner runs them, with no `jf` on `PATH`:
  today's blocks fail; the new ones upload every file with the right properties; a wrong token stops at the first
  upload; no token stops before any.
- No changelog line: only `.github/` changes, and nothing that ships.

## Acceptance

- No `jfrog/` action and no `jf` command under `.github/`; check-actions green.
- The first Publish on `main` after it is green, and its read-back finds every package in the index.
- `artifactory-smoke.yml`, started by hand, is green through the script.
- The `bash -x` trace of a Publish holds no token.
