# CC73 — The Local Sign-In Goes To A Host

```json
{
  "$schema": "issue-1.json",
  "id": "CC73",
  "title": "The Local Sign-In Goes To A Host",
  "state": "Review",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "on the person's machine the adapter lists each Claude Code sign-in it finds - provider, kind, account where known, age and expiry where known, and whether it can be carried, never its value - and hands a chosen one to Porelo, which carries it into a rented host's vault",
  "openQuestions": 2
}
```

**Why this size.** `ClaudeFindings` and `ClaudeCredentialExtractor` exist; this extends them. Each route below is read, told apart and tested against a fixture file; no new contract of
the adapter's own, the fields come from Porelo's half.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** A person with Claude Code signed in on their own machine carries that sign-in into a rented host's
vault from the window and is not asked to sign in a second time there. The adapter's half: for each route below it
says provider, kind, the account where the file names one, age and expiry where the file holds them, and whether it
can be carried, with no value in what it says; and taking one hands its value to Porelo through the extractor alone,
never through a file, a command line or a log.

Filed 2026-11-14 on the operator's order, under ST362 "Credentials carried to a host". Agent Core files Porelo's half,
the fields and the carrying by stdin or environment; Agent Frontend files the window's dialog.

## Plan

- `.credentials.json` in the definition's `config_dir`: the subscription's OAuth sign-in, with its expiry and scopes; carriable.
- `primaryApiKey` in the account file beside that directory: a Console sign-in's API key, with the account that file names; carriable.
- macOS's keychain: found, but not carriable, since the reader runs no helper and `security` would be one.
- The finder fills the fields Porelo's half adds to a finding; the extractor is the one an import already uses.
- Tests: one fixture per route, each finding's fields asserted, and a test that no finding's text holds any value
  of its fixture. The definition's `config_dir` stays the place read.

## Acceptance

- Each route above is found and said as listed, with its carriable answer, by a test.
- No finding holds a value of its fixture.
- With Porelo's half in, a sign-in found on the person's machine is in the host's vault and a task there runs on it.

## To be checked

- **Porelo's half.** The fields (account, age, expiry, carriable) and the carrying are Agent Core's issue; it goes in
  `blockedBy` once filed.
- **Two machines on one sign-in.** An OAuth sign-in whose refresh token is replaced at each renewal works on one
  machine at a time: the first to renew signs the other out. Whether a carried sign-in is said to sign the person's
  machine out, or is carried only as a kind that does not rotate, is the operator's call.

## What was built

- `ClaudeSignInFacts` says, for each sign-in `ClaudeFindings` finds in Claude Code's own files, the account
  (`oauthAccount`'s `emailAddress` in the account file), the access token's expiry (`expiresAt`, a subscription
  only) and the file's last change; an account with a control character or past 320 characters is none.
- Carriable is the finding's `importable`, as before: the credentials file and the Console key are; a sign-in in
  the settings has no facts and is not offered; the macOS keychain stays a finding that cannot say.
- The value goes to Porelo only through `ClaudeCredentialExtractor`, the one an import already uses.
- Tests: `ClaudeSignInFactsTest`, 8, each route, no account or expiry guessed, and no fixture value in any fact.
- Open until B775's wire shape is posted: the facts mapped onto Porelo's finding, and the third acceptance point.
