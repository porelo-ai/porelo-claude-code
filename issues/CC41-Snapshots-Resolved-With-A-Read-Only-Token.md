# CC41 — Snapshots Resolved With A Read-Only Token, A Forge Without Its Registry Refused

**Status:** Accepted.

**What must be true.** On a forge other than the central one, no step of `porelo-claude-code`'s workflows that builds or tests
holds a token that can write the forge's package registry: Maven resolves snapshots with the read-only package token
the forge's runner hands to builds, and the publisher's token stays in the one step that deploys, on `main`. And a
Build on a forge other than the central one whose `MAVEN_SNAPSHOTS_URL` is empty fails at its start and says so,
instead of falling back to Central's snapshots.

## Acceptance

- On the local forge, a branch's `verify` runs with the read-only token only; a write to the registry with the token
  its steps hold is refused, measured once.
- A Build on the local forge with `MAVEN_SNAPSHOTS_URL` unset is seen to fail at its first step, naming the variable;
  on the central forge, where the variable names Central's snapshots, the Build is unchanged.

## To be checked

- The name and the scope of the read-only token the forge's runner hands to builds, and that it reads the
  registry's snapshots where the organization there is private.
