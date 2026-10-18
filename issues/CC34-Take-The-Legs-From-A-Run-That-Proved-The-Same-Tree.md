# CC34 — Take The Legs From A Run That Proved The Same Tree

**Status:** Implementing.
**Size:** S - a job in `build.yml` that asks porelo's `proven-legs`, the legs' condition, and one step in
`update.yml` that names the tree it verifies; no contract another repository reads.

**What must be true.** A Build on `main` whose tree a run of this repository already proved on both legs within a
day rents no machine for its legs and says which run it took them from; a tag always runs its own legs, and a
Build that cannot tell runs them.

From porelo-buildtools [BT15](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt15),
which built the shared command.

## Plan

- **`build.yml`** gains the job "Legs already proven", on a push to `main` only, with `actions: read`. It runs
  `porelo-release proven-legs --jobs "Acceptance - ubuntu,Acceptance - fedora;Follow Claude Code"` on the pushed tree.
  The acceptance job runs unless that says `proven=yes`; a skipped or failed proof runs the legs.
- **`update.yml`** names the tree its legs verify, the pin's tree, written into a scratch index so the pin stays unstaged for the request that commits it, by an artifact `porelo-verified-tree-<tree>` made before
  the acceptance. Only the run itself can make one, and its job fails on any STOP, so a green "Follow Claude Code" with that
  artifact proved the tree. Merging its request on an unmoved `main` gives `main` that tree, and that Build takes
  the update's legs.

## Acceptance

- `proven-legs` asked for real with this repository's names: a tree a green Build proved is found, another is not.
- After the push: the Build of a tree already proven skips its legs and names the run; the next update request,
  once merged, gives a Build that takes the update run's legs.
