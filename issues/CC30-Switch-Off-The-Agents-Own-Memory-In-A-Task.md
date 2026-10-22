# CC30 — Switch Off The Agent's Own Memory In A Task

**Status:** Review.
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
  `permissions`, `skipDangerousModePermissionPrompt` and `env`. `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1` goes into
  its `env`, with a comment saying why.
- Red first: a test in `ClaudeContainerSetupTest`, beside the ones reading `DISABLE_UPDATES`, asserting
  the variable in the settings document of a task; seen red before it is written.
- `doc/decisions.md` gets a row: the agent's own memory is off in a task, and `AGENTS.md` is the one
  place what holds across tasks is kept.
- The changelog says it under `[Unreleased]`, since it changes what ships.

## Acceptance

- On the VM, an attended task on the pinned CLI shows "Auto-memory false" in its `/memory` menu, read by a
  scenario in `terminal.feature` that asks no model.
- Seen to fail first: the same scenario on the adapter without the switch shows "Auto-memory true".

## Measured

On Claude Code 2.1.292, in tasks of porelo 0.5.1~snapshot.307, with no model asked:

- The CLI knows two switches, the setting `autoMemoryEnabled` and the variable `CLAUDE_CODE_DISABLE_AUTO_MEMORY`.
  Before this change `/memory` said *"Auto-memory true"*. Either switch, and the variable in the `env` of
  `settings.json`, made it say *"false"*. With the variable, Enter on that menu entry left it false and the file
  unchanged, so the adapter writes the variable.
- A file left in one task's `/home/agent` was gone in the next task, `~/.claude/projects` with it. So today a
  memory outlives only a stop and start of the same task.
- The new scenario in `terminal.feature` failed against the adapter installed before, showing "Auto-memory true",
  and passed with this change's adapter installed.

## Measured after the push

- Build 142 on `11413ef` was green in every job, its proof, acceptance and `shared-rules` jobs on `ubuntu-26.04`,
  and `0.5.2~snapshot.142` is in the published index.
