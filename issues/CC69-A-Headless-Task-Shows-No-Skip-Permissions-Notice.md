# CC69 — A Headless Task Shows No Skip-Permissions Notice

```json
{
  "$schema": "issue-1.json",
  "id": "CC69",
  "title": "A Headless Task Shows No Skip-Permissions Notice",
  "state": "Implementing",
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
