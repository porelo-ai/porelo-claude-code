# CC62 — A Finished Run Answers Its Last Message

```json
{
  "$schema": "issue-1.json",
  "id": "CC62",
  "title": "A Finished Run Answers Its Last Message",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "a finished unattended Claude Code run answers the text of its last message, declared in session.waiting.last_message, so a reader's answer is not empty; the declaration adds no screen rule and so changes nothing in how Porelo wakes the agent"
}
```

**Why this size.** one declaration and its test; no contract another repository reads beyond the existing field.

**What must be true.** When an unattended Claude Code run ends, Porelo answers what the agent said last. A reader whose
findings stand in its last message is judged on them, not on an empty answer, which is what a run answers while the
definition declares no last message.

## Plan

- The contract built against: `session.waiting.last_message` names the last record whose fields match `record` and
  the field holding its text; with `part: { record: {...}, text: <field> }` that field may hold a list of parts, and
  the answer is the text of each matching part, joined by a newline. A `waiting` that holds no screen rule declares
  no screen: Porelo then types no wake line into the agent for it.
- Claude Code's unattended stream (`--output-format stream-json`) ends with a record of `type: result` carrying
  the run's last reply in its field `result`; `src/main/resources/agent/claude.yaml` declares it under its existing
  `waiting`, beside its screen rules.
- Tests: a unit test answers the last message over the agent's records of a run's end (red first, without the
  declaration), from records as the agent writes them in a recorded run, never as the code assumes them.

## Acceptance

- Over a recorded run's end - a reply, a tool call, its reasoning, a later reply - the answer is the text of the last
  reply, and a run without one answers nothing.
- The declaration changes neither the agent's screen rules nor whether Porelo wakes it.

## To be checked

- The record and field the agent's last reply stands in, read in a recorded unattended run at the pinned version.
  Read in a run of 2.1.311 recorded unattended in a Porelo task on 2026-11-12 (`src/test/resources/streams/unattended-run-2.1.311.jsonl`,
  its JSON records only, the init record's lists cut): reasoning, a reply, a `Bash` call, its output and the reply
  `ALL DONE`, then `{type: result, subtype: success, is_error: false, terminal_reason: completed, result: "ALL DONE"}`.
  The `result` field is the last reply alone, as the plan expected.
- A run the API refused also ends in `subtype: success`, with `is_error: true`, `terminal_reason: api_error` and its
  error as `result` (recorded with 2.1.322 on the host, `src/test/resources/streams/refused-run-2.1.322.jsonl`). Porelo's
  record match compares text values only, so `is_error` cannot be declared; the declaration adds
  `terminal_reason: completed`, and the refused run answers nothing (its test seen to fail without it). A run cut
  before its result answers nothing; the screen rules are unchanged, compared whole.
- The plan's `part` is not in the porelo-agent-api built against, and is not needed: the result's `result` is text.
- Read-only review by a Claude Code sub-agent, Sonnet: found that `terminal_reason` tells a refused run apart, which
  made the follow-up for `is_error` unneeded; found the yaml's "nothing to read" stale beside the new declaration,
  reworded. Not taken: loading the records from the classpath (the adapters' tests read them by path), and a test of
  several result records, which no recorded run holds.
