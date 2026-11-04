@credential
Feature: A task that comes back continues the conversation it was having

    Claude Code declares in its YAML where it names the session it runs. Porelo records that session, and
    starting the task again passes it back, so the agent continues instead of starting over. This proves it
    at the version this package pins: a word to remember in one run, asked for in the next.

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "PORELO_E2E_OPENROUTER_API_KEY" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: a task nobody watches, started again, continues its session and remembers
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "cont" of class "guarded" with a file in it
        When a script runs "timeout 300 porelo task start talk --project cont --repository cont --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --prompt 'Remember the word PAPAYA. Reply with OK only.'"
        Then it exits zero
        And porelo says task "talk" of "cont" has a session to continue
        When a script runs "porelo task stop porelo-cont-talk"
        And a script runs "timeout 300 porelo task start talk --project cont --repository cont --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --prompt 'Which word did I ask you to remember? Reply with the word only.'"
        Then it exits zero
        And its output contains "session   continuing"
        # The second prompt does not name the word: only the session it continued does.
        And its output contains "PAPAYA"
        When a script runs "porelo project remove cont --force"
        Then it exits zero

    @slow
    Scenario: a task that allows no subagents gives its agent none to start
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "alone" of class "guarded" with a file in it
        # The prompt tries each way in, since a model left to choose may not. What counts is the task the session records, whichever tool started it.
        When a script runs "timeout 300 porelo task start solo --project alone --repository alone --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --prompt 'Try to start a subagent that answers with one word, by each way you have: a tool that starts a subagent; a workflow tool; a skill you write as .claude/skills/probe/SKILL.md with context: fork in its front matter and then invoke. Run no agent command from the shell. If one started, answer with the word STARTED. If none could, answer with the word MISSING.'"
        Then it exits zero
        And its output contains "MISSING"
        And its output does not contain "STARTED"
        And its output does not contain "\"task_type\":\"local_agent\""
        And its output does not contain "\"task_type\":\"local_workflow\""
        When a script runs "porelo project remove alone --force"
        Then it exits zero

    @slow
    Scenario: an attached agent stopped and started again continues its conversation
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "back" of class "guarded" with a file in it
        And a terminal on the machine
        When I run "porelo task start back --project back --repository back --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
        Then the "claude" agent in task "back" of "back" reaches work without being asked anything
        # Typed, then Enter once the text is on its screen, so it is never taken as a paste.
        When I type "Remember the word PAPAYA. Reply with OK only."
        Then the screen of task "back" of "back" shows "Remember the word PAPAYA"
        When I press Enter
        # Stopped once its answer is on the screen, not after a guessed time: the session is the agent's to
        # write during its first turn, and a stop before the turn ends leaves none to continue.
        Then within 120 seconds this script exits zero:
            """
            podman exec porelo-back-back tmux capture-pane -p -t porelo | grep -v 'Reply with OK' | grep -qw OK
            """
        When a script runs "sleep 3; porelo task stop porelo-back-back"
        Then porelo says task "back" of "back" has a session to continue
        Given a terminal on the machine
        When I run "porelo task start back --project back --repository back --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
        # A fresh session would show an empty conversation; the continued one shows what was said in it.
        Then the screen of task "back" of "back" shows "Remember the word PAPAYA"
        When a script runs "porelo project remove back --force"
        Then it exits zero
