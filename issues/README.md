# Issues

What is open in this repository, grouped by state in the order of [workflow.yml](workflow.yml); a group
with nothing in it is left out. The index is generated from the issues' heads and never edited by hand.
Within a group, the order is the priority. The number is identity, not sequence.

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc63"></a>[CC63](CC63-The-Agent-s-Own-Telemetry-Goes-To-Porelo.md) | - | - | the adapter declares Claude Code's own OpenTelemetry export pointed at Porelo's local receiver in the task, with every switch that would carry content - prompts, tool arguments and output, responses - set explicitly off |
| <a id="cc48"></a>[CC48](CC48-The-Acceptance-Legs-Run-Side-By-Side.md) | - | - | a Build's acceptance legs run at the same time, two machines held at once instead of one, the Build done about one leg sooner |

## Accepted

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc64"></a>[CC64](CC64-A-WSL-Host-Keeps-The-Waived-rm-Check.md) | M | - | on a WSL host the task's Claude Code honours CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS again: a root-owned reg.exe in the task image answers Claude Code's two managed-settings queries as Windows does where no policy is set and refuses all else |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc23"></a>[CC23](CC23-Sign-The-RPMs-With-Porelos.md) | S | - | package-level signatures on the published RPMs |

## Backlog

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="cc69"></a>[CC69](CC69-A-Headless-Task-Shows-No-Skip-Permissions-Notice.md) | S | - | in a headless task the person never sees Claude Code's notice that CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS is set: the formatter drops its stream line and its stderr line, while the waiver stays and every other system line still passes |
| <a id="cc68"></a>[CC68](CC68-The-Vault-Scenarios-Leave-Once-Porelo-Holds-Them.md) | XS | - | credential.feature's three scenarios that test only Porelo's vault leave the legs once Porelo's own acceptance suite proves each |

