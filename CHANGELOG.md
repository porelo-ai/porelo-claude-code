# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Claude Code's version is what the package installs and
appears as an entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Changed

- The adapter is renamed with the product it serves: it is `org.fuin.porelo:porelo-agent-claude`, its packages `porelo-agent-claude` depend on `porelo`, provide `porelo-agent` and install into `/usr/libexec/porelo/agents`, and its repository is `porelo-ai/porelo-claude-code`.
- Every workflow's token reads only at its top; only the update job may push the pin's branch and open its pull request, and only the job that deletes old runs may delete them.
- The unattended broker scenario asks again when the model ends its turn with no text and the CLI exits 0, twice at most and said each time; a failing run is never asked again.
- A tag's build takes nothing from Central's snapshots and asks for nothing again, and a tag on a Porelo snapshot is seen refused by the channel step itself, run in a test.
- A model Claude Code's catalog does not describe, such as another vendor's over OpenRouter, keeps the window its provider gives it: no 200k-token cap and no notice at every start.
- The wake scenarios read the model's answer rather than the typed line, and wait for the agent at rest, or for its tool at work, instead of fixed pauses.
- Built against the snapshots of Porelo (0.5.0-SNAPSHOT) and `porelo-parent` (0.2.0-SNAPSHOT, with the release tooling 0.5.0-SNAPSHOT); a release moves them to released versions first.
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
