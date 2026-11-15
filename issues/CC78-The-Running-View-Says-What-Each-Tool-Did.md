# CC78 — The Running View Says What Each Tool Did

```json
{
  "$schema": "issue-1.json",
  "id": "CC78",
  "title": "The Running View Says What Each Tool Did",
  "state": "Review",
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
snapshot.553, porelo-agent-claude snapshot.186, which holds [CC76](README.md#cc76)) the running view of an unattended Claude Code task
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

## What was built

- `ClaudeStreamJsonFormatter` says a `tool_use` block as `[<tool>] <key input>`, folded to one line and cut like
  every other field: Bash by its command's first line that is not blank, Read/Edit/MultiEdit/Write by `file_path`,
  NotebookEdit by `notebook_path` (its own field) or `file_path`, Grep/Glob by `pattern`, WebFetch by `url`,
  WebSearch by `query`, Task/Agent by `description`, TodoWrite by its number of items; any other tool by the first of
  `description`, `file_path`, `path`, `pattern`, `query`, `url`, `command`, `subject`, `name` that holds text, else
  by its name alone. A field that holds what a tool writes or asks at length (a content, a prompt) is never shown.
- A `tool_result` block that did not fail says its first line that is not blank, cut, and its number of lines when
  it has more than one (`(5 lines)`). One with `is_error` set says `[failed]` and its first three lines that are not
  blank, joined with ` | `, folded and cut, since a cause rarely stands on the first line (Bash's begins
  `Exit code N`). Its content is read as a string or as a list of blocks with text. A result with no text says what it holds (`1 image`), or `no output`.
- Thinking is dropped, the choice the plan left open: a thinking or redacted-thinking block shows nothing, and a
  message of thinking alone shows no line, since the tool call it leads to says what was done. A message with no
  block at all still gets the `[unreadable]` marker.
- The assistant's own text is unchanged; the blocks of one message are joined with a space.
- Tests, each seen red before the code and again when its rule was broken on purpose (the input taken off, the
  failure mark taken off, the whole output shown, thinking shown, the cut and the fold taken off):
  `aRecordedRunSaysEachToolCallWithItsInputAndEachResultByHowItBegan` over the recorded 2.1.311 run,
  `aToolCallIsSaidWithWhatItWasCalledOn`, `aToolsInputIsCut`, `aResultSaysHowItBeganOrHowLongItWas`,
  `aResultThatFailedAlwaysSaysSo`, `thinkingIsLeftOut` and `aToolsInputOrResultCannotOpenALineOfItsOwn`.
  `ClaudeAgentTest` no longer holds `[Bash]`, `> [tool_result]` or `[thinking]`; it names an unknown block type
  instead.
- The recorded run has no failed result and only Bash calls; the other tools and a failure are shown by single lines.
- Open: the plan's level "seen once in a live task through the coordinator" is not done here.
- The reading by another model found two faults, both fixed with a test seen red first and again under a deliberate
  break: a failed result hid its cause behind its first line (now its first three lines), and a tool name or a block
  type holding `]` could close its mark and open a forged one like `[error]` (brackets are now taken out of both).
  The test of an empty result now checks that it says `no output`. Nothing of the reading is left open.
