# Issues

What is open in this repository, grouped by the MVP (`sokar-project` PJ17): **Now**
serves one person, one machine, one agent, from install to a reviewed push; **Soon** follows right
after it; **Later** is the rest. Within a group, ordered by what to do next rather than by number. The
number is identity, not sequence.

## Now

None open.

## Soon

| # | Status | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|---|
| [CC23](CC23-Sign-The-RPMs-With-Sokars.md) | open | S | sokar B99 | package-level signatures on the published RPMs | 1 |

## Later

| # | Status | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|---|
| [CC22](CC22-Depend-On-The-Agent-Protocol-Sokar-Provides.md) | open | S | Sokar (to be opened) | the agent contract named at install time | 1 |
| [CC24](CC24-Prove-OAuth-Isolation-End-To-End.md) | open | M |  | a subscription's refresh token never in a task | 2 |

## Where an agent's requirements live

An agent's work lives in its own repository, so a requirement for this agent is an issue here, and
what outlived the ones already met is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `sokar-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
