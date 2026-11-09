# CC45 — Mutation Testing With PIT

**Status:** Backlog.
**Size:** S - one profile in `pom.xml` and a report in this issue, no contract.

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
  `org.fuin.porelo.agent.impl.claude.*Test` (the 24 unit test classes; the `*Check` classes need a built package and
  stay out); `excludedClasses` the package's `package-info`; `excludedMethods` `main`; `threads` 4;
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
