# CC76 — The Unattended View Shows No Raw Event Lines

```json
{
  "$schema": "issue-1.json",
  "id": "CC76",
  "title": "The Unattended View Shows No Raw Event Lines",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the unattended view of a Claude Code task shows what the agent says and does, never a raw stream-json line: system events the person can use - a background command started or ended - in words, the rest dropped, an unknown event dropped unless it names an error"
}
```

**Why this size.** One class, `ClaudeStreamJsonFormatter`, and its tests; no contract, since the formatter's output is read only by a person.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person reading an unattended Claude Code task in the window sees the agent's text, its tool calls and its results, and a background command's start and end in words; no line of raw JSON.

## Plan

- **What is wrong today, read in the code.** `ClaudeStreamJsonFormatter.render` drops only `system/init` and the waiver notice; every other `system` line and every line of a type it does not know (`tool_progress`, for one) is returned as it came, by the class's own rule that anything unrecognised passes through.
- **Seen in an operator's live test on 2.1.311**, an unattended task: `system` lines with subtype `thinking_tokens`, `task_started`, `task_notification`, `background_tasks_changed`, `vcs_state_changed`, and `tool_progress` lines carrying heartbeats, each with its `session_id` and `uuid`, between the readable lines.
- **The new rule.** A `system` line is worded when it is one the person can use: `task_started` of a job in the background as `[background] <description> started`, its end (`task_updated` with an ended status, or `task_notification`) as `[background] <description> <status>`, reading the same fields `ClaudeToolEvents` already reads. Every other known subtype is dropped. A line of an unknown type or subtype is dropped too, unless it carries an error (`is_error`, an `error` field, a subtype beginning `error`, a `level` of `error` or `warning`), which shows as `[error] <message>` or `[warning] <message>`.
- **What still shows, by design.** A line that says something went wrong - an error or a warning, a failed run, a refused request - is never dropped: it shows in words, with its message cut at 300 code points as today. A line that is not JSON at all still shows as it is, without control characters.
- The class comment's rule that anything unrecognised passes through is replaced by the one above, with its reason.
- **Tests.** The harm guarded is an error hidden from the person: the existing tests that an error, a failed run and a non-JSON line still show stay, and one per new rule shows an unknown event dropped while an unknown event that names an error still shows, each seen red first. The words of a worded event are not held verbatim by a test.
- Fixtures are the lines 2.1.311 wrote, values made up, never shapes guessed from the code.

## Acceptance

- A recorded unattended run of the pinned CLI, with a background command and a push, rendered through the formatter, holds no line that begins with `{`.
- An error line of an unknown subtype still shows, and the test of it fails when the error rule is taken out.

## To be checked

- **The fixtures.** The fields of `task_notification`, `background_tasks_changed`, `vcs_state_changed` and `tool_progress`, read from an unattended run of 2.1.311 on a VM, values replaced; whether `vcs_state_changed` names a push worth wording, or only a change of the working tree.
