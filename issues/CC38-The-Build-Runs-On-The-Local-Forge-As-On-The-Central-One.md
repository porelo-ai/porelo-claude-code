# CC38 — The Build Runs On The Local Forge As On The Central One

**Status:** Idea.

**What must be true.** `porelo-claude-code`'s workflows run green on the local Forgejo and on the central forge on the same
commit, with no edit between them: the machines its legs rent are asked for by kind, and the registry and the
provider are variables of the setup.

From porelo-project [PJ60](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj60), for the move's wave 7a; the kinds come from porelo-buildtools [BT25](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt25).

## Acceptance

- Seen to fail first: today's `build.yml` on the local Forgejo.
- The kinds' check finds no provider's name in this repository's workflows.
