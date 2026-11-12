# CC64 — A WSL Host Keeps The Waived rm Check

```json
{
  "$schema": "issue-1.json",
  "id": "CC64",
  "title": "A WSL Host Keeps The Waived rm Check",
  "state": "Accepted",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "on a WSL host the task's Claude Code honours CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS again: a root-owned reg.exe in the task image answers Claude Code's two managed-settings queries as Windows does where no policy is set and refuses all else"
}
```

**Why this size.** one image file with its script and unit tests, one acceptance scenario on the VM, one decision note
and a watch in the update gate; no contract.

**What must be true.** On a WSL host, Claude Code in a task shows "CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS has
no effect while the Windows registry cannot be read for managed settings" and ignores the variable that the task image
sets, so its check of an `rm` on a variable that may be empty stops the task again, which [CC51](README.md#cc51) waived: attended with a
question, unattended with a refusal. After this, a task on a WSL host behaves as on a Linux host.

What is known, read in the binary of Claude Code 2.1.317:

- It takes itself to be under WSL when `WSL_DISTRO_NAME` is set, or when `/proc/version` names `microsoft` or `wsl`.
  A container shares the host's kernel, so a task on WSL2 is detected without the variable.
- Under WSL it runs `/mnt/c/Windows/System32/reg.exe query HKLM\SOFTWARE\Policies\ClaudeCode /v Settings` and the same
  for `HKCU`. The path is fixed, not looked up in `PATH`, and each call has a 5 s timeout.
- A hive counts as answered on exit 0, whose output is read as the policy, or on exit 1 with
  `unable to find the specified registry key or value` on stderr. The variable is ignored unless both are answered.
  In a task, `/mnt/c` does not exist, so neither is.
- It checks neither signature, hash nor publisher of `reg.exe`. Its check of who owns managed files skips `reg.exe`
  under WSL.
- No environment variable turns the query off.

The way through: the task image puts a script, owned by root with mode 0755, at `/mnt/c/Windows/System32/reg.exe`,
with its directories owned by root and not writable by others.

- It answers exactly those two queries the way Windows does where no policy is set: that line on stderr, exit 1.
- Anything else is refused with exit 2, so a later version that asks more brings the warning back rather than being
  told something false.
- The agent runs as `agent` and can change neither the script nor its directories.
- A task cannot read the host's Windows registry today and must not. So no Windows policy holds for the container,
  and saying so is true.
- On a Linux host nothing calls the script.

Not done: hiding WSL with a fake `/proc/version`, which would lie about the kernel to every tool in the task. Also not
done: passing `/mnt/c` and WSL interop into the task, which would let it start Windows programs as the person on the
host.

## To be checked

- Confirm the path, arguments, answer condition and warning at the pin 2.1.311 before building. The reading above is
  from 2.1.317.
- Reproduce it on the Linux VM first: a task with `WSL_DISTRO_NAME` set shows the warning, and the `rm` scenario of
  [CC51](README.md#cc51) is refused. With the script, there is no warning, the scenario passes, and any other call ends with exit 2.
  Run it once on a real WSL host at the end.
- Unit tests, red first: the image lines create the file root-owned at 0755, with its directories. The script answers
  the two queries with exit 1 and exactly that line, and anything else with exit 2.
- The weekly update's gate watches the new binary for the `reg.exe` path, its arguments and the answer condition. It
  fails when they change, so a change does not show first on a WSL host.
- A note in `doc/decisions.md`, beside [CC51](README.md#cc51)'s accepted risk: a Windows administrator's
  `HKLM\SOFTWARE\Policies\ClaudeCode` does not reach a Porelo task, and did not before this either.
- Whether Porelo's own update gate needs a part for this.
