# CC63 — The Agent's Own Telemetry Goes To Porelo

```json
{
  "$schema": "issue-1.json",
  "id": "CC63",
  "title": "The Agent's Own Telemetry Goes To Porelo",
  "state": "Idea",
  "size": "-",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter declares Claude Code's own OpenTelemetry export pointed at Porelo's local receiver in the task, with every switch that would carry content - prompts, tool arguments and output, responses - set explicitly off"
}
```

**What must be true.** A task's Claude Code sends its own telemetry - its events, metrics and, where it has them, traces -
over OTLP to the receiver Porelo runs for the task, and to nowhere else. Every switch that would put content into that
telemetry (the person's prompts, tool arguments and output, the agent's responses) is set off explicitly in the
definition, never left to a default, so a new version that changes a default changes nothing here.

The contract built against: Porelo runs a local OTLP receiver for each task and keeps what arrives in the task's
records under its run; the adapter only declares how its agent is pointed at it.

What is known today, to be confirmed at the pinned version:
- `CLAUDE_CODE_ENABLE_TELEMETRY=1` turns it on: metrics, and events as OTLP logs; traces are a beta behind
  `CLAUDE_CODE_ENHANCED_TELEMETRY_BETA`.
- Each content switch is set explicitly; response logging can follow the prompt switch, so it too is named off.

## To be checked

- The exporter variables (endpoint, protocol, per-signal exporters) and every content switch at the pinned version.
- Whether the traces beta adds content of its own, and whether it is worth declaring at all.
