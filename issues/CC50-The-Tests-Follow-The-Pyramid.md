# CC50 — The Tests Follow The Pyramid

```json
{
  "$schema": "issue-1.json",
  "id": "CC50",
  "title": "The Tests Follow The Pyramid",
  "state": "Analyzed",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the suites by level with measured times, no flow of the adapter only above the unit level; five end-to-end scenarios that test only Porelo leave the legs, and the timeout test waits 100 ms"
}
```

**Why this size.** two scenario files and one test class, no contract.

**What must be true.** Every flow this adapter owns is proven by unit tests that run in seconds. The end-to-end legs
show only what the real Claude Code and the installed package alone can show, and no test waits longer than the
thing it checks needs.

## The suites by level

Measured with a clean `./mvnw -o -B -s settings.xml clean test` on the host under a load of about 40 on 16 cores:
34 s in all, compilation included, 222 tests green and none skipped. The end-to-end times are those of the last
green Build on GitHub (run 42477630790). A class that starts a process counts as integration as a whole.

| Level | Tests | Run time | Share by count | What it holds |
|---|---|---|---|---|
| Unit | 160 | 3.1 s | 62 % | `ClaudeFindingsTest` 54, `ClaudeCredentialExtractorTest` 27, `ClaudeAgentTest` 26, `ClaudeContainerSetupTest` 16, the declaration tests (session id, waiting, run end) 11, and the document and pin rules 26 |
| Integration | 62 + 5 | 8.9 s, plus the checks inside the 2.7 min Build job | 26 % | `ClaudeCommitTrailerTest` 20 (real git, 3.0 s), the workflow-step tests 42 (bash with stand-in tools, real git), and the `dist` checks of the built packages (`NativeLinkageCheck` 2, `PackageVersionCheck` 3) |
| End-to-end | 32 scenarios | 17.6 min (fedora) and 19.0 min (ubuntu), one rented machine each | 12 % | `credential` 10, `installed` 8, `terminal` 5, `session` 3, `wake` 2, `removal` 2, `subscription` 1, `waiting` 1 |

The unit share is below the pyramid's 70-80 % because the Build's own workflow steps are bash and are proven by
running them with stand-in tools, which is their lowest level. The adapter's Java has no class reached only from
above.

## Flows proven only above the unit level

- **The commit trailer hook** (`ClaudeCommitTrailer`): which model the trailer names, and which transcript it reads,
  is a shell hook git runs, so its 20 tests commit with a real git. It stays integration: the hook is the real
  piece, and its tests take 3 s.
- **The workflow steps** (publish and snapshot tokens, the update pull request, release channels, commits a
  workflow makes): bash run with stand-in tools. They stay integration for the same reason.
- **End-to-end only:** no flow of this adapter. Every scenario's adapter fact - the screen at rest and waiting, the
  session id, the settings that answer the first-run questions, the subagent limit, the run's end, the credential
  kinds, the refused hosts, the mailbox instructions - has a unit test over the recorded screen or file. What the
  scenarios add is that the pinned Claude Code honors those facts, which only the real CLI can show.
- **Porelo's own flows in this suite:** five scenarios test Porelo and not this adapter: in `credential.feature`
  *a person unlocks the vault and the passphrase is not echoed*, *a person stores the key, and it is echoed
  nowhere* and *the credential is not recoverable from the vault file*; in `installed.feature` *the hooks register
  for this user* and *doctor says what this machine can do*. Each runs on both legs.

## Waits, sleeps and the network

- `WorkflowStepsTest` runs `sleep 20` under a limit of 1 s and so waits that second each run; a limit of 100 ms
  shows the same kill.
- No test reaches the network: every download in a workflow step is a stand-in, and no test opens a socket or a
  URL (searched for `HttpClient`, `openConnection`, `openStream`, `new URL(`, `InetAddress` and `ServerSocket`).

## Plan

- **Porelo's flows leave this suite** (S): the five scenarios above are deleted from `credential.feature` and
  `installed.feature` once Porelo's own suite is confirmed to cover each; one it does not is handed to Porelo's
  agent first. The end-to-end count falls from 32 to 27, on both legs.
- **The timeout test waits 100 ms** (XS): `WorkflowStepsTest` with a limit of 100 ms, seen red first by a limit
  the sleep does not reach.

## Acceptance

- The unit suite stays under a minute, and no flow of the adapter is reached only from an integration or an
  end-to-end run.
- The end-to-end legs hold 27 scenarios, none of which tests only Porelo.
- `WorkflowStepsTest` takes under 0.5 s.

## Open questions

- None. The five scenarios that test only Porelo leave the suite only where Porelo's own suite is confirmed to cover them (under *To be checked*); one it does not cover goes to Porelo's agent first, so no coverage is dropped.

## To be checked

- Whether Porelo's own acceptance suite proves the passphrase and the stored value are echoed nowhere, the vault file
  does not hold the value, `porelo setup` registers the hooks and `porelo doctor` reports them; asked of Porelo's agent.
