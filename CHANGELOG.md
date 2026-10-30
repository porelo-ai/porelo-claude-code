# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Claude Code's version is what the package installs and
appears as an entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Added

- The definition names how its sign-in is answered, `login.flow: paste-code`, so a person signing in on a host over SSH can be told to paste the code; the Porelo it builds against reads the key, and its agent listing names the flow.

### Changed

- Claude Code pinned to 2.1.311 (was 2.1.267).
- A task's agent keeps no memory of its own: Claude Code's auto memory is off, so what holds across tasks is only what the repository's `AGENTS.md` says.
- A task's agent no longer refuses `raw.githubusercontent.com`; it resolves where the project grants it, so `check-citations` runs in a Claude Code task.
- The code is in the shared style, google-java-format in its AOSP variant, with YAML and Gherkin at four spaces, and `validate` refuses code that is not; `.git-blame-ignore-revs` lets `git blame` skip the commit that reformatted it.
- No checkout leaves the job's token in `.git/config`; the weekly update's push alone gets it, from its own environment in that one step.
- The weekly update neither calls its pull request green nor merges it unless every step before succeeded, so a step ended by its time limit or a cancel stops it.
- A release is not uploaded when the release index cannot be read, and the weekly update fails when deleting its rented machines fails.
- The Build passes the event and the ref into its shell through the environment, so a branch's name is never run as a command.
- An acceptance scenario holds a subscription's refresh token out of its task: kept through `vault login --credentials-from` against a stand-in token endpoint, renewed once by the host, and found in neither the task's environment, its files nor its logs.
- The acceptance suite allows the agent 120 seconds to reach work and an unattended run 120 seconds to end, about four times what the adapters' rented test machine measured, so a run stuck at a question fails in minutes, not half an hour.
- Built on the snapshot of `porelo-parent` (0.2.1-SNAPSHOT, with the release tooling 0.5.1-SNAPSHOT), against Porelo 0.5.1-SNAPSHOT.

### Fixed

- A task's agent no longer stops at the question Claude Code 2.1.292 asks at its first start, whether to make auto mode the default; the adapter answers it as seen and the task keeps bypass mode.
- The log and the error a run ends with carry no terminal control characters but the line break and the tab, so nothing the model writes can clear or rewrite the screen that shows it.
- A failed run whose result line gives no reason ends with an error saying so, never with "success" or "null", and what it said is cut at 300 characters without splitting one.

## [0.5.0] - 2026-10-17

### Changed

- The adapter is renamed with the product it serves: it is `org.fuin.porelo:porelo-agent-claude`, its packages `porelo-agent-claude` depend on `porelo`, provide `porelo-agent` and install into `/usr/libexec/porelo/agents`, and its repository is `porelo-ai/porelo-claude-code`.
- Every workflow's token reads only at its top; only the update job may push the pin's branch and open its pull request, and only the job that deletes old runs may delete them.
- The unattended broker scenario asks again when the model ends its turn with no text and the CLI exits 0, twice at most and said each time; a failing run is never asked again.
- A tag's build takes nothing from Central's snapshots and asks for nothing again, and a tag on a Porelo snapshot is seen refused by the channel step itself, run in a test.
- A model Claude Code's catalog does not describe, such as another vendor's over OpenRouter, keeps the window its provider gives it: no 200k-token cap and no notice at every start.
- The wake scenarios read the model's answer rather than the typed line, and wait for the agent at rest, or for its tool at work, instead of fixed pauses.
- Built against Porelo 0.5.0 and on `porelo-parent` 0.2.0, which brings the release tooling 0.5.0.
- Every workflow run is titled by its workflow, the branch or tag and the commit; the shared rules run as `Shared rules check`, the weekly pin move as `Agent version update`.

## [0.4.1] - 2026-10-08

### Added

- The build reads the version from the `.deb` and `.rpm` it made and fails unless it is the project's, as the packages map it.

### Changed

- Built against Sokar 0.4.1 and on `sokar-parent` 0.1.2, which brings the release tooling 0.4.3.
- NullAway's compiler configuration and the plugin versions come from `sokar-parent`; `exec-maven-plugin` moves to 3.6.3.
- The build takes `org.fuin.sokar:sokar-parent` as its parent instead of `org.fuin:pom`; the packages are unchanged.
- The `shared-rules` workflow also runs `sokar-release check-readmes`.
- A release tag is refused while anything the build resolves is a snapshot, checked by `sokar-release` 0.4.2.
- A push that changes only documents - Markdown, `mkdocs.yml`, `doc/` or `issues/` - no longer starts the build.
- The release tooling is `sokar-release` 0.4.1, whose shared checks of issue citations and the documentation chapter replace this repository's own tests.

## [0.4.0] - 2026-10-05

- Initial public version.
