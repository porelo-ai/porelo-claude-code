# CC57 — Measure What The Agents' Streams Tell About Tool Calls

```json
{
  "$schema": "issue-1.json",
  "id": "CC57",
  "title": "Measure What The Agents' Streams Tell About Tool Calls",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "a measurement, no product code: each agent Porelo runs, given the same small order unattended in a task, and what its raw stream says of each tool call - name, input, start, end, own time, background jobs, sub-agents - and how the wall time splits"
}
```

**Why this size.** one prepared project and one order run once per agent in a task on a local VM, the raw streams
kept and read; no code.

**What must be true.** The mapping of each adapter's stream to Porelo's tool-call entry is planned from measured
streams, not from documentation: for each agent there is a recorded unattended run of the same order, and a table
says which tool calls appear, how their start and end show and whether with a time of their own, the tool's name and
input, how a background job's end shows, whether sub-agents show, what is missing, and how the run's wall time splits
into tool time and the rest.

## Plan

- One small Maven project in a task on the local VM, unattended (`porelo task start --prompt`), with each agent that
  can run with a test key: build and run the tests, read a file, edit one line, and one command in the background.
- The raw stream of each run kept with the task's logs; read into the table below.

## Acceptance

- One row per agent run, or the reason it could not run.
- The table is in this issue and in the room.

## To be checked

- Which agents run here with a test key alone.
