# CC44 — Subagents Only As Many As The Task Allows

**Status:** Accepted (operator, 2026-10-31).

**What must be true.** Claude Code in a Porelo task starts no subagent unless the task allows it, and then
no more at once and no deeper than the task says.

## Plan

- **The contract built against:** Porelo's agent interface carries the number of subagents an agent may
  start inside a task, 0 (none) by default; each agent definition declares what it can enforce - off, a
  concurrent count, a depth - and Porelo refuses a value the definition cannot enforce instead of
  enforcing less.
- Measured headless at the pin 2.1.311, on the VM through OpenRouter: by default a subagent starts, and
  one it starts can start another.
  - **Off:** `permissions.deny: ["Agent"]` in the settings file `ClaudeSettings` writes takes the tool out of
    the session entirely - the init record lists it as `Task`, and it is gone from that list.
  - **Concurrent count:** `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS=1` holds - of three asked at once, one
    ran and two were refused with "Concurrent subagent limit reached".
  - **Depth:** `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH=1` holds - the subagent gets no subagent tool; at 2
    and unset, the nested start works.
  - `CLAUDE_CODE_MAX_SUBAGENTS_PER_SESSION=1` did not hold: three at once and two in a row all ran. The
    definition does not declare a per-session count.
  - The three variables are not documented; they are named only in the binary's own error texts.

## Acceptance

- Seen to fail first: a task with the default asks Claude Code to start a subagent, and it does.
- With the change, the same task starts none; a task allowing N starts at most N at once and refuses the
  next.

## To be checked

- The same three switches attended, at a terminal.
- Whether an undocumented variable is safe to rely on across the weekly pin bumps, or needs a check in
  the update job.
