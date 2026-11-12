# CC65 — The Issue Heads Are Checked Every Day

```json
{
  "$schema": "issue-1.json",
  "id": "CC65",
  "title": "The Issue Heads Are Checked Every Day",
  "state": "Review",
  "size": "XS",
  "priority": "Low",
  "rank": 5,
  "blockedBy": [],
  "covers": "a daily run checks this repository's issue heads and index with the released porelo-release, as buildtools' shared rules do"
}
```

**Why this size.** One scheduled step in one workflow, copied from porelo-buildtools' `shared-rules.yml`; no code.

**What must be true.** An issue head or index that breaks the rules turns a daily run red, not only the next push.

## Plan

- **`.github/workflows/shared-rules.yml`**: a daily schedule and a step that runs `issues check issues` with the
  released porelo-release, taken as porelo-buildtools' `shared-rules.yml` does it (BT81, the source).
- **Level:** one dispatched run green; one with a broken head red, tried on a branch.
- Built only when the lane is through, by the operator's order of 2026-11-12.

## Acceptance

- The scheduled step runs `issues check issues` and passes on `main`.
- A head that breaks issue-1.json fails the step.

## To be checked

- Built as the step *Every issue head holds to its schema* in the job's existing order, with the porelo-release the
  `ci-tools` profile resolves (the version porelo-parent names, 0.5.2-SNAPSHOT today) rather than a released one, as
  every other step of the job does. Unlike porelo-buildtools, which builds its tool from the commit, the rule is the
  snapshot's: `issues check issues` with the published 0.5.2-SNAPSHOT jar, as the snapshot repository serves it,
  passes on today's heads locally; a dispatched run shows it on the forge.
- `WorkflowRulesTest` runs the step with the tooling stubbed: it calls exactly `issues check issues` on the resolved
  classpath, carries no `if:`, so it runs on every trigger, and fails when the tooling refuses a head; seen to fail
  first, while the step was missing. Read by a Claude Code sub-agent, Sonnet: the exact call, the trigger and the
  snapshot's rule were its findings, taken.
- The test also refuses `continue-on-error:` on the step, which would swallow a refused head; seen to fail with it.
  Read by a Claude Code sub-agent, Sonnet, together with its siblings in the other adapters: that was its finding,
  taken; the joined arguments and a classpath without a space were judged too minor to change.
- Still open, after the push: one dispatched run green on `main`, and one with a broken head red, tried on a branch.
