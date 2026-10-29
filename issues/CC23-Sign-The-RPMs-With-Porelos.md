# CC23 — Sign the RPMs, together with Porelo's

**Status:** Estimated; waits for Porelo's packaging key, the CI secret that carries it and the day every
repository switches to signed RPMs.
**Size:** S - here one pom property and a key step in the release job; the key, its CI secret, the day
every repository switches and the legs that install with `gpgcheck=1` are Porelo's signing change.

**What must be true.** A Fedora user installs this adapter with `gpgcheck=1`, and a package whose
signature does not fit is refused.

## Why

`rpm.sign.skip` is `true` everywhere, releases included, and the Fedora `.repo` file uses
`gpgcheck=0`. Porelo's own RPMs are unsigned the same way and say so (`porelo`
`doc/getting-started.md`); the repository metadata is what is signed. Signing this adapter alone
changes nothing for a user, because both share one repository and one `.repo` file.

## Plan

- **Nothing here moves before Porelo's signing change** has named the packaging key, the CI secret that carries it and the
  day `porelo`, the transport, the filter and the three agents switch together.
- **`pom.xml`:** `rpm.sign.skip` to `false`, and the rpm plugin's `<signature>` block given the key
  name that change sets.
- **`.github/workflows/build.yml`:** the job that builds `-Pnative,dist` imports the key from that
  secret first, for snapshots and releases alike, and removes it in an `always()` step.
- **The acceptance legs** (`AgentLeg` in `porelo-buildtools`) set `gpgcheck=1`, `repo_gpgcheck=1` and
  `gpgkey=` as part of that change, and this repository's acceptance runs with them.
- **Before the upload:** the publish job runs the release tools' shared `check-signed`, which refuses an
  unsigned package, as that change plans for every repository.
- **Red first:** `rpm -K` on the built `.rpm` says it is unsigned; green once it is signed by that key.

## Acceptance

- The release job imports the signing key and builds with `-Drpm.sign.skip=false`, in `porelo` and in
  the three agents.
- The `.repo` file uses `gpgcheck=1` with `gpgkey=`, and an acceptance leg installs with it on; an
  unsigned or re-signed package is seen to be refused.
