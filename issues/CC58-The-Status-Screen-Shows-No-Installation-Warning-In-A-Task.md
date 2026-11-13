# CC58 — The Status Screen Shows No Installation Warning In A Task

```json
{
  "$schema": "issue-1.json",
  "id": "CC58",
  "title": "The Status Screen Shows No Installation Warning In A Task",
  "state": "Implementing",
  "size": "S",
  "priority": "Low",
  "rank": 1,
  "blockedBy": [],
  "covers": "the pinned CLI placed in a task's image the way its own installer places it, so its status screen reports no installation problem, while it still never updates itself",
  "openQuestions": 0
}
```

**Why this size.** one artifact target and one `as_agent` line in `claude.yaml`, one key in `ClaudeFirstRun`, their
tests and a look at `/status` in a task; the install schema of porelo core already runs an agent's `as_agent` lines
after its artifacts, so nothing outside this module changes, and nothing of the update stop changes.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=167).

**What must be true.** In a task, `/status` under "System diagnostics" shows neither of the two warnings seen on
2.1.311:

- `<the agent's home>/.local/bin/claude was not created by the native installer (it is not a symlink into the
  versions/ directory), so auto-update leaves it untouched.`
- `Running native installation but config install method is 'not set'`

The pinned version still runs and still never updates itself: `DISABLE_UPDATES` in the settings and the refused
`downloads.claude.ai` stay as they are.

## Analysis

Read from the pinned 2.1.311 binary, not from documentation:

- The first check runs for every native installation and has no switch: it passes only when `.local/bin/claude` in the
  agent's home is a symlink into the versions directory (`$XDG_DATA_HOME/claude/versions`, by default
  `.local/share/claude/versions` there). `claude.yaml` writes the binary itself to `.local/bin/claude`.
- The second check compares the running kind (native) with `installMethod` in the agent's `.claude.json`, which
  `ClaudeFirstRun` writes without that key. `DISABLE_INSTALLATION_CHECKS=1` would hide this one, but not the first,
  and also hides checks that could matter (PATH).
- `autoUpdates: false` and `DISABLE_AUTOUPDATER` change neither check; updates are already stopped by
  `DISABLE_UPDATES`.

Read in porelo core, where the image is built from an agent definition's `install:` section:

- `as_root` cannot make the link: `Containerfile` writes the `as_root` lines before `USER agent`, and the artifacts
  are fetched after it, in the agent's part of the image (`Agent#imageLayer` puts `InstallScript.render(artifacts)`
  first in `asAgent`). So an `as_root` line runs before the binary exists.
- `as_agent` can: `Agent#imageLayer` appends the definition's `as_agent` lines after the artifacts' fetch, and they
  run as `agent`, so the link is owned by `agent` as the installer leaves it. `porelo-codex` already places its
  binaries this way. The install schema needs no link of its own; porelo core does not change.
- An artifact is placed with `install -D -m <mode>`, which creates the versions directory as `agent`.
- `${agent.cli.version}` is filled in by Maven resource filtering of `claude.yaml`, so it is filled in an `as_agent`
  line as in the artifact's target.

## Plan

- `src/main/resources/agent/claude.yaml`, `install:`: in the agent's home, the artifact's `target` becomes
  `.local/share/claude/versions/${agent.cli.version}`, and a new `as_agent` line creates `.local/bin` and links
  `.local/bin/claude` to that file (`mkdir -p`, `ln -s`), with a comment that says why the installer's layout is kept
  (the two checks above). `binary: claude` stays: it is found on `PATH`, which holds `.local/bin`.
- `ClaudeFirstRun.document` writes `"installMethod": "native"` beside `hasCompletedOnboarding`, with a comment naming
  the check it answers.
- Red first: `ClaudeAgentTest.installsAPinnedAndVerifiedBinary` expects the target under the versions directory,
  ending in the version; a new test expects in `imageLayer().asAgent()` the `ln -s` line after the `install -D` line,
  from the versions path to `.local/bin/claude`;
  `ClaudeContainerSetupTest` expects `installMethod` = `native` in the first-run document.
- Level: unit tests, then one attended task on a VM whose `/status` shows no installation warning and whose
  `claude --version` is the pinned one.
- The acceptance features that call `.local/bin/claude` in the agent's home keep working through the link; the commit-trailer hook
  calls no CLI, it reads the session files only.

## Acceptance

- The unit tests above pass and fail with the old target, without the link line, or without `installMethod`.
- In an attended task on a VM: `/status` shows neither warning; `readlink` of the agent's `.local/bin/claude` names
  the versions directory; `claude --version` prints the pinned version; an unattended run of the acceptance suite
  passes, so the adapter's start command finds the CLI through the link.
- The update stop is unchanged: `DISABLE_UPDATES` is still in the settings and `downloads.claude.ai` is still refused.

## What makes it uncertain

- The CLI may judge "installed by the native installer" by more than the link (a lock file, a file in the versions
  directory, the link's exact target form). If `/status` still warns, the size grows to what the binary reads,
  measured again on the pinned version.
- A later pin may add or change these checks; the comment in `claude.yaml` names the version they were read on.
- With `installMethod` native, the CLI may try to tidy the versions directory or reach the update host on start; the
  refused host and `DISABLE_UPDATES` should keep that quiet, and the attended task shows whether it complains.
