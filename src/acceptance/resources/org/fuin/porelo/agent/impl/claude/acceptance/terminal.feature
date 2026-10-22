Feature: What a person sees with this agent, before there is any credential

    Every scenario here needs no secret and no model. They are the things somebody meets in the
    first ten minutes with this agent on a machine, and each of them is a refusal that has to say
    what it is rather than fail later.

    Background:
        Given the suite runs as an unprivileged user

    Scenario: the agent says how it logs in, so a machine without it can still get a credential
        # This agent's tooling lives in the task image, not on the node, so 'porelo vault login' runs
        # the login in a throwaway container and collects what it produces. Dry-run says what it
        # would do and does nothing.
        When logging in to the "claude" agent is attempted
        Then it exits zero
        And its output contains "claude"

    Scenario: what the agent may reach, and what it is refused, are both visible to a person
        # An undeclared name is usually a bug that breaks the agent for everyone; a refused one is a
        # decision. The two look identical to the firewall and completely different to a reviewer.
        Given a terminal on the machine
        When I run "porelo agents --verbose"
        Then the terminal shows "platform.claude.com"
        And the terminal shows "refused:"
        And the terminal shows "http-intake.logs.us5.datadoghq.com"

    Scenario: starting a task without a credential says what is missing, before anything is built
        Given a project called "nocred" of class "guarded" with a file in it
        And a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds no credential for "anthropic"
        When a task nobody is watching is started in "nocred" for the "claude" agent
        Then its output mentions one of "no credential, is locked, cannot authenticate"
        And its output contains "anthropic"

    Scenario: a script is never asked a question by this agent's task
        Given a project called "noask" of class "guarded" with a file in it
        When a script asks what a task in "noask" for the "claude" agent would do
        Then its output does not contain "[Y/n]"
        And its output contains no escape sequences

    Scenario: the agent keeps no memory of its own in a task
        # What holds across tasks is the repository's reviewed AGENTS.md, never a note the agent wrote for
        # itself. Measured on 2.1.292: without the adapter's switch the menu says "Auto-memory  true". It is read
        # without asking a model, so a key that opens nothing is enough.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        When a script runs "printf sk-ant-opens-nothing | porelo vault put anthropic --type api-key"
        Then it exits zero
        Given a project called "nomem" of class "guarded" with a file in it
        And a terminal on the machine
        When I run "porelo task start nomem --project nomem --repository nomem --agent claude --clearance deny"
        Then the "claude" agent in task "nomem" of "nomem" reaches work without being asked anything
        When I type "/memory"
        And I press Enter
        Then within 30 seconds this script exits zero:
            """
            screen=$(podman exec porelo-nomem-nomem tmux capture-pane -p -t porelo)
            printf '%s\n' "$screen" | grep -qE 'Auto-memory +false' && exit 0
            printf 'screen, last lines:\n'; printf '%s\n' "$screen" | grep -v '^ *$' | tail -12
            exit 1
            """
        When a script runs "porelo project unfollow nomem --force"
        Then it exits zero
