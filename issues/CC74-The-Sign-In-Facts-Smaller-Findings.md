# CC74 — The Sign-In Facts' Smaller Findings

```json
{ "$schema": "issue-1.json", "id": "CC74", "title": "The Sign-In Facts' Smaller Findings", "state": "Idea",
  "size": "-", "priority": "Medium", "rank": 1, "blockedBy": [], "covers": "the smaller gaps left in how the adapter finds and describes a local Claude Code sign-in for carrying to a host, each fixed or measured" }
```

**What must be true.** What the adapter says of a Claude Code sign-in on the person's machine, before it is carried to a
host, holds in the cases below too, and each guard there has a test that shows it gone.

## To be checked

- **The account file is read without the regular-file check.** The facts read the account file beside the configuration directory without the check the extractor makes that it is a regular file, not a link or a device.
- **The facts on the wire.** Porelo's listing of what can be carried names agent, provider and kind only; the account, expiry and last change stay in the adapter until a listing takes them, and are then handed over.
