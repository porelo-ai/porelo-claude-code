# porelo-claude-code

<img src="doc/images/early-bird.svg" width="350" alt="Early bird - work in progress">

> **Early bird - work in progress.** Porelo is not stable yet: until release 1.0.0, its code, commands
> and file formats can change without notice.

Runs [Claude Code](https://github.com/anthropics/claude-code) inside
[Porelo](https://github.com/porelo-ai/porelo): in a hardened container, reaching only what it needs,
with your credential kept on the machine and its work waiting for your review.
It is an adapter, not Porelo: it describes the agent and shapes in Java only what the agent cannot
express as data, and it depends on Porelo's published agent API alone, never on Porelo's implementation.

Its documentation: **<https://porelo-ai.github.io/claude/>**.

## Install

Once Porelo's package repository is set up, as Porelo's
[getting started](https://github.com/porelo-ai/porelo/blob/main/doc/getting-started.md) describes:

```
sudo apt install porelo-agent-claude      # or: sudo dnf install porelo-agent-claude
```

How to sign in, start a task, what the task reaches and what to read when it does not work:
[doc/index.md](doc/index.md).

## More

- [build.md](build.md) - building the package, and moving the pinned Claude Code version.
- [CONTRIBUTING.md](CONTRIBUTING.md) - how to change it, and [SECURITY.md](SECURITY.md) - how to report a vulnerability.
- [doc/decisions.md](doc/decisions.md) - why it works the way it does.

## Licence

GNU General Public License v3.0 or later. See [LICENSE](LICENSE).
