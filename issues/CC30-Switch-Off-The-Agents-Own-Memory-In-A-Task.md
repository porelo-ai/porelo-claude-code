# CC30 — Switch Off The Agent's Own Memory In A Task

**Status:** Backlog.
**Size:** S - one key in the settings file the adapter already writes, its unit test, and one attended
measurement on the VM.

**What must be true.** Claude Code in a Porelo task keeps no memory of its own: nothing it writes down in
one task is read back in a later one. What holds across tasks reaches it only through the repository's
`AGENTS.md`, changed by a commit that is reviewed like any other, so nothing a task wrote reaches the next
one unseen.

From [PJ39](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj39), where Porelo's
own work moves into Porelo tasks and what an agent learns goes into its repository's `AGENTS.md`.

## Plan

- `ClaudeSettings.document()` writes `/home/agent/.claude/settings.json` into every task today, with
  `permissions`, `skipDangerousModePermissionPrompt` and `env`. The key that switches the CLI's automatic
  memory off goes there beside them, with a one-line comment saying why.
- Red first: a test in `ClaudeContainerSetupTest`, beside the ones reading `DISABLE_UPDATES`, asserting
  the key and its value in the settings document of a task; seen red before the key is written.
- `doc/decisions.md` gets a row: the agent's own memory is off in a task, and `AGENTS.md` is the one
  place what holds across tasks is kept.
- The changelog says it under `[Unreleased]`, since it changes what ships.

## Acceptance

- On the VM, in `smith`, an attended task on the pinned CLI is told to remember a fact. Its memory is
  reported off by the CLI, nothing is written under `/home/agent/.claude/projects/*/memory/`, and a
  second task of the same project is not told the fact.
- Seen to fail first: the same steps on the adapter without the key write the memory file.

## To be checked

- Which key the pinned CLI reads to switch automatic memory off - a setting or an `env` variable - and
  that it is honored, measured on the VM, not read from documentation.
- Whether anything under `/home/agent` survives from one task to the next, which decides whether the
  second-task step can fail at all without the key.
