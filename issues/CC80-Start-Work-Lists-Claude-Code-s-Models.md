# CC80 — Start Work Lists Claude Code's Models

```json
{
  "$schema": "issue-1.json",
  "id": "CC80",
  "title": "Start Work Lists Claude Code's Models",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 2,
  "blockedBy": [],
  "covers": "where Anthropic gives a sign-in no model list, the definition's `models` names the aliases `--model` takes - sonnet, fable, opus, haiku - with the names Claude Code's `/model` shows, for Anthropic alone, so Start work offers them; kept from [CC79](README.md#cc79)"
}
```

**Why this size.** The definition's models section and its tests, once Porelo's contract has it; no code.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person starting work with Claude Code on its own sign-in picks the model from a list, never
types it.

## Why

Kept from [CC79](README.md#cc79), which built the effort and was narrowed to it. Porelo's contract lists a task's models by asking its provider through the broker, Anthropic's `GET /v1/models` giving `id` and
`display_name`; only where the provider is the agent's own sign-in with no list a key can read does the agent's own
list stand in, through a `models` section whose shape Agent Core posts with its code. Claude Code has no command that
lists its models: `--model` takes an alias (`fable`, `opus`, `sonnet`, `haiku`) or a full id.

## Plan

- **Measured:** Start work with Claude Code on a subscription sign-in (in the task the token arrived as
  `CLAUDE_CODE_OAUTH_TOKEN` beside `ANTHROPIC_BASE_URL`, with no `ANTHROPIC_API_KEY`) showed the model as free text
  and no list: Anthropic's `GET /v1/models` serves an API key, not that sign-in.
- **The shape Porelo reads:** a definition's `models`, keyed by provider, each a list of `id` and `name`; Porelo
  answers it with `from: agent` where the provider gives no list, the name as the label and no efforts, and a list
  from the provider wins. Keyed by provider, since an alias names a model only at Anthropic.
- **Built:** `claude.yaml` declares `models.anthropic` with `sonnet`, `fable`, `opus` and `haiku`, named and ordered
  as `/model` shows them on the pinned 2.1.311; a new pin reads `/model` again. `ModelsReadByPoreloTest` reads it
  through the agent API's own reader, which refuses the earlier flat list ("models must be a mapping").
- **Level:** unit tests against the agent API that carries the section, then the operator's look at Start work.

## Acceptance

- A task on Claude Code's own sign-in offers a model list at Start work, from the provider or from the definition.
