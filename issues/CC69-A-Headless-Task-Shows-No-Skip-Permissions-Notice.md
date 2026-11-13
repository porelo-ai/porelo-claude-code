# CC69 — A Headless Task Shows No Skip-Permissions Notice

```json
{
  "$schema": "issue-1.json",
  "id": "CC69",
  "title": "A Headless Task Shows No Skip-Permissions Notice",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "in a headless task the person never sees Claude Code's notice that CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS is set: the formatter drops its stream line and its stderr line, while the waiver stays and every other system line still passes"
}
```

**Why this size.** one more case in `ClaudeStreamJsonFormatter` and its tests, the matching stderr line where the
adapter shows Claude Code's stderr, and a check at the pin; the waiver of [CC51](README.md#cc51) does not change.

**Estimate.** S: 27 min, the measured median of S over all repositories (p80 63).

**What must be true.** In a headless task (`-p --output-format stream-json --verbose`), the person never sees "CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS is set, so dangerous rm commands can run without a prompt
...":

- The stream line `{"type":"system","subtype":"notification","key":"bypass-removal-waiver",...}` is dropped by the
  formatter. It is dropped by that key, not by its words.
- The matching stderr line, which Claude Code writes in yellow beside it, is dropped too.
- Every other `system` line still passes as it does today, including other `notification` lines.
- The waiver of [CC51](README.md#cc51) stays untouched. `CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS` stays set,
  and the task image does not change.
- The attended terminal keeps the notice. It is Claude Code's own screen, and the adapter does not reach it.

What is known, read in the binary of Claude Code 2.1.318:

- The notice is shown when the variable is set, `CLAUDE_CODE_CHILD_SESSION` is not set, and the session is in bypass
  mode or bypass mode is available.
- No setting, environment variable or flag hides the notice alone. `CLAUDE_CODE_CHILD_SESSION` hides it but also turns
  the waiver off.
- In `stream-json` it is written twice: to stderr, and as a `system` line with subtype `notification` and key
  `bypass-removal-waiver`.
- `ClaudeStreamJsonFormatter` passes every `system` line except one benign subtype, so the person sees it today.

## To be checked

- Confirm the key, the subtype and the stderr line at the pin 2.1.311 before building. The reading above is from
  2.1.318.
- Find where the adapter or porelo core shows the agent's stderr, and drop the line there without touching other
  stderr lines.
- Unit tests, red first: the notification with that key gives no line, and a notification with another key and every
  other `system` subtype still pass.
- The weekly update's gate watches the new binary for the key. A renamed key would bring the notice back unseen by any
  test.

## Done

- `ClaudeStreamJsonFormatter` drops a `system` line of subtype `notification` whose key is `bypass-removal-waiver`, and
  a line of standard error that opens, once its color codes are taken off, with the notice's words. The task's log
  holds standard error beside the stream, since Porelo writes both to the one file, so the formatter is where both
  are shown and where both are dropped. Every other `system` line passes, and so does a notice of another key and
  the warning that the waiver has no effect, a line that holds the words further in, and the agent's own reply
  quoting them. Standard error is matched one line at a time by how it opens, so a notice split over two lines, or
  one interleaved into another write, would show its rest. The waiver and the task image are unchanged.
- Measured with Claude Code 2.1.318 here, in a headless run with an invalid key and an empty home: standard error
  carries the notice, in yellow (`ESC[33m` to `ESC[39m`) with a terminal's colors forced and plain without. That run
  wrote no stream line for it; the binary writes one under a condition that run did not meet, with the key and
  subtype above. 2.1.323 names the key and the words as well. The pin 2.1.311 is not installed here and was not
  checked.
- The weekly update stops on a release whose binary no longer names the key or the opening words, read out of the
  formatter, beside its check of the token URL. Tried on 2.1.318 and 2.1.323: both pass.
- Red first: `RemovalWaiverTest.theNoticeThatTheWaiverIsSetIsNotShownInATasksLog` failed before.
  `everyOtherSystemLineStillReachesTheLog` pins what still passes.
- Changelog under *Changed*.
- **Locally.** The unit suite (279) passed whole; after the reading's added cases, `RemovalWaiverTest` passed again.
