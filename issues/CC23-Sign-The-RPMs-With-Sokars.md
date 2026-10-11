# CC23 — Sign the RPMs, together with Sokar's

**Status:** soon; blocked by sokar B99.
**Size:** S - here one pom property and a key step in the release job; the key, its CI secret, the day
every repository switches and the legs that install with `gpgcheck=1` are B99's.

**What must be true.** A Fedora user installs this adapter with `gpgcheck=1`, and a package whose
signature does not fit is refused.

## Why

`rpm.sign.skip` is `true` everywhere, releases included, and the Fedora `.repo` file uses
`gpgcheck=0`. Sokar's own RPMs are unsigned the same way and say so (`sokar`
`doc/getting-started.md`); the repository metadata is what is signed. Signing this adapter alone
changes nothing for a user, because both share one repository and one `.repo` file.

## Plan

- **Nothing here moves before B99** has named the packaging key, the CI secret that carries it and the
  day `sokar`, the transport, the filter and the three agents switch together.
- **`pom.xml`:** `rpm.sign.skip` to `false`, and the rpm plugin's `<signature>` block given the key
  name B99 sets.
- **`.github/workflows/build.yml`:** the job that builds `-Pnative,dist` imports the key from that
  secret first, for snapshots and releases alike, and removes it in an `always()` step.
- **The acceptance legs** (`AgentLeg` in `sokar-buildtools`) set `gpgcheck=1`, `repo_gpgcheck=1` and
  `gpgkey=` as part of B99, and this repository's acceptance runs with them.
- **Red first:** `rpm -K` on the built `.rpm` says it is unsigned; green once it is signed by that key.

## Acceptance

- The release job imports the signing key and builds with `-Drpm.sign.skip=false`, in `sokar` and in
  the three agents.
- The `.repo` file uses `gpgcheck=1` with `gpgkey=`, and an acceptance leg installs with it on; an
  unsigned or re-signed package is seen to be refused.

## To be checked

- Whether the check that refuses an unsigned RPM before it is published is a step here or one shared
  check from `sokar-buildtools` - Agent Core's, with B99.
