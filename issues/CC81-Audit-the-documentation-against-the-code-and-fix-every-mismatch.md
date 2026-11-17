# CC81 — Audit the documentation against the code and fix every mismatch

```json
{
  "$schema": "issue-1.json",
  "id": "CC81",
  "title": "Audit the documentation against the code and fix every mismatch",
  "state": "Under estimation",
  "size": "M",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "Audit the documentation against the code and fix every mismatch"
}
```

**What must be true.** Every claim in README.md, doc/index.md, doc/decisions.md, build.md and the repository's own section of AGENTS.md (not the shared rule blocks) has been checked against the code, the agent definition (src/main/resources/agent/claude.yaml), the build (pom.xml, porelo.yml) and the workflows.

**Size:** M - About 900 lines in one repository, documents only: roughly 250 claims to check and record, about eight known mismatches to rewrite, and two new issues to open. No code or contract changes.

Where a document and the code disagree, the code wins: the document is rewritten to say what the code does now. Where the code itself looks wrong, a new issue records it and the code is not changed here.

The audit's record lists each claim checked, what it was checked against, and whether it was measured or inferred, so a reader can repeat any check.

After the fixes, a reader following the documents (install, sign in, start a task, read vault.log, build the package, move the pinned Claude Code version) meets no statement that the code contradicts.

## Plan

### What is audited, and against what

Five documents: `README.md` (36 lines), `doc/index.md` (80), `doc/decisions.md` (532, 23 sections), `build.md` (280) and AGENTS.md's own two sections, *Work in this repository* and *What this repository is* (lines 586 to 599). The shared blocks above them are left out. Each claim is checked against one of these sources:

- the agent definition `src/main/resources/agent/claude.yaml`, after filtering (`target/classes/agent/claude.yaml`), which is the file `describe` answers with;
- the Java in `src/main/java/org/fuin/porelo/agent/impl/claude/`: `ClaudeSettings`, `ClaudeFirstRun`, `ClaudeContainerSetup`, `ClaudeCommitTrailer`, `ClaudeFindings`, `ClaudeCredentialExtractor`, `ClaudeSignInFacts`, `ClaudeStreamJsonFormatter`, `ClaudeToolEvents`, `ClaudeAgent`;
- the build: `pom.xml` (properties, the profiles `documents`, `native`, `dist`, `acceptance` and `pit`, with `ci-tools` coming from `porelo-parent`), `src/deb/control/control`, `settings.xml`, `.mvn/`, `porelo.yml`;
- the workflows: `build.yml`, `update.yml`, `artifactory-smoke.yml`, `shared-rules.yml`, `workflows.yml`, `delete-runs.yml` and `.github/actions/pinned-jdk`;
- the tests and features the documents name, such as `PinAgreementTest`, `SessionIdDeclarationTest`, `UpstreamSnapshotsTest`, `ReleaseChannelTest`, `UpdateRulesTest`, `DocumentTestsTaggedTest`, `session.feature` and `subscription.feature`. Checking these only confirms that they exist and assert what the text says.

### Steps

1. **Claim list first.** Split each document into atomic claims and give each an id per document (`R1…`, `I1…`, `D1…`, `B1…`, `A1…`). For each, write the source it is checked against, as file and line, and how it was checked:
   - **measured**: a command was run, or a file was read and its value compared;
   - **inferred**: read in the code but not run;
   - **outside**: the claim is about Porelo's own CLI or vault (`porelo vault login`, `vault.log` lines, `--token-hours`, `porelo project default`) or about the vendor, so no code here can settle it.

   The operator decides where this record lives (see open questions). The planner's default is a table under the issue's own *Audit* section.
2. **Measured checks that can be run locally**, offline: `./mvnw -o -s settings.xml verify` for the filtered definition and `PinAgreementTest`; `-Pnative,dist verify` for the names of the `target/` artifacts that build.md lists; `dpkg-deb -I` and `rpm -qip` for the description line build.md promises; `mkdocs build --strict` for the anchors in the decisions table.
3. **Fix the documents.** The code wins, so each mismatch below is rewritten in the document to say what the code does now. Then the `documents` tests run alone, `./mvnw -o -B -s settings.xml -Pdocuments test`, together with the full `verify`.
4. **Where the code looks wrong**, open a new issue here and leave the code alone. Candidates found in this analysis are listed below.
5. **The walk-through the issue asks for.** Read install, sign in, start a task, read vault.log, build the package and move the pin in order, and confirm that no remaining statement is contradicted by any source in step 1.

### Mismatches already found while reading (to fix)

