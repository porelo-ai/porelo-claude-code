# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Review

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc31"></a>[CC31](CC31-Rename-The-Adapter-Porelo.md) | S | — | the adapter renamed Porelo, by script, with its exceptions |

## Analyzed

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cc29"></a>[CC29](CC29-Read-The-Code-Changed-Since-The-Last-Skills-Review.md) | M | — | the code changed since 2026-09-27 read against the seven skills | 0 |

## Accepted

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc30"></a>[CC30](CC30-Switch-Off-The-Agents-Own-Memory-In-A-Task.md) | — | porelo [B194](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b194) | Claude Code's own memory off in a task; `.AGENTS.md` in its place |
| <a id="cc32"></a>[CC32](CC32-Answer-The-Auto-Mode-Question-At-First-Run.md) | — | — | the new first-run question about auto mode answered, so the pin can move |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc24"></a>[CC24](CC24-Prove-OAuth-Isolation-End-To-End.md) | M | porelo [B173](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b173) | a subscription's refresh token never in a task |
| <a id="cc23"></a>[CC23](CC23-Sign-The-RPMs-With-Porelos.md) | S | porelo [B99](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b99) | package-level signatures on the published RPMs |
| <a id="cc22"></a>[CC22](CC22-Depend-On-The-Agent-Protocol-Porelo-Provides.md) | S | Porelo (to be opened) | the agent contract named at install time |

## Where an agent's requirements live

An agent's work lives in its own repository, so a requirement for this agent is an issue here, and
what outlived the ones already met is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `porelo-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
