# CC46 — A Task Writes Its Own Commit Trailer

```json
{
  "$schema": "issue-1.json",
  "id": "CC46",
  "title": "A Task Writes Its Own Commit Trailer",
  "state": "Review",
  "size": "M",
  "priority": "Medium",
  "rank": 4,
  "blockedBy": [],
  "covers": "a task's commits end with `Assisted-by: Claude Code, <model>` written by a git hook from the session's own model, with Claude Code's own attribution switched off in the settings the adapter writes"
}
```

**Why this size.** the settings file, a git hook and its git configuration written into the container, a measurement at the
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
  - leaves a message that is then empty once comments and the verbose diff are gone, so git still aborts it;
  - reads the model of the last answer in the session's transcript, `CLAUDE_CODE_SESSION_ID`'s file and only it,
    or without one the newest transcript that names a model; an answer's own model is the one followed by
    `"stop_reason"` after its content, as the pin writes it, or first in its message followed by `"id"`, as a later CLI
    does, never a `"model"` inside a tool call's input or result, and never `<synthetic>`;
  - writes `Assisted-by: Claude Code, <model>` with `git interpret-trailers`, replacing one the agent guessed;
  - refuses the commit, saying why, when it finds no model or one that is not a plain identifier, unless the
    message already ends with an `Assisted-by:` trailer that names a plain one.
- **The executable bit.** `ContainerFile` writes no mode, and git ignores a hook that is not executable, so a
  `SessionStart` hook in the settings runs `chmod +x` on it before the first turn.
- **Red first:** `ClaudeContainerSetupTest` asks for the attribution keys, the git configuration and the hook; a test
  runs the written hook with `sh` and git (`GIT_CONFIG_GLOBAL=/dev/null`) on a message with a `Co-Authored-By:` line and
  a stand-in transcript, and on one with no transcript.

## Acceptance

- In a task on the VM, a commit the agent makes without being told any trailer ends with exactly one
  `Assisted-by: Claude Code, <the session's model>` and no `Co-Authored-By:`; `check-commits` passes over it.
- A commit whose transcript names no model is refused with a message saying why; broken on purpose, the tests go red.

## Readings declined

- A message of `#` lines only, given with `-m` or `-F`, is committed bare: under those cleanups git keeps such lines,
  so the hook cannot tell them from comments, and a subject that is a comment is no message an agent writes.
- A path with a space under the transcripts' folder splits in the fallback loop: in a task the folder is fixed and
  has none.
- A last paragraph of one prose line that starts `Assisted-by:` is a trailer to git, and `check-commits` reads it
  the same way, so the hook follows git's rule.
- The replacement takes any `Assisted-by:` trailer, another tool's too: in a task Claude Code is the only one that
  commits.

## Measured

- On the pinned CLI on a local VM, with the adapter's settings and the hook written without a mode, one `-p` run over
  OpenRouter: the `SessionStart` hook made the hook executable and set `core.hooksPath`, and the agent's
  `git commit -am` gave `Assisted-by: Claude Code, z-ai/glm-5.3-flash` and no `Co-Authored-By:`.
- `CLAUDE_CODE_SESSION_ID` reaches the agent's shell and names the transcript the hook reads.
- The pinned CLI writes an answer's `"model"` after its `"content"`, followed by `"stop_reason"`; 2.1.319 writes it
  first in the message, followed by `"id"`; a record's line starts with `"parentUuid"`, not its type.
- In a task, the acceptance scenario *a task's commit names the model that made it* passed on a local VM: the hook's
  folder was there, and the commit ended with exactly the model the task ran.
