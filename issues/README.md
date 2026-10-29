# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Implementing

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc41"></a>[CC41](CC41-Snapshots-Resolved-With-A-Read-Only-Token.md) | S | — | on a forge other than the central one, snapshots resolved with the runner's read-only token, the publisher's token only where `main` deploys, and a Build without `MAVEN_SNAPSHOTS_URL` refused at its start |

## Backlog

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc38"></a>[CC38](CC38-The-Build-Runs-On-The-Local-Forge-As-On-The-Central-One.md) | S | — | the workflows run 1:1 on the local Forgejo and the central forge: snapshots from `MAVEN_SNAPSHOTS_URL`, publishing switched by `PORELO_PUBLISH`, legs asked by kind; the GitHub side built, the Forgejo half once a host exists |
| <a id="cc39"></a>[CC39](CC39-The-Repository-s-Agent-Works-In-A-Porelo-Task.md) | S | [CC38](#cc38) | the repository's agent moves into its own Porelo task, after a dry run on this machine |

## Analyzed

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cc36"></a>[CC36](CC36-Report-What-Claude-Code-Finds-Of-Itself-On-The-Host.md) | S | — | Claude Code's sign-in on the host reported by kind, provider and path, never by value | 0 |

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc40"></a>[CC40](CC40-The-Acceptance-Legs-Run-On-The-Host-s-Forge.md) |  | [CC38](#cc38) | the acceptance legs on the host's pool again, once it lends a forge job a machine by kind |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc23"></a>[CC23](CC23-Sign-The-RPMs-With-Porelos.md) | S | — | package-level signatures on the published RPMs |
| <a id="cc22"></a>[CC22](CC22-Depend-On-The-Agent-Protocol-Porelo-Provides.md) | S | — | the agent contract named at install time |

## Finished

| # | Title | What was built |
|---|---|---|
| <a id="cc34"></a>[CC34](README.md#cc34) | Take The Legs From A Run That Proved The Same Tree | A Build on `main` asks `porelo-release proven-legs` whether a Build or an update run already proved the pushed tree on both legs within a day and takes those legs instead of renting again, while a tag always runs its own. |
| <a id="cc37"></a>[CC37](README.md#cc37) | Let The Project Grant What Its Build Needs | A task's Claude Code no longer refuses `raw.githubusercontent.com`, so it resolves where the project grants it and `check-citations` runs there, while the Datadog log intake and `downloads.claude.ai` stay refused. |
| <a id="cc35"></a>[CC35](README.md#cc35) | Publish Without A Downloaded Client | The Build and the smoke workflow talk to Artifactory with `curl` through `.github/scripts/artifactory.sh`, with the JFrog CLI and its setup action removed. |
| <a id="cc24"></a>[CC24](README.md#cc24) | Prove OAuth Isolation End To End | An acceptance scenario signs in with a subscription against a stand-in token endpoint and shows the refresh token never reaches the task: only the host renews, and the broker refuses a renewal from inside it. |
| <a id="cc30"></a>[CC30](README.md#cc30) | Switch Off The Agent's Own Memory In A Task | A task's Claude Code keeps no memory of its own: auto memory is off in the settings the adapter writes, so what holds across tasks is only the repository's `AGENTS.md`. |
| <a id="cc32"></a>[CC32](README.md#cc32) | Answer The Auto Mode Question At First Run | A task on Claude Code 2.1.292 or later reaches work without being asked whether to make auto mode the default: the adapter marks the question seen and the task keeps bypass mode. |
| <a id="cc33"></a>[CC33](README.md#cc33) | Reformat To The Shared Style | The code is in the shared style, google-java-format in its AOSP variant with YAML and Gherkin at four spaces; `validate` refuses code that is not, and `git blame` with `.git-blame-ignore-revs` skips the reformatting. |
| <a id="cc29"></a>[CC29](README.md#cc29) | Read The Code Changed Since The Last Skills Review | The code changed since 2026-09-27 read against seven skills, each finding fixed, with a test where it could do harm: no terminal control character reaches a log, and no checkout keeps the job's token. |
| <a id="cc31"></a>[CC31](README.md#cc31) | Rename The Adapter Porelo | The repository, its artifact, its packages, its Java packages and its documents say Porelo; a documents test keeps the old name to the released part of `CHANGELOG.md`. |

## Where an agent's requirements live

An agent's work lives in its own repository, so a requirement for this agent is an issue here, and
what outlived the ones already met is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `porelo-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
