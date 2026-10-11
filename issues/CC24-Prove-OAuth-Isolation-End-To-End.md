# CC24 — Prove OAuth isolation end to end

**Status:** later.
**Size:** M - one acceptance scenario and a stand-in token endpoint in the kit, no change to the
adapter's code; a real CI subscription instead would make it the operator's to provide.

**What must be true.** A task started with a Claude subscription holds only its own short-lived token:
the refresh token never reaches it, and only the host renews.

## Why

The acceptance scenarios run with an OpenRouter API key only. OAuth is covered by unit tests here
(`ClaudeCredentialExtractorTest`, `ClaudeContainerSetupTest`) and by Sokar's own tests and its
stub-agent `agent-login.feature`; no scenario signs in with a subscription and looks inside a Claude
Code task. A live subscription in CI would spend a person's login: every renewal rotates the refresh
token.

## Plan

- **A stand-in token endpoint** on the host's loopback, in the acceptance kit
  (`src/acceptance/java/.../acceptance/`), answering `grant_type=refresh_token` with a new access token
  and a rotated refresh token, and counting what it is asked. Sokar's renewal (`TokenPurchase`) takes
  https, or http on loopback, so no certificate is needed.
- **The scenario stores a subscription login as `ClaudeCredentialExtractor` would:** type `oauth`, an
  access token about to expire, a refresh token, the client id, and the token URL naming the stand-in.
- **It starts a Claude Code task with that entry,** then checks the task's environment, its files under
  `/home/agent` and its logs for the refresh token, as `credential.feature` checks for the API key.
- **Renewal:** a request after the access token's expiry is seen renewed by the host - the stand-in
  counts one call from the host - and a renewal tried from inside the task, at the real token URL or
  the stand-in, is refused.
- **Red first:** the same scenario with the refresh token planted in the task's environment fails.

## Acceptance

- A scenario shows the task holds only its own token, and the refresh token is in neither its
  environment nor its files nor the logs.
- A renewal from inside the task is seen to be refused, and the host renews.

## To be checked

- How the kit stores a refresh token without a person: today only the agent's own login writes one
  (`ClaudeCredentialExtractor`), and `vault put --setting` is for what is not secret - Agent Core's.
- A stand-in endpoint or a dedicated CI subscription - the operator's; the plan above assumes the
  stand-in.
