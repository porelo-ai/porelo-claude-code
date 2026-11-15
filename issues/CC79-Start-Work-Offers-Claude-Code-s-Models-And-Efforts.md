# CC79 — Start Work Offers Claude Code's Models And Efforts

```json
{
  "$schema": "issue-1.json",
  "id": "CC79",
  "title": "Start Work Offers Claude Code's Models And Efforts",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter declares the models Claude Code takes - aliases and full ids with readable names - and the effort levels it accepts with how they are passed, so Start work offers them to pick instead of typing; Claude Code has no command that lists them"
}
```

**Why this size.** The agent definition and its tests, once Porelo's contract has the fields; no code beyond the
declaration.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person starting work with Claude Code picks the model from a list of what the pinned CLI
takes, and the effort from the levels it accepts, with "default" passing nothing.

## Why

Part of ST369, *Findings of the second live test on a rented host* (High). The operator wants to pick the model and
the reasoning effort from what the chosen agent offers, not type them. Codex (`codex debug models`) and omp
(`omp models ls --json`) list their models with the effort each takes; Claude Code has no such command. Its
`--model` takes an alias (`fable`, `opus`, `sonnet`, `haiku`) or a full id, and its `--effort` takes `low`,
`medium`, `high`, `xhigh` or `max`, read from the help of 2.1.325. No adapter passes an effort today.

## Plan

- **Contract first.** Porelo's definition gets the fields for a model list and an effort flag, agreed by Agent Core
  and Agent Planner. Until then this issue is planned, not built.
- **Declared here:** the aliases and the full ids the pin takes, each with a readable name, and the effort levels with
  `--effort` as the flag; "default" passes nothing. Whether the broker should rather ask the provider (Anthropic's
  `GET /v1/models` gives `id` and `display_name`) is the contract's choice, which then makes the declared list only
  the aliases.
- **To be checked on 2.1.311:** that `--effort` exists with these levels, and which models refuse which level.
- **Level:** unit tests of the definition; one live start with a picked model and effort through the coordinator.

## Acceptance

- The definition declares the models and the effort levels, and a task started with a picked pair runs with them.
