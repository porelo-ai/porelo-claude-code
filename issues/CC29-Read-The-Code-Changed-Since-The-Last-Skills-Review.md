# CC29 — Read The Code Changed Since The Last Skills Review

**Status:** Review.
**Size:** M - 33 files read against seven skills, each finding that can do harm fixed with its own
test; no contract another repository reads.

**What must be true.** The code changed since this repository was last read against its skills, on
2026-09-27, has been read against them as well, and the versions read are on record.

## Why

The reading of 2026-09-27 went through the whole repository against the seven skills `AGENTS.md` names
and was closed in `8caca0c` (on `pre-public`, since the history was squashed into `main`). Everything
written after that was never read against them, and that reading did not record the version and SHA-256
of the skills it read, although its acceptance asked for both. The shared rule now says a repository's
skills are applied, not only named.

## Plan

- **Fetch the seven skills** - `graal`, `java-code-review`, `test-quality`, `security-audit`,
  `concurrency-review`, `clean-code`, `solid-principles` - in their latest version from
  `skills.json`, as *Skills* in `AGENTS.md` says, each checked against the SHA-256 its storage
  reports, and load them in the harness (asked of the harness, not read from a directory).
- **Fetched on 2026-10-17**, each SHA-256 matching what `agent-skills` reports:
  - `graal` 2026.910.3, SHA-256 `b24ec6272fb13490e32977c21d4543869a7e85ed474fdc0ff34022fbddd88bf6`
  - `java-code-review` 2026.906.3, SHA-256 `802f86339442ed9fa9d5bca14b6d16e1f6420ae92a047b18bba11239e32c698b`
  - `test-quality` 2026.906.3, SHA-256 `9c5d23ad7a6d9c6f5c3679f3cc44a7fcc2fb3cc1c0f0d0ba4a2c0f7812428bad`
  - `security-audit` 2026.906.3, SHA-256 `a46fd1be07bd539b955291537dea0f24f65a2f8b15d40d50a6a9098bbd992747`
  - `concurrency-review` 2026.906.3, SHA-256 `3a0aab612f25483a3fe777eb18fbcc434e936b19298708bf88beff8b22d79e56`
  - `clean-code` 2026.906.3, SHA-256 `5cf777a24630298b4b801c934f599722978c21aeac53d61683243908fc041c7b`
  - `solid-principles` 2026.906.3, SHA-256 `bd47cdab9126dde76cf1703472167a1c88c85306a4e0263a8f2db4c462740cf5`
- **No interface another repository reads changes:** the manifest's fields, the packages' names and paths,
  the service file, the guide written into a task. A finding that would change one is named in the channel,
  not built.
- **`persist-credentials: false` on every `actions/checkout`**, as decided for every repository on
  2026-10-17. Its red check is the shared `check-actions` from Agent Core's tools snapshot, run on today's
  workflows. The checkouts in `build.yml` and `shared-rules.yml` push nothing.
- **`update.yml` pushes its branch with git** (`git push --force origin "$BRANCH"`, :267), today through
  the write token the checkout leaves in `.git/config` for the whole job, the build and both acceptance legs
  included. The operator allowed the change on 2026-10-17: its checkout gets `persist-credentials: false`
  as well, and only the push step gives the token, never on a command line. In that one step the shell
  builds `x-access-token:<token>` in base64 with `printf` (a builtin) into `base64` on stdin, masks it with
  `::add-mask::`, and exports it as `GIT_CONFIG_COUNT=1`, `GIT_CONFIG_KEY_0=http.https://github.com/.extraheader`
  and `GIT_CONFIG_VALUE_0=AUTHORIZATION: basic <value>` for the `git push` after it; nothing goes to
  `$GITHUB_ENV`, so no later step holds it. `gh pr create` keeps `GH_TOKEN`. **Still to be seen:** only
  the next real update run pushing its branch proves it.
- **Scope:** `git diff --name-only 8caca0c main -- src pom.xml`, taken again on the day the
  reading starts. Today that is 33 files (33 files changed, 1510 insertions(+), 561 deletions(-)):
  - production and build: `pom.xml`, `AnthropicCredentialFile.java`, `ClaudeAgent.java`, `ClaudeContainerSetup.java`, `ClaudeCredentialExtractor.java`, `ClaudeFirstRun.java`, `ClaudeSettings.java`, `ClaudeStreamJsonFormatter.java`, `src/main/resources/agent/claude.yaml`;
  - 19 unit test files and 4 acceptance files under `src/test` and
    `src/acceptance`.

  Files unchanged since `8caca0c` were read on 2026-09-27 and stay out.