- **build.md, Publishing**: says a push to `main` is what publishes. `build.yml` also builds a tag `v*` and publishes it to the `releases` channel in its own `release` job, after every acceptance leg. The text is silent on tags.
- **build.md, Publishing**: says "Three repository settings are needed" (`PORELO_PUBLISH`, `JF_URL`, `JF_ACCESS_TOKEN`). The workflows also read `PORELO_PACKAGES_READ_TOKEN`, `MAVEN_SNAPSHOTS_URL`, `MAVEN_UPSTREAM_SNAPSHOTS_URL`, `PORELO_MACHINES_API`, `PORELO_MACHINES_SSH`, `PORELO_POOL_SSH`, `OPEN_ROUTER_API_KEY`, `PORELO_UPDATE_TOKEN` and `PORELO_UPDATE_AUTO_MERGE`. The fix lists them, or says that the three are only the publishing ones.
- **build.md, Acceptance**: says the tier 2 scenarios are skipped without the key, "so a fork or a revoked key loses coverage rather than turning the build red". In CI, `build.yml` (around line 406) fails the leg when the secret `OPEN_ROUTER_API_KEY` is empty. Skipping holds only for a local run.
- **build.md, Acceptance**: says CI runs the suite "on every push to `main`". `build.yml` also runs it on a tag (always its own legs) and on a pull request or dispatch, unless already proven.
- **build.md, Bumping**: the YAML excerpt shows `sha256: "6c8818fa…"`. The pinned digest is `86dc1fcc…`. Either show a placeholder or drop the excerpt and point to the file.
- **doc/decisions.md, "Reaching work is checked by what the screen shows"**: says the attended step "waits its 300 s". `claude.yaml` declares `ready_within_seconds: 120`.
- **doc/decisions.md, "The release tooling is Porelo's"**: names five `porelo.release.*` facts. `pom.xml` has seven; `license` and `min-age` are missing from the text.
- **doc/decisions.md, "The CLI does not update itself"**: says `claude` on the task's path is root's link. Since the `as_agent` lines were added, `/home/agent/.local/bin/claude` also exists, a link through the agent's home to root's binary. The text has to name both links, and what a forced update could change depends on `PATH` order (see the new issue below).

### Candidates for new issues (code looks wrong; not changed here)

- **The `refused_domains` comment in `claude.yaml`** says "the pinned binary is fetched when the package is built". The `install.artifacts` entry fetches it when the task image is built, and `doc/index.md` says the same. This is a comment in the agent definition, so the fix is a code change.
- **The agent's own links to the CLI.** `as_agent` puts `~/.local/bin/claude` and `~/.local/share/claude/versions/<v>` in the agent's writable home. If `~/.local/bin` comes before `/usr/local/bin` on `PATH`, a forced `claude update` that relinks them would run unpinned bytes on the next start. That contradicts the decision's "the next start still runs the pinned file". This needs a measurement in a task.

### The test that goes red first

None is planned as a new test: the change is to documents. The record lets anyone repeat each check. `DocumentTestsTaggedTest` and the `documents` profile stay green throughout.

## To be checked

- Where should the audit record live: a table in the [CC81](README.md#cc81) issue file, a new section of doc/decisions.md, or a separate file under doc/? The planner's default is the issue file.
- Should claims about Porelo itself (vault login, the vault.log lines, --token-hours, porelo project default, porelo agents --verbose) be measured on a rented machine with Porelo installed, or recorded as outside this repository's code and inferred?
- doc/decisions.md carries dates and a role-named acceptance ('by the coordinator on 2026-11-12', the measurement dates). The shared rule says documentation has no dates or history. Should the audit remove them, or is a measurement's date part of its record?
- Is a wrong comment in claude.yaml (the refused_domains note about when the binary is fetched) a documentation fix inside [CC81](README.md#cc81), or a code change for a new issue as the issue's text requires?
- Should build.md list every secret and variable the workflows read, or only the publishing ones, with the rest named per workflow?

## Findings

- build.md's Publishing section says only a push to main publishes; build.yml also publishes a v* tag to the releases channel in its release job.
- build.md names three repository settings; the workflows read at least nine more secrets and variables, among them PORELO_PACKAGES_READ_TOKEN, PORELO_MACHINES_API and OPEN_ROUTER_API_KEY.
- build.md says the tier 2 scenarios are skipped without the OpenRouter key; in CI, build.yml fails the leg when the secret OPEN_ROUTER_API_KEY is empty.
- build.md says CI acceptance runs on every push to main; build.yml also runs it on tags, pull requests and dispatches.
- build.md's excerpt of claude.yaml shows an old digest (6c8818fa); the pinned one is 86dc1fcc.
- doc/decisions.md says the attended ready step waits 300 s; claude.yaml declares ready_within_seconds 120.
- doc/decisions.md lists five porelo.release properties; pom.xml has seven (license and min-age are missing).
- doc/decisions.md says claude on the task's path is root's link; the as_agent lines also add a link in the agent's home, ~/.local/bin/claude, and whether a forced update can win depends on PATH order (unmeasured).
- A comment in claude.yaml says the pinned binary is fetched when the package is built; install.artifacts fetches it at image build, as doc/index.md says. Candidate new issue.
- -Pci-tools, named in build.md and AGENTS.md, is not in this pom.xml; inferred to come from porelo-parent, to be confirmed against the effective pom.
- The README, doc/index.md's network list (allowed and refused hosts) and AGENTS.md's own section agree with claude.yaml, pom.xml and the documents profile, as far as read.
- The work order asked for one sub-agent per issue; this session has no tool to start one, so [CC81](README.md#cc81) was analyzed directly in the main session.
- Outcome: [CC81](README.md#cc81) is analyzed, size M, with five questions for the operator; it is ready to move to Analyzed.
