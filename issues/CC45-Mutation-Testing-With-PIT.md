# CC45 — Mutation Testing With PIT

```json
{
  "$schema": "issue-1.json",
  "id": "CC45",
  "title": "Mutation Testing With PIT",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "mutation testing by hand: a `pit` profile outside CI with no threshold, and a report sorting survivors into real gaps and harmless ones"
}
```

**Why this size.** one profile in `pom.xml` and a report in this issue, no contract.

**What must be true.** A person can run mutation testing over this repository by hand and read which changes to the
code no test notices, sorted into real gaps and harmless ones.

- A `pit` profile, run by hand: not in CI and with no threshold.
- PIT and its JUnit 5 plugin are pinned.
- It runs on a VM where tests call real tools.
- A report sorts the survivors into real gaps and harmless ones.

## Plan

- `pom.xml`: a profile `pit` with `org.pitest:pitest-maven` 1.30.2 and, as its plugin dependency,
  `org.pitest:pitest-junit5-plugin` 1.2.3. Configuration: `targetClasses` from a property `pit.classes`, by default
  `org.fuin.porelo.agent.impl.claude.*`, so one class can be named by `-Dpit.classes=`; `targetTests`
  `org.fuin.porelo.agent.impl.claude.*Test` (the unit test classes, 29 at the run; the `*Check` classes need a built
  package and stay out); `excludedClasses` the package's `package-info`; `excludedMethods` `main`; `threads` 4;
  `timeoutConstant` 10000; `timestampedReports` false; output HTML and XML into `target/pit-reports`. No execution is
  bound to a phase, so `verify` and CI never run it.
- Run by hand: `./mvnw -B -s settings.xml -Ppit test-compile org.pitest:pitest-maven:mutationCoverage`.
- A section `## Survivors` in this issue: each surviving mutant by class and line, sorted into real gaps (a change
  that can do harm and no test sees) and harmless ones (with one line why). Gaps are recorded, not fixed here; a
  grave one becomes an issue of its own.

## Acceptance

- The command above ends with exit 0 and writes `target/pit-reports/mutations.xml`; `./mvnw -B -s settings.xml
  verify` runs no PIT goal.
- `## Survivors` names every survivor the report lists, each sorted.

**The model.** The `pit` profile in the messaging repository's `pom.xml`.

## Survivors

The run by hand over the ten classes of `org.fuin.porelo.agent.impl.claude`, with the 29 unit test classes: 193
mutants, 186 killed, 4 survived, 3 without coverage, line coverage of the mutated classes 94%; it ended with exit 0
in about a minute. It ran on the developer's machine as an account other than root, not on a VM as *What must be
true* asks, as PI43's ran in a task container. The tests that
start real processes (git, bash) ran there, and none was skipped. PIT appears in `pom.xml` only inside the `pit`
profile, so `verify` and CI run no PIT goal.

**Real gaps** - a change that can do harm and no test sees; recorded here, none grave enough for an issue of its
own:

- `ClaudeFindings.isOn`, line 207: the return for a number, replaced by `true`, survived. Every test hands a provider
  switch a string; none hands it a JSON number. So `"CLAUDE_CODE_USE_BEDROCK": 0` in a settings file's `env` could
  read as on, and the findings would name Bedrock as the provider.
- `ClaudeFindings.onThisMachine`, line 64: the macOS check, negated, survived. No test holds which
  managed-settings file each system reads. On Linux the macOS path would be read, and an administrator's managed
  settings would go unseen.
- `ClaudeCredentialExtractor.extract(Path, String)`, line 70: the call without a provider, replaced by
  `Optional.empty`, has no coverage. Tests call the method with a provider or call `extract(Path)` directly, so an
  import that names no provider could find nothing.

**Harmless ones:**

- `ClaudeStreamJsonFormatter.cut`, line 180, `>` as `>=`: survived. A text of exactly the limit would show the
  cut mark after all of it; only the output reads differently.
- `ClaudeStreamJsonFormatter.render`, line 75, and `text`, line 97, each return replaced by `""`: no coverage. A
  line that parses to JSON but not to an object, and a message whose content holds a block that is not an object,
  would show an empty line instead of the line itself or the line marked unreadable; only the log reads differently.
- `ClaudeContainerSetup.credentialed`, line 82, replaced by `true`: survived. With `api_key` as the kind, a token of
  only blanks would be written as a secret file holding blanks, which `ClaudeFirstRun` already skips as no key, and
  a null token would throw instead of setting no key up. Only `ClaudeAgent` builds the setup's context, and it does
  so with the task's token, so neither reaches a task.
