# CC68 — The Vault Scenarios Leave Once Porelo Holds Them

```json
{
  "$schema": "issue-1.json",
  "id": "CC68",
  "title": "The Vault Scenarios Leave Once Porelo Holds Them",
  "state": "Backlog",
  "size": "XS",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": [],
  "covers": "credential.feature's three scenarios that test only Porelo's vault leave the legs once Porelo's own acceptance suite proves each"
}
```

**Why this size.** Three scenarios deleted from `credential.feature`, no code. The scenarios Porelo's suite needs
are Porelo's agent's work.

**Estimate.** XS: 9 min, the measured median of XS over all repositories (n=105). Expect more for the acceptance
run that proves it.

**What must be true.** The end-to-end legs hold no scenario that tests only Porelo, so they hold 27 scenarios. The
three below test Porelo's vault, not this adapter:

- *a person unlocks the vault and the passphrase is not echoed*;
- *a person stores the key, and it is echoed nowhere*;
- *the credential is not recoverable from the vault file*.

## Why

Split from [CC50](README.md#cc50) on 2026-11-13. Porelo's acceptance suite on `main` (916577457) proves `porelo
setup` and `porelo doctor`, so those two scenarios left this suite with CC50. It proves none of these three:
`vault-commands.feature` uses `--passphrase-command` and `--passphrase-stdin` only, and nothing types a passphrase
or a value at a terminal or reads the vault file for the value. So they were handed to Porelo's agent first, and
they stay here until Porelo's suite holds them; no coverage is dropped.

## Plan

- Once Porelo's acceptance suite proves each of the three, delete them from `credential.feature`. The end-to-end
  count falls from 30 to 27, on both legs.
- **Level:** the acceptance leg on Ubuntu and Fedora, through the Build.

## Acceptance

- `credential.feature` holds none of the three, and Porelo's suite on `main` names a scenario for each.
- Both legs pass with 27 scenarios.
