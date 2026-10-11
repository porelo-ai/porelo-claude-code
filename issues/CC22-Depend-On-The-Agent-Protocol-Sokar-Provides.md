# CC22 — Depend on the agent protocol Sokar's package provides

**Status:** Analyzed; blocked by sokar (an issue to be opened).
**Size:** S - here two dependency lines and one test; Sokar's `Provides` is its own issue, and
`sokar-pi` and `sokar-omp` take the same lines.

**What must be true.** Installing this adapter beside a Sokar that speaks another agent protocol is
refused by the package manager, not at the first task.

## Why

Both packages require only `sokar` (`src/deb/control/control`, `pom.xml` `<require>`), with no
version, while the adapter is built against one agent contract. It is not a live defect: Sokar checks
an adapter's `protocolVersion` before using it and refuses a mismatch with a message
(`agents/api/.../InstalledAgent.java`), but that refusal comes at the first task.

## Plan

- **Sokar first, in its own issue:** `Provides: sokar-agent-protocol-<N>` in
  `dist/dist-deb/src/deb/control/control` and a `<provide>` in `dist/dist-rpm/pom.xml`, with `<N>` from
  `AgentProtocol.VERSION` (3 today). Published before any agent depends on it, or the agents stop
  installing.
- **Here:** `Depends: sokar-agent-protocol-[[agent.protocol.version]]` in `src/deb/control/control` and
  `<require>sokar-agent-protocol-${agent.protocol.version}</require>` in `pom.xml`'s rpm `<requires>`,
  from one pom property. `sokar` stays a plain dependency beside it, so a Sokar from before the `Provides`
  is refused by name.
- **Red first:** a unit test holds that property to `AgentProtocol.VERSION` from the API jar this
  adapter compiles against; with the property one off it fails.
- **On the VM:** the adapter's `.deb` beside a Sokar package providing another number is refused by
  `apt`, and installs beside the right one.
- Then the same in `sokar-pi` and `sokar-omp`.

## Acceptance

- Sokar's packages declare `Provides: sokar-agent-protocol-<N>` (deb and rpm).
- This adapter's packages depend on that instead of on `sokar` alone, and a test holds the declared
  number to the API's `AgentProtocol.VERSION`; it is seen to fail with the numbers apart.
- The same in `sokar-pi` and `sokar-omp`.