- **Read each file against each skill.** A finding that can do harm is a guess until a test reproduces
  it: it is fixed with a test seen to fail on the unfixed code, or declined with the reason written
  here, or opened as its own issue when it is larger than this one or belongs to another repository.
  A finding that only reads wrong - a wording, a comment, a test's own configuration - is fixed
  without a test, as the shared testing rules say.
- **A fix to what ships** gets its changelog entry in the same commit; the native build and the
  acceptance suite run as for any change to the adapter.
- **Red first:** each fix's test, run against the code before the fix.

## Acceptance

- The seven skills listed here with the version read and its verified SHA-256.
- Every file in scope read; each finding that can do harm fixed with a test seen red first, each that
  only reads wrong fixed without one, declined here with its reason, or opened as an issue named here.
- Unit tests, the native build and the acceptance suite green on the commit handed over.

## Findings, read on 2026-10-17

Read on `f8a9d2a` with the skills above, `SKILL.md` read from outside the repository, as *Skills* in `AGENTS.md`
allows a harness that cannot install them. **The scope** is 30 files whose content changed beyond the rename to
Porelo since `8caca0c` (the rename touched every path, so a plain diff lists all): `pom.xml`, `src/deb/control/control`,
`claude.yaml`, `ClaudeAgent`, `ClaudeContainerSetup`, `ClaudeCredentialExtractor`, `ClaudeFirstRun`, `ClaudeSettings`,
`ClaudeStreamJsonFormatter`, 17 unit test files and the four features `credential`, `session`, `waiting`, `wake`.
`AnthropicCredentialFile` and three document tests were deleted since. The workflows were read against
`security-audit` too, which answers what was to be checked.

