# CC54 — The Busy Scenario Holds Its Loop In The Foreground

```json
{
  "$schema": "issue-1.json",
  "id": "CC54",
  "title": "The Busy Scenario Holds Its Loop In The Foreground",
  "state": "Accepted",
  "size": "-",
  "priority": "Medium",
  "rank": 6,
  "blockedBy": [],
  "covers": "`wake.feature`'s busy scenario no longer fails when the model sends the wait loop to the background: it sees the loop in the foreground before it sends the message, or sets the loop up again"
}
```

**What must be true.** `wake.feature`'s scenario *a message that arrives while the agent works wakes it once it is at
rest* proves what it names whichever way the model runs the wait loop it is given, and fails only when Porelo wakes an
agent that is still at work.

## Why

The scenario asks the agent to run `until [ -e /tmp/porelo-busy-done ]; do sleep 2; done` in the foreground, waits
until the loop runs, sends a message, and then holds that the agent is still at work: the loop in `podman top`,
`Running…` and `esc to interrupt` on the screen, and no announced message. On the ubuntu leg of a Build on `main`, the
model (`z-ai/glm-5.3-flash` through OpenRouter) sent the loop to the background instead ("the harness doesn't permit a
true foreground sleep"), ended its turn and so was at rest; Porelo then announced the message at once, as it should,
and the check after the message failed. The step before it took the loop for work from `podman top` alone, which a
background loop passes too. Fedora passed the same scenario in the same Build, and the scenario had not failed this way
in the twenty Builds on `main` before.

## The shape

- The wait before the message holds the loop in the foreground - the loop in `podman top` and `Running…` with
  `esc to interrupt` on the screen, no "shell still running" at an empty prompt - not the loop's process alone.
- When the model has sent the loop to the background, the scenario says so in its failure, or asks once more for the
  loop in the foreground, rather than sending the message to an agent at rest.

## Acceptance

- The scenario passes on both legs, and a run where the model backgrounds the loop no longer fails at the check after
  the message; a Porelo that wakes a busy agent still fails it.

## To be checked

- Whether Claude Code at the pin can be kept from backgrounding a shell command in a task (a setting or an
  environment variable) without changing what a person's task does.
