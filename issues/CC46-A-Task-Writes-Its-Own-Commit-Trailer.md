# CC46 — A Task Writes Its Own Commit Trailer

**Status:** Implementing.
**Size:** M - the settings file, a git hook and its git configuration written into the container, a measurement at the
pin; no contract of another repository changes.

**What must be true.** A commit Claude Code makes in a Porelo task ends with `Assisted-by: Claude Code, <model>`, the
model that actually served the session, and carries none of Claude Code's own attribution, no `Co-Authored-By:` line
and no generated-with note in a pull request. This holds without the task's first words having to overrule
Claude's default.

## Why

In a moved task nothing set the trailer: Claude Code's own attribution asked for `Co-Authored-By: Claude …`, which the
shared rules forbid, and only a line in the task's first words, followed by hand, gave `Assisted-by:`. A line the first
words forget is a commit the forge's checks refuse after the push.

## Plan

Read in today's `ClaudeSettings`, `ClaudeContainerSetup` and the agent API's `ContainerFile` and `SetupContext`, and
measured on the pinned 2.1.311:

- **Nobody outside the session knows the model.** `SetupContext` names none, Porelo's run request may leave it to the
  agent's default, and `attribution.commit` is taken literally, with no model filled in. A `SessionStart` hook's input
  named no model in a `-p` run. The session's transcript does: every assistant record carries `"model"`, the one that
  served it, so it follows a `/model` change too.
- **Claude's own attribution off.** `ClaudeSettings` writes `"attribution": {"commit": "", "pr": ""}`; the CLI's
  reminder then says to add no attribution lines.
- **A `commit-msg` hook writes the trailer.** `ClaudeContainerSetup` writes a POSIX `sh` hook and a git configuration
  as git's XDG file (`$XDG_CONFIG_HOME/git/config`, read beside the global one) whose `core.hooksPath` names the hook's
  folder. The hook, given the message file:
  - drops a `Co-Authored-By:` line naming `noreply@anthropic.com`;
  - keeps a message that already has an `Assisted-by:` trailer;
  - else reads the last `"model"` of the session's transcript (`CLAUDE_CODE_SESSION_ID`'s file, else the newest
    transcript), skipping `<synthetic>`, and adds `Assisted-by: Claude Code, <model>` with `git interpret-trailers`;
  - refuses the commit, saying why, when it finds no model: a commit without its trailer is not made.
- **The executable bit.** `ContainerFile` writes no mode, and git ignores a hook that is not executable, so a
  `SessionStart` hook in the settings runs `chmod +x` on it before the first turn.
- **Red first:** `ClaudeContainerSetupTest` asks for the attribution keys, the git configuration and the hook; a test
  runs the written hook with `sh` and git (`GIT_CONFIG_GLOBAL=/dev/null`) on a message with a `Co-Authored-By:` line and
  a stand-in transcript, and on one with no transcript.

## Acceptance

- In a task on the VM, a commit the agent makes without being told any trailer ends with exactly one
  `Assisted-by: Claude Code, <the session's model>` and no `Co-Authored-By:`; `check-commits` passes over it.
- A commit whose transcript names no model is refused with a message saying why; broken on purpose, the tests go red.

## To be checked

- That `CLAUDE_CODE_SESSION_ID` reaches the agent's shell at the pin, and the transcript lies where the hook looks.
- That Porelo writes the task's git identity where the XDG configuration does not override it.
- That the `SessionStart` hook runs before the first commit in an interactive task and in a `-p` run.
