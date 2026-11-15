# CC79 — Start Work Offers Claude Code's Efforts

```json
{
  "$schema": "issue-1.json",
  "id": "CC79",
  "title": "Start Work Offers Claude Code's Efforts",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the definition declares the reasoning efforts Claude Code takes and the flag that passes one, so Start work offers them to pick instead of typing and an absent effort passes nothing; its own model list went to [CC80](README.md#cc80)"
}
```

**Why this size.** The agent definition and its tests, once Porelo's contract has the fields; no code beyond the
declaration.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person starting work with Claude Code picks the effort from the levels the CLI accepts, and
no effort picked passes nothing. The model list is [CC80](README.md#cc80)'s.

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

- The definition declares the effort levels and the flag, in the contract's shape, and today's reader still reads the
  definition.
- Open beyond this issue: a task started with a picked effort, once Porelo passes it, and the model list, [CC80](README.md#cc80).

## What was built

- `claude.yaml` declares the effort section of the agent contract that Agent Core handed over locally (porelo
  5328c5df4, not yet pushed): `effort: { arguments: ["--effort", "{effort}"], values: [low, medium, high, xhigh, max] }`.
  The values come from `claude --help` on 2.1.325, in its words and order; a start naming no effort passes nothing.
  Today's definition reader skips an unknown top-level section, so an older Porelo is not broken.
- No model list is declared; it went to [CC80](README.md#cc80). The contract lists a task's models by asking its provider through the broker
  (Anthropic's `GET /v1/models` gives `id` and `display_name`), and only an agent whose own sign-in has no such list
  declares one of its own. Claude Code has no command that lists its models, so that declaration is left until the
  contract's `models` section stands and shows that a subscription sign-in needs it. The aliases (`fable`, `opus`,
  `sonnet`, `haiku`) would then be its content.
- The reading by another model found the issue still promising a model list it did not build; this issue was narrowed
  to the effort and the model list filed as [CC80](README.md#cc80). Test: `EffortDeclarationTest`, 2 tests, read from the definition itself since the API record does not hold the
  section yet. Both seen red first; a value taken out on purpose was seen red and restored identical. Unit suite 306
  run, 0 failures, 0 skipped.
- Checked on the pin: `claude --help` of 2.1.311, run offline in the task image on the test VM (2026-11-15), names
  `--effort <level>` with low, medium, high, xhigh and max - the declared flag and values.
