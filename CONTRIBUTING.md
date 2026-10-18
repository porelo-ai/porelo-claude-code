# Contributing

Changes here follow the project's guide, [Contributing to Porelo](https://porelo-ai.github.io/porelo-project/contributing/):
how a change starts as a GitHub issue, what a commit carries, and what a change must satisfy. The
rules are kept there and in the shared block of [`AGENTS.md`](AGENTS.md), not here.

## In this repository

**How it is built and tested** is in [`build.md`](build.md): `./mvnw -B -s settings.xml verify` for
the code and its tests, and `-Pnative,dist` with GraalVM as `JAVA_HOME` for the adapter and its
`.deb` and `.rpm`. What is particular here:

- **It compiles against Porelo's published agent contract**, never its sources. A change the
  contract does not allow is agreed in [porelo](https://github.com/porelo-ai/porelo) first.
- **The documents tests** run with `-Pdocuments test`; they read the documentation, not the code.
- **The acceptance suite** in `src/acceptance` runs as a person at a terminal on a machine with
  Porelo installed, a VM or a rented one, and needs an OpenRouter key and a model in
  `PORELO_E2E_OPENROUTER_API_KEY` and `PORELO_E2E_MODEL`; [`build.md`](build.md#acceptance) says how.
- **The pinned Claude Code version** moves as [`build.md`](build.md#bumping-the-cli-version) says,
  normally through the weekly update.
