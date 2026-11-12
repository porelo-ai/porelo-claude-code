# CC42 — A Sign-In Without A Browser Is Tested On Every CLI Update

```json
{
  "$schema": "issue-1.json",
  "id": "CC42",
  "title": "A Sign-In Without A Browser Is Tested On Every CLI Update",
  "state": "Implementing",
  "size": "M",
  "priority": "Medium",
  "rank": 3,
  "blockedBy": [],
  "covers": "the sign-in at a terminal without a browser, checked on every CLI update so a release that drops it fails its pin bump",
  "openQuestions": 0
}
```

**Why this size.** one acceptance scenario and no Java: the update job already runs the whole suite on both legs
against the candidate packages and stops the pin bump when a leg fails, and Porelo's acceptance kit already has every
step it needs. M because it is an acceptance step, written against the real CLI's screens and proven on a VM.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** `porelo vault login` for Claude Code, started over `ssh -t` in a host account with no browser
opener set, shows its link and waits for the pasted code, and the link it shows names no loopback address as its
redirect; the weekly update job runs that login against each new CLI release, and a release that no longer does so fails
its pin bump, so a person signing in from a host is never left with a login that waits for a callback nobody can reach.

## How it stands

- `claude.yaml` declares `login: {arguments: [], flow: paste-code}`, measured once on 2.1.267 (without `BROWSER` the
  login prints a link and waits at "Paste code here"; with it, on a loopback callback). Nothing checks it again.
- `terminal.feature` only previews the login (`logging in to the "claude" agent is attempted`, a `--dry-run`), which
  builds and runs nothing, so a release that changes the flow passes it.
- `update.yml` pins the new release, builds the packages and runs `porelo-machines acceptance --cucumber .
  --candidate target` on ubuntu and fedora; a failed leg writes `STOP`, so the pull request says "not ready" and is not
  merged. A new scenario in the suite therefore runs on every pin bump with no change to the workflow.
- Porelo runs the login in a throwaway container of a login image built from the agent's pinned artifacts, labelled
  `porelo.login.agent=claude`, with `BROWSER` unset and a shim for `xdg-open` that prints the URL instead of opening it.

## Plan

- **`terminal.feature`, one scenario** "a sign-in at a terminal without a browser shows its link and waits for the
  code", made only of the kit's steps:
  - `a vault of this scenario's own, unlocked ...`, then `a terminal on the machine` (an SSH session with a pty, as
    `ssh -t` gives, so `SSH_CONNECTION` is set).
  - `I run "unset BROWSER; porelo vault login claude"`, then the first-run screens as measured (the theme, then the
    login method: `I press Enter` each), then `within <n> seconds the terminal shows "Paste code here"`; `<n>` covers
    the first build of the login image in a fresh account.
  - A script step reads `podman logs` of the container labelled `porelo.login.agent=claude`, strips escape sequences
    and line breaks, and requires a `redirect_uri=` that names none of `localhost`, `127.0.0.1` or `[::1]`, so a link
    the CLI wraps across lines is still read whole.
  - Cancel: `podman stop` of that container by its label (or Ctrl-C, if it ends the login as measured), then a script
    that sees the container gone and no `anthropic` credential in the vault.
- **`claude.yaml`**: the comment on `flow:` says the acceptance checks it on every pin, instead of naming the one
  version it was measured on.
- **Red first**: the redirect check, run by hand on a local VM against a login of the same image started with
  `BROWSER` set (the loopback flow), fails; against the real login it passes.
- **Level**: acceptance only, on a local VM, then one green update-job or Build leg pair on rented machines. No unit
  test: there is no logic of the adapter's in it.

## Acceptance

- The scenario passes on ubuntu and fedora against the pinned CLI, with no secret and no model.
- Its redirect check is seen to fail against a loopback login.
- The update job's legs run it (the run log of one pin bump, or a dispatched run, names the scenario).
- No login container and no credential is left after it.

## What makes it uncertain

- The first-run screens before the link (theme, login method) and the prompt's words may move with a release; that is
  the point of the scenario, but a cosmetic change also stops a pin bump until the scenario follows.
- Building the login image in each scenario account takes minutes; the scenario's wait must cover it.
- The sign-in contacts Anthropic from a rented machine on every run; a refusal of cloud addresses would read as a
  failed sign-in.

## To be checked

- On 2.1.311 in a fresh account: the screens before the link and their keys, the words of the paste prompt, and whether
  Ctrl-C ends the login at the paste prompt.
- Whether `podman logs` of the login container holds the link whole once escape sequences and line breaks are removed.
- How long the first build of the login image takes on a rented machine.
