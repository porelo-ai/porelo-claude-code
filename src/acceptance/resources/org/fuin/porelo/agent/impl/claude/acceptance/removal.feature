@credential
Feature: A task never stops on Claude Code's question about an rm

    Bypass mode does not skip Claude Code's check of an rm whose variable may be empty: at a terminal it asks a
    person, unattended it refuses. The task image waives that check, so such a command runs at once, as every other
    command does. The loop is the shape that stopped a task; its directories are made by the loop itself, and only
    when both are gone does it print its marker and leave its file.

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "PORELO_E2E_OPENROUTER_API_KEY" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: a task nobody watches runs an rm on a variable that may be empty
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "sweep" of class "guarded" with a file in it
        When a script runs "timeout 300 porelo task start sweep --project sweep --repository sweep --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --prompt 'Use your Bash tool to run this exact command as one call, unchanged, and run nothing before it: DEST=$PWD/cc51-loop; for slug in one two; do mkdir -p $DEST/$slug/x; rm -rf $DEST/$slug; done; rmdir $DEST && echo LOOP-$((6*7))-GONE. Then reply with what it printed.'"
        Then it exits zero
        # The marker is computed by the shell, and no tool call was denied, so the command ran as it was given rather
        # than rewritten after a refusal; the denial is the record's type, whatever a later CLI words it as.
        And its output contains "LOOP-42-GONE"
        And its output does not contain "permission_denied"
        And its output does not contain "Dangerous rm"
        When a script runs "porelo project remove sweep --force"
        Then it exits zero

    @slow
    Scenario: an attached agent runs an rm on a variable that may be empty without asking
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "brush" of class "guarded" with a file in it
        And a terminal on the machine
        When I run "porelo task start brush --project brush --repository brush --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
        Then the "claude" agent in task "brush" of "brush" reaches work without being asked anything
        # Typed, then Enter once the text is on its screen, so it is never taken as a paste.
        When I type "Use your Bash tool to run this exact command as one call, unchanged, and run nothing before it: DEST=$PWD/cc51-loop; for slug in one two; do mkdir -p $DEST/$slug/x; rm -rf $DEST/$slug; done; rmdir $DEST && touch cc51-gone"
        Then the screen of task "brush" of "brush" shows "touch cc51-gone"
        When I press Enter
        # The file is left only when both directories the loop made are gone, so it is the effect, not the agent's word.
        # Within 90 seconds: the CLI denies its own question after about two minutes, and an agent that then rewrote
        # the command would leave the file too.
        # And the session's own record holds no refusal of the check, which a question timed out would have left.
        Then within 90 seconds this script exits zero:
            """
            podman exec "$(porelo task status brush --json | grep -o '"name": *"[^"]*"' | head -n 1 | cut -d'"' -f4)" sh -c 'test -e /workspace/cc51-gone && ! grep -rqi "dangerous rm operation" /home/agent/.claude/projects'
            """
        And the terminal does not show "Do you want to proceed"
        When a script runs "porelo project remove brush --force"
        Then it exits zero
