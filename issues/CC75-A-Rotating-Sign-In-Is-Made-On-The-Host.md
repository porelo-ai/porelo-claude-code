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

- **The contract, in words.** Porelo no longer carries a sign-in whose refresh token is replaced at each renewal from the person's machine to a host; the host makes its own, and an API key is carried as before. So the definition says, for each provider and kind it signs in to, whether that refresh token rotates. The key's name and shape are agreed with Porelo's agent before it is written; until then this plan names it `rotates`.
- **How rotation is measured, never assumed.** First, read in the pinned CLI whether it stores a refresh token its renewal answers with. Then one real renewal per kind on a test sign-in, on a VM: the digest of the refresh token before and after, compared, never the token itself printed. A kind no account here can reach stays unmeasured and is said so, never declared from the CLI's code alone.
- **The kinds, as read in the code today (2.1.311):**
  - Anthropic subscription, `.credentials.json`: OAuth with a refresh token; `ClaudeCredentialExtractor` keeps it and Porelo renews on the host. Whether Anthropic replaces it at renewal is the measurement.
  - Console key, `primaryApiKey`: a key, no refresh token; does not rotate.
  - macOS keychain: found, not carriable today; it stays so.
- **The sign-in on a headless host:**
  - `porelo vault login claude` already runs Claude Code's own login in a throwaway container with `BROWSER` unset: it prints a link and waits at the paste prompt (`login.flow: paste-code`). On a host that is the same login, started over SSH from the window.
  - What Porelo reads back is the `.credentials.json` it writes, through the extractor an import already uses; nothing new in the adapter beyond checking it holds on a host.
- The findings and the facts stay; a kind declared rotating is still found and described, and Porelo, not the
  adapter, refuses to carry it.
- **Tests:** a definition test that each provider and kind carries its measured answer; the existing `terminal.feature` sign-in without a browser is the host flow's proof, run against a VM.

## Acceptance

- Each kind above has its rotation answer in the definition, each from a measurement named in `doc/decisions.md`,
  or is said unmeasured.
- On a VM without a browser, the sign-in made on the host as above ends in the host's vault, and a task runs on it.
- A test fails when a measured kind's answer is taken out of the definition.

## Open questions

- **The accounts for the measurement.** A real renewal per kind needs a test sign-in to each provider; a renewal of a rotating one signs out every other place that holds it. Which accounts may be used, and that none of them is one the person also uses on their own machine, is the operator's call.

## To be checked

- **The key's name.** `rotates` and where it stands in the definition, agreed with Porelo's agent, which reads it.
