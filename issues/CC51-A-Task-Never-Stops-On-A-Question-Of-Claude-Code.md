# CC51 — A Task Never Stops On A Question Of Claude Code

```json
{
  "$schema": "issue-1.json",
  "id": "CC51",
  "title": "A Task Never Stops On A Question Of Claude Code",
  "state": "Review",
  "size": "M",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "a task's Claude Code never stops to ask whether to run a command: what its safety checks still question under bypass mode, such as an `rm -rf` on a possibly-empty variable, runs at once, proven by an unattended acceptance scenario"
}
```

**Why this size.** one change to the settings the adapter writes, measured on the pin first, and one acceptance scenario
with the real CLI.

**What must be true.** A task's Claude Code never stops to ask a person whether to run a command. A command one of
its own safety checks questions, such as `rm -rf $DEST/$slug` on a variable that may be empty, runs at once in bypass
mode, attended or not, as every other command does. The container and its egress are the boundary in a task, not
Claude Code's prompts.

## Why

At the pin, 2.1.311, bypass mode does not skip every check. An `rm -rf` on a possibly-empty variable path stopped a
task with "Dangerous rm operation on possibly-empty variable path ... Do you want to proceed?", and the CLI would have
denied it by itself only after about two minutes, "to avoid blocking progress on an unattended session". Rewriting
the one command that hit it is not enough: the agent writes its own commands, and any like it stops the task again.

## Plan

Read in today's `ClaudeSettings` (the settings file with `defaultMode: bypassPermissions`, its deny rules and the
`SessionStart` hook that installs the commit trailer), `claude.yaml`'s `install` block, and Porelo's Containerfile,
which takes the definition's `as_root` lines unchanged into the agent's layer of the task image.

- **The answer, measured below:** the image sets `CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS=1` in its
  environment, by an `ENV` line in `claude.yaml`'s `install.as_root`, so every start of the CLI in a task has it,
  attended, headless and resumed, and the dangerous-rm check is waived in bypass mode. Not in the settings' `env`:
  there it has no effect. The `PreToolUse` hook is not taken: it does not reach past the check.
- **The settings stay free of what clears the waiver:** the binary drops it while a `PermissionRequest` hook is set,
  so `ClaudeSettings` keeps none.
- **Unit tests (red first, before the line and the guard exist):** the packaged definition's `as_root` holds that
  `ENV` line, read through the agent API's own reader; the settings document holds no `PermissionRequest` hook and
  does not carry the variable in its `env`, where it would look set and do nothing. They keep the line from being
  dropped or moved; that the CLI honors it only the scenarios below show.
- **Acceptance scenarios (the real CLI in a real task, which nothing lower can show), on the rented test host, one
  unattended and one attended:** the task is told to run the loop below unchanged, its directories made by the loop
  itself. The scenarios assert the effect - the loop's directories are gone and the file it touches after its `rm`
  exists - and, unattended, that no tool result in the session's `stream-json` carries the check's refusal; attended,
  that the screen never shows the question. Each fails fast on the check's text. Red first: both with the `ENV` line
  taken out of the installed definition. The update job runs these legs on every pin bump, so a CLI that renames or
  drops the variable fails its bump.
- **The decision** goes into `doc/decisions.md`: the exposure (an `rm` whose variable is empty removes what the
  agent may write under `/`: the workspace, its home, and the two mounts it writes, its own mailbox and the vault
  socket's directory; nothing of the host besides), why it stays (the check is no boundary - it reads only the text
  of an `rm` - while the container and its egress are, and the workspace is a clone behind the gate), and what would
  change it (a task that mounts a host path writable beyond its own mailbox).

## Acceptance

- In a task at the pin, a command the rm check questions runs at once, attended and headless, and no question is
  shown.
- The acceptance scenario is green with the change and was seen red without it.
- Every check found in the measurement is listed in this issue with how it is answered.

## Measured on 2.1.311

The binary run directly on a VM with its own home, the settings the adapter writes today, attended in a terminal and
headless (`-p`), with an OpenRouter model:

- `rm -rf $UNSET/name` with the variable unset in the environment runs at once, attended and headless: the check
  questions only a target that can expand to `/`, `/*` or a top-level directory.
- `DEST=$PWD/x; for slug in one two; do mkdir -p $DEST/$slug/y; rm -rf $DEST/$slug; done` stops on the check.
  Attended, the question is on the screen within 14 seconds and the CLI denies it by itself later; headless, it is
  denied at once and the agent reads "Dangerous rm operation detected ... requires explicit approval".
- The same loop written `rm -rf "${DEST:?}/${slug:?}"` runs at once, attended and headless.
- A `PreToolUse` hook for `Bash` answering `allow` is called, and the check still asks attended and still denies
  headless: a hook does not reach past it.
- `CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS=1` in the process environment lets the loop run at once,
  attended and headless, with the adapter's deny rules in place; the CLI then shows a one-line notice at its start
  and waits for nothing. In the settings' `env` it changes nothing: the loop stops as before.

What still asks under bypass mode, read in the binary's table of safety checks: the dangerous-rm check (also covering
`rm` inside `sh -c` scripts and command substitutions) is the only one marked waivable. Four more are marked immune
to bypass mode and not waivable; by their names and the texts beside them they belong to features a task does not
switch on: reads outside the working directories when `permissions.blockReadsOutsideWorkingDirectories` is set,
isolation from peer machines, a restricted mode, and an unfinished check that only a person at the host may answer.
That last reading is inferred from the names and was not measured.

Read where the binary raises them: `restrictedMode` only while a restriction is configured, which the adapter's
settings never set; `isolatePeerMachines` only for messages and files sent to another session; `unfinishedCheck`
only when the CLI's own permission check fails to finish ("its safety checks did not run"), an internal failure no
setting waives and no scenario provokes, so it stays a known exposure.

## Plan reading

Read by another model; taken: the attended scenario, asserting the removal and the session's tool results rather than
the agent's words, the pin bump as the guard of the variable's name, the mailbox in the exposure. Declined, with the
reason:

- A `PermissionRequest` hook, an `ENV` that clears the variable, or a tmux setting that unsets it, written by a
  project, its image or the person starting the task, drops the waiver: that is their own choice, which Porelo does
  not guard against. Written by the agent, it stops only its own task, which its pushed change shows.
- An agent removing its own home or settings breaks only its own task; the key the CLI would then show is the phantom
  token Porelo minted for that task.
- An image built before the change keeps the old environment: not so, the agent's layers are rendered into every
  task's build and a changed line rebuilds them; the fingerprint leaves them out only to tell a project's image apart.
- A waived check lets the agent push or send more: it does not, the waiver touches only the `rm` check, and what
  leaves a task goes through its gate and egress as before.

## Code reading

Read by another model; taken: the unattended scenario also fails on any `permission_denied` record, whatever the
refusal is worded as, so a command rewritten after a refusal does not pass; the attached scenario reads the session's
own record for the refusal instead of a screen that may have moved on. Both seen red on the adapter without the `ENV`
line and green with it.
