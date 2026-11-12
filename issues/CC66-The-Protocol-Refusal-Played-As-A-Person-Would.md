# CC66 — The Protocol Refusal Played As A Person Would

```json
{
  "$schema": "issue-1.json",
  "id": "CC66",
  "title": "The Protocol Refusal Played As A Person Would",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "a script plays story ST35 on the test VM as a person would: Claude Code's adapter installs beside a Porelo it speaks with and is refused beside one it does not, and `task start --now` is refused rather than accepted without effect; part of ST347"
}
```

**Why this size.** One script and one run on the test VM, no production code. The packages it installs are built by
CC22, B230 and B201, and the stand-ins it needs are repacked copies of those packages.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=167). Expect more for the run on the VM and
the clean-up after it.

**What must be true.** One command on the test VM plays
[ST35](https://github.com/porelo-ai/porelo-project/blob/main/stories/README.md#st35), *Refuse an incompatible
protocol and a meaningless option*, the way a person at the terminal would. It is one story of
[ST347](https://github.com/porelo-ai/porelo-project/blob/main/stories/README.md#st347), *Every story played through
once, as a user would*. It prints one verdict for the story: `passed`, `failed at <step>`, or `waiting on <issue>`
for an issue not yet in Review.

**Note (operator, 2026-11-12, for every story-test issue).**

- A test script is test tooling, not production code. It gets no tests of its own, no red-first and no review loop;
  one Sonnet reading at most. The script is proven by its run on the VM.
- On the VM, a failing script is fixed on the spot and the run carries on. Every fault found, in the script or in
  the product, is collected rather than fixed one by one. After the run:
  - product faults become estimated Backlog issues;
  - script fixes go into one commit;
  - then one new run.
- One VM run at a time on this host, through the Coordinator: ask before starting a run, and post when it ends. No
  second VM and no Molecule beside it; clean up after.
- A story whose issues are not all in Review still gets its script and one run; the run names the missing issue as
  an expected failure.

## Plan

- **`stories/ST35.sh`**, run on the VM as the person's account, which may use `sudo` for the package manager as a
  person installing does. It takes the directory holding the packages it installs:
  - `porelo_*.deb` from B230's build, whose `Provides: porelo-agent-protocol (= 3)` is `AgentProtocol.LOWEST`;
  - `porelo-agent-claude_*.deb` from CC22's build, requiring `porelo-agent-protocol (>= 3)` and `(<= 5)`.
- **Before:** it records the installed Porelo and adapter versions, and puts them back at the end, after a failure
  too.
- **The stand-ins.** Two copies of B230's `porelo` deb, repacked with `dpkg-deb -R`/`-b`, provide the protocol at 6
  (a Porelo whose lowest has passed this adapter) and at 2 (a Porelo older than the break this adapter was built
  after). The real Porelo of the day, from before B230, provides none.
- **The plays,** each with `sudo apt-get install ./<package>` as a person would:
  1. **Compatible.** B230's `porelo`, then the adapter. Both install, and `porelo agent list` names `claude`.
  2. **Lowest above the adapter's version.** The stand-in at 6, then the adapter. apt refuses it, naming
     `porelo-agent-protocol (<= 5)`, and the adapter is not installed.
  3. **Lowest below the adapter's build.** The stand-in at 2: refused, naming `porelo-agent-protocol (>= 3)`.
  4. **A Porelo without the provide** (the day's published one): refused, naming `porelo-agent-protocol`.
  5. **`--now`.** `porelo task start --now` is refused as an unknown option, with a message naming `--now`, and no
     task is started. Until B201 is in Review, a run where it is accepted reports `waiting on B201`.
- **Verdict:** one line per play and the story's verdict last. Until CC22 and B230 are in Review, plays 1 to 4
  report `waiting on CC22` or `waiting on B230`.
- **Level:** the run on the test VM, through the Coordinator.

## Acceptance

- One run of `stories/ST35.sh` on the test VM prints a verdict per play and one for ST35. It leaves the VM's
  packages as it found them.
- Faults the run finds are filed as estimated Backlog issues, and script fixes are in one commit followed by one new
  run.

## To be checked

- The test VM's account may use `sudo` for the package manager. `smith` on the VM used tonight may not, so the run
  needs an account that may, or the Coordinator names the VM.
- `porelo task start --now` with nothing else: whether the unknown option is reported before a missing argument once
  B201 removes it.
