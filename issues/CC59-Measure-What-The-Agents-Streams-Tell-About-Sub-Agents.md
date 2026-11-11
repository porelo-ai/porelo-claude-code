# CC59 — Measure What The Agents' Streams Tell About Sub-Agents

```json
{
  "$schema": "issue-1.json",
  "id": "CC59",
  "title": "Measure What The Agents' Streams Tell About Sub-Agents",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "a measurement, no product code: each agent Porelo runs that has sub-agents of its own, ordered unattended in a task to start one sub-agent by a given name, and what its raw stream says of that sub-agent - its start, its end, its name, and which calls are its own"
}
```

**Why this size.** the project and image of CC57 reused, one short order run once per agent, the raw streams kept
and read; no code.

**What must be true.** The operator decided that a task gives each issue a sub-agent of its own, named by the issue's
id, and that each issue's time is its sub-agent's span. So each adapter must map its agent's sub-agent start and end,
with the name it was given, from measured streams: for each agent that has sub-agents there is a recorded unattended
run where one sub-agent is started by a given name, and a table says how its start and end show, whether with a time
of their own, whether the name shows, and how the sub-agent's own tool calls are told apart from the main agent's.

## Plan

- The project and image of CC57 on the local VM; one unattended task per agent, with sub-agents allowed
  (`--subagents 1`) where the adapter offers it.
- The order: start exactly one sub-agent named `ISSUE-7`, which counts with a shell command the Java files holding
  "Sluice"; wait for its answer and report it.
- The raw stream of each run kept with the VM's spike files; the table below; the result written into CC56, PI57,
  OM51, CX19 and AG18 of each adapter.

## Acceptance

- One row per agent, or the reason it has no sub-agents or could not run.
- The table is in this issue and in the room; each adapter's issue holds its own row.
