# CC38 — The Build Runs On The Local Forge As On The Central One

**Status:** Analyzed; blocked by porelo-buildtools [BT25](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt25)
and [BT24](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt24), porelo-host
[HO12](https://github.com/porelo-ai/porelo-host/blob/main/issues/README.md#ho12) and
[HO27](https://github.com/porelo-ai/porelo-host/blob/main/issues/README.md#ho27), and porelo
[B306](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b306), Porelo's snapshots in the forge's registry.
**Size:** S - the workflows and `settings.xml` of this one repository, no contract another repository reads; about
3 hours of work, the first of the three adapters of wave 7a, whose shape porelo-pi
[PI38](https://github.com/porelo-ai/porelo-pi/blob/main/issues/README.md#pi38) and porelo-omp
[OM35](https://github.com/porelo-ai/porelo-omp/blob/main/issues/README.md#om35) then copy, plus the runs on the host's
forge.

**What must be true.** `porelo-claude-code`'s workflows run green on the local Forgejo and on the central forge on the same
commit, with no edit between them: the machines its legs rent are asked for by kind, and the registry and the
provider are variables of the setup.

From porelo-project [PJ60](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj60), for the move's wave 7a; the kinds come from porelo-buildtools [BT25](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt25).

## Plan

Read in today's `.github/workflows/`, `settings.xml` and `pom.xml`. The shape is porelo-message-sluice
[SL33](https://github.com/porelo-ai/porelo-message-sluice/blob/main/issues/README.md#sl33)'s, the variables those of
porelo-project [PJ60](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj60)'s table.

- **What ties the Build to GitHub today:**
  - `settings.xml` reads snapshots from Sonatype only, through the `standard` profile, active unless
    `-Dporelo.release`. `porelo.version` is the release 0.5.0, from Maven Central; the snapshots are
    `porelo-acceptance-kit` (`porelo.kit.version` 0.5.1-SNAPSHOT, profile `acceptance`), `porelo-parent`,
    `porelo-release` and `porelo-machines`.
  - Publish and its index check, in the `build` and the `release` job of `build.yml`, take `vars.JF_URL` and
    `secrets.JF_ACCESS_TOKEN` unconditionally; so does `artifactory-smoke.yml`.
  - The `proof` job asks GitHub's API for a run that proved the tree (`proven-legs`, `github.token`).
  - The acceptance legs (`build.yml`'s *Acceptance on …*, `update.yml`'s two legs) pass `HETZNER_API` and `HETZNER_SSH`
    to `porelo-machines acceptance` and `sweep --mine`, and on `main` install the package last published to Artifactory,
    not the one the run built.
  - `delete-runs.yml` calls `gh api`. In `update.yml`, *Open the pull request* pushes with GitHub's extra header and
    runs `gh pr` and `gh api`, *Merge, or say why not* runs `gh pr merge`, and *What else changed in the shipped tree*
    reads the published bill from Artifactory's fixed address.
- **Where Porelo's snapshots come from on the local forge:** the `forge` profile replaces Sonatype, so the forge's
  registry must hold `porelo`'s Maven snapshots before this Build resolves there, while `porelo` itself moves last.
  `porelo`'s Build on the host's forge deploys them there (porelo
  [B306](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b306), `forge-release`, as porelo-parent
  and porelo-buildtools do), its legs not moved yet, so a Porelo change made in a task can be built against before it
  reaches GitHub. Here only `porelo-acceptance-kit` is concerned, as `porelo.version` is a release.
- **Steps:**
  1. `settings.xml` gains the `forge` profile and the `porelo-forge` server as porelo-message-sluice has them
     (activated by `MAVEN_SNAPSHOTS_URL`, the token from `PORELO_PUBLISHER_TOKEN`), and `standard` stays off where
     `MAVEN_SNAPSHOTS_URL` is set, while a tag's `-Dporelo.release` still turns every snapshot repository off. Every
     Maven call passes both values, the `pinned-jdk` action's too, in `build.yml`, `update.yml`, `workflows.yml` and
     `shared-rules.yml`, through the step's `env`, since a workflow's `env` cannot read a secret.
  2. `vars.PORELO_PUBLISH`, checked at the start to be `true` or `false`, switches what only the central forge does:
     Publish and its index check, the `release` job, `artifactory-smoke.yml`, `delete-runs.yml`, the `proof` job, and
     the whole of `update.yml`, which updates the central forge only. Where it is `false`, the legs install the packages
     this run built (the `packages` artifact, as a tag does today), so a leg never tests what Artifactory held before.
  3. The legs ask `porelo-machines acceptance --kind amd64` with `PORELO_MACHINES_API` and `PORELO_MACHINES_SSH`.
     Today the host's pool answers only to `--pool <file>` with `PORELO_POOL_SSH`, and `sweep --mine` asks the cloud
     provider alone; the job learns where the pool is from the forge's runner (porelo-host [HO27](https://github.com/porelo-ai/porelo-host/blob/main/issues/README.md#ho27)), so the same lines take a pool
     account on the forge and rent on GitHub.
  4. `porelo-release check-providers` joins the Build's checks in the same commit; the provider's names leave
     `build.md` and the machine type in `claude.yaml`'s comment too.
- **Red first:** today's `build.yml` on the host's Forgejo, which stops at the snapshot resolution or at Publish; and
  `check-providers` on today's tree, which names the workflows and `build.md` and the machine type in `claude.yaml`'s comment.

## Acceptance

- Seen to fail first: today's `build.yml` on the local Forgejo.
- The same commit's Build green on the local Forgejo (legs on the host's pool, nothing published) and on GitHub (legs
  rented, published as today).
- The kinds' check finds no provider's name in this repository's workflows.

## To be checked

- That the host's Forgejo runner serves `actions/upload-artifact` v7, `actions/download-artifact` v8 and
  `actions/cache` v6, and that the `pinned-jdk` action (`JAVA_HOME_25_X64`, `porelo-machines jdk --github`) works there.
- That the host's VM pool is larger than none: the rented server of the 6e round reported a pool of 0 KiB.
- That a pool VM reaches the package and CLI download hosts a leg installs from.
- That `OPEN_ROUTER_API_KEY` is set on the forge, verified by the operator, as the shared rule on separate access
  lists asks.
