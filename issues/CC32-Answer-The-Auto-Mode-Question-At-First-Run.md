# CC32 — Answer The Auto Mode Question At First Run

**Status:** Idea.

**What must be true.** A task running a Claude Code version that asks at start "Make auto mode your default
permission mode?" reaches work without being asked anything, as it does with the pinned version, so the update job
can move the pin past that version.

## To be checked

- Which version first asks it: the update runs following 2.1.287 and 2.1.292 both stopped at it in every scenario
  that waits for the agent, with "Yes, set auto mode as my default permission mode" and "No, keep bypass
  permissions" offered; the pin is 2.1.267, which does not ask.
- Which setting or file the answer is stored in, measured attended on an account where the agent never ran, so the
  definition can write it before the agent starts; the adapter keeps bypass permissions.
- Why "Acceptance - ubuntu" waited 300 s per scenario on the same screen until its 30-minute limit, instead of
  stopping once the same failure repeated.
