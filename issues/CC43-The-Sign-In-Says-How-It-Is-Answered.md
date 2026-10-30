# CC43 — The Sign-In Says How It Is Answered

**Status:** Backlog; Estimated and Backlog (operator, 2026-10-30, sprint 6m).
**Size:** XS - the key in `claude.yaml` and the comment above it, and the Porelo version in `pom.xml`, under an hour.

**What must be true.** A person signing in to Claude Code from a host, at a terminal or in the GUI, is told to open the
link and paste the code it shows.

## Plan

- **The contract built against:** Porelo's agent API takes `login.flow` with the values `paste-code`, `device-code`
  and `loopback`, refuses a `loopback` flow on a remote account by naming the agent, and never sets a browser opener for
  a login on a host; the agent listing it gives the frontend names the flow.
- **`src/main/resources/agent/claude.yaml`** gains `login.flow: paste-code`.
  With `BROWSER` unset, its login shows a link and waits at "Paste code here" (measured on 2.1.267).
- **The comment above `login`** loses its note that a remote login needs the callback port forwarded: Porelo never
  sets a browser opener on a host, so the login there always takes the pasted code.
- **The key lands first:** Porelo 0.5.0's `AgentDefinitionReader` reads `login` key by key and ignores one it
  does not know, so the key is added now and the unit suite stays green.
- **`pom.xml`:** `porelo.version` then moves to the Porelo version whose agent API reads the key; that is when the
  listing names it.
- No test: a wrong value either shows the wrong hint, which only reads wrong, or, as `loopback`, is refused by name
  at once; neither does harm.

## Acceptance

- Seen to fail first: the agent listing for today's definition names no flow.
- With the change, the definition loads and the agent listing names `paste-code`.
