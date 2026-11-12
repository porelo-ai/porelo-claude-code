# CC22 — Depend on the agent protocol Porelo's package provides

```json
{
  "$schema": "issue-1.json",
  "id": "CC22",
  "title": "Depend on the agent protocol Porelo's package provides",
  "state": "Implementing",
  "size": "S",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": [],
  "covers": "the agent contract named at install time"
}
```

**Why this size.** here two dependency lines and one test; Porelo's `Provides` is Porelo's own change, and
`porelo-pi`, `porelo-omp`, `porelo-codex` and `porelo-agy` take the same lines, each in an issue of its own.

**How it stands.** Waits for Porelo's packages providing its lowest agent protocol version.

**What must be true.** Installing this adapter beside a Porelo that speaks another agent protocol is
refused by the package manager, not at the first task.

## Why

Both packages require only `porelo` (`src/deb/control/control`, `pom.xml` `<require>`), with no
version, while the adapter is built against one agent contract. It is not a live defect: Porelo checks
an adapter's `protocolVersion` before using it and refuses one it cannot use with a message
(`agents/api/.../InstalledAgent.java`), but that refusal comes at the first task.

Porelo accepts every agent at or above its lowest protocol version, a newer one included, and
raises that lowest version only for a change that breaks the contract, so the one pairing to refuse is an
adapter older than Porelo's lowest version; a dependency on one exact number would refuse pairings that
work.

## Plan

- **Porelo first:** `Provides: porelo-agent-protocol-<L>` in
  `dist/dist-deb/src/deb/control/control` and a `<provide>` in `dist/dist-rpm/pom.xml`, with `<L>` from
  `AgentProtocol.LOWEST` (3), not from `VERSION`. Published before any agent depends on it, or the
  agents stop installing.
- **Here:** a dependency on any lowest version up to the adapter's own `VERSION`, from 3, the first lowest
  version there is: `Depends: porelo-agent-protocol-3 | porelo-agent-protocol-4` in `src/deb/control/control`
  for an adapter at 4, and the same as an rpm rich dependency
  (`(porelo-agent-protocol-3 or porelo-agent-protocol-4)`) in `pom.xml`'s `<requires>`, both from one pom
  property. `porelo` stays a plain dependency beside it, so a Porelo from before the `Provides` is refused
  by name.
- **Red first:** a unit test holds that property to run from 3 to `AgentProtocol.VERSION` of the API jar
  this adapter compiles against; with the last number one off it fails.
- **On the VM:** the adapter's `.deb` beside a Porelo package whose lowest version is above the adapter's
  `VERSION` is refused by `apt`, and installs beside one at or below it, a lower one included.
- Then the same in `porelo-pi`, `porelo-omp`, `porelo-codex` and `porelo-agy`, each by an issue of its own there. `porelo-pi`, `porelo-codex` and `porelo-agy`
  keep the version floor they have on `porelo` beside it; a floor refuses a Porelo too old, not one whose lowest
  version has passed the adapter.

## Acceptance

- Porelo's packages declare `Provides: porelo-agent-protocol-<L>` with their lowest version (deb and rpm).
- This adapter's packages depend on one of the lowest versions from 3 to its own `VERSION`, beside
  `porelo`, and a test holds that list to the API's `AgentProtocol.VERSION`; it is seen to fail with the
  numbers apart.
- The same in `porelo-pi`, `porelo-omp`, `porelo-codex` and `porelo-agy`.
