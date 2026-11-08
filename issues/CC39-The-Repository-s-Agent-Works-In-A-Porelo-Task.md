# CC39 — The Repository's Agent Works In A Porelo Task

**Status:** Review; blocked by [CC38](README.md#cc38), in Review.
**Size:** S - no code here beyond `porelo.yml`, the move itself done by the procedure; about 2 hours of the agent's
work, plus the operator's attended start of the task.

**What must be true.** `porelo-claude-code`'s agent works in a Porelo task of the project `porelo` on the move's host, reached
in the project's room under the repository's name, and works this issue from start to Review inside that task with
the local forge's build green.

One agent per repository, each move checked before the next, as the project's procedure for an agent's move lays out.

## Plan

Read in today's `porelo.yml`, `settings.xml` and agent definition, porelo's `doc/project-file.md` (*A repository's own
image*) and `doc/running.md` (the agent's layer), and porelo-project's `project.yml`.

- **The agent:** Claude Code, the repository's own CLI, with the operator's subscription brokered from the host's
  vault, as [CC24](README.md#cc24) showed before the first move.
- **The image:** today's `porelo.yml` holds the pinned GraalVM, shfmt and the build tools, and `mvnw` brings
  Maven, so the unit suite and the native packages build in the task as porelo-codex's did in a real task. The agent's
  CLI comes from its package's own layer, not from `porelo.yml`. The acceptance legs do not run in a task (no container
  inside it, no rented machine); they run in the forge's Build ([CC38](README.md#cc38)).
- **What the task reaches while it runs:** Maven Central, Sonatype or the forge's registry, `fuinorg.jfrog.io` for
  the skills, `github.com` for `check-citations`, and the forge's address as a host service. That egress is the
  project's, in porelo-project's `project.yml`; the image, the agent's CLI with it, is built on the host outside the
  task's firewall. The task's build reads Sonatype's snapshots: the forge's registry refuses a task today (the organization is private), and nothing sets
  `MAVEN_SNAPSHOTS_URL` in a task yet, so the `forge` profile stays off there until both are in.
- **The sub-agent reading:** no sub-agent CLI is in the task's image, since a project file names one
  agent per repository and porelo describes no second sign-in inside a task. Until Porelo has a task that reviews, an agent outside the task reads the pull request's diff against the skills, as today's sub-agent reading does, and posts its
  findings in the room.
- **Steps:**
  0. *A dry run on this machine first*, only the part that needs nobody: the task's image built from
     `porelo.yml` with podman, and inside it, as a user other than root on an empty local Maven repository,
     `./mvnw -B -s settings.xml verify` and `./mvnw -B -s settings.xml -Pnative,dist verify` on a fresh clone.
     No task starts here and nothing goes through a gate: the room, the wake and the gate are shown on the
     rented host directly. Passed: the image built in 2 min; `verify` ran 196 tests,
     none failed or skipped, in 44 s; the native packages, `.deb` and `.rpm`, built and checked in 78 s.
  1. *Before, from this machine:* a dry run of the task start on the host names each image key's source as
     `porelo.yml`; the image builds; inside it, `./mvnw -s settings.xml verify` and the native packages pass.
  2. *porelo-project (the coordinator):* `repositories.claude.agent` in `project.yml`, no nickname, since the key
     `claude` gives the room's name, and the upstream switched to the host's forge, as the project's procedure for an agent's move lays out.
  3. *The start:* the operator starts the task attended; inside it the agent takes this issue to Review, its push
     through the gate to `porelo/<task>`, the forge's Build read through the `forgejo` reader.
  4. *After the check:* the session on this machine gives up `porelo-claude-code`;
     its test accounts stay with the repositories not moved yet.
- **Red first:** the move's check from the project's procedure for an agent's move, for
  `porelo-claude-code` before the move, each line failing by name.

## Acceptance

- The task's image is built from `porelo-claude-code`'s `porelo.yml`, with no sub-agent CLI in it.
- The task follows `porelo-claude-code` through the gate; its pushes reach the local Forgejo, and it reads its builds through the
  `forgejo` reader.
- Its build runs on the local forge as on the central one, [CC38](README.md#cc38).
- The room shows it as `Agent Claude`, from the capitalized key `claude` in porelo-project's `project.yml`.
- The build in the task passes on Sonatype's snapshots; it takes them from the local forge's registry only once that registry lets a task read it and the task sets `MAVEN_SNAPSHOTS_URL`, which is not this move's. Claude Code works with the operator's subscription from
  the vault.
- The move's check is red for `porelo-claude-code` before the move and green after it; the agent's session on this machine and its
  test accounts are no longer used, and the way back the project's procedure for an agent's move describes is open.

## Findings from the trial

- **What ran, each as its own step, exit code 0:** the tests tagged `documents` (5 tests, 21 s), `verify` (218 tests, none
  failed or skipped, 11 s) and `-Pnative,dist verify` (the `.deb` and `.rpm` built, 90 s, the bill of materials made),
  on an empty local Maven repository, as a user other than root.
- **The seven skills** came from the skills repository, each checked against its sha256 before unpacking; the harness's
  skills folder did not exist and was created. The context said they are read as files, since the settings deny the
  `Skill` tool.
- **Reached:** Maven Central, Sonatype's snapshots, the skills repository and the host's forge (`git ls-remote` on the
  gate's remote lists `main` and the release tags).
- **Not reached:** `raw.githubusercontent.com`, `github.com` and `api.github.com`, so `check-citations` cannot run in
  a task and is left to the forge's *Shared rules check*.
- **Missing in the context:** the repository has no `origin` remote, only the gate's, so `origin/main` in the shared
  rules' commit check is read as the gate's `main`; no `forgejo` reader was in the task, so the Build is read from the
  operator's relay.
- **The forge's Build and *Shared rules check* both failed** before any check ran: Maven could not resolve the parent
  pom `porelo-parent` 0.2.1-SNAPSHOT from the local forge's registry, where it is absent. The same build passed in the
  task, which reads Sonatype's snapshots, so the cause is not the commit; the parent has to be published to that
  registry before either can go green.
- **The commit trailer** is written by the task's hook from the session's model, and Claude Code's own attribution is
  off, so no `Co-Authored-By` line is made.

## To be checked

- That the project's egress lets the task reach `raw.githubusercontent.com`, which `check-citations` reads.
- That the outside reader can fetch `porelo/<task>` from the host's forge, not only from GitHub.
- The way back, rehearsed once: the task's open work fetched from the host's forge into the checkout here.
- That a revoked sign-in is renewed with `porelo vault login claude --force` and a stop and start of the task: a
  running task keeps the credential it started with.
- That the outside reader may join the room from its harness; otherwise it posts its findings in the channel and the
  operator relays them.
