# CC81 — Audit the documentation against the code and fix every mismatch

```json
{
  "$schema": "issue-1.json",
  "id": "CC81",
  "title": "Audit the documentation against the code and fix every mismatch",
  "state": "Estimated",
  "size": "M",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "Audit the documentation against the code and fix every mismatch"
}
```

**What must be true.** Every claim in README.md, doc/index.md, doc/decisions.md, build.md and the repository's own section of AGENTS.md (not the shared rule blocks) has been checked against the code, the agent definition (src/main/resources/agent/claude.yaml), the build (pom.xml, porelo.yml) and the workflows.

**Size:** M - Documents only in one repository: about 930 lines, about 250 claims to record, eight confirmed mismatches to rewrite, two new issues; no code or contract change. Long record and native build push it toward the M p80.

**Estimate:** 53 min

Where a document and the code disagree, the code wins: the document is rewritten to say what the code does now. Where the code itself looks wrong, a new issue records it and the code is not changed here.

The audit's record lists each claim checked, what it was checked against, and whether it was measured or inferred, so a reader can repeat any check.

After the fixes, a reader following the documents (install, sign in, start a task, read vault.log, build the package, move the pinned Claude Code version) meets no statement that the code contradicts.

## Plan

### Estimate

M at 53 minutes, the measured median of M over all repositories (p80 114). The work is documents only: five files of about 930 lines, roughly 250 atomic claims, eight known mismatches to rewrite and two new issues to open. No code or contract changes. Expect more than the median, toward the p80, because the claim record (source file and line, plus how each claim was checked) is long. The offline native and dist build that checks the artifact names also adds machine time. Per the shared rule, these reasons do not change the number.

### Spot checks made while estimating

- doc/decisions.md line 226 says the attended step waits 300 s; claude.yaml line 106 declares ready_within_seconds 120. Confirmed.
- build.md line 43 shows the digest 6c8818fa; claude.yaml line 212 pins 86dc1fcc. Confirmed.
- build.yml line 406 fails the leg when OPEN_ROUTER_API_KEY is empty, so build.md's claim that tier 2 is skipped holds only locally. Confirmed.
- build.md line 278 says acceptance runs on every push to main; tags, pull requests and dispatches are not mentioned. Confirmed.
- doc/decisions.md line 337 lists five porelo.release facts; pom.xml lines 53 to 60 hold seven. min-age is described later in the same document (line 456), so the fix is to the list, not a missing fact.
- claude.yaml line 195 says the binary is fetched when the package is built. Confirmed as a candidate new issue.
- -Pci-tools is named in build.md line 57 and AGENTS.md line 562 but is not in this pom.xml, so it must come from porelo-parent. Not yet confirmed against the effective pom.

### Where the time goes

1. Claim list with sources: about half of the time.
2. Offline verify, -Pnative,dist verify, dpkg-deb, rpm and mkdocs checks: mostly machine time.
3. Rewriting the eight mismatches and running the documents profile: about a quarter.
4. Two new issues and the reader walk-through: the rest.

## To be checked

- The audit record's home (issue file, decisions.md or a new doc file) changes how much is written; the plan assumes the issue file.
- If the operator wants claims about Porelo's vault and CLI measured on a rented machine, not marked outside, the work grows past M and needs a VM.
- Whether the dates in doc/decisions.md are removed under the no-history rule adds to or keeps the rewrite.
- The native and dist build offline may need a GraalVM and packaging tools that are not in the task image; if missing, artifact names are inferred.
- PATH order for the agent's own claude link needs a measurement in a task; the plan leaves it to the new issue, and the estimate assumes it stays there.

## Findings

- Velocity used: XS 9, S 27, M 53 (p80 114) minutes, the medians over all repositories quoted in this repository's issues; no L median is recorded here.
- Confirmed by reading: decisions.md says 300 s while claude.yaml declares 120; build.md shows digest 6c8818fa while 86dc1fcc is pinned; build.yml fails on an empty OpenRouter key.
- Correction to the plan: min-age is described at doc/decisions.md line 456, so only the list of five facts at line 337 is short (license and min-age missing there).
- Outcome: [CC81](README.md#cc81) is estimated at M, 53 minutes, with a risk toward the M p80 of 114 minutes; ready to move to Estimated.
