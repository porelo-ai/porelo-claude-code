# CC51 — A Task Never Stops On A Question Of Claude Code

**Status:** Implementing.
**Size:** M - one change to the settings the adapter writes, measured on the pin first, and one acceptance scenario
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

Read in today's `ClaudeSettings` (the settings file with `defaultMode: bypassPermissions`, the `SessionStart` hook
that installs the commit trailer) and `claude.yaml`'s `sandboxed` key.

- **Measure first, on the pin, in a task on the VM, attended and headless (`-p`):** which checks still ask under
  bypass mode. The rm check is one; the others are read from the pinned binary's texts and each is tried once.
- **The answer, in the order tried:** a `PreToolUse` hook for `Bash` in the settings `ClaudeSettings` writes, which
  answers `allow`; if the safety check still asks after it, the setting the binary names for that check. What is
  taken is the first one measured to let the command run at once. If none lets it run, the check is answered with
  an immediate deny, so the agent sees the refusal and rewrites the command instead of waiting; that is said in the
  channel before it is built.
- **Unit test:** the settings document holds the answer for `Bash`, read from the document and not from a constant
  of the class; if the answer is a command, its output for a `Bash` tool call is asserted.
- **Acceptance scenario (the real CLI, which nothing lower can show):** an unattended task is told to run
  `rm -rf $EMPTY/<marker>` with `EMPTY` unset and a marker directory under the workspace; the scenario asserts the
  effect, that the command ran and the session went on, within one minute, and fails fast when the screen shows the
  question.
- **Red first:** that scenario on today's settings, seen to stop on the question.

## Acceptance

- In a task at the pin, a command the rm check questions runs at once, attended and headless, and no question is
  shown.
- The acceptance scenario is green with the change and was seen red without it.
- Every check found in the measurement is listed in this issue with how it is answered.

## Measured on 2.1.311

With the settings the adapter writes today, the binary run directly with its own home, attended in a terminal and
headless (`-p`):

- `rm -rf $UNSET/name` with the variable unset in the environment runs at once, attended and headless: the check
  questions only a target that can expand to `/`, `/*` or a top-level directory.
- `DEST=$PWD/x; for slug in one two; do mkdir -p $DEST/$slug/y; rm -rf $DEST/$slug; done` stops on the check.
  Attended, the question is on the screen within 14 seconds and the CLI denies it by itself later; headless, it is
  denied at once and the agent reads "Dangerous rm operation detected ... requires explicit approval".
- A `PreToolUse` hook for `Bash` answering `allow` is called, and the check still asks attended and still denies
  headless: a hook does not reach past it.
- The binary marks this check as immune to bypass mode but waivable in it, by an environment variable that names
  itself as even more dangerous than skipping permissions, and it says the variable has no effect while
  permission rules or `PermissionRequest` hooks are set.

## To be checked

- Whether that variable, set in the settings' `env` or in the process environment, lets the loop above run at once,
  attended and headless, with the adapter's own deny rules in place.
- Which other checks of 2.1.311 still ask under bypass mode, besides the rm check.
