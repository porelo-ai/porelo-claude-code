@credential
Feature: A task authenticates without ever holding the credential

    The half only a real credential can answer, done as a person does it: typed at a terminal, never
    on a command line. Without PORELO_E2E_OPENROUTER_API_KEY and PORELO_E2E_MODEL in the environment of
    the machine running this suite, every scenario here is skipped - and shows as skipped, so a green
    run says what it proved and not more.

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "PORELO_E2E_OPENROUTER_API_KEY" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    Scenario: a person unlocks the vault and the passphrase is not echoed
        # A vault made for this scenario alone, so whatever vault the account holds is never touched.
        # Porelo refuses to unlock a vault that does not exist, so it is created first, asked twice,
        # and locked again: what is measured is the unlock a person does every day.
        Given a terminal on the machine
        When I run "export PORELO_VAULT=$(mktemp -d)/vault.bin"
        And I run "porelo vault init"
        Then the terminal shows "New vault passphrase:"
        When I type "porelo-acceptance-passphrase"
        Then the terminal shows "New vault passphrase again:"
        When I type "porelo-acceptance-passphrase"
        Then the terminal shows "created "
        When I run "porelo vault unlock --forget"
        # Not "locked" alone: everything seen so far is searched, and the init's "unlocked" contains it.
        Then the terminal shows "locked    use"
        When I run "porelo vault unlock"
        Then the terminal shows "Vault passphrase:"
        When I type "porelo-acceptance-passphrase"
        # Only the unlock says "kernel keyring"; the init above says "this account's keyring".
        Then the terminal shows "kernel keyring"
        And the terminal does not show "porelo-acceptance-passphrase"
        When I run "porelo vault unlock --forget && rm -r ${PORELO_VAULT%/vault.bin}"

    Scenario: a person stores the key, and it is echoed nowhere
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And a terminal on the machine
        When I run "porelo vault put openrouter --type api-key"
        Then the terminal shows "Value for 'openrouter'"
        When I type the value of "PORELO_E2E_OPENROUTER_API_KEY"
        Then the terminal shows "stored"
        And the terminal does not show the value of "PORELO_E2E_OPENROUTER_API_KEY"
        When I run "porelo vault list"
        Then the terminal shows "openrouter"
        And the terminal does not show the value of "PORELO_E2E_OPENROUTER_API_KEY"

    Scenario: the credential is not recoverable from the vault file
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        When the vault file is read as it lies on disk
        Then it exits zero
        And its output does not contain the value of "PORELO_E2E_OPENROUTER_API_KEY"

    @slow
    Scenario: a person starts a task with the agent, and the model answers through the broker
        # The real credential stays in the vault; the task gets a token minted for it and nothing
        # else. Only a real credential makes the leak searchable, which is why this needs one.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "live" of class "guarded" with a file in it
        And a terminal on the machine
        When I run "porelo task start shell --project live --repository live --agent claude --provider openrouter --attach shell --clearance deny"
        And I wait for the shell inside the container
        Then the terminal shows "token"
        And the terminal does not show the value of "PORELO_E2E_OPENROUTER_API_KEY"
        # Each command splits the word it waits for with '', so its typed line never shows it and only its output can.
        When I run "claude --model ${PORELO_E2E_MODEL} -p 'Reply with exactly the word PORELO''LIVE and nothing else.'"
        Then within 240 seconds the terminal shows "PORELOLIVE"
        # Only the line asked for: the whole env is taller than the screen, and the attached tmux draws only what is
        # left on it, so a line that scrolled past in one burst never reached this terminal.
        When I run "env | grep '^ANTHROPIC_API''_KEY='"
        Then the terminal shows "ANTHROPIC_API_KEY="
        And the terminal does not show the value of "PORELO_E2E_OPENROUTER_API_KEY"
        When I run "exit"
        Then the terminal shows "ready$"
        # The start names its container; the task is removed by its own name, never one built from that rule.
        And the terminal shows "container porelo-"
        When a script runs "porelo task remove shell --force"
        Then it exits zero

    @slow
    Scenario: a task nobody attaches to answers through the broker, and the credential is nowhere in it
        # What a script sees rather than a person: the prompt goes in as the task's own command, and
        # the environment, the logs and the broker are asked afterwards rather than looked at.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "broker" of class "guarded" with a file in it
        When a task is started in "broker" for the "claude" agent through "openrouter" and left running
        # A model over OpenRouter sometimes ends its turn with no text at all, and the CLI exits 0 with nothing on
        # standard output (measured on 2.1.267: 1 in 6 to 1 in 3). That empty answer is asked again, twice at most and
        # said each time; a run that fails - a refused credential, a broker that is not there - is never asked again.
        And the task's container runs:
            """
            for try in 1 2 3; do
              out=$(timeout 240 /usr/local/bin/claude --model "${PORELO_E2E_MODEL}" -p "Reply with exactly the word PORELOLIVE and nothing else.") || { printf '%s\n' "$out"; exit 1; }
              case "$out" in *[![:space:]]*) printf '%s\n' "$out"; exit 0 ;; esac
              echo "try $try: an empty answer, exit 0"
            done
            """
        Then its output contains "PORELOLIVE"
        And the task's container environment does not contain the value of "PORELO_E2E_OPENROUTER_API_KEY"
        And no log the task left contains the value of "PORELO_E2E_OPENROUTER_API_KEY"
        And the task's broker saw a request
        When a script runs "porelo project remove broker --force"
        Then it exits zero

    Scenario: a task's commit names the model that made it, and none of Claude Code's own attribution
        # The shared rules ask for "Assisted-by: <tool>, <model>" and refuse Claude's "Co-Authored-By:". Only the
        # session knows its model, so the hook reads it from the transcript; the agent is told no trailer at all.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "trailer" of class "guarded" with a file in it
        When a task is started in "trailer" for the "claude" agent through "openrouter" and left running
        And the task's container runs:
            """
            cd /workspace
            base=$(git rev-list --count HEAD)
            for try in 1 2 3; do
              timeout 300 /usr/local/bin/claude --model "${PORELO_E2E_MODEL}" -p "Append the line 'one more' to any tracked file here, then run exactly: git commit -am 'Add one more line'" >/dev/null || exit 1
              [ "$(git rev-list --count HEAD)" -gt "$base" ] && break
              echo "try $try: no commit made"
            done
            [ "$(git rev-list --count HEAD)" -gt "$base" ] || { echo "no commit in three tries"; exit 1; }
            git log -1 --format=%B
            last=$(git log -1 --format=%B | sed '/^$/d' | tail -n 1)
            [ "$last" = "Assisted-by: Claude Code, ${PORELO_E2E_MODEL}" ] && echo "the trailer names the model that ran"
            """
        Then its output contains "Add one more line"
        And its output contains "the trailer names the model that ran"
        And its output does not contain "Co-Authored-By"
        When a script runs "porelo project remove trailer --force"
        Then it exits zero

    @slow
    Scenario: a person's fresh task reaches work without being asked anything
        # "Reached work, and nothing came first" rather than "the dialogs we know about are absent": a
        # release that adds a question fails this the day it arrives. The agent declares what being at
        # work looks like; the kit types nothing and waits for it.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "ready" of class "guarded" with a file in it
        And a terminal on the machine
        When I run "porelo task start ready --project ready --repository ready --agent claude --provider openrouter --clearance deny"
        Then the "claude" agent in task "ready" of "ready" reaches work without being asked anything
        When a script runs "porelo project remove ready --force"
        Then it exits zero

    @slow
    Scenario: a task nobody is watching ends rather than waiting on a question
        # A question in a run nobody watches is a run that never ends. The kit stops it at the bound
        # and fails, instead of letting the suite hang with nothing to report. Through OpenRouter, the
        # key the suite holds; Claude Code's own default is Anthropic.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "unwatched" of class "guarded" with a file in it
        When a task nobody is watching is started in "unwatched" for the "claude" agent through "openrouter" and ends within 120 seconds
        And a script runs "porelo project remove unwatched --force"
        Then it exits zero

    @slow
    Scenario: a task whose provider refuses its key says it ended with an error
        # The agent reads its own end and Porelo's task status says it. A key OpenRouter does not know is
        # refused, so the run stops before its end; the key is fixed in the pom, so it is never a real one.
        # Claude Code writes the refusal into its result line as "API Error: <status>", which makes it the provider's.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_REFUSED_KEY" as "openrouter" of kind "api-key"
        And a project called "refused" of class "guarded" with a file in it
        When a script runs "timeout 300 porelo task start talk --project refused --repository refused --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --prompt 'Reply with OK only.'"
        # The start returns while the agent still runs; its end shows once the agent has written it.
        And a script runs "for i in $(seq 60); do porelo task status talk | grep -q 'with an error' && break; sleep 5; done; porelo task status talk"
        Then its output contains "with an error"
        And its output contains "the provider"
        When a script runs "porelo project remove refused --force"
        Then it exits zero

    @slow
    Scenario: a task's agent is told how its mailbox works
        # Porelo puts the agent's declared instructions arguments on its start, with the guide's path. Asked
        # without a tool, only the guide in its system prompt can say where its new messages arrive; the
        # answer is read from the agent's own lines in its log, not from anything else there.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "mailbox" of class "guarded" with a file in it
        When a script runs "timeout 300 porelo task start talk --project mailbox --repository mailbox --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --prompt 'Without using any tool: in which directory do new messages for you arrive? Reply with its absolute path only.'"
        And a script runs "for i in $(seq 60); do porelo task status talk | grep -q 'ended' && break; sleep 5; done; grep -E '\"(assistant|result)\"' /run/user/$(id -u)/porelo/$(porelo task status talk --json | grep -o '\"name\": *\"[^\"]*\"' | head -n 1 | cut -d'\"' -f4)/task.log"
        Then its output contains "/run/porelo/mail/inbox/new"
        When a script runs "porelo project remove mailbox --force"
        Then it exits zero
