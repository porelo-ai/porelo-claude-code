# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Analyzed

| # | Status | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|---|
| [CC23](CC23-Sign-The-RPMs-With-Sokars.md) | Analyzed | S | sokar B99 | package-level signatures on the published RPMs | 1 |
| [CC24](CC24-Prove-OAuth-Isolation-End-To-End.md) | Analyzed | M |  | a subscription's refresh token never in a task | 2 |
| [CC22](CC22-Depend-On-The-Agent-Protocol-Sokar-Provides.md) | Analyzed | S | Sokar (to be opened) | the agent contract named at install time | 1 |

## Where an agent's requirements live

An agent's work lives in its own repository, so a requirement for this agent is an issue here, and
what outlived the ones already met is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `sokar-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
