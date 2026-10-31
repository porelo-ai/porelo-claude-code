# CC44 — Subagents Only As Many As The Task Allows

**Status:** Accepted (operator, 2026-10-31).

**What must be true.** Claude Code in a Porelo task starts no subagent unless the task allows it, and then
no more at once and no deeper than the task says.

## Plan

- **The contract built against:** Porelo's agent interface carries the number of subagents an agent may
  start inside a task, 0 (none) by default; each agent definition declares what it can enforce - off, a
  concurrent count, a depth - and Porelo refuses a value the definition cannot enforce instead of
  enforcing less.
- Read in 2.1.316, not yet at the pin: the subagent tool is `Agent`, denied through `permissions.deny` in
  the settings file `ClaudeSettings` writes; the binary's own error texts name
  `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS`, `CLAUDE_CODE_MAX_SUBAGENTS_PER_SESSION` and
  `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH`, which look undocumented.

## Acceptance

- Seen to fail first: a task with the default asks Claude Code to start a subagent, and it does.
- With the change, the same task starts none; a task allowing N starts at most N at once and refuses the
  next.

## To be checked

- On the VM, at the pinned version: that denying `Agent` stops every subagent, attended and headless,
  and that each limit variable holds, or what it does instead.
- Whether an undocumented variable is safe to rely on across the weekly pin bumps, or needs a check in
  the update job.
