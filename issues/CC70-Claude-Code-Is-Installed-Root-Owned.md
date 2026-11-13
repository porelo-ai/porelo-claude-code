# CC70 — Claude Code Is Installed Root-Owned

```json
{
  "$schema": "issue-1.json",
  "id": "CC70",
  "title": "Claude Code Is Installed Root-Owned",
  "state": "Backlog",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the claude binary a task starts, and the link that names it, are installed by root and owned by root, so the agent in a task cannot replace what Porelo starts as Claude Code next time; its version stays pinned by the definition and moves only through the update job, and a self-update inside a task fails"
}
```

**Why this size.** M: the install moves from the agent's phase to the root phase of the image, which Porelo's agent
contract does not offer for a verified download today, so it takes one contract change agreed with Porelo; then the
definition, its tests and one check at the pin.

**Estimate.** M: 53 min, the measured median of M over all repositories (p80 114).

**What must be true.** Whatever the agent writes during a task, the next start of Claude Code runs the pinned binary
Porelo built into the image.

- The binary goes to `/usr/local/lib/claude/versions/<version>`, mode 0755, owned by `root:root`, in a directory owned by
  root and not writable by the agent; `/usr/local/bin/claude` is a root-owned link to it, and nothing named `claude`
  stays under `/home/agent`.
- The version is pinned in `pom.xml` (`agent.cli.version`) with the digest from Anthropic's per-version
  `manifest.json`, checked before the file is installed; it moves only through the update job, outside any task.
- Updates stay off in the task (`DISABLE_UPDATES`, `downloads.claude.ai` refused), and a self-update that is tried
  anyway fails to write over the binary, by design.

## Plan

- Today the definition fetches the binary as an `install.artifacts` entry into a
  versions directory in the agent's home, and an `as_agent` line links the home's own `claude` to it. Porelo
  renders every artifact after `USER agent`, so the agent owns both.
- Preferred: Porelo's agent contract lets an artifact be fetched as root, rendered with the same digest check before
  `USER agent`; the definition then names the root target and the link moves to `as_root`. The pinned artifact stays
  what `porelo agents --supply-chain` and `PinAgreementTest` read. Without that change the definition would write its
  own verified fetch in `as_root` and lose that record, so the contract change comes first.
- Tests, unit level: the definition's install puts the binary and the link under `/usr/local`, owned by root, with no
  line that writes them as the agent (red first against today's definition); `PinAgreementTest` still holds the pin.
- At the pin, in a task: `/status` shows no install warning with the binary outside the home, or the warning is
  answered in the definition; a write to the binary as the agent fails.

## Acceptance

- In a built image, `stat` of `/usr/local/bin/claude` and of what it links to shows `root`, and the agent's user cannot
  write either or their directory.
- An update tried in a task as the agent fails, and the next start runs the pinned version.

## To be checked

- The contract change for a root-owned artifact, agreed with Porelo's agent in the channel.
- Whether Claude Code's `/status` warns of an install outside the agent's home, measured at the pin.
