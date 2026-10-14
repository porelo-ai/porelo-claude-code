# CC30 — Switch Off The Agent's Own Memory In A Task

**Status:** Accepted; blocked by sokar [B194](https://github.com/sokar-ai/sokar/blob/main/issues/base/README.md#b194).

**What must be true.** Claude Code in a Sokar task keeps no memory of its own - nothing it writes down in one task
is read back in the next - and what holds across tasks reaches it only through `.AGENTS.md`, handed in read-only at
the task's start, so nothing a task wrote reaches the next one unseen.

From [PJ39](https://github.com/sokar-ai/sokar-project/blob/main/issues/README.md#pj39), where Sokar's own work moves into Sokar tasks and `.AGENTS.md` replaces the agents' memory.
