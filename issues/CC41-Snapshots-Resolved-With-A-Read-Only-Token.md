# CC41 — Snapshots Resolved With A Read-Only Token, A Forge Without Its Registry Refused

**Status:** Implementing.
**Size:** S - the workflows and `settings.xml` of this one repository, no contract another
repository reads.

**What must be true.** On a forge other than the central one, no step of `porelo-claude-code`'s workflows that builds or tests
holds a token that can write the forge's package registry: Maven resolves snapshots with the read-only package token
the forge's organization holds for builds, the secret `PORELO_PACKAGES_READ_TOKEN` of scope `read:package` (a
placeholder under the same name on the central forge), and the publisher's token stays in the one step that deploys, on `main`. And a
Build on a forge other than the central one whose `MAVEN_SNAPSHOTS_URL` is empty fails at its start and says so,
instead of falling back to Central's snapshots.

## Acceptance

- On the local forge, a branch's `verify` runs with the read-only token only; a write to the registry with the token
  its steps hold is refused, measured once.
- A Build on the local forge with `MAVEN_SNAPSHOTS_URL` unset is seen to fail at its first step, naming the variable;
  on the central forge, where the variable names Central's snapshots, the Build is unchanged.

## To be checked

- That the read-only token reads the registry's snapshots where the organization there is private, with the user
  name Maven sends today.
