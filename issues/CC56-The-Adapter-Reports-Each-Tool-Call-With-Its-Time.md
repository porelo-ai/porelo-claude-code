# CC56 — The Adapter Reports Each Tool Call With Its Time

```json
{
  "$schema": "issue-1.json",
  "id": "CC56",
  "title": "The Adapter Reports Each Tool Call With Its Time",
  "state": "Backlog",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter reads each tool call from its agent's output and hands Porelo its tool, kind, start, end, a shell call's command line, whether it ran in the background, and a sub-agent's name and the calls made under it, in Porelo's common entry",
  "openQuestions": 0
}
```

**Why this size.** a mapping of six kinds of line (`tool_use`, `tool_result`, and the `system` lines `task_started`, `task_updated`, `task_notification` and the sub-agent's), a name table, and tests from the recorded runs of [CC57](README.md#cc57) and [CC59](README.md#cc59). It builds against `ToolEvents` of Porelo's agent API, contract version 5.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** For every tool call the agent makes in an unattended task, Porelo records one entry in its
common tool-call format: the task, the tool as the agent names it, its kind (shell, edit, read, search, web,
sub-agent, mcp or other), when it started and when it ended, for a shell call its command line, whether it ran in the
background, and for a sub-agent the name it was given and the calls made under it. Porelo joins those calls to the
issue whose id names the sub-agent. The adapter only maps its agent's own output; the format, the time a line without
its own time gets, the masking of a command line and where the entry is kept are Porelo's.

## Plan

- Source: Claude Code's stream-json, the lines `ClaudeStreamJsonFormatter` reads today. Measured on 2.1.311 ([CC57](README.md#cc57), [CC59](README.md#cc59)).
- START: a `tool_use` block in an `assistant` line: `id`, `tool` = `name`, `at` = the line's ISO `timestamp`;
  for `Bash` `command` = `input.command`. END: the `tool_result` with that `tool_use_id` in a `user` line, `at` = its
  `timestamp`; `is_error` marks a failed call.
- `parent` = the line's `parent_tool_use_id`: the id of the `Agent` call a sub-agent's own call ran under.
- `ClaudeAgent` overrides `Agent#toolEvents(String line)`; `ClaudeToolEvents` maps one output line to its
  `ToolEvent(line, id, phase, tool, kind, command, background, jobId, at, name, parent)`. It keeps no state between
  lines: Porelo joins a start to its end by `id`, a background job's by `jobId`, and a call to its sub-agent by
  `parent`.
- Kinds: `Bash` -> shell, `Edit`/`MultiEdit`/`Write`/`NotebookEdit` -> edit, `Read` -> read, `Grep`/`Glob` -> search, `WebFetch`/`WebSearch` -> web, `Agent`/`Task` -> sub-agent, `mcp__*` -> mcp, any other (`ToolSearch`, `TaskStop`, ...) -> other.
- A shell call's `command` is its whole command line as the stream gives it, unmasked; Porelo masks it before it is
  stored.
- Sub-agent: the `Agent` call's START carries `name` = `input.description`. The Agent tool has no name field;
  the order to put the issue's id there is the task's. Its END is the call's `tool_result`, or for
  `run_in_background: true` the job's end below.
- Background: `input.run_in_background: true` marks the call `background`; its `tool_result` comes at once.
  `system/task_started` (`task_id`, `tool_use_id`, `is_backgrounded`) gives `jobId` = `task_id` for that `id`, and the
  job ends with `system/task_updated` where `patch.status` is completed, failed or killed: `at` = `patch.end_time` (epoch
  ms). A foreground call past its timeout is moved to the background the same way (`patch.is_backgrounded: true`).
- Red first: `ClaudeToolEventsTest` feeds recorded lines (a shell call, a background shell and its `task_updated` end, an `Agent` call with `description` and one call under it by `parent_tool_use_id`, a failed call, and a line that is not JSON) and expects the events above; it fails while
  `toolEvents` still answers the API's default, nothing.
- Level: unit tests over recorded output only; the acceptance legs show the entries of a real run once Porelo writes
  them.

## Acceptance

- A recorded unattended run yields one entry per tool call with tool, kind, start, end and, for a shell call, its
  command line; a background job's start and end are joined by its id; a sub-agent carries its name.
- No entry holds a file's content or a tool's output; a command line reaches Porelo unmasked and is stored masked.
- A line the reader cannot read yields nothing and never stops the run.

## What makes it uncertain

- The `system/*` lines are not documented and carry no `timestamp`; their names were read on 2.1.311 and may
  move with a new pin.
- A sub-agent's name is only what the model put into `description`; a model that ignores the order gives a wrong name.
- `tool_progress` heartbeats (every 30 s, `elapsed_time_seconds`) are read as nothing.

## Decisions

- Only unattended tasks are measured. Attended (tmux) sessions write no JSON stream; whether their tool calls are
  recorded is decided later.
- A shell call keeps its whole command line, not only its program. Porelo masks it before it is stored; the adapter
  passes the line as the stream gives it.
- A task gives each issue a sub-agent of its own, named by the issue's id; an issue's time is its sub-agent's span.

## To be checked

- The agent API with `name` and `parent` on `ToolEvent`, installed locally to build against.
