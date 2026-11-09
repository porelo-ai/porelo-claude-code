# CC52 — The First Words Agree With The Task

**Status:** Review.
**Size:** S - the wording of the three first-words texts for the adapters' tasks of class `online`, no code and no
contract.

**What must be true.** The first words an adapter repository's agent gets at the start of its task (Claude Code's,
Pi's and Omp's) agree with what the task really does, so the agent neither works around them nor asks about them:
where it pushes, which checks it runs before a push and where that list comes from, which rules of `AGENTS.md` they
replace inside a task, when it writes in the room, and a skill fetch no safety check of an agent questions.

## Plan

The first words are kept outside the repository, beside the guide for a move, one text per task; each changes in the
same way.

- **Where it pushes.** A plain `git push` from the task reaches the gate's ref `incoming/<task>`, and the gate
  passes it on to the forge as the branch `porelo/<task>`, which the pull request shows. The texts say: push with a
  plain `git push`, never to `main`, and name that branch as the one the pull request shows.
- **The checks.** The texts called `.github/workflows/shared-rules.yml` the list and then named `check-actions`,
  which that workflow does not run. The list is the hand-over checks of `AGENTS.md`'s shared block: `check-shared`,
  `check-citations`, `check-commits`, `check-readmes`, `check-doc-site`, `check-actions` and the tests tagged
  `documents`. The texts name that block as the list and say where the forge runs them again: the Build on a push
  (`check-commits`, `check-actions`), the Shared rules check on the pull request (the others, and `check-commits`).
- **What they replace.** Three rules of the shared block do not hold in a task: the agent pushes its task branch
  itself, it sets Implementing and Review on that branch, and it writes to the operator itself, in its session and
  the host's room. The texts say all three plainly, as replacing those rules in the task, rather than leaving the
  agent to find the contradiction.
- **The room.** An agent writes in the room only when it needs a decision or an action from a person, when it is
  stuck, when it answers a line or a finding posted there, when a step of its first words asks for it, or when its
  work is done. Progress, check lists, estimates and Build results stay in its session.
- **The skill fetch** writes `rm -rf "${DEST:?}/${slug:?}"`, so the shell refuses an empty variable. Measured on
  Claude Code 2.1.311 under bypass mode, attended and headless: the loop with this form runs at once, the same loop
  with `rm -rf $DEST/$slug` stops on the dangerous-rm check.
- **No test:** the texts are wording a person reads, and the next move is where they are seen to work; that the
  agent never stops on such a command is guarded by the adapter itself
  ([CC51](README.md#cc51)).

## Acceptance

- Each of the three texts states the push, the list of checks, the three replaced rules, the room rule and the guarded
  `rm` as above.
- An agent given them reports no contradiction between them and its task at the start of the next move.

## Reading

Declined findings of the reading, with the reason:

- The check of citations may be named as not run when the task's egress refuses its host: the texts state that
  exception and forbid reporting it green, so nothing is done silently less.
- Pi's packages left to the forge's Build, and Pi's planted red Build kept as two commits, are what that move asked
  of Pi, not what the first words claim of every task.
