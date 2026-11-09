# CC52 — The First Words Agree With The Task

**Status:** Backlog.
**Size:** S - the wording of the three first-words texts for the adapters' tasks of class `online`, no code and no
contract.

**What must be true.** The first words an adapter repository's agent gets at the start of its task (Claude Code's,
Pi's and Omp's) agree with what the task really does, so the agent neither works around them nor asks about them:
where it pushes, which checks it runs before a push and where that list comes from, which rules of `AGENTS.md` they
replace inside a task, when it writes in the room, and a skill fetch no safety check of an agent questions.

## Plan

The first words are kept outside the repository, beside the guide for a move, one text per task; each changes in the
same way.

- **Where it pushes.** The texts name the branch `porelo/<task>`, while the task's push setting sends a plain
  `git push` to the gate's ref `incoming/<task>`, and the move's pull requests came from `porelo/<task>`. They say:
  push with a plain `git push`, never to `main`, and name the branch the pull request shows.
- **The checks.** The texts call `.github/workflows/shared-rules.yml` the list and then name `check-actions` and
  `check-commits`, which that workflow does not run. The list is the hand-over checks of `AGENTS.md`'s shared block:
  `check-shared`, `check-citations`, `check-commits`, `check-readmes`, `check-doc-site`, `check-actions` and the tests
  tagged `documents`. The texts name that block as the list and say which of them the forge runs again
  (`shared-rules.yml`, and `check-actions` in `workflows.yml`); `check-commits` runs only in the task.
- **What they replace.** Two rules of the shared block do not hold in a task: the agent pushes its task branch itself,
  and it sets Implementing and Review on that branch. The texts say both plainly, as replacing those rules in the
  task, rather than leaving the agent to find the contradiction.
- **The room.** An agent writes in the room only when it needs a decision or an action from a person, when it is
  stuck, or when its work is done. Progress, check lists and estimates stay in its session.
- **The skill fetch** writes `rm -rf "${DEST:?}/${slug:?}"`, so the shell refuses an empty variable and no agent's
  safety check stops on it.
- **No test:** the texts are wording a person reads, and the next move is where they are seen to work; that the
  agent never stops on such a command is guarded by the adapter itself
  ([CC51](README.md#cc51)).

## Acceptance

- Each of the three texts states the push, the list of checks, the two replaced rules, the room rule and the guarded
  `rm` as above.
- An agent given them reports no contradiction between them and its task at the start of the next move.

## To be checked

- How a plain push to `incoming/<task>` becomes the pull request's branch, read in Porelo's gate, so the texts name
  what the agent sees.
