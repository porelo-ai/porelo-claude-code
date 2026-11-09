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

## To be checked

- Which checks of 2.1.311 still ask under bypass mode, besides the rm check.
- Whether a `PreToolUse` hook's `allow` reaches past the safety checks, or only past the permission rules.
- Whether the CLI's own deny after about two minutes also happens headless, or a `-p` run waits there for good.
