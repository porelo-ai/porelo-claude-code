# CC36 — Report What Claude Code Finds Of Itself On The Host

**Status:** Analyzed; blocked by porelo [B224](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b224)
and [B225](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b225).
**Size:** S - one class beside `ClaudeCredentialExtractor` and its tests, in this module only; the contract it
answers is porelo's.

**What must be true.** Asked what it finds of itself on the host, the Claude Code adapter answers whether Claude Code
is configured and signed in there, as a subscription or an API key and to which provider, each finding with the path
it came from, and never a credential's value. Where it cannot tell, it says "cannot say", never "not signed in".

## Why

[PJ53](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj53) makes the agent already on the
machine the default: each packaged agent reports what it finds of itself, and `porelo` chooses. This is Claude Code's
part. Its sign-in is already read for `vault import` by `ClaudeCredentialExtractor`, so the place is known.

## Plan

- **`ClaudeFindings`**, beside `ClaudeCredentialExtractor`, answering porelo's `Find` with the given config directory
  (`~/.claude` as `config_dir` declares it):
  - `.credentials.json` with `claudeAiOauth` holding an access token: signed in, a subscription, provider
    `anthropic`; with `apiKey`: signed in, an API key, provider `anthropic`. Only the kind and the path go out.
  - **A file holding neither is a finding, not an exception**: "a credentials file Claude cannot use", with its
    path. The extractor's throw stays for `vault import`, where it is right.
  - **An `env` block in `settings.json` that sets `ANTHROPIC_BASE_URL`, or Bedrock's or Vertex's switch**, makes the
    provider "not Anthropic, named by settings.json" rather than `anthropic`; the variable's name is reported, never
    its value.
  - A file that cannot be read or parsed is "cannot say", with the path and the reason.
- **`ClaudeAgent`** overrides the agent API's new findings method with it.
- **Red first:** `ClaudeFindingsTest` over fixtures of the files Claude Code 2.1.267 writes: a subscription, an API
  key, a file with neither, a settings `env` naming another base URL, an unreadable file; and a guard that no
  finding's text contains the fixture's token or key.

## Acceptance

- On the VM's `smith`, signed in with a subscription, the installed adapter reports a subscription, provider
  `anthropic`, from `~/.claude/.credentials.json`; after `claude` signs out, it reports not signed in.
- No answer contains a credential's value, checked on the answers of every fixture.

## To be checked

- What Claude Code 2.1.267 writes into `~/.claude.json` and `settings.json` at a login and with `/model` or an API key
  set, measured attended on the VM, so the fixtures state what the real tool writes.
- Whether `CLAUDE_CONFIG_DIR` moves `.credentials.json` with it, and so whether `config_dir` alone is enough.
