# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code. When one was taken is in
`git log`.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

Ordered by what each covers: the task container first, then the pinned CLI, then how the
repository is built.

| What | Decided |
|---|---|
| **The task container** | |
| [A task gets its credential in a variable and no credential file](#a-task-gets-its-credential-in-a-variable-and-no-credential-file) | the CLI reads the variable and ignores the file, for both kinds |
| [Why this agent needs container setup at all](#why-this-agent-needs-container-setup-at-all) | it calls the vendor before a session, and a fresh container has never logged in |
| [What was actually proven about brokering this agent](#what-was-actually-proven-about-brokering-this-agent) | which transport, which credential kind, against what |
| [`api.anthropic.com` stays reachable, and a log intake is refused](#apianthropiccom-stays-reachable-and-a-log-intake-is-refused) | the CLI quits without it; the task's token is all a deny would keep in |
| [The API-key dialog is answered, because the key is the task's own](#the-api-key-dialog-is-answered-because-the-key-is-the-tasks-own) | the key shown is a phantom token, and the recommended answer refuses it |
| [Reaching work is checked by what the screen shows, not by the dialogs known](#reaching-work-is-checked-by-what-the-screen-shows-not-by-the-dialogs-known) | a declared marker, waited for attended; proven to fail against the dialog put back |
| [Waiting for a person is read from the question's own last line](#waiting-for-a-person-is-read-from-the-questions-own-last-line) | the line only a question draws, in the last lines of the screen |
| [A task that comes back continues its conversation](#a-task-that-comes-back-continues-its-conversation) | the session id read from Claude Code's own record, unattended or attached |
| **The pinned CLI** | |
| [The CLI does not update itself in a task](#the-cli-does-not-update-itself-in-a-task) | the pinned version is the one that runs |
| [A model the CLI does not know keeps its own window](#a-model-the-cli-does-not-know-keeps-its-own-window) | the provider says when the context is full, not a window the CLI assumes |
| [Accepted risk: the release binary and its digest share one trust root](#accepted-risk-the-release-binary-and-its-digest-share-one-trust-root) | nothing independent to verify the download against, and why that stays |
| **The build** | |
| [The release tooling is Porelo's, configured from the pom](#the-release-tooling-is-porelos-configured-from-the-pom) | data beside the pin, never a copy of code |
| [A release is built from releases only](#a-release-is-built-from-releases-only) | on a tag nothing from Central's snapshots, and a tag on a Porelo snapshot refused by the step itself |
| [Actions run from a commit, and Dependabot moves them](#actions-run-from-a-commit-and-dependabot-moves-them) | every `uses:` by commit, GraalVM by Porelo's pin, checked by `check-actions` |
| [What an update takes, and when](#what-an-update-takes-and-when) | three days old and still the newest, verified on rented machines before the pull request |
| [NullAway comes from `porelo-parent`](#nullaway-comes-from-porelo-parent) | one compiler configuration for every repository; `.mvn/jvm.config` stays here |
| [No check requires a changelog entry](#no-check-requires-a-changelog-entry) | requiring one belongs to Porelo's changelog check, on logchange |

## Why this agent needs container setup at all

Two measured facts are the reason `ClaudeContainerSetup` exists rather than the agent simply being
installed and started.

- **It contacts the vendor directly before an interactive session**, ignoring the endpoint it was
  given. So that host has to be reachable even when every model request goes through the broker,
  which is why `claude.yaml` lists `platform.claude.com`, `claude.ai` and `statsig.anthropic.com`
  in `allowed_domains` rather than relying on the provider's host alone.
- **A fresh container has never been logged in**, so the first-run wizard must be answered for it
  or the session stops waiting for input nobody will type. That is what `ClaudeFirstRun` writes,
  with the answer to the question 2.1.292 added, whether to make auto mode the default: it is marked
  seen, so the task stays in bypass mode, which the container is what makes safe.
  The file is readable by others unless it approves an API key, which puts part of the key in it.

## What was actually proven about brokering this agent

Written down because "verified" without the shape of the verification is a claim rather than a
record.

It honors **both** a base URL and a unix socket, with **either** credential kind - an API key or a
subscription token - **including a token minted for the task** rather than the real credential. That
is why `claude.yaml` sets `ANTHROPIC_UNIX_SOCKET` and `ANTHROPIC_BASE_URL` together: the socket
selects the transport, and without the base URL the CLI falls back to its compiled-in endpoint.
That fallback was measured - it resolved `api.anthropic.com` 184 times in one run and never touched
the socket.

## `api.anthropic.com` stays reachable, and a log intake is refused

**`api.anthropic.com` is reachable from the container on purpose.** Before it starts interactively,
the CLI opens a connection to that host, ignoring both the base URL and the socket. Measured in one
container, only the reachability of that one name differing:

| `api.anthropic.com` | interactive `claude` |
|---|---|
| not resolvable | `ENOTFOUND`, quits |
| resolvable, nothing listening | `ConnectionRefused`, quits |
| reachable | starts normally |

What a deny would keep inside is the task's token, which is random, ends with the task and is worth
nothing to Anthropic. The real credential never enters the container. `platform.claude.com` is the
same case: it is contacted before an interactive session starts, and the CLI quits without it.

**A Datadog log intake is refused, not left out.** Measured on 2.1.236, the CLI resolves
`http-intake.logs.us5.datadoghq.com` during a normal run and works without it. It is declared under
`refused` so a test can tell a policy from a mistake, and a person can see what is blocked.

## A task gets its credential in a variable and no credential file

**The token reaches Claude Code in the variable the definition names for its kind** -
`ANTHROPIC_API_KEY` or `CLAUDE_CODE_OAUTH_TOKEN` - and the container gets no `.credentials.json`.
Measured on 2.1.267 at a terminal, a fresh task per run: with the variable removed, a
`.credentials.json` holding the same token gave no API key and no auth token at all, for both kinds;
with the file removed and the variable set, the task authenticated. The OAuth shape is read only once
it carries the `scopes` a real login writes. So the file would be a second copy of the token that nothing
reads, and a claim of a subscription nobody checks.

An unknown credential kind is still refused when the task is set up, rather than passed on in the
default variable where it would fail looking like a wrong key. A person logging in inside a container
gets the file from the login itself.

## The API-key dialog is answered, because the key is the task's own

Started with a key in `ANTHROPIC_API_KEY`, Claude Code asks whether to use *"a custom API key"* and
recommends **No**. Answering it is not a billing decision: the key shown is the **phantom token
Porelo minted for this task**, not a payment credential. The billing decision is made elsewhere -
when the real credential goes into the vault and a provider is chosen.

**The recommended answer is the harmful one.** Taking *No* refuses the only credential the task
has, in a dialog that describes it as suspicious. Unattended there is nobody to answer at all.

**How it is answered, measured on Claude Code 2.1.267:** the last 20 characters of the key under
`customApiKeyResponses.approved` in `.claude.json` remove the dialog completely. `ClaudeFirstRun`
writes the approval only for an `api-key` task, the one kind the dialog is about - an OAuth token
travels in `CLAUDE_CODE_OAUTH_TOKEN`, which the CLI does not ask about. Measured a fresh task per
run at a real terminal: with the approval the CLI reaches its prompt; without it, it opens at the
dialog, recommending No. The file then holds part of the key, so it is written owner-only. **A
headless run never shows the dialog either way** - with and without the approval it answers and
exits - so only an attended start tells the two apart.

**What would change the answer:** the environment carrying a real payment credential instead of a
task-scoped token. Then this would be a billing decision again, and it would be asked.

## The CLI does not update itself in a task

**Measured on Claude Code 2.1.267**, in a home laid out like the container's: within 90 seconds of
starting, the CLI asks `downloads.claude.ai/claude-code-releases/latest`, downloads a newer version
into `~/.local/share/claude/versions/` and shows *"Update installed · Restart to update"*. The next
start would run a version nobody pinned and whose digest nobody compared.

**How it is stopped:** `DISABLE_UPDATES=1` in the `env` of the container's `settings.json`. With it,
the same run downloads nothing, and `claude update` answers *"Updates are disabled by your
administrator"*.

- **Not `DISABLE_AUTOUPDATER`**, which stops only the background update. The agent has a shell and
  could run `claude update` itself.
- **Not `autoUpdates: false`**, which the CLI writes into `.claude.json` itself and then ignores for a
  native install, because it also sets `autoUpdatesProtectedForNative`.
- **In `settings.json` rather than the container's environment**, because that file is this
  repository's and the environment is Porelo's. Measured to work from there.

**Behind it, the network:** `downloads.claude.ai` is in `refused_domains`, so inside a task it
answers NXDOMAIN - although `claude.ai` above it is allowed - while `claude.ai` and
`platform.claude.com` still resolve and the CLI starts. Measured in a task; without the entry the name
resolved. It is a second line and a weak one: a name that shares an address with an allowed host keeps
that address reachable, and nothing finer than a host can be refused, since the traffic is TLS and not
terminated.

**What would change it:** nothing in the CLI's behavior. A new version comes through the weekly
update job, which verifies it on both distributions before anything is published.


## A model the CLI does not know keeps its own window

**Measured on Claude Code 2.1.267** with `z-ai/glm-5.3-flash` over OpenRouter, in a task: at every
start the CLI says the model *"isn't described by this version's model catalog"*, and that until it is
mapped *"auto-compact keeps this session within 200k tokens (the context window it assumes)"*. That
holds for every model outside the CLI's catalog, which other vendors' models over OpenRouter are.

**How it is answered:** `CLAUDE_CODE_DISABLE_UNKNOWN_MODEL_WINDOW_ENFORCEMENT=1` in the `env` of the
container's `settings.json`. With it the notice is gone, the model answers, and the CLI compacts when
the provider reports the context full, as it did before 2.1.267. By the CLI's own check it applies
only to a model outside its catalog. Measured to work from `settings.json`.

- **Not `CLAUDE_CODE_MAX_CONTEXT_TOKENS`**, which also silences the notice but needs each model's real
  window. The adapter does not know it; the provider does.
- **Not a `modelPicker` row with `behavesAs`**, which silences it by giving the model the prompt profile,
  capabilities and effort defaults of a model the CLI knows. That claims for an arbitrary model what
  nobody measured.
- **Not `[1m]` on the model name**, which asserts a 1M window for whatever model is named.

## Reaching work is checked by what the screen shows, not by the dialogs known

**The acceptance run fails when anything comes before Claude Code's prompt**, known or not. The
definition declares the text the CLI shows once at work - `bypass permissions on`, the footer at the
prompt - and the kit's step waits for it attended, typing nothing; a second scenario requires an
unattended run to end within its bound. Measured on a test VM: both pass, and with the API-key
approval taken out of the build the attended step waits its 300 s and fails with the dialog on the
screen it reports. So a release that adds a question fails the run the day it arrives, which "the
known dialogs are absent" would not.

## Accepted risk: the release binary and its digest share one trust root

This package installs Claude Code from `downloads.claude.ai`, pinned to a version and checked
against a SHA-256 recorded in `agent/claude.yaml`. The digest that pin is compared with comes from
`manifest.json` on **the same service**.

That protects against a corrupted or partial download, a mismatched pin and an accidental version
drift. It does **not** prove authenticity: anybody who can change both objects - a compromised CDN,
release account, publication path or TLS termination - passes every check this repository makes.

**Why it is accepted rather than fixed:** the vendor publishes no signature, attestation or
independent digest source that we could verify against. A check we cannot perform cannot be
written. The alternative - refusing to ship the agent at all - removes a product rather than a
risk.

**What would change it:** a signed release, a provenance attestation, or a digest published
through a channel with a different operator.

**What reduces it:** the version is pinned rather than floating, so an unreviewed release cannot
arrive on its own; the digest is checked before installation in the image layer; and the update job
opens a pull request rather than publishing by itself.

## Waiting for a person is read from the question's own last line

Porelo tells a person the agent is waiting for them from `session.waiting` in the agent's YAML: literal
lines on the attached screen that only a question draws. Measured on 2.1.267 at a terminal, a
question Claude Code puts ends with `Enter to select · ↑/↓ to navigate · Esc to cancel`, and its first-run consents with `Enter to confirm · Esc to cancel`. The rule is `Esc to cancel` in the last 5 lines. At work its last lines say `esc to interrupt`, never "cancel", and matching ignores case, so the rule cannot be the shorter `esc to`.

**Proven three ways.** `WaitingDeclarationTest` reads the declaration against the measured screens, and
fails without it. By hand on the VM, Porelo read the agent as not waiting at rest and as waiting once it
asked. `waiting.feature` drives it to a question at the version it pins and asks Porelo, never the
screen - so a release that words its questions differently fails the build instead of going quiet. The scenario types the prompt, then presses Enter as a separate step. A line feed is a new line in its prompt, and text with its carriage return in one write reads as a paste, whose carriage return does not submit either - so `I enter` alone never asks anything here.

**Unattended there is nothing to declare**: a run that wanted to ask ends, and Porelo says what it said
last rather than that it waits.

**What would change the answer:** a release that draws its questions differently, which the scenario is
there to catch.

## A task that comes back continues its conversation

Porelo records the session a task's agent ran and passes it back with `--resume` when the task starts
again. Where the id is, `claude.yaml` declares under `session.session_id`. Measured on 2.1.267: an
unattended run's first record, `{"type":"system","subtype":"init"}`, carries `session_id`, and the same id
names the session file of any run, `~/.claude/projects/-workspace/<id>.jsonl` - the working directory with
`/` as `-`, and Porelo's is always `/workspace`. So one id serves an unattended and an attached task alike.

**Proven at the version it pins.** `SessionIdDeclarationTest` reads the declaration against the measured
records and file names. `session.feature` drives it through Porelo on a VM: a word to remember in one run,
the task started again, Porelo reporting that it continues the session, and the word given back although
the second prompt never names it.
The attached half is proven the same way: a message, the task stopped and started
again, and the earlier message back on its screen.

## The release tooling is Porelo's, configured from the pom

The release tooling is the shared `porelo-release`, not tools copied into each agent repository:
copies drift apart.

**What differs between agents is data**, and it lives in `pom.xml` as `porelo.release.*`, beside
`agent.cli.version`: the label, the source definition, where upstream is read, the channel, where
the digest is read. The pin lives there and every command reads it there. The alternative, flags on
each call, would put the same five facts on every workflow line - twice in `update.yml` - and the
second copy is the one that goes stale.

**At package time the tool is a plugin dependency of the exec plugin**, put on the classpath with
`includePluginDependencies`, not a dependency of the project: a project dependency would land in the
bill of materials and on the native image's classpath. The workflows resolve it through
`-Pci-tools`, beside `porelo-machines`.

**Measured against the tools it replaces, on the same inputs:** the component `add-fetched-cli`
records is identical; `compare-bills` stops with exit 1 on a wrongly named update, on an unnamed new
component and on a changed license; `upstream-version` answers `rollback` for a channel pointer
older than the pin; `check-pin` fails on a digest changed by one character.

**What would change it:** a second agent-specific fact that is not a property of the pin - then it
belongs in the tool as a named strategy, as the three upstream kinds are.


## A release is built from releases only

Between releases everything is built on snapshots: `porelo.version`, the parent, and with the parent the release
tooling. A tag's build takes none of them.

- **Nothing from Central's snapshots on a tag.** The snapshot repository is a profile in `settings.xml` that
  switches itself off when `porelo.release` is set, and `build.yml` sets `MAVEN_ARGS` to `-Dporelo.release` on a tag
  and to `-U` elsewhere. Measured: with it, a build from an empty local repository cannot resolve the snapshot
  parent; without it, it does. Not `-P!standard`: Maven then lists the profile as inactive and still takes the
  parent from its repository, also measured.
- **Nothing asked for again on a tag.** `-U` is in no command; `MAVEN_ARGS` carries it where it belongs, also into
  the `pinned-jdk` action.
- **A tag on a snapshot is refused.** "Which channel" refuses a `porelo.version` that names a snapshot, in the build
  job and in the release job. `ReleaseChannelTest` runs both steps as a tag's run would, sees the refusal, and sees
  a released Porelo let through; it was seen to fail with the refusal taken out. `check-releases` then refuses any
  snapshot left in the effective pom.
- **Secrets only where a tool needs them.** The tooling is resolved and the channel decided in steps that hold no
  secret. The steps that hold one run only what needs it: the acceptance legs and their clean-up the cloud's key and
  the provider's, publishing the repository's token.
## Actions run from a commit, and Dependabot moves them

Every `uses:` names a full commit with its release beside it, `@<commit> # vX.Y.Z`. A tag is a name its
owner may point anywhere, and these jobs hold the publishing token, the machine credentials and a
token that merges pull requests. GitHub's own actions are held to the same rule: the argument does not
depend on who publishes the action.

**Dependabot keeps the pins current**, in every repository. A
pin nobody moves rots, and a stale action with a known flaw is not safer than the current tag. Each
release arrives as a pull request with the new commit and its version, and nothing merges it
automatically: the review is what the pin buys.

**`porelo-release check-actions` fails the build on a step that names a tag, a branch, a bare hash or a
line of releases like `# v7`**, and says how to pin it, so a new workflow cannot bring a tag back. It is
Porelo's one check for this rule, run in the build job, and this repository keeps no second test for it.
A step of this repository and an image by digest pass. Measured against this repository's workflows,
with a tag, a `# v7`, a `setup-graalvm` at a commit and a Dependabot without `/.github/actions/*` each
put in: every one refused.

**Dependabot watches the local actions too, and waits three days.** `dependabot.yml` lists
`/.github/actions/*` beside `/`, because the pinned-JDK action pins its cache, and a Dependabot that
watches only the workflows never moves that pin. A release is taken after three days, as every other
pin here, and the week's moves come as one grouped pull request. `check-actions` fails when the local
actions are not watched.

**`mvnw` checks the Maven it downloads**: `distributionSha256Sum` in `.mvn/wrapper/maven-wrapper.properties`,
for 3.9.15 the digest Apache's own SHA-512 confirms. A wrong one stops the wrapper before Maven runs.

**Nothing else in these workflows is fetched by a name**: `curl` reads and writes this product's own
Artifactory through `artifactory.sh`, and reads plainly the Debian index a release is checked against
before it is published - and, in the update job, the Claude Code binary the pin names, checked against the pinned
SHA-256 before it is read. The setup actions download their tools themselves:

- **Publishing downloads no client.** `.github/scripts/artifactory.sh`, taken byte for byte from porelo,
  speaks to Artifactory's REST interface with `curl`; the token goes into a curl config file of mode
  600, so neither `set -x` nor a process listing shows it. The JFrog CLI and its setup action are gone.
- **GraalVM is the one Porelo pins, checked against its digest.** `setup-graalvm` with `'25'` resolves
  the newest 25.x at run time and checks nothing for community builds - and that JDK compiles the
  native binary this repository publishes. So every job uses `./.github/actions/pinned-jdk`: the
  runner's own Java 25 runs `porelo-machines jdk --github` once, which installs the GraalVM
  `porelo-machines` pins (`machines.graalvm.*`, moved by Porelo's Machines workflow under the same
  three-day rule), checks its digest before unpacking, and sets `JAVA_HOME`. The build and the
  acceptance machines use the same pin, by construction. `check-actions` refuses `setup-graalvm` and
  `setup-java` even at a pinned commit, because the commit fixes the action, not the JDK it fetches.
  Measured on a VM as a stand-in runner - an empty home, Temurin 25 in place of
  the runner's Java, Porelo's artifacts at `0bce03a`: GraalVM 25.0.2 installed and checked, and this
  repository's native build and packages made with it.

## What an update takes, and when

Porelo's release tool applies these rules; `UpdateRulesTest`
fails when this repository stops asking for them.

**A release is taken once it is three days old, and still the newest**: `porelo.release.min-age` is
`3d`. A release withdrawn or patched within days never becomes a pull request, and the people who
install on the first day get the days to report what breaks. A younger release is not skipped for the
one before it: the job waits and says until when. A release whose date cannot be read is never old
enough.

**A pin move bumps the package's patch version**, `0.4.1-SNAPSHOT` to `0.4.2-SNAPSHOT`, so a package
version names what it installs and `apt` sees an upgrade.

**The verifying tier makes a real model call**, with its key in the workflow's secrets - intended, not
temporary. Published metadata proves the download is intact; only a real request proves the new
version still starts without a question, reads its credential variable, routes through the broker and
gets an answer.

**The CI machines are not kept current here.** GraalVM and the pre-pulled base images are Porelo's
machine tooling, under the same rule, once for every repository.

**What would change the answer:** a release that cannot wait three days. Dispatching the job with the
version named takes it at once - that is the way round the rule, not a change to it.

## NullAway comes from `porelo-parent`

The compiler configuration that runs NullAway - Error Prone with only NullAway, `OnlyNullMarked`, the
two processor paths - is `porelo-parent`'s, managed for every Porelo repository, so this pom declares no
compiler plugin. Only the `default-compile` execution runs it: tests pass `null` on purpose, and Error
Prone never sees them.

What stays here is `.mvn/jvm.config`: Error Prone runs inside javac in Maven's own JVM, and from JDK 16
on that JVM refuses it the compiler's internals - measured on JDK 25, an `IllegalAccessError` on
`com.sun.tools.javac.api` before a single file is checked.

## No check requires a changelog entry

The changelog is written by hand in the same commit as the change; nothing enforces it. Porelo is
moving to logchange - one YAML file per change, and a generated `CHANGELOG.md` - and a check for a
hand-kept file would have to be rebuilt the moment that reaches this repository. Requiring an entry
is Porelo's changelog check, proposed to logchange upstream first, and keeps three rules: a waiver answers for its
own commit only, documentation is not exempt, and a range that cannot be compared fails.

**What would change it:** Porelo's changelog check landing, or logchange being adopted here.
