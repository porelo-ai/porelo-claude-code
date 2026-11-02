# CC36 — Report What Claude Code Finds Of Itself On The Host

**Status:** Implementing; its adapter part waits for the agent API's method that asks an agent what it finds of
itself on the host.
**Size:** S - one class beside `ClaudeCredentialExtractor` and its tests, in this module only; the contract it
answers is porelo's.

**What must be true.** Asked what it finds of itself on the host, the Claude Code adapter answers whether Claude Code
is configured and signed in there, as a subscription or an API key and to which provider, each finding with the path
it came from, and never a credential's value. Where it cannot tell, it says "cannot say", never "not signed in".

## Why

Porelo makes the agent already on the machine the default: each packaged agent reports what it finds of
itself, and `porelo` chooses. This is Claude Code's
part. Its sign-in is already read for `vault import` by `ClaudeCredentialExtractor`, so the place is known.

## Plan

- **`ClaudeFindings`**, beside `ClaudeCredentialExtractor`, answering porelo's `Find` with the given config directory.
  It only reads files; it runs nothing, reads no environment of its own, and only names and kinds go out.
- **The sign-in, in the order Claude Code takes it** (*What Claude Code does*, below):
  - an `env` block in `settings.json` with `ANTHROPIC_AUTH_TOKEN`, `ANTHROPIC_API_KEY` or `CLAUDE_CODE_OAUTH_TOKEN`, or
    `apiKeyHelper` there: signed in, by that name, never run and no value read out;
  - `.credentials.json` with `claudeAiOauth` holding an access token and the scope `user:inference`: a subscription;
  - `primaryApiKey` in `.claude.json`: an API key;
  - **a credentials file Claude Code cannot use is a finding, not an exception**, with its path: neither block, a block
    with no token or without the scope, broken JSON, or `apiKey` alone, which Claude Code does not use. The extractor's
    throw stays for `vault import`, where it is right.
  - `~/.config/anthropic` present and nothing above: "cannot say", as it may hold a Console sign-in without a key.
  - On macOS, no file is "cannot say": the login is in the Keychain, whose read prints the value, so it is never read.
- **`.claude.json` beside the default directory, inside a moved one:** the sibling of `<home>/.claude`, otherwise
  inside the given directory; `settings.json` and `.credentials.json` are always inside it.
- **The provider:** `anthropic` unless `settings.json`'s `env` says otherwise. `CLAUDE_CODE_USE_BEDROCK`, `_VERTEX`,
  `_FOUNDRY`, `_ANTHROPIC_AWS`, `_ANTHROPIC_GOOGLE_CLOUD` or `_MANTLE` set to `1`, `true`, `yes` or `on` in any case, or
  the number 1, name that provider; `ANTHROPIC_BASE_URL` with any value is "not Anthropic". The variable's name goes out,
  never its value. Broken JSON there counts as no settings, as it does for Claude Code; an unreadable file, or a managed
  settings file present, is "cannot say", with the path and the reason.
- **What it cannot see, and says so instead of "not signed in":** the person's shell, which an agent the daemon spawns
  does not share (`ANTHROPIC_API_KEY`, `CLAUDE_CODE_OAUTH_TOKEN`, `CLAUDE_CONFIG_DIR`, a switch), and a project's own
  `.claude/settings.json`, which depends on the directory a task starts in.
- **`ClaudeCredentialExtractor`** also reads `primaryApiKey`, below a subscription, so that a key found can be imported.
- **`ClaudeAgent`** overrides the agent API's new findings method with it.
- **Red first:** `ClaudeFindingsTest` over fixtures of the cases below; a guard that no finding's text contains a
  fixture's token or key; and that a helper command in a fixture is not run.

## What Claude Code does

Measured on 2.1.311 with `claude auth status --json`, in an empty home with made-up values, and read in its binary
where noted. That command reports `apiProvider` and `authMethod`, but not a base URL.

| In the files or the environment | Claude Code says |
|---|---|
| nothing; a `.credentials.json` with neither block, or broken | not signed in |
| `claudeAiOauth` with a token and `user:inference` | signed in, subscription; also when `expiresAt` has passed |
| `claudeAiOauth` with no or an empty token, without that scope, or with no `scopes` | not signed in |
| `apiKey` alone in `.credentials.json` | not signed in |
| `primaryApiKey` in `.claude.json`; beside a subscription | signed in, API key; the subscription wins |
| `oauthAccount` alone in `.claude.json` | not signed in |
| `ANTHROPIC_API_KEY` in the shell or in `settings.json`'s `env`; beside a subscription | signed in, API key; the key wins |
| `ANTHROPIC_AUTH_TOKEN` or `CLAUDE_CODE_OAUTH_TOKEN`, either place | signed in, `oauth_token` |
| `apiKeyHelper` in `settings.json`; beside a subscription | signed in, `api_key_helper`, command not run; the helper wins |
| a provider switch in the shell or in `settings.json`'s `env` | that provider, `third_party`; wins over a subscription |
| the switch as `0`, `false`, `no`, `off`, `2`, `x` or empty | no switch |
| `CLAUDE_CODE_USE_GATEWAY=1`, or `ANTHROPIC_BEDROCK_BASE_URL` alone | no switch |
| `settings.json` broken, or `env` not an object | ignored |
| `ANTHROPIC_BASE_URL`, either place | `firstParty`, yet the requests go there: one reached a listener named in `settings.json` |
| `settings.local.json` in the config directory | ignored |
| `.claude/settings.json` in the working directory | applied |
| `CLAUDE_CONFIG_DIR` | moves `.credentials.json`, `settings.json` and `.claude.json` inside it; `~/.claude` and `~/.claude.json` are then ignored. Set to `<home>/.claude` it reads `.claude.json` inside as well |

Not run here: the Keychain (the binary reads it with `security find-generic-password -w`; a fallback file is used only
when the Keychain refuses the write); managed settings, which need root to write; a profile in `~/.config/anthropic`,
which the login without a key writes and which ranks below a working subscription.

## Acceptance

- On this machine's signed-in Claude Code, the installed adapter reports a subscription, provider `anthropic`, from
  `~/.claude/.credentials.json`; with a config directory holding nothing, it reports not signed in.
- No answer contains a credential's value, checked on the answers of every fixture.
- A helper command in a fixture was not run.

## To be checked

- The shape of `Find`'s answer in porelo's contract, and whether "signed in, but by a route the import cannot take"
  (`settings.json`, a helper) is expressed there.
