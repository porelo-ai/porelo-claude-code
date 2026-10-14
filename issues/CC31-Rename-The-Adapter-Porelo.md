# CC31 — Rename The Adapter Porelo

**Status:** Analyzed; blocked by sokar-project [PJ41](https://github.com/sokar-ai/sokar-project/blob/main/issues/README.md#pj41)'s
preparation and by Porelo's `0.5.0` snapshot.
**Size:** S - one commit made by a script and read through; 730 occurrences in one module, and the build,
the native build and the acceptance as for any change to the adapter.

**What must be true.** The Claude Code adapter, its repository, its artifact, its packages and its documentation are
named Porelo; nothing a user installs, types or reads from this repository still says Sokar, except where it says
what the old name was.

From [PJ41](https://github.com/sokar-ai/sokar-project/blob/main/issues/README.md#pj41), where Sokar is renamed
Porelo in one window, every repository by a script its agent runs.

## What carries the name

Counted on `main`, case-insensitive: **730 occurrences in 54 of 65 files, 34 paths**, no binary
file, and no image that draws the name.

- **Paths:** the package directories `org/fuin/sokar/agent/impl/...` under `src/main`, `src/test` and
  `src/acceptance`, and `META-INF/services/org.fuin.sokar.agent.api.Agent`, all moved by `git mv`. The issue files `CC22` and `CC23` carry the name in their file names and move with the rest.
- **Read by others:** the artifact and native executable `sokar-agent-claude`; the packages it
  builds, which `Depends: sokar` and `Provides: sokar-agent` and install into `/usr/libexec/sokar/agents`, where the
  daemon looks; the service file naming Sokar's `Agent` interface; the guide written into every task, which names
  `/sokar/files`. Each follows Sokar's own rename, so they move with it and nothing is kept for the old name.
- **Read from others:** `org.fuin.sokar` (100 times) - the BOM, `sokar-agent-api`, `sokar-wire`, the acceptance
  kit, the parent, `sokar-release` and `sokar-machines` called by name in the workflows; the properties
  `-Dsokar.release` and `-Dsokar.acceptance.*`; the variables `SOKAR_CI_LEG`, `SOKAR_E2E_EXPECT_CLI`, `SOKAR_E2E_MODEL`, `SOKAR_E2E_OPENROUTER_API_KEY`, `SOKAR_E2E_REFUSED_KEY`, `SOKAR_VAULT`, of which `SOKAR_VAULT` is Sokar's own
  and the CI leg and most of the `SOKAR_E2E_*` set are read by `sokar-machines` and Sokar's acceptance kit.
- **At the forge and Artifactory:** the secret `SOKAR_UPDATE_TOKEN` and the variable `SOKAR_UPDATE_AUTO_MERGE`, made
  again as `PORELO_*` in [PJ41](https://github.com/sokar-ai/sokar-project/blob/main/issues/README.md#pj41)'s preparation; the repositories `sokar-dist-deb` and `sokar-dist-rpm` the workflows
  upload into, and the links to `github.com/sokar-ai/...`.

## Plan

- **Red first:** a test tagged `documents` that lists every tracked file and fails while one says `sokar` in any case
  outside the exceptions below, seen red on today's tree before the script runs, and kept.
- **The script** (`git mv`, then `sokar`/`Sokar`/`SOKAR` to `porelo`/`Porelo`/`PORELO`, then the exceptions put back),
  run and its diff read, then the one thing replacement gets wrong:
  - **the Debian pool's letter:** `pool/main/s/sokar-agent-...` becomes `pool/main/p/porelo-agent-...` in the
    7 places of the workflows that upload to or look into the pool, since the letter is the package's first.
- **Exceptions, the whole list:** the released entries in `CHANGELOG.md` (every section below `[Unreleased]`), which
  stay as released; `0.5.0`'s first entry says the adapter was renamed. Nothing else: this repository names nothing
  outside that keeps the old name.
- **Lengths:** `porelo` is one longer; the test tokens `sokar_pt_...` become `porelo_pt_...`, and the tests that
  assert on them are read, not regenerated.
- **Checked as any change to the adapter:** unit tests, the native build with its linkage check, the packages, and the
  acceptance on the VM against Porelo installed afresh; then the whole workflow on the renamed repository.

## Acceptance

- The red-first test fails on today's tree and passes on the renamed one, with exactly the exceptions above.
- The packages are named `porelo-agent-...`, install into Porelo's agent directory and are found by `porelo agents`;
  the acceptance passes on the VM against Porelo installed afresh.
- The renamed repository's Build uploads into `porelo-dist-deb` and `porelo-dist-rpm`, at the pool's letter `p`.

## To be checked

- With Core, that Sokar's renamed contract is what replacement makes of today's: the interface
  `org.fuin.porelo.agent.api.Agent`, `/usr/libexec/porelo/agents`, `/porelo/files`, the token prefix `porelo_pt_`,
  the variables `PORELO_CI_LEG` and `PORELO_E2E_*`, the properties `porelo.release` and `porelo.acceptance.*`.
- What must be published before this builds: `porelo-parent` `0.2.0`, `porelo-release` and `porelo-machines`, and
  Porelo's `0.5.0` snapshot with its packages for the VM.
