# CC81 — Audit the documentation against the code and fix every mismatch

```json
{
  "$schema": "issue-1.json",
  "id": "CC81",
  "title": "Audit the documentation against the code and fix every mismatch",
  "state": "Idea",
  "size": "-",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "Audit the documentation against the code and fix every mismatch"
}
```

**What must be true.** Every claim in README.md, doc/index.md, doc/decisions.md, build.md and the repository's own section of AGENTS.md (not the shared rule blocks) has been checked against the code, the agent definition (src/main/resources/agent/claude.yaml), the build (pom.xml, porelo.yml) and the workflows.

Where a document and the code disagree, the code wins: the document is rewritten to say what the code does now. Where the code itself looks wrong, a new issue records it and the code is not changed here.

The audit's record lists each claim checked, what it was checked against, and whether it was measured or inferred, so a reader can repeat any check.

After the fixes, a reader following the documents (install, sign in, start a task, read vault.log, build the package, move the pinned Claude Code version) meets no statement that the code contradicts.
