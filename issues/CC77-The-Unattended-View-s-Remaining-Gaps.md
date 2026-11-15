# CC77 — The Unattended View's Remaining Gaps

```json
{
  "$schema": "issue-1.json",
  "id": "CC77",
  "title": "The Unattended View's Remaining Gaps",
  "state": "Backlog",
  "size": "S",
  "priority": "Low",
  "rank": 1,
  "blockedBy": [],
  "covers": "what [CC76](README.md#cc76) left open in the unattended view: a rate limit read one level down, a NaN cost, and assistant text on one line"
}
```

**Why this size.** One class, `ClaudeStreamJsonFormatter`, and its tests, plus one recorded run with a push on the
test VM.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** Nothing [CC76](README.md#cc76) left open is lost: each point below is fixed, or shown by a test or a recording
not to happen.

## Why

Kept from [CC76](README.md#cc76) (*The Unattended View Shows No Raw Event Lines*, Finished 2026-11-15) when it was deleted: the points
the reading by another model raised and [CC76](README.md#cc76) noted but did not fix, and the one point to be checked that its
recording did not answer.

## Plan

- **A rate limit one level down.** A `rate_limit_event` carries its status inside `rate_limit_info`, which the error
  rule does not read; record one, or read its shape from the pinned CLI, before the rule names it.
- **A cost of NaN.** The result line formats `total_cost_usd`; a NaN or an infinity there would throw. Shown by a test,
  then guarded.
- **Assistant text on one line.** Assistant text, a tool's command and a notice's text before [CC76](README.md#cc76) keep their line
  breaks, so a reply can hold a line that reads like `[error] ...`. Decide with the coordinator whether a person's
  view folds them, since a reply's own line breaks are what a person reads.
- **A push** is answered: the operator's live test of ST369 showed `[git] push`, so a push writes `vcs_state_changed`
  with `kind: push` ([CC78](README.md#cc78)).
- **Level:** unit tests of the formatter; the recording through the coordinator's VM slot.

## Acceptance

- Each point above is fixed with a test seen red first, or recorded as not happening with its evidence.
