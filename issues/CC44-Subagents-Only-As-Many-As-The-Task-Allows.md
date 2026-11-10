# CC44 — Subagents Only As Many As The Task Allows

```json
{
  "$schema": "issue-1.json",
  "id": "CC44",
  "title": "Subagents Only As Many As The Task Allows",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 5,
  "blockedBy": [],
  "covers": "no subagent in a task unless the task allows it, then at most as many as it says; off, count and depth measured to hold at the pin"
}
```

**Why this size.** one module and its tests, no contract other repositories read; about 45 minutes: the S median of
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
  - **Off:** `permissions.deny` in the settings file `ClaudeSettings` writes takes a tool out of the session
    entirely, measured by its init record. It takes three: `Agent` (the init record lists it as `Task`),
    `Workflow`, whose agents start with `Agent` denied, and `Skill`, since a skill with `context: fork`
    starts one with both denied and with each of the three count variables below at 0.
    `CLAUDE_CODE_DISABLE_WORKFLOWS` also removes `Workflow`, and is not needed beside the denial.
  - **Workflows and forked skills run past the count and the depth:** at
    `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS=1` and `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH=1` a workflow's three agents
    started at once, a forked skill started beside a running background subagent, and a subagent started a forked
    skill of its own. `CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS` is a second count beside the first. So
    `Workflow` and `Skill` are denied at every number, and no task has skills.
  - **Concurrent count:** `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS=1` holds - of three asked at once, one
    ran and two were refused with "Concurrent subagent limit reached".
  - **Depth:** `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH=1` holds - the subagent gets no subagent tool; at 2
    and unset, the nested start works.
  - `CLAUDE_CODE_MAX_SUBAGENTS_PER_SESSION=1` did not hold: three at once and two in a row all ran. The
    definition does not declare a per-session count.
  - The three variables are not documented; they are named only in the binary's own error texts.
- **`claude.yaml`** declares off and a count.
- **`ClaudeSettings.document`** takes the allowed number from the context `ClaudeContainerSetup` receives: at
  0 it denies `Agent`, `Workflow` and `Skill`; above 0 it denies `Workflow` and `Skill`, sets `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS` to it and
  `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH` to 1 in the file's `env`, beside `DISABLE_UPDATES`. The depth is the
  adapter's own: a subagent then starts none of its own, so the count holds for the whole task.
- **Red first:** `ClaudeContainerSetupTest` - a context with the default yields a settings file that denies
  `Agent`, `Workflow` and `Skill`, and one allowing 2 denies `Workflow` and `Skill` and yields the count 2 and the depth 1.
- **On the VM:** a `session.feature` scenario asks the agent for a subagent in a task with the default and
  sees none start, so a pin bump that drops a denial fails the update job.
- **A soft switch:** the agent has a shell and can start another copy of its CLI, measured with all three
  tools denied; this issue governs only the built-in subagents. A hard bound is Porelo's own, outside this
  repository.
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

- **On the Fedora leg of the Build:** at `subagents none` with only `Agent` denied, the agent started a subagent
  through `Workflow`, and the Ubuntu leg, whose model answered `MISSING`, stayed green. The scenario now asks for each
  way in and looks for the task the session records, whichever tool started it.
- **The scenario's prompt, on the pinned CLI alone, not through Porelo:** with only `Agent` denied the agent started
  a workflow, the session recorded `local_workflow`, and it answered `STARTED`. With the three denied the session
  recorded no such task, the agent wrote the skill, found nothing to invoke it with, and answered `MISSING`.
- **The first scenario, which asked only for a subagent and looked for an `[Agent]` call, seen to fail first:** against
  the adapter before the change the agent called `Agent` and answered `STARTED`.
- **With `Agent` denied, headless:** that first scenario was green: the agent answered `MISSING`.
- **At a count of 1, headless, one run not kept as a scenario:** of three asked at once, one ran and two were
  refused with "Concurrent subagent limit reached".
- **Attended, at a terminal, one run each, not kept as scenarios:** at the default the settings file denied `Agent`
  and the agent answered that it has no such tool. At `--subagents 1` the file holds the count 1 and the depth 1, and
  the agent started the three one after another, so the refusal was not seen at a terminal.

## To be checked

- Whether an undocumented variable is safe to rely on across the weekly pin bumps. The new scenario covers the
  denials, which fails the update job if one stops holding; nothing in the suite covers the count and the depth.
- The depth of 1 from a subagent itself: the settings are written as measured in this issue's plan, not measured
  again with this adapter.
