# CC58 — The Status Screen Shows No Installation Warning In A Task

```json
{
  "$schema": "issue-1.json",
  "id": "CC58",
  "title": "The Status Screen Shows No Installation Warning In A Task",
  "state": "Idea",
  "size": "S",
  "priority": "Low",
  "rank": 1,
  "blockedBy": [],
  "covers": "the pinned CLI placed in a task's image the way its own installer places it, so its status screen reports no installation problem, while it still never updates itself"
}
```

**Why this size.** one artifact target and one link in `claude.yaml`, one key in `ClaudeFirstRun`, their tests and a
look at `/status` in a task; nothing of the update stop changes.

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

## Plan

- `claude.yaml`: in the agent's home, the artifact's target becomes `.local/share/claude/versions/${agent.cli.version}`,
  and `.local/bin/claude` a symlink to it, owned by `agent`, as the installer leaves it.
- `ClaudeFirstRun` writes `"installMethod": "native"` beside `hasCompletedOnboarding`.
- Red first: `ClaudeAgentTest` expects the artifact under the versions directory and the link at the old path, and
  the first-run document `installMethod` native.
- Level: unit tests, then one attended task on a VM whose `/status` shows no installation warning and whose
  `claude --version` is the pinned one.

## To be checked

- Whether an agent definition's `as_root` lines run after its artifacts are placed, so the link can be made there,
  or whether the install schema needs a link of its own.
- That `claude --version`, the command the adapter starts, and the trailer hook still find the CLI through the link.
