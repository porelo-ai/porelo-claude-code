# CC22 — Depend on the agent protocol Porelo's package provides

**Status:** Estimated; blocked by porelo [B224](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b224) and [B230](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b230).
**Size:** S - here two dependency lines and one test; Porelo's `Provides` is its own issue, and
`porelo-pi` and `porelo-omp` take the same lines.

**What must be true.** Installing this adapter beside a Porelo that speaks another agent protocol is
refused by the package manager, not at the first task.

## Why

Both packages require only `porelo` (`src/deb/control/control`, `pom.xml` `<require>`), with no
version, while the adapter is built against one agent contract. It is not a live defect: Porelo checks
an adapter's `protocolVersion` before using it and refuses one it cannot use with a message
(`agents/api/.../InstalledAgent.java`), but that refusal comes at the first task.

Under [B224](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b224) Porelo accepts every agent at or above its lowest protocol version, a newer one included, and
raises that lowest version only for a change that breaks the contract. So the one pairing to refuse is an
adapter older than Porelo's lowest version; a dependency on one exact number would refuse pairings that
work.

## Plan

- **Porelo first, in [B230](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b230):** `Provides: porelo-agent-protocol-<L>` in
  `dist/dist-deb/src/deb/control/control` and a `<provide>` in `dist/dist-rpm/pom.xml`, with `<L>` from
  the new `AgentProtocol.LOWEST` (3), not from `VERSION`. Published before any agent depends on it, or the
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
- Then the same in `porelo-pi` and `porelo-omp`.

## Acceptance

- Porelo's packages declare `Provides: porelo-agent-protocol-<L>` with their lowest version (deb and rpm).
- This adapter's packages depend on one of the lowest versions from 3 to its own `VERSION`, beside
  `porelo`, and a test holds that list to the API's `AgentProtocol.VERSION`; it is seen to fail with the
  numbers apart.
- The same in `porelo-pi` and `porelo-omp`.
