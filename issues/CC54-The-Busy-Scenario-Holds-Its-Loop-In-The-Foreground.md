# CC54 — The Busy Scenario Holds Its Loop In The Foreground

```json
{
  "$schema": "issue-1.json",
  "id": "CC54",
  "title": "The Busy Scenario Holds Its Loop In The Foreground",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 6,
  "blockedBy": [],
  "covers": "`wake.feature`'s busy scenario no longer fails when the model sends the wait loop to the background: it sees the loop in the foreground before it sends the message, or sets the loop up again",
  "openQuestions": 0
}
```

**Why this size.** One scenario in `wake.feature` changes: its project's image turns Claude Code's background tasks
off, and the wait before the message reads the foreground from the screen. No Java, no contract, no step of the kit;
every step it needs exists. What costs time is measuring the variable at the pin and two real runs on the VM.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=167).

**What must be true.** `wake.feature`'s scenario *a message that arrives while the agent works wakes it once it is at
rest* proves what it names whichever way the model would run the wait loop it is given, and fails only when Porelo
wakes an agent that is still at work. When it fails for another reason, its failure says which.

## Why

The scenario asks the agent to run `until [ -e /tmp/porelo-busy-done ]; do sleep 2; done` in the foreground, waits
until the loop runs, sends a message, and then holds that the agent is still at work: the loop in `podman top`,
`Running…` and `esc to interrupt` on the screen, and no announced message. On the ubuntu leg of a Build on `main`, the
model (`z-ai/glm-5.3-flash` through OpenRouter) sent the loop to the background instead ("the harness doesn't permit a
true foreground sleep"), ended its turn and so was at rest; Porelo then announced the message at once, as it should,
and the check after the message failed. The step before it took the loop for work from `podman top` alone, which a
background loop passes too. Fedora passed the same scenario in the same Build, and the scenario had not failed this way
in the twenty Builds on `main` before.

Read in Claude Code (2.1.322 here; the pin is 2.1.311): the Bash tool blocks a foreground `sleep` with "To wait for a
condition, use Monitor with an until-loop (e.g. `until <check>; do sleep 2; done`). To wait for a command you started,
use run_in_background: true." The very loop the scenario asks for is thus what Claude Code itself tells the model to
run in the background, and its tool description says so before any call. The model's choice is a reading of its own
tools, not a fault of Porelo. That block, `run_in_background` and moving a call past its timeout (120 s by default) to
the background all hang on one switch: `CLAUDE_CODE_DISABLE_BACKGROUND_TASKS` set in the process environment turns all
three off. The timeout is not the cause here: the loop runs only from the first wait until the `touch`, seconds after
it is seen.

## Plan

- `src/acceptance/resources/org/fuin/porelo/agent/impl/claude/acceptance/wake.feature`, the busy scenario only.
- The project: `a project called "busy" of class "guarded" whose project file also says:` with
  `run: |` / `ENV CLAUDE_CODE_DISABLE_BACKGROUND_TASKS=1`, the kit step `task-image-settings.feature` in porelo uses
  for an `ENV` line. It sets the variable in this scenario's image alone, so no person's task changes, and the
  model has no background to send the loop to: no `run_in_background`, no Monitor, no sleep block steering it there.
  A comment says why, and that it is the scenario's choice, not the adapter's (`claude.yaml` stays as it is).
- The wait before the message: work is the loop in `podman top` and, on the screen, `Bash(until [ -e
  /tmp/porelo-busy-done ]`, `Running…` and `esc to interrupt`; `podman top` alone no longer passes. The Enter-again
  for a line left in the input box stays.
- A loop in `podman top` while the screen is at rest (`bypass permissions on` without `esc to interrupt`) is a loop
  in the background: the try says so in its output ("the loop runs, but in the background: the agent is at rest"),
  so a failure there names the model's choice and never reaches the message. No second request: with the variable
  set it should not occur, and a request typed from a retried script would need a guard of its own.
- The check after the message stays as it is; it already holds the foreground.
- Red first, on the VM and not committed: the busy scenario with its typed line asking for the loop with
  `run_in_background: true` and without the variable. Today the first wait passes on `podman top` and the check after
  the message fails; with the new wait it fails before the message, saying the loop is in the background.
- Level: acceptance only, the scenario on both legs of the VM against the model in `PORELO_E2E_MODEL`.

## Acceptance

- The busy scenario passes on both legs, with the variable visible in the task (`podman exec <task> printenv
  CLAUDE_CODE_DISABLE_BACKGROUND_TASKS` says `1`).
- A run whose loop is in the background fails at the wait before the message and says so; it no longer fails at the
  check after the message.
- A Porelo that announces the message while the loop still runs in the foreground still fails the check after the
  message.
- The first scenario in `wake.feature` and every other scenario are unchanged.

## What makes it uncertain

- The model may still not run the loop in the foreground, for example running it with a short timeout, or refusing;
  the scenario then fails at the first wait with a clear message, which is what the issue asks, but not green.
- Turning background tasks off may change the footer that `at_rest` reads (a shell count, a hint); the scenario
  would show it, and `claude.yaml` would not change for it, since only this image sets the variable.
- The variable is undocumented in the pin's sources and read in a later version; a new pin may rename it. The
  scenario's printenv check does not catch that, only a background loop seen again.
- An image of its own for this project costs one build more per leg, unless the guarded image of the scenario is
  already built for it alone.

## To be checked

- At the pin 2.1.311 on the VM, with `CLAUDE_CODE_DISABLE_BACKGROUND_TASKS=1` in the task's image: the Bash tool
  has no `run_in_background`, Monitor is gone, the until-loop runs in the foreground, and the footer at rest and at
  work still matches `at_rest` in `claude.yaml`.
- That an `ENV` line in a project's `run` reaches the process the tmux session starts, not only `podman exec`.

## Done

- The busy scenario's project sets `ENV CLAUDE_CODE_DISABLE_BACKGROUND_TASKS=1` in its own image, so there is no
  background to send the loop to; `claude.yaml` is unchanged. The wait before the message passes only on the loop in
  the foreground, and says when the loop runs in the background with the agent at rest.
- On the test VM on 2026-11-13 against 2.1.311, through the acceptance kit: green, the loop running in the foreground
  when the message arrived and the agent woken once at rest. Red with the variable taken out and the loop asked for in
  the background: it fails at the wait before the message, saying "the loop runs, but in the background: the agent is
  at rest".
- Both CI legs follow the push.
