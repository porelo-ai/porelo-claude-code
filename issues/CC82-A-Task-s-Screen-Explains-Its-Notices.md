# CC82 — A Task's Screen Explains Its Notices

```json
{
  "$schema": "issue-1.json",
  "id": "CC82",
  "title": "A Task's Screen Explains Its Notices",
  "state": "Backlog",
  "size": "XS",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": [],
  "covers": "the documentation says that a task's Claude Code warns of the waived rm check on purpose, with nothing to check in a shell profile, and names a subscription sign-in \"Claude API\""
}
```

**Why this size.** One documentation section, measured once in an attended task; no code.

**Estimate.** XS: 9 min, the measured median of XS over all repositories (n=107).

**What must be true.** A person attached to a task knows that the yellow skip-permissions warning and the header's
"Claude API" are expected there, and is not sent to their shell profile.

## Why

Seen at a task started on a subscription sign-in, Claude Code 2.1.311:

- The CLI warns in yellow: "`CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS` is set, so dangerous rm commands can
  run without a prompt in bypass permissions mode … If you didn't set it, check your shell profile." The task image
  sets it on purpose (`doc/decisions.md`, "Accepted risk: the CLI's rm check is waived in a task"), and a task has
  no shell profile of the person's. Read in 2.1.329: the CLI shows the warning whenever the variable is set in
  bypass mode, with no setting to silence it, and only a child session of its own skips it. The headless log
  already drops it; attended it cannot be.
- The header says "Sonnet 5.5 · Claude API". Read in 2.1.329: the CLI names the plan from the account's subscription
  type and falls back to "Claude API" when it knows none, which is what a token handed in `CLAUDE_CODE_OAUTH_TOKEN`
  leaves it.

## Plan

- Measure both on the pin in one attended task on a sign-in, the header's words and the warning's.
- A section in the user documentation, "What a task's Claude Code shows", says both are expected and why, and links
  the decision.

## Acceptance

- The section names the two texts as the pin shows them, and the documentation build passes.
