# Building

Nothing here needs a checkout of [Porelo](https://github.com/porelo-ai/porelo). This
repository compiles against the **published agent contract** - `porelo-agent-api`
and `porelo-wire` - resolved from Maven, and that is the property worth keeping: if
a build here ever needs the Porelo sources, the split has been undone without
anyone deciding to undo it.

```
./mvnw -s settings.xml verify                     # unit tests
./mvnw -s settings.xml -Pnative,dist verify       # + the binary, the .deb and the .rpm
```

`-s settings.xml` is not optional while the contract is a snapshot: it declares the
repository the snapshot comes from. The native build needs GraalVM as `JAVA_HOME`.

What the second command leaves in `target/`:

```
porelo-agent-claude                              the adapter, run by Porelo on the host
porelo-agent-claude_0.4.0~SNAPSHOT_amd64.deb
porelo-agent-claude-0.4.0~SNAPSHOT-1.x86_64.rpm
```

**The package version is this agent's own**, not Porelo's. An agent released against
an unchanged CLI is still an upgrade, and a Porelo release does not move it. `~`
rather than `-` before `SNAPSHOT` because dpkg and rpm sort `~` below everything;
left as `-SNAPSHOT` it would sort *above* the release and apt would refuse the
upgrade.

The Claude Code version the image installs is carried in the package description
instead, so `dpkg -s porelo-agent-claude` and `rpm -qi` still answer it.

## Bumping the CLI version

`src/main/resources/agent/claude.yaml` pins one version and one digest:

```yaml
install:
  version: "${agent.cli.version}"
  artifacts:
    - url: https://downloads.claude.ai/claude-code-releases/${agent.cli.version}/linux-x64/claude
      sha256: "6c8818fa…"
      license: "SEE LICENSE IN README.md"
```

Anthropic publishes a per-version `manifest.json` carrying a SHA-256 per
platform, at
`https://downloads.claude.ai/claude-code-releases/<version>/manifest.json`, so
this is verifiable rather than trusted. Until it is bumped, every image build
installs the same bytes.

**Three lines change, and none is typed.** Porelo's release tool does it, resolved with the
machine tooling:

```
./mvnw -s settings.xml -Pci-tools dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "$(cat target/cp.txt)" org.fuin.porelo.release.Main update 2.1.267
```

What this agent differs in - where upstream is read, where the digest is read - is data, the
`porelo.release.*` properties beside `agent.cli.version` in `pom.xml`. It reads the digest from that release's manifest and the license from npm's entry for that
version, writes the `agent.cli.version` property in `pom.xml` and the `sha256` and `license` here,
and leaves a diff to review. The license lands in the bill, where the update gate stops on a change
to it. It
refuses a version that does not exist, one published without a `linux-x64` build,
and anything it cannot write in exactly one place. `--dry-run` says what it would do.

Everything else naming the version - the definition's `install` section, the download
URL, the description in both packages - is filtered from that one property and cannot
drift from it - `PinAgreementTest` reads the *filtered* definition from the classpath,
the file the binary answers `describe` with, and fails the unit tests when the version,
the pom and the download URL disagree. **The digest is the only thing that can drift**,
and asking Anthropic's manifest needs the network, so the tool's `check-pin`
checks it on every push. Exit 1 says they disagree; exit 2 says the manifest could not
be read, which is deliberately not the same answer.

**A CLI move is a patch bump of the module**, and `update` does that.
`agent.snapshot.run` - the CI run number - makes every build between two versions a strictly
newer package than the last, which is what apt and dnf sort on.

`porelo agents --supply-chain` reports what is pinned, so "which version ran" is
answerable from the installed adapter rather than from a build log. The acceptance
suite asks the installed machine that question and compares the answer with the bill
the package ships, so the two cannot drift apart unnoticed.

## The changelog

`CHANGELOG.md`, in [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.
**One sentence per change** - it is a compressed summary of the commits it covers, and
`git log` is where anyone who wants the reasoning goes.

Headings name this package's version; Claude Code's version is what the package installs and
appears as an entry rather than a heading.

**A version bump is not written by hand.** The tool's `update` writes its own line and
replaces the one it wrote last time, so a weekly job leaves one line rather than fifty-two a
year. The `(was ...)` it keeps is the version at the last release, not the last hop:

```
- Claude Code pinned to 2.2.0 (was 2.1.236).
```

Written by hand, the changelog would be one more place a bump has to be applied, and the one
that goes stale first.

**Everything else is by hand**, and nothing checks for it; requiring one is Porelo's changelog check - see
[`doc/decisions.md`](doc/decisions.md).

## Following Claude Code without watching it

`.github/workflows/update.yml` runs **once a week**, on Monday. It asks what
upstream's `stable` channel points at and, if that is not what this module pins,
does what a person would: the tool's `update`, rebuild, and prove the result on a real
Ubuntu machine and a real Fedora one **before anything is published**.

```
https://downloads.claude.ai/claude-code-releases/stable   the channel this follows
https://downloads.claude.ai/claude-code-releases/latest   every release, promoted or not
```

`stable` rather than `latest`, because the question "should a release have to be a
certain age before it is picked up" already has an answer that is not ours to
invent. The gap between the two is an observation, not a contract - nothing says
`stable` cannot move twice in a day. `workflow_dispatch` takes a version to
override both.

**The point is the stopping.** The run refuses to publish when:

| | |
|---|---|
| an acceptance suite failed | on either distribution |
| the third-party component set changed | the tool's `compare-bills` against the published bill |
| a license in the shipped tree changed | the same comparison |
| the upstream **major** version moved | flags, configuration and extension APIs move with it |

`--expect-moved claude-code` is what makes that comparison usable: the pinned CLI
carries its version in its purl, so the thing being updated always reads as one
component removed and another added. It is excused from the count and **its license
is still compared**; anything that arrived beside it still stops the run.

**A fact newly recorded in the bill stops the next update once.** The published bill does not have it
yet, so the comparison sees it change from nothing - a newly recorded license reads as *relicensed*
until a push publishes a bill that carries it. Expected, and cleared by publishing, not by approving
the update.

Whatever the outcome, the run opens a pull request - a green one says so, a stopped
one says which condition stopped it. Merging is what publishes.

**Auto-merge is off**, and the repository variable `PORELO_UPDATE_AUTO_MERGE` turns it
on. The run says which state it is in on every run rather than skipping a step in
silence. Turning it on also needs the secret `PORELO_UPDATE_TOKEN`, and that is not a
preference: a push made with the workflow's own `GITHUB_TOKEN` triggers no further
workflow, so merging with it would update `main`, publish nothing, and report success.
The job refuses that rather than producing it.

**How an update is proved before it is published.**
`Main acceptance --candidate target` installs the packages built in that run
instead of the published ones, in the same command that installs `porelo` from
Artifactory - so the repository, its signature, the index and `Depends: porelo` are
all still exercised, and one package comes from a file. `PORELO_E2E_EXPECT_CLI` makes
the suite fail if the machine ended up on any other version, so a candidate that
quietly did not install cannot pass.

## Publishing

A push to `main` uploads the two packages to Artifactory, into the **same repositories
Porelo itself publishes to** - `porelo-dist-deb` and `porelo-dist-rpm`. They belong
together: this package declares `Depends: porelo`, so split across repositories an
operator would have to configure both for the dependency to resolve.

A pull request builds and packages but publishes nothing.

Two repository settings are needed, the same ones Porelo uses: the variable `JF_URL`
(the platform url, **without** `/artifactory`) and the secret `JF_ACCESS_TOKEN`. The
token needs Read, Deploy/Cache, **Annotate** and **Delete** on both repositories -
Annotate because the Debian index is driven by properties, Delete because the snapshot
file name is stable and every build overwrites it.

`.github/workflows/artifactory-smoke.yml` checks all of that in about twenty seconds,
without building anything. Run it after rotating the token, or before wondering why a
publish failed.

## Acceptance

The scenarios in `src/acceptance` are the last step, and the only one that installs what an
operator installs. Everything before it proves the code is right; this proves the
**package** is. It runs on a stock Hetzner image that has never seen this project -
never a prepared snapshot - so it exercises the package repository itself: the
signature, the index, and `Depends: porelo` resolving from the same place.

`org.fuin.porelo.machines.Main acceptance` provisions the machine, installs from Artifactory
the way [the README](README.md#install) says, creates an unprivileged user (a task runs
rootless, so running the suite as root would prove less - and the scenarios refuse it), runs the
scenarios from the runner and destroys the server in a `finally`. A `cpx12` is enough - one core and 2 GB, because this installs
packages and runs a single prompt.

Both distributions, because they differ where packaging breaks: the `.deb`
path and AppArmor on Ubuntu against the `.rpm` path and SELinux enforcing on Fedora. Ubuntu
26.04, not 24.04 - Porelo needs podman 5, and 24.04 ships 4.9.3 for the whole of its life.

Two halves:

- **install** - the packages install, `porelo agents` lists an agent it was never linked
  against, `porelo setup` registers the hooks. No credential needed.
- **tier 2** - a task authenticates against OpenRouter and completes a prompt, and the
  real credential is then searched for in the container's environment and in every log
  the task left, and the broker is asked whether it saw the request. Needs
  `PORELO_E2E_OPENROUTER_API_KEY`; without it those scenarios are skipped and shown as
  skipped, so a fork or a revoked key loses coverage rather than turning the build red with
  no information. Each scenario brings a vault of its own, so what vault the account holds
  never matters.

**The model is set in the workflow**, `PORELO_E2E_MODEL` in `.github/workflows/build.yml`,
rather than in a repository setting - so it is visible in the diff, reviewable, and changes
with a commit rather than silently. Each agent picks its own: what is measured is that the
credential was accepted, so the model only has to be cheap and able to follow one instruction.
The same variable overrides a local run.

It is `z-ai/glm-5.3-flash`, about $0.00002 a run, chosen by measurement:
a `:free` variant returned `rate_limit_exceeded` on three consecutive attempts, and
`anthropic/claude-3.5-haiku` does not exist on OpenRouter at all despite this being the
anthropic-messages dialect - OpenRouter routes any model through it. Watch out for reasoning
models with a small token budget: at `max_tokens=64` the GLM models return empty text with
`stop_reason: max_tokens`, having spent the budget before saying anything.

Those last two checks are worth having even when authentication fails. A credential
scheme that authenticates by handing the real key to the agent has not failed loudly -
it has failed quietly, and only a real credential makes the leak searchable.

Run it by hand with `workflow_dispatch`, or locally:

```
HETZNER_API=... HETZNER_SSH="$(cat key)" PORELO_E2E_OPENROUTER_API_KEY=... \
  java -cp "$(cat target/cp.txt)" org.fuin.porelo.machines.Main acceptance \
      --os fedora --package porelo-agent-claude --cucumber .
```

`--candidate <dir>` in place of `--package` installs packages built locally rather than the
published ones - `porelo`'s among them, if the directory holds a build of each.

## What this repository cannot check

That a real Claude Code CLI reaches only the hosts its definition declares. That is the
domain-coverage check, it needs the resolver log from inside a task, and it lives in the
Porelo repository as `buildtools/e2e-tier1.sh`.

## Acceptance, as a person at a terminal

`src/acceptance` holds Cucumber scenarios that drive a real machine over ssh - a pty for what a
person sees, no pty for what a script gets - through Porelo's published acceptance kit. They are
off unless a host is named, so an ordinary build neither resolves the kit nor compiles them:

```
./mvnw -s settings.xml verify \
    -Dporelo.acceptance.host=<machine with porelo and this agent's package installed> \
    -Dporelo.acceptance.user=acceptance \
    -Dporelo.acceptance.key=$HOME/.ssh/id_ed25519
```

The report lands in `target/acceptance.html`. The scenarios tagged `@credential` need
`PORELO_E2E_OPENROUTER_API_KEY` and `PORELO_E2E_MODEL` in the environment of the machine running the
suite - typed into the vault at a terminal, never on a command line - and are **skipped**, not
passed, without them. There is no glue class here: every step is the kit's, which is what keeps
this repository free of test code that knows about ssh.

**A scenario's vault passphrase must be a string no terminal prints.** Each scenario that needs a
vault brings its own - a temporary file and a passphrase the scenario states - and the kit then checks
the passphrase never appears on screen. The rented machine's account is named `acceptance`, and
Ubuntu 26.04's shell integration prints `user=acceptance` in its escape sequences, so a passphrase of
`acceptance` reads as echoed there - and not on a VM whose account is `claude`, which is why a
passphrase can pass locally and fail on a rented machine.

In CI the same suite runs from the runner against the rented machine on every push to `main`.
A run that produces no scenarios fails rather than passing quietly.
