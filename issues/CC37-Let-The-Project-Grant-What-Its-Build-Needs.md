# CC37 — Let The Project Grant What Its Build Needs

**Status:** Review.
**Size:** S - one entry leaves the definition's `refused_domains`, with its comment, the documentation, the
changelog and one acceptance step; no code and no contract changes.

**What must be true.** A task with Claude Code as its agent reaches `raw.githubusercontent.com` when its project
grants it, so `check-citations` runs there, and the host still does not resolve where no project grants it. The
Datadog log intake and the CLI's update host stay refused whatever the project says.

## Why

`claude.yaml` listed `raw.githubusercontent.com` under `refused_domains`, and in a running task a refusal wins
over the project's `domains` and sets. Measured in a task on porelo 0.5.1~snapshot.307: with agent `claude`, the
host answered NXDOMAIN although the project granted it. `check-citations` reads other repositories' index rows
from that host, so every repository with cross-repository links failed it in such a task.

The refusal was added for the agent's own traffic: the CLI asks for the host, but nothing it needs to start comes
from there. A narrower refusal does not exist: a refusal covers a whole host, since the traffic is TLS and not
terminated. And a task is one network, so whatever its build needs, its agent reaches too. Once a project needs
the host, the refusal no longer protects anything; it only blocks the work.

**`downloads.claude.ai` stays refused, unlike this host.** `claude.ai` is an allowed domain and admits every name
under it, so an undeclared `downloads.claude.ai` would resolve in every task, granted or not, and only
`DISABLE_UPDATES` would stop the CLI's self-update. That is why it was refused in the first place. This
repository's own `-Pnative,dist` fetches the pinned CLI from there, so that build runs in a task whose agent is
not Claude Code.

## Plan

- **`src/main/resources/agent/claude.yaml`:** `raw.githubusercontent.com` leaves `refused_domains` and is
  undeclared. The comment says why it is undeclared rather than refused, and what a project that grants it
  opens. The Datadog intake and `downloads.claude.ai` stay.
- **`doc/index.md`:** the host moves from *Refused on purpose* to *Only where the project grants it*.
- **`terminal.feature`:** the scenario that shows the refused names to a person names the Datadog intake, which is
  still refused.
- **`CHANGELOG.md`:** one line under *Changed*, since the definition ships.
- **No new unit test.** An unrefused host does none of the harms a test guards. What can do harm, the update
  host becoming reachable, is already guarded: `ClaudeAgentTest` keeps `downloads.claude.ai` refused.

## Acceptance

- `porelo agents --verbose` on the installed package lists the Datadog intake and `downloads.claude.ai` as
  refused, and no longer `raw.githubusercontent.com`.
- In a task with agent `claude` whose project grants `git-hosting`, `raw.githubusercontent.com` resolves and
  `check-citations` passes. In one whose project grants neither, the host answers NXDOMAIN.
- The unit suite, the tests tagged `documents` and the VM suite are green on the commit.

## Measured

On porelo 0.5.1~snapshot.307, with this change's adapter installed account-locally:

- `porelo agents --verbose` refuses `http-intake.logs.us5.datadoghq.com` and `downloads.claude.ai`, and no
  longer `raw.githubusercontent.com`.
- In a task with agent `claude` whose project grants `git-hosting`, the host resolves, and `check-citations`
  passes. In one whose project grants nothing, `getent hosts` exits 2 and the task's resolver logs
  `config raw.githubusercontent.com is NXDOMAIN`.
- The VM suite is green: 26 scenarios, none skipped.
- In a task with agent `claude`, the unit tests and the shared checks pass. `check-pin` cannot, since it reads
  the published manifest's digest from `downloads.claude.ai`. So the pin check, like the package build, runs
  where the agent is not Claude Code.

## Measured after the push

- Build 142 on `11413ef`, the first that built this change, was green on both legs, and `0.5.2~snapshot.142` is in
  the published index.
