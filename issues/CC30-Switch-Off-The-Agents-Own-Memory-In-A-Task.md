# CC30 — Switch Off The Agent's Own Memory In A Task

**Status:** Accepted; blocked by porelo [B194](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b194).

**What must be true.** Claude Code in a Porelo task keeps no memory of its own - nothing it writes down in one task
is read back in the next - and what holds across tasks reaches it only through `.AGENTS.md`, handed in read-only at
the task's start, so nothing a task wrote reaches the next one unseen.

From [PJ39](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj39), where Porelo's own work moves into Porelo tasks and `.AGENTS.md` replaces the agents' memory.
