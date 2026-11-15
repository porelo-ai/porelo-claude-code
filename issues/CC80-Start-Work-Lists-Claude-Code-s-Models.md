# CC80 — Start Work Lists Claude Code's Models

```json
{
  "$schema": "issue-1.json",
  "id": "CC80",
  "title": "Start Work Lists Claude Code's Models",
  "state": "Backlog",
  "size": "S",
  "priority": "High",
  "rank": 2,
  "blockedBy": [],
  "covers": "where a task's provider is Claude Code's own sign-in and no list can be read from the provider, the definition declares the models Claude Code takes - its aliases and the pin's full ids with readable names - in the contract's models section, so Start work offers them; kept from [CC79](README.md#cc79)"
}
```

**Why this size.** The definition's models section and its tests, once Porelo's contract has it; no code.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person starting work with Claude Code on its own sign-in picks the model from a list, never
types it.

## Why

Part of ST369. Kept from [CC79](README.md#cc79), which built the effort and was narrowed to it. Porelo's contract
(B795) lists a task's models by asking its provider through the broker, Anthropic's `GET /v1/models` giving `id` and
`display_name`; only where the provider is the agent's own sign-in with no list a key can read does the agent's own
list stand in, through a `models` section whose shape Agent Core posts with its code. Claude Code has no command that
lists its models: `--model` takes an alias (`fable`, `opus`, `sonnet`, `haiku`) or a full id.

## Plan

- **Wait for the shape.** Agent Core posts the `models` section; until then this issue is planned, not built.
- **Then:** whether a subscription sign-in can read `GET /v1/models`. If it can, nothing is declared and this issue
  closes with that evidence. If not, the definition declares the aliases with readable names, the form the section
  takes for a static list.
- **Level:** unit tests of the definition.

## Acceptance

- A task on Claude Code's own sign-in offers a model list at Start work, from the provider or from the definition.
