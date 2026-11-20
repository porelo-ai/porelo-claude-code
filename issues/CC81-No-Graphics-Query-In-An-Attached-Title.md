# CC81 — No Graphics Query In An Attached Title

```json
{
  "$schema": "issue-1.json",
  "id": "CC81",
  "title": "No Graphics Query In An Attached Title",
  "state": "Backlog",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "a task's Claude Code attached from a terminal in tmux sends no kitty graphics query that tmux shows as the window's title"
}
```

**Why this size.** One fact to measure in a task, then one line in the definition or a finding for Porelo's attach,
and a test; no contract.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person who attaches to a task from a terminal running tmux sees the task's title, never
`Gi=31,s=1,v=1,a=q,t=d,f=24;AAAA`.

## Why

Seen at a task started by command line on Claude Code 2.1.311, attached from a terminal in tmux: the terminal's title
and tmux's status line read `Gi=31,s=1,v=1,a=q,t=d,f=24;AAAA`.

Read in Claude Code 2.1.329 (the version on the build host, not the task's pin): at start the CLI asks the terminal
`XTVERSION`; when an answer came and its own environment has no `TMUX`, it sends the kitty graphics query
`ESC _ G i=31,s=1,v=1,a=q,t=d,f=24;AAAA ESC \`. Inside a task the CLI's environment has no `TMUX`, since tmux runs
on the person's side of the attach, so the CLI sends the query; tmux does not speak the kitty graphics protocol and
takes an `ESC _ … ESC \` string as the window's title. Under tmux the CLI otherwise names the terminal from tmux's
own facts, which it reads only when `TMUX` is set.

## Plan

- **Measure first** on the pinned 2.1.311 in a task attached from tmux: that the query is the CLI's, from the
  attach's raw output, and which setting stops it. Candidates, in order: a variable the CLI reads to skip its
  terminal probes or images; the attach handing the task the fact that the terminal is tmux.
- **A setting of the CLI stops it:** the definition sets it in the image (`ENV`, as the other task variables), a
  unit test reads it from the packaged definition, and a comment names what was measured.
- **Only the attach can:** a finding for Porelo's attach, with the measurement, and this issue closes on it.

## Acceptance

- Attached from tmux, the title stays the task's through the CLI's start, measured on the pin; with the line taken
  out again, the query shows in the title once more.
