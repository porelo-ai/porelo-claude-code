# CC44 — Subagents Only As Many As The Task Allows

**Status:** Review.
**Size:** S - one module and its tests, no contract other repositories read; about 45 minutes: the S median of
24.5, raised by the scenario on the VM and the attended check of the three switches.

**What must be true.** Claude Code in a Porelo task starts no subagent unless the task allows it, and then no
more at once than the task says.

**This does not hold today.** Porelo takes a task's default of 0 as none and reads `claude.yaml`, which declares nothing, as
off, but this adapter switches nothing off, so Claude Code in such a task still starts a subagent, as measured below.

## Plan

- **The contract built against:** Porelo's agent interface carries one number, the most subagents an agent may
  run at the same time inside a task (not a total over the task), 0 (none) by default, and no depth; each
  agent definition declares what it can enforce of it - off only, or a count - and Porelo refuses a value the
  definition cannot enforce with exit code 64 instead of enforcing less. Where a CLI needs a depth to make the
  count hold, the adapter sets it itself.
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
- **`claude.yaml`** declares off and a count.
- **`ClaudeSettings.document`** takes the allowed number from the context `ClaudeContainerSetup` receives: at
  0 it adds `"Agent"` to `permissions.deny`; above 0 it sets `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS` to it and
  `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH` to 1 in the file's `env`, beside `DISABLE_UPDATES`. The depth is the
  adapter's own: a subagent then starts none of its own, so the count holds for the whole task.
- **Red first:** `ClaudeContainerSetupTest` - a context with the default yields a settings file that denies
  `Agent`, and one allowing 2 yields the count 2 and the depth 1; today neither is written.
- **On the VM:** a `session.feature` scenario asks the agent for a subagent in a task with the default and
  sees it refused, so a pin bump that drops the variables or renames the tool fails the update job.
- **A soft switch:** the agent has a shell and can start another copy of its CLI; this issue governs only
  the built-in subagents. A hard bound is Porelo's own, outside this repository.
- **Porelo's version:** `porelo.version` in `pom.xml` is already 0.5.1-SNAPSHOT, and the published snapshot's agent API
  carries the value (`SubagentLimit`, `subagents` in `SetupContext`).

## Acceptance

- Seen to fail first: a task with the default asks Claude Code to start a subagent, and it does.
- With the change, the same task starts none; a task allowing N starts at most N at once and refuses the
  next.

## Open questions

- None.

## Measured

On the VM at the pin 2.1.311, through OpenRouter with `z-ai/glm-5.3-flash`, the adapter installed in the account,
against a Porelo older than the one on `main`, so `project remove` in the new scenario's last step was run as
`project unfollow`:

- **Seen to fail first:** the new `session.feature` scenario against the adapter before the change: the agent
  called `Agent` and answered `STARTED`.
- **With the change, headless:** the same scenario is green: the agent answers `MISSING` and no `[Agent]` call
  appears.
- **At a count of 1, headless, one run not kept as a scenario:** of three asked at once, one ran and two were
  refused with "Concurrent subagent limit reached".
- **Attended, at a terminal, one run each, not kept as scenarios:** at the default the settings file denies `Agent`
  and the agent answers that it has no such tool. At `--subagents 1` the file holds the count 1 and the depth 1, and
  the agent started the three one after another, so the refusal was not seen at a terminal.

## To be checked

- Whether an undocumented variable is safe to rely on across the weekly pin bumps. The new scenario covers `Agent`
  being denied, which fails the update job if it stops holding; nothing in the suite covers the count and the depth.
- The depth of 1 from a subagent itself: the settings are written as measured in this issue's plan, not measured
  again with this adapter.
