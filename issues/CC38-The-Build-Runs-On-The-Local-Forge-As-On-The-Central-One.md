# CC38 — The Build Runs On The Local Forge As On The Central One

**Status:** Backlog; the GitHub side is built, the Forgejo half waits for a host and for Porelo's and the build
tools' snapshots in the forge's registry, and moves to Implementing when it starts.
**Size:** S - the workflows and `settings.xml` of this one repository, no contract another repository reads; about
3 hours of work, the first of the three adapters, whose shape porelo-pi and porelo-omp then copy, plus the
runs on the host's forge.

**What must be true.** `porelo-claude-code`'s workflows run green on the local Forgejo and on the central forge on the same
commit, with no edit between them: the machines its legs rent are asked for by kind, and the registry and the
provider are variables of the setup.

The machine kinds are porelo-buildtools' (`porelo-machines acceptance --kind amd64`).

## Plan

Read in today's `.github/workflows/`, `settings.xml` and `pom.xml`. The shape is the one porelo-message-sluice's workflows have, the
variables those of porelo-host's `doc/local-forge.md`, *The local registry and publication settings*.

- **What ties the Build to GitHub today:**
  - `settings.xml` reads snapshots from Sonatype only, through the `standard` profile, active unless
    `-Dporelo.release`. `porelo.version` is the release 0.5.0, from Maven Central; the snapshots are
    `porelo-acceptance-kit` (`porelo.kit.version` 0.5.1-SNAPSHOT, profile `acceptance`), `porelo-parent`,
    `porelo-release` and `porelo-machines`.
  - Publish and its index check, in the `build` and the `release` job of `build.yml`, take `vars.JF_URL` and
    `secrets.JF_ACCESS_TOKEN` unconditionally; so does `artifactory-smoke.yml`.
  - The `proof` job asks GitHub's API for a run that proved the tree (`proven-legs`, `github.token`).
  - The acceptance legs (`build.yml`'s *Acceptance on …*, `update.yml`'s two legs) pass the cloud provider's token and key
    secrets to `porelo-machines acceptance` and `sweep --mine`, and on `main` install the package last published to Artifactory,
    not the one the run built.
  - `delete-runs.yml` calls `gh api`. In `update.yml`, *Open the pull request* pushes with GitHub's extra header and
    runs `gh pr` and `gh api`, *Merge, or say why not* runs `gh pr merge`, and *What else changed in the shipped tree*
    reads the published bill from Artifactory's fixed address.
- **Where Porelo's snapshots come from on the local forge:** the `forge` profile replaces Sonatype, so the forge's
  registry must hold `porelo`'s Maven snapshots before this Build resolves there, while `porelo` itself moves last.
  `porelo`'s Build on the host's forge deploys them there (`forge-release`, as porelo-parent and
  porelo-buildtools do), its legs not moved yet, so a Porelo change made in a task can be built against before it
  reaches GitHub. Here only `porelo-acceptance-kit` is concerned, as `porelo.version` is a release.
- **Steps:**
  1. `settings.xml` gains the `forge` profile and the `porelo-forge` server as porelo-message-sluice has them
     (activated by `MAVEN_SNAPSHOTS_URL`, the token from `PORELO_PUBLISHER_TOKEN`), and `standard` stays off where
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
- **Red first:** today's `build.yml` on the host's Forgejo, which stops at the snapshot resolution or at Publish; and
  `check-providers` on today's tree, which names the workflows and `build.md` and the machine type in `claude.yaml`'s comment.
- **Built on the central forge's side:** steps 1, 2 and 4 and step 3's GitHub half; `check-providers` was seen red on
  the tree before and green after, and a tag's `-P!standard,!forge` was measured to resolve no snapshot parent from an
  empty local repository, with `MAVEN_SNAPSHOTS_URL` set and without.
- **The reading against `security-audit`, `clean-code` and `java-code-review` (2026.906.3)** declined three findings:
  the `release` job needs no `acceptance` result in its condition, since a job whose needed job was skipped is
  skipped itself; a missing `PORELO_PUBLISH` that leaves `update.yml` skipped is seen first in the Build, which fails
  on it with every push of content; and `PORELO_PUBLISH` rather than the forge's address switches the jobs that call
  GitHub's API, as the plan and the sluice's workflows have it. Two findings concern the shape every repository on the
  forge shares and wait on it: the snapshots are read with the publisher's token, so a branch's tests hold a token that
  writes the forge's registry; and a forge without `MAVEN_SNAPSHOTS_URL` falls back to Central's snapshots without a
  word.

## Acceptance

- Seen to fail first: today's `build.yml` on the local Forgejo.
- The same commit's Build green on the local Forgejo (the `acceptance` job skipped, nothing published) and on GitHub (legs
  rented, published as today).
- The kinds' check finds no provider's name in this repository's workflows.

## To be checked

- That the host's Forgejo runner serves `actions/upload-artifact` v7, `actions/download-artifact` v8 and
  `actions/cache` v6, and that the `pinned-jdk` action (`JAVA_HOME_25_X64`, `porelo-machines jdk --github`) works there.
- That the runner reaches GitHub where the checks still read it: `check-citations` fetches the other repositories'
  indexes from `raw.githubusercontent.com`, so a cited issue pushed only to the host's forge fails it, and
  `check-pin` asks `api.github.com` without a token, inside the limit of 60 requests an hour per address.
- That the runner has `python3` with `yaml` for `workflows.yml`'s load check.
