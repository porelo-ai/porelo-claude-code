# CC24 — Prove OAuth isolation end to end

**Status:** Implementing.
**Size:** M - one acceptance scenario and an embedded stand-in token endpoint, no change to the
adapter's code.

**What must be true.** A task started with a Claude subscription holds only its own short-lived token:
the refresh token never reaches it, and only the host renews.

## Why

The acceptance scenarios run with an OpenRouter API key only. OAuth is covered by unit tests here
(`ClaudeCredentialExtractorTest`, `ClaudeContainerSetupTest`) and by Porelo's own tests and its
stub-agent `agent-login.feature`; no scenario signs in with a subscription and looks inside a Claude
Code task. A live subscription in CI would spend a person's login: every renewal rotates the refresh
token.

## Plan

- **A stand-in token endpoint** on the host's loopback, not a CI subscription, written by script steps
  in `src/acceptance/resources/org/fuin/porelo/agent/impl/claude/acceptance/subscription.feature`,
  answering `grant_type=refresh_token` with a new access token
  and a rotated refresh token, and counting what it is asked. Porelo's renewal (`TokenPurchase`) takes
  https, or http on loopback, so no certificate is needed.
- **The scenario stores a subscription login without a person:** script steps write a credentials file as Claude
  Code leaves it, with an expired access token and a refresh token, and store it with `porelo vault
  login claude --credentials-from <its copy> --token-url http://127.0.0.1:<port>/token` ([B173](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b173)).
  `ClaudeCredentialExtractor` reads it as after a real login; the token URL comes only from that host-side
  option, never from the file, and is accepted only as http on loopback.
- **It starts a Claude Code task with that entry,** then checks the task's environment, its files under
  `/home/agent`, file and link target names, inspection, state file names and logs for both refresh and
  both access tokens, including across line breaks. The raw host vault file and captured vault login
  output are searched too; login output is printed only after it passes. Task-start output stays in
  the kit's `World.Task.said()` and is not exposed to scripts by its `{task}` and `{state}` substitutions.
- **Renewal:** a request after the access token's expiry is seen renewed by the host - the stand-in
  accepts only Claude Code's client id and counts one accepted call from the host across two refused
  requests through the task's broker socket. Both `127.0.0.1` and `169.254.1.2`, where `host.containers.internal` maps the host's loopback, must fail
  at the transport (HTTP 000) when the task probes the stand-in.
- **The real token URL stays reachable:** its host stays allowed, since Claude Code quits without it
  (`doc/decisions.md`). What keeps a renewal out of the task there is that the task holds no refresh
  token, which the scenario searches for.
- **Red first:** on the test VM, the same scenario with the original refresh token planted in a file
  under the task's `/home/agent` fails without printing the value. Repeat with the renewed access token
  to check that access-token leaks fail too.

## Acceptance

- A scenario shows the task holds only its own token, and the refresh token is in neither its
  environment nor its files nor the logs.
- A renewal from inside the task is seen to be refused, and the host renews.

## Measured

On the test VM, Porelo 0.5.1~snapshot.933 per account, 2026-10-22:

- **Green at 13:47Z in 73 s:** one renewal by the host, accepted with Claude Code's client id; the broker refused
  both renewals from inside the task; the stand-in unreachable from the task at `127.0.0.1` and `169.254.1.2`;
  none of the four tokens in the vault file, the login's output, the inspection, the environment, the files
  under `/home/agent` and their names, `podman logs` or the task's state files.
- **Red with a token planted** in a file under `/home/agent`: "original refresh token found in a file under
  /home/agent" at 13:49Z, and "renewed access token found in a file under /home/agent" at 13:50Z. No value
  was printed in either log.
- **From inside a task**, `https://platform.claude.com/v1/oauth/token` answered a made-up refresh token with
  HTTP 400.
