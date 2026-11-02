# CC36 — Report What Claude Code Finds Of Itself On The Host

**Status:** Review; acceptance through the installed adapter on a VM, built against the published agent API.
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
  - a profile in `.config/anthropic/configs` in the home: "cannot say", as it may hold a Console sign-in without a key; an empty
    directory is nothing.
  - On macOS, no file is "cannot say": the login is in the Keychain, whose read prints the value, so it is never read.
- **`.claude.json` beside the default directory, inside a moved one:** the sibling of a directory named `.claude`,
  otherwise inside the given directory; `settings.json` and `.credentials.json` are always inside it. A broken
  `.claude.json` is "cannot say".
- **The provider:** `anthropic` unless `settings.json`'s `env` says otherwise. `CLAUDE_CODE_USE_BEDROCK`, `_VERTEX`,
  `_FOUNDRY`, `_ANTHROPIC_AWS`, `_ANTHROPIC_GOOGLE_CLOUD` or `_MANTLE` set to `1`, `true`, `yes` or `on` in any case, or
  the number 1, name that provider; `ANTHROPIC_BASE_URL` with any value is "not Anthropic". The variable's name goes out,
  never its value. Broken JSON there counts as no settings, as it does for Claude Code; an unreadable file, or a managed
  settings file present, is "cannot say" with the path, and leaves every route's provider open. Once requests go
  elsewhere, no route beneath is offered for import. A finding has no free text, so nothing can carry a value.
- **What it cannot see, and says so instead of "not signed in":** the person's shell, which an agent the daemon spawns
  does not share (`ANTHROPIC_API_KEY`, `CLAUDE_CODE_OAUTH_TOKEN`, `CLAUDE_CONFIG_DIR`, a switch), and a project's own
  `.claude/settings.json`, which depends on the directory a task starts in.
- **`ClaudeCredentialExtractor`** also reads `primaryApiKey`, below a subscription, so that a key found can be imported.
- **`ClaudeAgent`** overrides the agent API's new findings method with it. Until Porelo publishes that method, the branch
  holds `Finding` and `Finder` under the names the agent API will give them, which go when it does.
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
| `CLAUDE_CONFIG_DIR` | moves `.credentials.json`, `settings.json` and `.claude.json` inside it; `.claude` and `.claude.json` in the home are then ignored. Set to `.claude` in the home it reads `.claude.json` inside as well |

Not run here: the Keychain (the binary reads it with `security find-generic-password -w`; a fallback file is used only
when the Keychain refuses the write); managed settings, which need root to write; a profile in `.config/anthropic` in the home,
which the login without a key writes and which ranks below a working subscription.

## The reading

Against `java-code-review`, `security-audit`, `test-quality`, `clean-code` and `solid-principles` (2026.906.3). Declined:

- **A directory named `.claude` elsewhere** counts as the default, so its account file is read beside it, and
  `CLAUDE_CONFIG_DIR=<home>/.claude` reads inside where this looks beside it. Only the environment tells them apart, and
  the adapter reads none; Porelo passes the definition's directory.
- **`importable: true` while a file that may redirect could not be read:** the import can still take the route, which is
  all the field says, and `provider` stays open beside a "cannot say".
- **A login in the Keychain outranks a Console key this reports first:** the Keychain is never read, a "cannot say" stands
  beside it, and no package is built for macOS today.
- **A file that is not UTF-8 is "cannot say"**, not broken: Claude Code's answer to one was not measured, and "cannot say"
  claims nothing.
- **`isRegularFile` in the extractor reads an unsearchable directory as absent**, as it did before; `Find` says "cannot
  say" there, so no import is offered on a wrong premise.

## Acceptance

- On a machine with a Claude Code sign-in, the installed adapter reports a subscription, provider `anthropic`, from
  `.claude/.credentials.json` in the home; with a config directory holding nothing, it reports not signed in.
- No answer contains a credential's value, checked on the answers of every fixture.
- A helper command in a fixture was not run.

## Checked

- On a VM, with a made-up subscription in place of a real sign-in, the installed adapter answered as above over its
  socket, and Porelo took it at a start, naming the provider and the file, without a value in any answer.
- From the reading, declined: a blank `ANTHROPIC_BASE_URL` reads as unset, its effect on Claude Code not measured; a
  config directory through a symlink and `..`, since Porelo passes the definition's directory.
- `vault import claude --provider anthropic` stores it, and `--provider openrouter` refuses. The import a start offers
  names the provider, so the extractor answers for `anthropic`, and holds nothing for another provider.
