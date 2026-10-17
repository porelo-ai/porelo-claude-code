# CC22 — Depend on the agent protocol Porelo's package provides

**Status:** Estimated; blocked by porelo (an issue to be opened).
**Size:** S - here two dependency lines and one test; Porelo's `Provides` is its own issue, and
`porelo-pi` and `porelo-omp` take the same lines.

**What must be true.** Installing this adapter beside a Porelo that speaks another agent protocol is
refused by the package manager, not at the first task.

## Why

Both packages require only `porelo` (`src/deb/control/control`, `pom.xml` `<require>`), with no
version, while the adapter is built against one agent contract. It is not a live defect: Porelo checks
an adapter's `protocolVersion` before using it and refuses a mismatch with a message
(`agents/api/.../InstalledAgent.java`), but that refusal comes at the first task.

## Plan

- **Porelo first, in its own issue:** `Provides: porelo-agent-protocol-<N>` in
  `dist/dist-deb/src/deb/control/control` and a `<provide>` in `dist/dist-rpm/pom.xml`, with `<N>` from
  `AgentProtocol.VERSION` (3 today). Published before any agent depends on it, or the agents stop
  installing.
- **Here:** `Depends: porelo-agent-protocol-[[agent.protocol.version]]` in `src/deb/control/control` and
  `<require>porelo-agent-protocol-${agent.protocol.version}</require>` in `pom.xml`'s rpm `<requires>`,
  from one pom property. `porelo` stays a plain dependency beside it, so a Porelo from before the `Provides`
  is refused by name.
- **Red first:** a unit test holds that property to `AgentProtocol.VERSION` from the API jar this
  adapter compiles against; with the property one off it fails.
- **On the VM:** the adapter's `.deb` beside a Porelo package providing another number is refused by
  `apt`, and installs beside the right one.
- Then the same in `porelo-pi` and `porelo-omp`.

## Acceptance

- Porelo's packages declare `Provides: porelo-agent-protocol-<N>` (deb and rpm).
- This adapter's packages depend on that instead of on `porelo` alone, and a test holds the declared
  number to the API's `AgentProtocol.VERSION`; it is seen to fail with the numbers apart.
- The same in `porelo-pi` and `porelo-omp`.
