# CC32 — Answer The Auto Mode Question At First Run

**Status:** Implementing.
**Size:** S - one more answer in the file the adapter already writes and its unit test; no contract another repository reads, and the acceptance on the VM with the new CLI.

**What must be true.** A task running a Claude Code version that asks at start "Make auto mode your default
permission mode?" reaches work without being asked anything, as it does with the pinned version, so the update job
can move the pin past that version.

## What asks it, read in 2.1.292

- **The question is offered once per account, and the answer is kept in `~/.claude.json`**, the file
  `ClaudeFirstRun` writes. 2.1.292 offers it when all of these hold: the first-run wizard is done
  (`hasCompletedOnboarding`), `hasSeenAutoDefaultNudge` is not set, its own feature gate is on, the user settings'
  `permissions.defaultMode` is set and is not `auto`, no other settings source sets `defaultMode`, and auto mode is
  available to the session. Either answer sets `hasSeenAutoDefaultNudge: true` in `~/.claude.json`; "Yes" also
  writes `defaultMode: auto` into the user settings.
- **A task meets every condition.** `ClaudeSettings` puts `defaultMode: bypassPermissions` in the user settings, and
  nothing else sets a mode. The pin, 2.1.267, has no such question. The update runs for 2.1.287 and 2.1.292 stopped
  at it in every scenario that waits for the agent.
- **Measured on `ubuntu26.04`, a fresh home with the adapter's two files and nothing typed:**
  - 2.1.267 with `hasSeenAutoDefaultNudge: true` reaches "bypass permissions on" and keeps the key.
  - 2.1.292 with the key reaches it too.
  - The question itself did not show there with or without the key, in two minutes. With a key that is not
    accepted, auto mode is not available, which is its last condition. So the red run is the task, where the key
    is accepted.
- **Why the update job ran 30 minutes:** the manifest's `ready_within_seconds: 300` is the bound for every
  scenario that waits for the agent. Its comment says it covers a first image build. Porelo's acceptance kit counts
  the bound from the session existing, after the build. The whole step took 15.4 to 15.9 s in each of the five
  scenarios in the last VM run. Five scenarios waiting 300 s each fill the step's 30 minutes.

## Plan

- **`ClaudeFirstRun.document`** adds `hasSeenAutoDefaultNudge: true` beside `hasCompletedOnboarding`. Its comment
  says what the key answers and that the adapter keeps bypass mode, since the container is what makes that safe
  (`ClaudeSettings`).
- **`claude.yaml`'s bound is no longer part of this issue:** it was set to 120 s on 2026-10-17 with the other
  adapters', about four times the 31 s measured on the leg's machine type, `cpx12`. The 60 s first planned here
  came from the local VM, which is the fast end.
- **`doc/decisions.md`**, "A fresh container has never been logged in", names this answer with the wizard's.
- **Red first:**
  - `ClaudeContainerSetupTest.answersTheQuestionsAFreshContainerWouldBeAsked` asserts the new key, seen red before
    `ClaudeFirstRun` changes.
  - On the VM, the acceptance with the adapter built against 2.1.292 is seen stopping at the question before the
    change.
- **Not in this issue:** stopping the suite after its first failure. That would be Porelo's acceptance kit or
  `porelo-machines`. With the bound at 120 s, five failures cost ten minutes, not 25.

## Acceptance

- On the VM, the acceptance passes with the adapter built against the newest Claude Code that asks the question,
  and with the pinned 2.1.267, `PORELO_E2E_EXPECT_CLI` set to each.
- After the push, the update workflow dispatched for that version is green on both legs and opens its pull
  request.
- The two unit tests above go red without the change and green with it.
