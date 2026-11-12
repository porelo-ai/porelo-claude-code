# CC61 — An Attended Session Takes An Opening Text

```json
{
  "$schema": "issue-1.json",
  "id": "CC61",
  "title": "An Attended Session Takes An Opening Text",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "an attended session of Claude Code opens on the person's opening text as its first turn and then waits for the person, the form (positional) declared in the agent definition and measured in a task"
}
```

**Why this size.** one definition field, a unit test and one attended check on the local VM; no contract another repository reads beyond
the agreed `opening_flag`.

**What must be true.** A person who starts an attended session of Claude Code with an opening text, as a discussion does,
sees the agent answer that text as its first turn and then wait at its prompt for them. Porelo takes the form from the
agent definition, never from the flag that runs the agent once and ends.

## Plan

- The contract built against: the agent definition's session block takes `opening_flag`, beside `resume_flag`.
  `""` hands an attended session its opening text as the last argument, positionally; any other value is one
  argument beginning with `-`, which the text follows, last. Absent, the text is positional when the headless prompt
  flag is empty and refused otherwise, before anything is made. A text that is blank, holds a NUL, or begins with `-`
  or `@` is refused for every agent, after a flag too.
- `src/main/resources/agent/claude.yaml` declares the opening text as positional: `claude --help` reads "starts an
  interactive session by default ... [prompt] Your prompt", while its headless `-p` is print mode and ends after
  the first answer. The text goes last, behind the instructions arguments and the model.
- Tests: a unit test reads the definition and asserts the form it declares (red first, against a definition without
  it); the session check below runs once on the local VM, never in the unit suite.

## Acceptance

- In a task on the local VM, attended, with the opening text "Name the file in /workspace that you read first":
  the first turn answers it, and afterwards the screen matches `at_rest` and the session is still open.
- The same start with the text handed as `-p` would end after the answer: seen once, so the declared form is the one
  that keeps the session open.
- A text that begins with `-` or `@` is refused by Porelo before the agent starts, and the agent never sees it.
- A task that already exists takes no opening text: Porelo refuses a resume paired with one, so no resumed session
  receives it.

