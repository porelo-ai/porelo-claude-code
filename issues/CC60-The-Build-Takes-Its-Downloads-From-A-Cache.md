# CC60 — The Build Takes Its Downloads From A Cache

```json
{
  "$schema": "issue-1.json",
  "id": "CC60",
  "title": "The Build Takes Its Downloads From A Cache",
  "state": "Implementing",
  "size": "S",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": [],
  "covers": "the Claude Code binary the update job checks and the runner's packages come from the Actions cache, keyed by digest or package manifest, so an outage of their hosts no longer fails a run that already ran once; what must stay live is named, and an entry nobody reads for 7 days is removed by GitHub"
}
```

**Why this size.** one composite action and three workflows of this repository; no contract another repository
reads.

**What must be true.** A Build, an update run or the workflow check that runs on a pin it has seen before takes every
outside download it can from the Actions cache, not from the vendor's download host or the runner's package mirrors: an
outage of one of those hosts then fails only the first run on a new pin. Each cached file is still checked against its
pin before it is used, and an entry of an old pin goes by itself, so the cache never fills up.

## Plan

- Already cached by the shared `pinned-jdk` action, and left as it is: the pinned GraalVM (`pinned-graalvm-<sha256>`),
  the wrapper's Maven and the Maven repository.
- `.github/actions/agent-downloads` keeps the pinned Claude Code binary that `update.yml` fetches for the renewal check
  under `agent-cli-<sha256>`; on a hit and on a miss the file goes through `pinned_fetch`, which keeps a file that
  matches its pin and fetches one that does not, and a verified fetch is saved at once.
- The runner's packages (`gcc`, `rpm`, `dpkg`, `zlib1g-dev` in `build.yml`, `python3-yaml` in `workflows.yml`): the
  package list is resolved from fresh, signed apt metadata, the `.deb` files are kept under
  `runner-apt-<runner image>-<manifest sha256>`, and each restored file is checked against the hash the metadata names
  before it is installed. The indexes stay live.
- Not cacheable, and kept online on purpose: the release pointer and manifests the update job reads to find a new
  release, the npm registry's license entry `check-pin` compares, the published bill of materials, the publication
  indexes on Artifactory (a cached one could approve replacing a release), Maven snapshots asked for anew with `-U`,
  and the Artifactory smoke test.
- Not cacheable from here: what the acceptance legs install on a rented machine (the OS packages, Porelo's packages,
  the task's base image and its packages, the CLI inside the task image). A rented machine has no Actions cache; that
  needs a staging step in the shared machine tooling, not this repository.
- Retention: GitHub removes a cache entry nobody read for 7 days, and every key carries the digest or the manifest's
  hash, so an old pin's entries go by themselves while the current ones are read on every run. No step deletes
  entries.

## Acceptance

- The second update run on an unchanged pin shows a cache hit for the Claude Code binary and makes no request to its
  download host for it; the second Build shows a hit for the runner's packages.
- A cache entry changed on purpose (one byte of the binary) is refused by its digest and fetched again.
- After a pin moves, the run writes an entry under the new digest, and the old one is gone once nobody read it for
  7 days.
