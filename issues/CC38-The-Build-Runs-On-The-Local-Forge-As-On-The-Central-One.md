# CC38 — The Build Runs On The Local Forge As On The Central One

```json
{
  "$schema": "issue-1.json",
  "id": "CC38",
  "title": "The Build Runs On The Local Forge As On The Central One",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 6,
  "blockedBy": [],
  "covers": "the workflows run 1:1 on the local Forgejo and the central forge: snapshots from `MAVEN_SNAPSHOTS_URL`, publishing switched by `PORELO_PUBLISH`, legs asked by kind, green on a local Forgejo"
}
```

**Why this size.** the workflows and `settings.xml` of this one repository, no contract another repository reads; about
3 hours of work, the first of the three adapters, whose shape porelo-pi and porelo-omp then copy, plus the runs on the
local Forgejo.

**How it stands.** On a local Forgejo the Build, the shared rules and the workflow check run green, and the central forge's side is unchanged; what only a host shows is named below.

**What must be true.** `porelo-claude-code`'s workflows run green on the local Forgejo and on the central forge on the same
commit, with no edit between them: the machines its legs rent are asked for by kind, and the registry and the
provider are variables of the setup.

The machine kinds are porelo-buildtools' (`porelo-machines acceptance --kind amd64`).

## Plan

Read in today's `.github/workflows/`, `settings.xml` and `pom.xml`. The shape is the one porelo-message-sluice's workflows have, the
variables those of porelo-host's `doc/local-forge.md`, *The local registry and publication settings*.

- **What ties the Build to GitHub today:**
  - `settings.xml` reads snapshots from `MAVEN_SNAPSHOTS_URL` through the `forge` profile, and from Sonatype through the
    `standard` profile where that variable is unset; a tag's `-P!standard,!forge` reads none. From there come `porelo-bom`, `porelo-agent-api` and `porelo-wire` (`porelo.version`
    0.5.1-SNAPSHOT), `porelo-acceptance-kit` (`porelo.kit.version` 0.5.1-SNAPSHOT, profile `acceptance`), and `porelo-parent`,
    `porelo-release` and `porelo-machines`.
  - Publish and its index check, in the `build` and the `release` job of `build.yml`, take `vars.JF_URL` and
    `secrets.JF_ACCESS_TOKEN` and `artifactory-smoke.yml`, only where `vars.PORELO_PUBLISH` is `true`.
  - The `proof` job asks GitHub's API for a run that proved the tree (`proven-legs`, `github.token`), only where
    `vars.PORELO_PUBLISH` is `true`.
  - The acceptance legs (`build.yml`'s *Acceptance on …*, `update.yml`'s two legs) pass `PORELO_MACHINES_API` and
    `PORELO_MACHINES_SSH` to `porelo-machines acceptance --kind amd64` and `sweep --mine`. `build.yml`'s `acceptance` job runs
    only where `github.server_url` is GitHub's; on `main` with `vars.PORELO_PUBLISH` `true` it installs the package last
    published to Artifactory, on a tag or elsewhere the one the run built.
  - `delete-runs.yml` calls `gh api`. In `update.yml`, *Open the pull request* pushes with GitHub's extra header and
    runs `gh pr` and `gh api`, *Merge, or say why not* runs `gh pr merge`, and *What else changed in the shipped tree*
    reads the published bill from Artifactory's fixed address. Both workflows run only where `vars.PORELO_PUBLISH` is
    `true`.
- **Where Porelo's snapshots come from on the local forge:** the `forge` profile replaces Sonatype, so the forge's
  registry must hold `porelo`'s Maven snapshots before this Build resolves there, while `porelo` itself moves last.
  `porelo`'s Build on the host's forge deploys them there (`forge-release`, as porelo-parent and
  porelo-buildtools do), its legs not moved yet, so a Porelo change made in a task can be built against before it
  reaches GitHub. Here that concerns `porelo-bom`, `porelo-agent-api`, `porelo-wire` and `porelo-acceptance-kit`.
- **How it is tested:** locally against a Forgejo started in podman, with a Forgejo Actions runner and Forgejo's own
  package registry as `MAVEN_SNAPSHOTS_URL`, and against GitHub by the push. The pattern is the live check of
  porelo-forge-clients' `forgejo` client: the server and the runner images pinned by digest, the runner in the server's
  network, and only what the run created removed, after a failure too. That check uses no registry, so this run adds
  it; the registry starts empty, so the snapshots this Build resolves are deployed there first.
