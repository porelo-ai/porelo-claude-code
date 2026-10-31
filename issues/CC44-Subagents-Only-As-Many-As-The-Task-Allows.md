# CC44 — Subagents Only As Many As The Task Allows

**Status:** Estimated (operator, 2026-10-31).
**Size:** S - one module and its tests, no contract other repositories read; about 45 minutes: the S median of 24.5, raised by the scenario on the VM, the attended check of the three switches and the move to the Porelo snapshot that carries the value.

**What must be true.** Claude Code in a Porelo task starts no subagent unless the task allows it, and then
no more at once and no deeper than the task says.

## Plan

- **The contract built against:** Porelo's agent interface carries the number of subagents an agent may
  start inside a task, 0 (none) by default; each agent definition declares what it can enforce - off, a
  concurrent count, a depth - and Porelo refuses a value the definition cannot enforce instead of
  enforcing less.
- Measured headless at the pin 2.1.311, on the VM through OpenRouter with `z-ai/glm-5.3-flash` (the same
  model for subagents, through `CLAUDE_CODE_SUBAGENT_MODEL`), in bypass mode: by default a subagent starts, and
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
- **`claude.yaml`** declares what this agent enforces: off, a concurrent count, a depth - not a
  per-session count.
- **`ClaudeSettings.document`** takes the allowed number and depth from the context `ClaudeContainerSetup`
  receives: at 0 it adds `"Agent"` to `permissions.deny`; above 0 it sets
  `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS` and `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH` in the file's `env`,
  beside `DISABLE_UPDATES`.
- **Red first:** `ClaudeContainerSetupTest` - a context with the default yields a settings file that denies
  `Agent`, and one allowing 2 at depth 1 yields both variables; today neither is written.
- **On the VM:** a `session.feature` scenario asks the agent for a subagent in a task with the default and
  sees it refused, so a pin bump that drops the variables or renames the tool fails the update job.
- **A soft switch:** the agent has a shell and can start another copy of its CLI; this issue governs only
  the built-in subagents. A hard bound is Porelo's own, outside this repository.
- **Porelo's version:** `porelo.version` in `pom.xml` moves to the Porelo snapshot whose agent API
  carries the value; until then nothing here compiles against it.

## Acceptance

- Seen to fail first: a task with the default asks Claude Code to start a subagent, and it does.
- With the change, the same task starts none; a task allowing N starts at most N at once and refuses the
  next.

## Open questions

- None.

## To be checked

- The same three switches attended, at a terminal.
- Whether an undocumented variable is safe to rely on across the weekly pin bumps, or needs a check in
  the update job.
