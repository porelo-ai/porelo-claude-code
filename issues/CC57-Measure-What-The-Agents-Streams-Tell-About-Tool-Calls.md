# CC57 — Measure What The Agents' Streams Tell About Tool Calls

```json
{
  "$schema": "issue-1.json",
  "id": "CC57",
  "title": "Measure What The Agents' Streams Tell About Tool Calls",
  "state": "Review",
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

## Result

Measured 2026-11-11 on a local VM, porelo 0.5.1 snapshot 489, project porelo-message-sluice (one commit, the GraalVM
JDK and Maven Central allowed), one unattended task per agent. The order was the same for all:

1. start `./mvnw -B -s settings.xml -pl filter/app -am test > /tmp/build.log 2>&1` in the background, without waiting;
2. read README.md and reword its first line;
3. count the Java files holding "Sluice";
4. wait for the build and report the tests.

All five agents ran with a test key and finished. Each task's raw stream (its task.log) is kept outside the
repository with the VM's spike files. agy ran a second time in its container with `--output-format stream-json`,
because the adapter's `json` output holds no step at all.

| Agent (provider, model) | Calls | Start and end of a call | Own time | Tool name and input | Background job and its end | Sub-agents | Missing | Wall time split |
|---|---|---|---|---|---|---|---|---|
| claude (anthropic, claude-opus-5-5, CLI 2.1.311) | 8 | `tool_use` block in an `assistant` line (`id`, `name`, `input`); `tool_result` with `tool_use_id` in a `user` line, `is_error` | yes: ISO `timestamp` on both lines | `Bash` `input.command` (whole line), `ToolSearch`, `TaskStop` | `input.run_in_background: true`: `system/task_started` (`task_id`, `tool_use_id`, `is_backgrounded`); end as `system/task_updated` `patch.status` completed/killed with `patch.end_time` (epoch ms) and `system/task_notification` (`task_id`, `tool_use_id`, `status`). A foreground call past its 600 s timeout is moved to the background (`task_updated` `is_backgrounded: true`) and its `tool_result` comes at once | none asked for; lines carry `parent_tool_use_id` | the `system/*` lines have no `timestamp`; `tool_progress` heartbeats every 30 s carry `elapsed_time_seconds` only | wall 623.6 s, model 22.6 s (`duration_api_ms`), tools 601 s: 600 s of it a wait that never ended (below) |
| codex (openai, CLI as pinned) | 9 | `item.started` / `item.completed` with the same `item.id`; `command_execution` (`command`, `exit_code`, `status`), `file_change` (`changes[].path`, `kind`) | no time on any line | `command` is the whole line wrapped in `/bin/bash -lc '…'`; a file change names paths only | none of its own: a `&` inside a command is invisible, the item completes at once. Items overlap: item_4 (the build with `wait`) stayed open while item_5 and item_6 started and ended | none | every time; the end of a job started with `&` | not in the stream; the task ran 233 s from start to end, container start included |
| pi (openrouter, moonshotai/kimi-k2.6) | 6 | `tool_execution_start` / `_end` by `toolCallId`, `toolName`, `args`; `_end` has `isError`; `_update` lines between | `_end` carries `durationMs`; `message_end` carries the message's epoch-ms `timestamp` (an assistant message's is its start) | `bash` `args.command` (whole line, with `timeout`), `read`, `edit` (`path`, `edits`) | none of its own; `&` invisible. Two calls run side by side in one turn | none | a start time on the tool lines (the assistant message's `timestamp` stands in) | span 58.5 s: tools 45.1 s (the longest call per turn), the rest 13.4 s |
| omp (openrouter, openai/gpt-5.5) | 15 | as pi (`tool_execution_start` / `_end` by `toolCallId`), plus `intent`; `tool_stream_update` while an edit's input streams | none on tool lines; `message_end` epoch-ms `timestamp` (an assistant message's is its start) | `bash` `args.command` (whole line, `timeout`, `async`), `read` (`path`), `edit` (`input` in its own patch form), `grep`, `todo` | its own: a `bash` call with a short `timeout` came back as "Backgrounded as job bg_1"; the job's end is a `message_end` with `role: custom`, `customType: async-result`, `details.jobs[{jobId, status, durationMs}]`. The `&` inside it stays invisible | none (`task` tool not used) | `durationMs` on `tool_execution_end` | span 61.0 s; tool and model time cannot be told apart from the stream |
| agy, adapter's `--output-format json` (google) | 0 | none: one final object (`status`, `response`, `duration_seconds`, `num_turns`, `usage`) | only the run's `duration_seconds` | none | a plain line before it: "root agent idle; waiting up to 30m0s for 2 background task(s)" | none | every tool call | 42.1 s in all; no split |
| agy, `--output-format stream-json` (second run) | 32 | `step_update` with `step_index`, `step_type: tool`, `state` ACTIVE then DONE, `tool_name`, `tool_info.parameters`; DONE adds `output` | DONE carries `duration_seconds`; no clock time | `run_command` `CommandLine` (whole line), `read_file`, `edit_file` (path only), `manage_task`, `schedule` | its own: a long `run_command` is moved to a task `<conversation>/task-<step>`; its step ends DONE early, the job's state is seen only in `manage_task` status/kill outputs the agent asks for (RUNNING, then cancelled); no event of its own at the job's end | none | a clock time; a job end event | wall 72.3 s: tool steps 70.5 s (the background wait included), model steps 51.8 s; they overlap, so the parts do not add up |

What the streams show beyond the table:

- **The time a call starts and ends** is in claude's stream alone. pi and agy give a duration at the end; codex and
  omp give nothing. Porelo has to stamp each line as it arrives; the adapters cannot.
- **A `&` inside a shell command** is invisible to every agent; only claude (`run_in_background`), omp (its own jobs)
  and agy (its own tasks) have background jobs with ids. Only claude reports a job's end by itself.
- **A wait that never ends:** claude and agy both waited with `pgrep -f "mvnw…"`, which matches the waiting shell
  itself. claude sat 600 s until its tool timeout moved the call to the background; agy's call was moved to a task.
  Most of claude's wall time was this wait, not the build (48 s).
- **Calls overlap** in codex, pi and omp; a start and its end are joined by id, never by order.
- **Secrets in command lines:** none in these runs. The command lines held no token, key, password or
  `Authorization:` header; env names such as `ANTHROPIC_API_KEY` appear only in claude's `init` line
  (`apiKeySource`), not in a command. omp's stream holds long base64 signatures of the model's thinking in which
  `sk-` occurs by chance: a masking rule for credential shapes must not touch anything but the command line.
- **Sub-agents** were not asked for and none ran; claude marks a sub-agent's lines with `parent_tool_use_id`, omp has
  a `task` tool. A run that asks for one is still to be measured.