- **Steps:**
  1. `settings.xml` gains the `forge` profile and the `porelo-forge` server as porelo-message-sluice has them
     (activated by `MAVEN_SNAPSHOTS_URL`, the token from `PORELO_PACKAGES_READ_TOKEN`), and `standard` stays off where
     `MAVEN_SNAPSHOTS_URL` is set. A profile's activation takes one property only, so `-Dporelo.release` no longer
     turns the snapshot repositories off: a tag's `MAVEN_ARGS` adds `-P!standard,!forge`. Every Maven call passes
     both values through the step's `env`, since a workflow's `env` cannot read a secret: the `pinned-jdk` action's,
     those in `build.yml`, `update.yml`, `workflows.yml` and `shared-rules.yml`, and the `./mvnw verify` that
     `porelo-machines acceptance` starts on the runner for a leg's suite (its `acceptance` profile resolves
     `porelo-acceptance-kit`).
  2. `vars.PORELO_PUBLISH`, a variable of the organization already in place on the central forge, checked at the
     start to be `true` or `false`, switches what only the central forge does:
     Publish and its index check, the `release` job, `artifactory-smoke.yml`, `delete-runs.yml`, the `proof` job, and
     the whole of `update.yml`, which updates the central forge only. Where it is `false`, the legs install the packages
     this run built (the `packages` artifact, as a tag does today), so a leg never tests what Artifactory held before.
  3. The legs ask `porelo-machines acceptance --kind amd64` with `PORELO_MACHINES_API` and `PORELO_MACHINES_SSH`.
     Today the host's pool answers only to `--pool <file>` with `PORELO_POOL_SSH`, and `sweep --mine` asks the cloud
     provider alone; the job learns where the pool is from the forge's runner (`PORELO_MACHINES_POOL`, as porelo-host
     proposes it), so the same lines take a pool
     account on the forge and rent on GitHub. **On the host's forge the `acceptance` job is off while the
     repository moves:** it runs only where `github.server_url` is GitHub's, until the host's pool lends a
     forge job a machine by kind; [CC40](README.md#cc40) switches it on there.
  4. `porelo-release check-providers` joins the Build's checks in the same commit; the provider's names leave
     `build.md` and the machine type in `claude.yaml`'s comment too.
- **Red first:** the `build.yml` from before these changes on the local Forgejo, which stops at the snapshot resolution or
  at Publish; and `check-providers` on the tree from before them, which named the workflows and `build.md` and the machine type in `claude.yaml`'s comment.
- **Built on the central forge's side:** steps 1, 2 and 4 and step 3's GitHub half; `check-providers` was seen red on
  the tree before and green after, and a tag's `-P!standard,!forge` was measured to resolve no snapshot parent from an
  empty local repository, with `MAVEN_SNAPSHOTS_URL` set and without.
- **The reading against `security-audit`, `clean-code` and `java-code-review` (2026.906.3)** declined three findings:
  the `release` job needs no `acceptance` result in its condition, since a job whose needed job was skipped is
  skipped itself; a missing `PORELO_PUBLISH` that leaves `update.yml` skipped is seen first in the Build, which fails
  on it with every push of content; and `PORELO_PUBLISH` rather than the forge's address switches the jobs that call
  GitHub's API, as the plan and the sluice's workflows have it. Two findings concern the shape every repository on the
  forge shares and are built in [CC41](README.md#cc41): the snapshots were read with the publisher's token, so a branch's
  tests held a token that writes the forge's registry; and a forge without `MAVEN_SNAPSHOTS_URL` fell back to Central's
  snapshots without a word.

## Acceptance

- Seen to fail first: the `build.yml` from before these changes on the local Forgejo.
- The same commit's Build green on the local Forgejo (the `acceptance` job skipped, nothing published) and on GitHub (legs
  rented, published as today).
- The kinds' check finds no provider's name in this repository's workflows.

## Measured on a local Forgejo

Forgejo 17 and its runner 13.2.0, pinned as the host's forge pins them, with the host's job image `act-24.04` as root and
a JDK mounted as `JAVA_HOME_25_X64`; the registry held the snapshots this Build resolves, copied in from a Maven cache.

- **The red runs, each stopping the Build in turn:** Maven's default http blocker refused a `MAVEN_SNAPSHOTS_URL` on
  `http://<host>.localhost` and took the same address on `http://localhost`; the tests of a denied read failed as root,
  which reads through any mode; the image holds no `rpm`, which the package check runs; and `actions/upload-artifact`
  refuses every server but GitHub's.
- **What changed for them:** those tests skip where the account reads through a mode, so they still run on the central
  forge; *The tools the packages are built and read with* installs only what is missing; and the packages are uploaded
  only where `vars.PORELO_PUBLISH` is `true`, since no job on a forge that publishes nothing reads them yet.
- **Green:** the Build, with the `acceptance`, `proof` and `release` jobs skipped, nothing published and every snapshot
  read from the forge's registry.
- **What the central forge sees:** no change. Its image holds the tools, its `PORELO_PUBLISH` is `true`, and its account
  is not root.

## What only a host shows

- The registry through the host's proxy and gate, with `MAVEN_SNAPSHOTS_URL` as the host sets it, and the runner's jobs in
  their own containers rather than in the runner's.
- `porelo`'s, `porelo-parent`'s and `porelo-buildtools`' snapshots deployed there by their own Builds, not copied.