## Implementing

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cc22"></a>[CC22](CC22-Depend-On-The-Agent-Protocol-Porelo-Provides.md) | S | - | the agent contract named at install time | - |
| <a id="cc40"></a>[CC40](CC40-The-Acceptance-Legs-Run-On-The-Host-s-Forge.md) | S | [CC38](README.md#cc38) | the acceptance legs on the host's pool again, once it lends a forge job a machine by kind | 0 |

## Review

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="cc55"></a>[CC55](CC55-The-Acceptance-Takes-A-Task-s-Container-Name-From-Porelo.md) | XS | - | the acceptance scenarios name a task by its own name in Porelo's commands and take its container's name from `porelo task status --json`, never built as `porelo-<project>-<task>` | - |
| <a id="cc56"></a>[CC56](CC56-The-Adapter-Reports-Each-Tool-Call-With-Its-Time.md) | M | - | the adapter reads each tool call from its agent's output and hands Porelo its tool, kind, start, end, a shell call's command line, whether it ran in the background, and a sub-agent's name and the calls made under it, in Porelo's common entry | 0 |
| <a id="cc57"></a>[CC57](CC57-Measure-What-The-Agents-Streams-Tell-About-Tool-Calls.md) | S | - | a measurement, no product code: each agent Porelo runs, given the same small order unattended in a task, and what its raw stream says of each tool call - name, input, start, end, own time, background jobs, sub-agents - and how the wall time splits | - |
| <a id="cc59"></a>[CC59](CC59-Measure-What-The-Agents-Streams-Tell-About-Sub-Agents.md) | S | - | a measurement, no product code: each agent Porelo runs that has sub-agents of its own, ordered unattended in a task to start one sub-agent by a given name, and what its raw stream says of that sub-agent - its start, its end, its name, and which calls are its own | - |
| <a id="cc61"></a>[CC61](CC61-An-Attended-Session-Takes-An-Opening-Text.md) | S | - | an attended session of Claude Code opens on the person's opening text as its first turn and then waits for the person, the form (positional) declared in the agent definition and measured in a task | - |
| <a id="cc62"></a>[CC62](CC62-A-Finished-Run-Answers-Its-Last-Message.md) | S | - | a finished unattended Claude Code run answers the text of its last message, declared in session.waiting.last_message, so a reader's answer is not empty; the declaration adds no screen rule and so changes nothing in how Porelo wakes the agent | - |
| <a id="cc66"></a>[CC66](CC66-The-Protocol-Refusal-Played-As-A-Person-Would.md) | S | - | a script plays story ST35 on the test VM as a person would: Claude Code's adapter installs beside a Porelo it speaks with and is refused beside one it does not, and `task start --now` is refused rather than accepted without effect; part of ST347 | - |
| <a id="cc45"></a>[CC45](CC45-Mutation-Testing-With-PIT.md) | S | - | mutation testing by hand: a `pit` profile outside CI with no threshold, and a report sorting survivors into real gaps and harmless ones | - |
| <a id="cc49"></a>[CC49](CC49-The-Clean-Up-Says-When-Nothing-Was-Rented.md) | XS | - | a leg that failed before resolving the machine tooling rented nothing, and its clean-up says so instead of failing on a missing class | 0 |
| <a id="cc50"></a>[CC50](CC50-The-Tests-Follow-The-Pyramid.md) | S | - | the suites by level with measured times, no flow of the adapter only above the unit level; five end-to-end scenarios that test only Porelo leave the legs, and the timeout test waits 100 ms | - |
| <a id="cc51"></a>[CC51](CC51-A-Task-Never-Stops-On-A-Question-Of-Claude-Code.md) | M | - | a task's Claude Code never stops to ask whether to run a command: what its safety checks still question under bypass mode, such as an `rm -rf` on a possibly-empty variable, runs at once, proven by an unattended acceptance scenario | - |
| <a id="cc52"></a>[CC52](CC52-The-First-Words-Agree-With-The-Task.md) | S | - | the first words of the adapters' tasks agree with the task: the push, the list of checks, the two rules of `AGENTS.md` they replace, the room only for what needs a person, and a guarded `rm` in the skill fetch | - |
| <a id="cc60"></a>[CC60](CC60-The-Build-Takes-Its-Downloads-From-A-Cache.md) | S | - | the Claude Code binary the update job checks and the runner's packages come from the Actions cache, keyed by digest or package manifest, so an outage of their hosts no longer fails a run that already ran once; what must stay live is named, and an entry nobody reads for 7 days is removed by GitHub | - |
| <a id="cc42"></a>[CC42](CC42-A-Sign-In-Without-A-Browser-Is-Tested-On-Every-CLI-Update.md) | M | - | the sign-in at a terminal without a browser, checked on every CLI update so a release that drops it fails its pin bump | 0 |
| <a id="cc47"></a>[CC47](CC47-Snapshots-Not-Published-On-The-Forge-Come-From-Upstream.md) | S | - | on a forge whose registry holds only its organization's packages, the other Porelo snapshots come from the anonymous repository `MAVEN_UPSTREAM_SNAPSHOTS_URL` names, the forge's token sent to the forge alone, a tag still built from releases only | - |
| <a id="cc46"></a>[CC46](CC46-A-Task-Writes-Its-Own-Commit-Trailer.md) | M | - | a task's commits end with `Assisted-by: Claude Code, <model>` written by a git hook from the session's own model, with Claude Code's own attribution switched off in the settings the adapter writes | - |
| <a id="cc44"></a>[CC44](CC44-Subagents-Only-As-Many-As-The-Task-Allows.md) | S | - | no subagent in a task unless the task allows it, then at most as many as it says; off, count and depth measured to hold at the pin | - |
| <a id="cc53"></a>[CC53](CC53-Third-Party-Notices-Ship-With-The-Package.md) | S | - | the license texts of what the executable compiles in from outside Porelo - SnakeYAML, JSpecify, the GraalVM runtime - in one THIRD-PARTY-NOTICES file, in the deb's doc directory and as `%license` in the rpm | - |
| <a id="cc38"></a>[CC38](CC38-The-Build-Runs-On-The-Local-Forge-As-On-The-Central-One.md) | S | - | the workflows run 1:1 on the local Forgejo and the central forge: snapshots from `MAVEN_SNAPSHOTS_URL`, publishing switched by `PORELO_PUBLISH`, legs asked by kind, green on a local Forgejo | - |
| <a id="cc54"></a>[CC54](CC54-The-Busy-Scenario-Holds-Its-Loop-In-The-Foreground.md) | S | - | `wake.feature`'s busy scenario no longer fails when the model sends the wait loop to the background: it sees the loop in the foreground before it sends the message, or sets the loop up again | 0 |
| <a id="cc41"></a>[CC41](CC41-Snapshots-Resolved-With-A-Read-Only-Token.md) | S | - | on a forge other than the central one, snapshots resolved with the runner's read-only token, the publisher's token in no step, since nothing here deploys to the registry, and a Build without `MAVEN_SNAPSHOTS_URL` refused at its start | - |
| <a id="cc39"></a>[CC39](CC39-The-Repository-s-Agent-Works-In-A-Porelo-Task.md) | S | [CC38](README.md#cc38) | the repository's agent moves into its own Porelo task, after a dry run on this machine | - |
| <a id="cc58"></a>[CC58](CC58-The-Status-Screen-Shows-No-Installation-Warning-In-A-Task.md) | S | - | the pinned CLI placed in a task's image the way its own installer places it, so its status screen reports no installation problem, while it still never updates itself | 0 |
| <a id="cc67"></a>[CC67](CC67-A-Continued-Session-Is-Asked-For-Its-Word.md) | XS | - | session.feature's continued-conversation check asks the restarted Claude Code for the word it was told, instead of looking for the first turn's text on the visible screen | - |
| <a id="cc65"></a>[CC65](CC65-The-Issue-Heads-Are-Checked-Every-Day.md) | XS | - | a daily run checks this repository's issue heads and index with the released porelo-release, as buildtools' shared rules do | - |

## Finished

| # | Title | What was built |
|---|---|---|
| <a id="cc36"></a>[CC36](README.md#cc36) | Report What Claude Code Finds Of Itself On The Host | The adapter reports Claude Code's sign-in on the host by kind, provider and path, never by value, and says "cannot say" where it cannot tell; `vault import` also takes a `primaryApiKey`. |
| <a id="cc43"></a>[CC43](README.md#cc43) | The Sign-In Says How It Is Answered | `claude.yaml` declares `login.flow: paste-code`, so a person signing in to Claude Code from a host is told to open the link and paste the code it shows; tested in sprint 6m: passed. |
| <a id="cc34"></a>[CC34](README.md#cc34) | Take The Legs From A Run That Proved The Same Tree | A Build on `main` asks `porelo-release proven-legs` whether a Build or an update run already proved the pushed tree on both legs within a day and takes those legs instead of renting again, while a tag always runs its own. |
| <a id="cc37"></a>[CC37](README.md#cc37) | Let The Project Grant What Its Build Needs | A task's Claude Code no longer refuses `raw.githubusercontent.com`, so it resolves where the project grants it and `check-citations` runs there, while the Datadog log intake and `downloads.claude.ai` stay refused. |
| <a id="cc35"></a>[CC35](README.md#cc35) | Publish Without A Downloaded Client | The Build and the smoke workflow talk to Artifactory with `curl` through `.github/scripts/artifactory.sh`, with the JFrog CLI and its setup action removed. |
| <a id="cc24"></a>[CC24](README.md#cc24) | Prove OAuth Isolation End To End | An acceptance scenario signs in with a subscription against a stand-in token endpoint and shows the refresh token never reaches the task: only the host renews, and the broker refuses a renewal from inside it. |
| <a id="cc30"></a>[CC30](README.md#cc30) | Switch Off The Agent's Own Memory In A Task | A task's Claude Code keeps no memory of its own: auto memory is off in the settings the adapter writes, so what holds across tasks is only the repository's `AGENTS.md`. |
| <a id="cc32"></a>[CC32](README.md#cc32) | Answer The Auto Mode Question At First Run | A task on Claude Code 2.1.292 or later reaches work without being asked whether to make auto mode the default: the adapter marks the question seen and the task keeps bypass mode. |
| <a id="cc33"></a>[CC33](README.md#cc33) | Reformat To The Shared Style | The code is in the shared style, google-java-format in its AOSP variant with YAML and Gherkin at four spaces; `validate` refuses code that is not, and `git blame` with `.git-blame-ignore-revs` skips the reformatting. |
| <a id="cc29"></a>[CC29](README.md#cc29) | Read The Code Changed Since The Last Skills Review | The code changed since 2026-09-27 read against seven skills, each finding fixed, with a test where it could do harm: no terminal control character reaches a log, and no checkout keeps the job's token. |
| <a id="cc31"></a>[CC31](README.md#cc31) | Rename The Adapter Porelo | The repository, its artifact, its packages, its Java packages and its documents say Porelo; a documents test keeps the old name to the released part of `CHANGELOG.md`. |

## Where an agent's requirements live

An agent's work lives in its own repository, so a requirement for this agent is an issue here, and
what outlived the ones already met is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `porelo-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
