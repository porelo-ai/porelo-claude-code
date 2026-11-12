# CC67 — A Continued Session Is Asked For Its Word

```json
{
  "$schema": "issue-1.json",
  "id": "CC67",
  "title": "A Continued Session Is Asked For Its Word",
  "state": "Backlog",
  "size": "XS",
  "priority": "Low",
  "rank": 1,
  "blockedBy": [],
  "covers": "session.feature's continued-conversation check asks the restarted Claude Code for the word it was told, instead of looking for the first turn's text on the visible screen"
}
```

**Why this size.** One scenario's last check in `src/acceptance/.../session.feature`, taken from porelo-omp's fix;
no code.

**Estimate.** XS: 9 min, the measured median of XS over all repositories (n=105). Expect more for the acceptance run that proves it.

**What must be true.** The scenario *an attached agent stopped and started again continues its conversation* passes
however long the first turn's answer is. Today, after the restart, it looks for "Remember the word PAPAYA" with the
acceptance kit's screen step, which reads only the visible pane (`tmux capture-pane -p -J`). A first turn whose
reasoning or answer fills the screen pushes that text off it, and the check fails although the conversation went on.

## Why

Found when porelo-omp's `main` went red on 2026-11-12 (Build 43296826933, *Acceptance - fedora*). The restarted omp
showed the first turn's long reasoning, and the check timed out. porelo-omp now asks the continued session for the
word (241374d); Claude Code shows a short first turn today and is green, so the same check is a latent failure here.
Filed on the Coordinator's word.

## Plan

- After the restart: the agent reaches work, the scenario types "Which word did I ask you to remember? Reply with
  that word only.", waits for it on the screen and presses Enter. Then a script passes within 180 s once
  `tmux capture-pane -p -J` shows PAPAYA outside the question. A fresh session does not know the word.
- **Level:** the acceptance leg on Ubuntu and Fedora, through the Build.

## Acceptance

- The scenario's last check asks for the word, and both legs pass with it.
