# CC59 — Measure What The Agents' Streams Tell About Sub-Agents

```json
{
  "$schema": "issue-1.json",
  "id": "CC59",
  "title": "Measure What The Agents' Streams Tell About Sub-Agents",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "a measurement, no product code: each agent Porelo runs that has sub-agents of its own, ordered unattended in a task to start one sub-agent by a given name, and what its raw stream says of that sub-agent - its start, its end, its name, and which calls are its own"
}
```

**Why this size.** the project and image of [CC57](README.md#cc57) reused, one short order run once per agent, the raw streams kept
and read; no code.

**What must be true.** The operator decided that a task gives each issue a sub-agent of its own, named by the issue's
id, and that each issue's time is its sub-agent's span. So each adapter must map its agent's sub-agent start and end,
with the name it was given, from measured streams: for each agent that has sub-agents there is a recorded unattended
run where one sub-agent is started by a given name, and a table says how its start and end show, whether with a time
of their own, whether the name shows, and how the sub-agent's own tool calls are told apart from the main agent's.

## Plan

- The project and image of [CC57](README.md#cc57) on the local VM; one unattended task per agent, with sub-agents allowed
  (`--subagents 1`) where the adapter offers it.
- The order: start exactly one sub-agent named `ISSUE-7`, which counts with a shell command the Java files holding
  "Sluice"; wait for its answer and report it.
- The raw stream of each run kept with the VM's spike files; the table below; the result written into [CC56](README.md#cc56), PI57,
  OM51, CX19 and AG18 of each adapter.

## Acceptance

- One row per agent, or the reason it has no sub-agents or could not run.
- The table is in this issue and in the room; each adapter's issue holds its own row.

## Result

Measured 2026-11-11 on the VM and project of [CC57](README.md#cc57), one unattended task per agent with `--subagents=1`. The order: start
exactly one sub-agent named `ISSUE-7`, which counts with a shell command the Java files holding "Sluice" (7); wait for
it and report. pi has no sub-agents and agy's adapter turns them off (`subagents: off`), so they did not run.

| Agent | Sub-agent's start | Its name | Its end | Its own calls | Time | Missing |
|---|---|---|---|---|---|---|
| claude 2.1.311 | `Agent` `tool_use` (`subagent_type`, `prompt`, `run_in_background`); `system/task_started` with `task_type: local_agent`, `tool_use_id`, `spawn_depth` | `input.description` (the Agent tool has no name field; the model put the name there) | the `Agent` call's `tool_result`; `system/task_updated` `patch.status` completed with `patch.end_time`; `task_notification` | in the stream, each line with `parent_tool_use_id` = the Agent call's id | ISO `timestamp` on the tool lines; `end_time` (epoch ms) on the job's end | a name field of its own |
| codex 0.163.0 | **not in the stream**: `exec --json` showed only a `collab_tool_call` item (`tool: wait`, no receivers) | not in the stream; the session file has `spawn_agent` with `task_name` `issue_7`, path `/root/issue_7` | the `wait` item's completion | **not in the stream** (they live in the sub-agent's own session file) | none in the stream; the session file has `started_at_ms`/`completed_at_ms` | the sub-agent's start, name, end and calls. The sub-agent answered "0" without any call |
| omp | `task` call, `args.tasks[]` each with `name` (one call may start several); the call ends at once: "Spawned agent `ISSUE-7` (job `ISSUE-7`)" | `args.tasks[].name`, also the job id | `tool_execution_update` lines of the same `task` call after its end, `details.progress[].status` running then completed, `totalDurationMs`; the `wait` tool's result | **not in the stream**: only the progress lines | `totalDurationMs` / `durationMs` in progress and `wait`; no clock time | the sub-agent's own calls |

What it means for the adapters:

- Only claude gives what the operator's rule needs in full: the sub-agent's start, end, name and its own calls by
  `parent`.
- omp gives a sub-agent's name and span, not its calls; `parent` stays empty for them.
- codex gives neither in its stream. Its session files have them (spawn with name, start and end with times), but
  those are not lines of the stream the adapter reads.
- In a pi or agy task no issue can be measured alone.
