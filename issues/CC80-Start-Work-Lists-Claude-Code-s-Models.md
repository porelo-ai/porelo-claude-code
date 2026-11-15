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

- **The shape stands.** Porelo's `Models` (B795, porelo 6cdb7a158) asks the provider: `providers/anthropic.yaml`
  declares `GET /v1/models`, with `anthropic-beta: oauth-2025-04-20` beside a sign-in's token. Claude Code's
  provider is `anthropic` and its `--model` takes a full id, so the ids that list gives pass as they are. With an
  API key this already serves the list. An agent's own list (`from: agent`) is reserved in the contract, not built.
- **The one open fact:** whether a subscription sign-in's token reads `GET /v1/models`. Agent Core has not seen it;
  its tests only assume the beta header, and no test vault holds a sign-in. It is one check in the operator's live
  test: `Models` with `fresh` for `anthropic` on a machine whose vault holds the sign-in.
- **A list comes back:** this issue is done by B795 and closes with that evidence; nothing is declared here.
- **`ModelsFailed`:** this issue waits on Agent Core's "An Agent's Own Model List" (a definition's `models` section
  with `from: agent`, a static list of id and name, offered where the provider refuses the sign-in), and then the
  definition declares the aliases `fable`, `opus`, `sonnet` and `haiku` with readable names.
- **Level:** the operator's live check; unit tests of the definition only in the second case.

## Acceptance

- A task on Claude Code's own sign-in offers a model list at Start work, from the provider or from the definition.
