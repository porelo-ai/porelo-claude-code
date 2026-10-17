# Claude Code for Porelo

Runs [Claude Code](https://github.com/anthropics/claude-code) inside
[Porelo](https://github.com/porelo-ai/porelo): in a hardened container, reaching only what it needs,
with your credential kept on the machine and its work waiting for your review.

## Install

Set Porelo's package repository up once, as Porelo's
[getting started](https://github.com/porelo-ai/porelo/blob/main/doc/getting-started.md) describes,
then:

```
sudo apt install porelo-agent-claude      # or: sudo dnf install porelo-agent-claude
```

Setting a machine up from Porelo's interface offers it too. `porelo agents` lists it at once; nothing
has to be registered. The package is the adapter only: the CLI itself is fetched into the task's
image at a pinned version and checked against its digest.

## Sign in

With a **Claude subscription**:

```
porelo vault login claude
```

It runs Claude Code's own login on this machine. Choose the subscription, open the link it prints in
any browser, sign in, and paste the code shown back into the terminal. Porelo keeps the sign-in and
renews it when it nears its end, so a task never holds it and a night off needs no new sign-in.

With an **API key** from the Anthropic Console:

```
porelo vault put anthropic --type api-key
```

It asks for the key without showing it. The two kinds are not interchangeable; the vault records
which one it holds, and `porelo vault list` shows it.

## Start a task

```
porelo project default add <address>
porelo task start <name> -p default -r <repository> --agent claude
```

No project file is needed. The address is where the repository is cloned from and where its approved
work goes; `porelo project default list` shows the name `-r` takes. Started inside a checkout,
`porelo task start` adds that checkout's repository by itself. A project of your own, with a
`project.yml`, is for settings beyond that.

## What the task holds and reaches

- **Not your credential.** The container gets a token that works for this task only, and Porelo's
  broker swaps the real credential in on the way to Anthropic.
- **Reachable:** the provider (`api.anthropic.com`, or OpenRouter with `--provider openrouter`) and
  the hosts Claude Code needs to start: `platform.claude.com`, `claude.ai`, `statsig.anthropic.com`.
  Nothing else resolves.
- **Refused on purpose:** a Datadog log intake, `raw.githubusercontent.com` and the CLI's own
  downloads. Claude Code works without them.

`porelo agents --verbose` shows all of it for the installed version.

## When it does not work

The task's broker logs every request in `vault.log`, in `/run/user/<uid>/porelo/<container>/`:

| What it says | What it means |
|---|---|
| `401 from the provider` | the request reached Anthropic, which refused the credential: wrong, expired, or of the other kind |
| `the sign-in … was revoked or has expired` | sign in again with `porelo vault login claude` |
| `401 token not accepted` | a request came without this task's token |
| `401 this task's token expired at …` | the task outlived `--token-hours` |
| `503` | the vault was locked or the entry removed: `porelo vault unlock` |

Why it works the way it does is in [the decisions](decisions.md).
