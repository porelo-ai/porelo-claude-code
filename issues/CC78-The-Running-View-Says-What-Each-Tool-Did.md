# CC78 — The Running View Says What Each Tool Did

```json
{
  "$schema": "issue-1.json",
  "id": "CC78",
  "title": "The Running View Says What Each Tool Did",
  "state": "Backlog",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the running view of an unattended Claude Code task says each tool call with its key input and each result by whether it failed and its first line or its size, and thinking as one marker at most, instead of only the tool's name"
}
```

**Why this size.** One class, `ClaudeStreamJsonFormatter`, and its tests; the fixture of [CC76](README.md#cc76) already holds tool calls and results.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person reading an unattended Claude Code task sees for each tool call what it was called on, and for each result whether it failed and how it began or how long it was; never the whole output.

## Why

Part of ST369, *Findings of the second live test on a rented host* (High). In the operator's live test (porelo
snapshot.553, porelo-agent-claude snapshot.186, which holds CC76) the running view of an unattended Claude Code task
showed almost nothing of the work: its lines were only `[Bash]`, `> [tool_result]`, `[thinking]` and once `[git] push`,
over and over. The operator asked whether it is normal to see nothing of the tools. It is not: a reader must see what
each tool did, in short.

Read in the code: `ClaudeStreamJsonFormatter.text` renders a `tool_use` block as `[<name>]` alone and any other block
without text by its type, so a `tool_result` in a user message shows as `> [tool_result]` and a thinking block as
`[thinking]`, one line each. The `[git] push` line also answers the open point of [CC77](README.md#cc77): a push does
write `vcs_state_changed`, with `kind: push`.

## Plan

- **A tool call** on one line with its key input, folded to one line and cut: Bash its command's first line,
  Read/Edit/Write/NotebookEdit the path, Grep/Glob the pattern, WebFetch the URL, WebSearch the query, Task/Agent the
  description, TodoWrite the number of items; any other tool its one most telling string field, or its name alone.
- **A result** as failed or not (`is_error`) with its first non-blank line, cut, or its size (`12 lines`) when the first
  line says nothing; never the whole output.
- **Thinking** dropped, or one `[thinking]` per turn at most, not one line per block.
- **The assistant's own text** stays as [CC76](README.md#cc76) shows it. Its line breaks are [CC77](README.md#cc77)'s
  point; a tool's input and a result's line are folded here, since they are not the agent's words.
- **Tests:** each rule seen red first against the recorded 2.1.311 run and single lines; no test holds a worded line
  verbatim.
- **Level:** unit tests of the formatter; seen once in a live task through the coordinator.

## Acceptance

- Rendered through the formatter, the recorded 2.1.311 run shows each tool call with its key input and each result with
  its failure and its first line or size; no `> [tool_result]` line and no run of `[thinking]` lines.
- A result that failed always says so, and its test fails when the rule is taken out.
