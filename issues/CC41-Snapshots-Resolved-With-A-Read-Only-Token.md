# CC41 — Snapshots Resolved With A Read-Only Token, A Forge Without Its Registry Refused

**Status:** Review; the central forge's side built, the local forge's checked when the repository moves there.
**Size:** S - the workflows and `settings.xml` of this one repository, no contract another
repository reads.

**What must be true.** On a forge other than the central one, no step of `porelo-claude-code`'s workflows that builds or tests
holds a token that can write the forge's package registry: Maven resolves snapshots with the read-only package token
the forge's organization holds for builds, the secret `PORELO_PACKAGES_READ_TOKEN` of scope `read:package` (a
placeholder under the same name on the central forge), and the publisher's token stays in the one step that deploys, on `main`. And a
Build on a forge other than the central one whose `MAVEN_SNAPSHOTS_URL` is empty fails at its start and says so,
instead of falling back to Central's snapshots.

## Plan

- **No step here deploys to the forge's registry**: the packages go to Artifactory, so the publisher's token leaves
  every workflow, and each step that held it for Maven - the `pinned-jdk` action's caller, the build, the tooling,
  the packages and the acceptance legs in `build.yml` and `update.yml`, `workflows.yml` and `shared-rules.yml` -
  holds `secrets.PORELO_PACKAGES_READ_TOKEN` instead. `settings.xml`'s `porelo-forge` server sends it as the
  publisher account's password, the account the read-only token belongs to.
- **The `pinned-jdk` action, the first thing every job that runs Maven does, starts with *The forge names its
  snapshot repository***: outside `https://github.com` an empty or unset `MAVEN_SNAPSHOTS_URL` stops the job and
  names the variable; on the central forge it changes nothing.
- **Red first:** `SnapshotTokenTest`, self-contained so the same file stands in every adapter, failed three of its
  four cases on the unchanged tree (no check in the action; the read-only token in no workflow) and passes after.
  Each guard was broken on purpose and seen red: the publisher's token put back into one step of `build.yml`; the
  check turned to run only on the central forge; the check without its forge condition, which stops the central
  forge too.
- **The reading against `security-audit`, `clean-code`, `java-code-review` and `test-quality` (2026.906.3)** found
  four things, each now held by a case seen red: a `MAVEN_SNAPSHOTS_URL` of white space only passed the
  check; the scan missed the token in bracket notation, so it now looks for the bare name in every file of steps
  and in `settings.xml`; the check could have been moved behind a Maven call or given an `if:`, so it must be the
  action's first step and unconditional; and the test's scripts are now kept outside the step's directory and
  ended with their children. One is declined: that every Maven step holds the read-only token under the right
  name is not guarded, since a step without it fails on the forge with a 401 and gives no write token away.

## Acceptance

- On the local forge, a branch's `verify` runs with the read-only token only; a write to the registry with the token
  its steps hold is refused, measured once.
- A Build on the local forge with `MAVEN_SNAPSHOTS_URL` unset is seen to fail at its first step, naming the variable;
  on the central forge, where the variable names Central's snapshots, the Build is unchanged.

## To be checked

- On the local forge, once the repository moves there: a branch's `verify` green with the read-only token, a
  write to the registry with it refused, and a Build with `MAVEN_SNAPSHOTS_URL` unset stopped at its first step.
- That the read-only token reads the registry's snapshots where the organization there is private, with the user
  name Maven sends today.
