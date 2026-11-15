# CC75 — A Rotating Sign-In Is Made On The Host

```json
{
  "$schema": "issue-1.json",
  "id": "CC75",
  "title": "A Rotating Sign-In Is Made On The Host",
  "state": "Idea",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "which of Claude Code's sign-ins rotate their refresh token, measured on the pinned CLI and declared per provider and kind, and how Claude Code signs in on a headless host instead of having a rotating sign-in carried there",
  "openQuestions": 1
}
```

**Why this size.** M: one measurement per sign-in kind on the pinned CLI, and one contract change - the definition says per provider and kind whether its refresh token rotates, which Porelo reads; its tests and the docs move with it.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** A person who runs Claude Code on a rented host signs in there, at a terminal without a browser, and is never offered to carry a sign-in that would sign their own machine out; a Console key is still carried.

## Plan

- **The contract, in words.** Porelo no longer carries a sign-in whose refresh token is replaced at each renewal from the person's machine to a host; the host makes its own, and an API key is carried as before. So the definition says, for each provider and kind it signs in to, whether that refresh token rotates. The key is `refresh_token_rotation`, at the definition's top level: provider, then credential kind, then `does-not-rotate`, written only for a kind measured so. A kind not named there is taken as rotating, and a credential whose reading carries `refreshToken` is then not carried - fail closed.
- **How rotation is measured, never assumed.** First, read in the pinned CLI whether it stores a refresh token its renewal answers with. Then one real renewal per kind on a test sign-in, on a VM: the digest of the refresh token before and after, compared, never the token itself printed. A kind no account here can reach stays unmeasured and is said so, never declared from the CLI's code alone.
- **The kinds, as read in the code today (2.1.311):**
  - Anthropic subscription, `.credentials.json`: OAuth with a refresh token; `ClaudeCredentialExtractor` keeps it and Porelo renews on the host. Whether Anthropic replaces it at renewal is the measurement.
  - Console key, `primaryApiKey`: a key, no refresh token; does not rotate.
  - macOS keychain: found, not carriable today; it stays so.
- **The sign-in on a headless host:**
  - `porelo vault login claude` already runs Claude Code's own login in a throwaway container with `BROWSER` unset: it prints a link and waits at the paste prompt (`login.flow: paste-code`). On a host that is the same login, started over SSH from the window.
  - What Porelo reads back is the `.credentials.json` it writes, through the extractor an import already uses; nothing new in the adapter beyond checking it holds on a host.
- **The login relayed to the window.** Porelo runs the login in a pty and matches each line of its output, control sequences removed, against `login.relay`: `link` (a Java regex, its first group the link, https to a host the agent or provider declares), `code` for a device code, `answer: line` where the code is typed back as one line. Without `relay` a relayed start of the agent is refused. The patterns are taken from the pinned CLI's login run in a pty with no browser, never from its source alone:
  - `link` and `answer: line`: the link Claude Code prints, and the code pasted back at its paste prompt.
- The findings and the facts stay; a kind declared rotating is still found and described, and Porelo, not the
  adapter, refuses to carry it.
- **Tests:** a definition test that each provider and kind carries its measured answer; the existing `terminal.feature` sign-in without a browser is the host flow's proof, run against a VM.

## Acceptance

- Each kind above has its rotation answer in the definition, each from a measurement named in `doc/decisions.md`,
  or is said unmeasured.
- On a VM without a browser, the sign-in made on the host as above ends in the host's vault, and a task runs on it.
- A test fails when a measured kind's answer is taken out of the definition.
- A login relayed on a VM shows the link (and a device code where there is one) as the patterns take it, and a test fails when a pattern no longer matches the measured output.

## Open questions

- **The accounts for the measurement.** A real renewal per kind needs a test sign-in to each provider; a renewal of a rotating one signs out every other place that holds it. Which accounts may be used, and that none of them is one the person also uses on their own machine, is the operator's call.