Each finding that can do harm is a guess until its test is seen red; none is fixed before [CC33](README.md#cc33).
**Without a test,** since they only read wrong: A1, A2, A4, B7 and W6.

**What ships:**
- **A1, `ClaudeStreamJsonFormatter.ended`: a failed run's error reads "success" or "null".** With a blank result text
  it falls back to `String.valueOf(event.get("subtype"))`, so `{"type":"result","subtype":"success","is_error":true}`
  ends with the error "success" and one without a subtype with "null"; `porelo task status` shows it. `result()`
  leaves both out for the same event: what counts as failed is written twice and has drifted. Fix: one `failed`
  and one `said`, used by both; a failed run that says nothing ends with an error saying so. No test: it is a
  wording.
- **A2, the same class: the cut at 300 characters can split a surrogate pair**, which the log writes as `?`. Fix:
  cut on a code point, the limit named. No test: a `?` in a log line only reads wrong.
- **A3, terminal control characters pass through the formatter** (found in porelo-pi's reading, the same shape
  here): JSON's `\u001b` escapes are parsed back into live bytes and printed to the person's terminal. Red: a
  result text `"a\u001b[2Jb"` formats to text with no ISO control character, through `format` and `ended`.
- **A4, comments:** `ClaudeAgent`'s Javadoc says two overrides where there are four; `src/deb/control/control` says
  `~SNAPSHOT` where the pom writes `~snapshot.<run>`. No test: comments.

**Tests that cannot fail, or prove less than they say:**
- **B1, `ReleaseChannelTest`:** output is read to its end before `waitFor(30 s)`, so a hanging step hangs the suite.
  Red: the helper, shared by every test that runs a step, given `sleep 20` and a limit of 1 s, returns after 20 s
  instead of failing at its limit. Fix as `PackageVersionCheck` does it, through a file.
- **B2, `WorkflowRulesTest` and `WorkflowTimeLimitTest` read nothing they assert:** no package build or job found is
  asserted, only `*.yml` is read, and an upper-case job id is skipped. Red: `-P native,dist … -DskipTests` and a
  `.yaml` job without a limit, each green today.
- **B3, `WorkflowRulesTest`'s empty-key guard matches a string.** Red: the step run with an empty key, as
  `ReleaseChannelTest` runs "Which channel", exits non-zero.
- **B4, `PackageVersionCheck.aSnapshotSortsBelowItsReleaseAndAboveTheRunBefore`** checks its own helper and sorts
  nothing. Fix: `dpkg --compare-versions` on both sides, skipped by an assumption where `dpkg` is missing. Red: the
  mapping to `-snapshot.<run>` sorts above its release.
- **B5, `wake.feature`, the message while the agent works:** nothing asserts it still worked when the message came.
  Red: a `sleep 15` before the tell keeps it green. Fix: a longer task, and the screen still at work after the tell.
- **B6, no scenario proves the provider half of `ended()`:** the pom sets `PORELO_E2E_REFUSED_KEY` for it, but only
  pi and omp have "a task whose provider refuses its key says it ended with an error". It is ported here; that
  `API Error: NNN` is what 2.1.267 prints for a refused key is so far assumed.
- **B7, `NoOldNameTest` runs git with the machine's global configuration**, against the shared rule. Fix:
  `GIT_CONFIG_GLOBAL=/dev/null`. No test: a test's own configuration.

**Workflows, against `security-audit`:**
- **W1, `update.yml`: a killed step opens a "Green" pull request.** Every gate reports by `|| echo STOP=...`; a step
  ended by its `timeout-minutes`, the job's limit or a cancel writes none. "Open the pull request" and "Merge, or say
  why not" run under `always()` and decide on STOP alone, so the body says **Green** and, with
  `PORELO_UPDATE_AUTO_MERGE=true`, an unverified pin is merged. Fix: both steps treat anything but the job's success
  as STOP. Red: the two steps' scripts run with STOP unset, a failed status given and a `gh` stub on `PATH`; no
  `gh pr merge`, no "Green".
- **W2, `build.yml`: "A published release is never replaced" fails open.** `curl -sfL … | grep -qF` sits in an `if`
  under `set -eux` without `pipefail`, so a failed fetch reads as "not published" and `jf rt upload` overwrites the
  release. Fix: fetch to a file, stop on curl's failure, then look. Red: the step's script with `CHANNEL=releases`, a
  `curl` stub exiting 22 and a `jf` stub recording its calls; non-zero exit, no upload.
- **W4, `build.yml`: `${{ github.event_name }}` and `${{ github.ref }}` are interpolated into a shell line.** A
  pushed branch named `x$(cmd)` runs `cmd`; it needs push access and the step holds no secret, so it is hardening.
  Fix: the runner's `GITHUB_EVENT_NAME` and `GITHUB_REF`, the same values through the environment. Red: the step,
  interpolated as the runner does, with the branch `x$(touch${IFS}ran)` runs `touch`.
- **W5, `update.yml`: `sweep --mine || true`** hides a failed cleanup while rented machines keep running, against the
  shared rule that a run exits non-zero naming what it left. Fix: drop `|| true`. Red: every cleanup step run with
  a machine tooling that exits 1 exits 0 in the update job.
- **W6, `doc/decisions.md`, "Actions run from a commit":** "the only `curl` is `jf rt curl`" is no longer true
  (`build.yml` uses plain `curl` for the index). The text is corrected; no test.
- **W7, `persist-credentials`** and the push through the environment, as planned above. Red: `check-actions` on
  today's workflows names five checkouts; the request step run with a `git` that records which calls see the
  header shows none, where only the push must, and no argument list may carry the token or its base64.

**Declined:** the broad `catch (RuntimeException)` around `Json.parse` (passing an unreadable line through is
documented); a blank token of an unknown kind (documented in its `@throws`); a redundant null check under
`@NullMarked`; the decisions already taken in `doc/decisions.md`; taste (field order, qualified `java.util.regex`).
`graal`, `concurrency-review` and `solid-principles` found nothing new.

**Coupling the reformatting must keep:** `update.yml` reads `TOKEN_URL` and `CLIENT_ID` from
`ClaudeCredentialExtractor` and `- url:`/`sha256:` from `claude.yaml` by their exact indentation; [CC33](README.md#cc33)
changes those patterns with the 4-space YAML, or every update run stops.

**Size:** about 15 fixes, 10 of them with a test; M holds.
