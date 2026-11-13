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

    Scenario: a sign-in at a terminal without a browser shows its link and waits for the code
        # What claude.yaml's login flow 'paste-code' says, checked on every pin rather than once: a release whose
        # login waits on a loopback callback would leave a person signing in over ssh with nothing to paste. Measured
        # on 2.1.311: the theme, then the login method with the subscription preselected, then the link and the paste
        # prompt. The login image's first build in a fresh account is in the first wait. Nothing is pasted, so nothing
        # is stored; neither Ctrl-C nor Esc ends the login at the prompt, so it is ended by its container. Each screen
        # is waited for by one word of it: Claude Code draws the space between two words as a cursor move, so a
        # phrase never stands in what the terminal read.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And a terminal on the machine
        When I run "unset BROWSER; porelo vault login claude"
        Then within 600 seconds the terminal shows "/theme"
        When I press Enter
        Then within 30 seconds the terminal shows "subscription"
        When I press Enter
        Then within 30 seconds the terminal shows "prompted"
        # The link as the login printed it, escape sequences and line breaks taken out, so a link wrapped across lines
        # is read whole: its redirect is Anthropic's page that shows the code, never this machine.
        Then within 30 seconds this script exits zero:
            """
            id=$(podman ps -q --filter label=porelo.login.agent=claude | head -n 1)
            [ -n "$id" ] || { echo 'no login container is running'; exit 1; }
            link=$(podman logs "$id" 2>&1 | sed 's/\x1b\][^\x07\x1b]*\(\x07\|\x1b\\\)//g; s/\x1b\[[0-9;?]*[a-zA-Z]//g' | tr -d '\r\n' | grep -oE 'redirect_uri=[^&]*' | head -n 1)
            printf '%s\n' "$link"
            [ -n "$link" ] || { echo 'the login printed no redirect_uri'; exit 1; }
            printf '%s\n' "$link" | grep -qiE 'localhost|127\.0\.0\.1|%5B%3A%3A1%5D|\[::1\]' && { echo 'the redirect is this machine'; exit 1; }
            exit 0
            """
        When a script runs "podman stop -t 2 $(podman ps -q --filter label=porelo.login.agent=claude)"
        Then within 60 seconds this script exits zero:
            """
            [ -z "$(podman ps -a -q --filter label=porelo.login.agent=claude)" ] || { echo 'the login container is still there'; exit 1; }
            porelo vault list | grep -q anthropic && { echo 'a credential was stored'; exit 1; }
            exit 0
            """

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
            screen=$(podman exec "$(porelo task status nomem --json | grep -o '"name": *"[^"]*"' | head -n 1 | cut -d'"' -f4)" tmux capture-pane -p -t porelo)
            printf '%s\n' "$screen" | grep -qE 'Auto-memory +false' && exit 0
            printf 'screen, last lines:\n'; printf '%s\n' "$screen" | grep -v '^ *$' | tail -12
            exit 1
            """
        When a script runs "porelo project remove nomem --force"
        Then it exits zero
