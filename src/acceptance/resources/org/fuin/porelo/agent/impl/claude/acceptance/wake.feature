@credential
Feature: An agent at rest is woken by a message for it

  Porelo types one line into a task's session when a message for it arrives and its agent's declared
  at_rest matches the screen. These scenarios leave tasks running while they wait for the agent, so they
  are in a file of their own that runs after every other: a wake that fails, and skips its clean-up, takes
  no other scenario down with it. Without PORELO_E2E_OPENROUTER_API_KEY and PORELO_E2E_MODEL every scenario
  here is skipped.

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "PORELO_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "PORELO_E2E_MODEL" is set

  @slow
  Scenario: an agent at rest is woken by a message for it, and answers it
    # Porelo types its one wake line into the session when a message for the task arrives and the agent's
    # declared at_rest matches its screen. The message asks for a sum, so the answer is the agent's own, not the
    # message read back.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    # The wake comes from the daemon's passes, which a leased machine's account does not run by itself.
    And a daemon of this scenario's own
    And a project called "wake" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "porelo task start wake --project wake --repository wake --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
    Then the "claude" agent in task "wake" of "wake" reaches work without being asked anything
    When I type "Reply with the single word PONG."
    And I press Enter
    # The model's answer, not the typed line, which has the word in it too. A notice Claude Code draws at start can
    # take the Enter, leaving the line in its input box (the line under a rule, after the prompt sign); a person
    # presses Enter again, and so does a try that finds it there.
    Then within 120 seconds this script exits zero:
      """
      screen=$(podman exec porelo-wake-wake tmux capture-pane -p -t porelo 2>&1)
      printf '%s\n' "$screen" | grep -v 'single word PONG' | grep -qw PONG && exit 0
      if printf '%s\n' "$screen" | grep -A1 '^──' | grep '^❯' | grep -qF 'single word PONG'; then
        podman exec porelo-wake-wake tmux send-keys -t porelo Enter; echo 'the line still stood in the input box: Enter again'
      fi
      printf 'screen, last lines:\n'; printf '%s\n' "$screen" | grep -v '^ *$' | tail -12
      exit 1
      """
    # An answer is on the screen a moment before its turn ends. At rest is what claude.yaml's at_rest reads, so
    # the message below comes to an agent at rest, not to one still finishing its turn.
    Then within 60 seconds this script exits zero:
      """
      screen=$(podman exec porelo-wake-wake tmux capture-pane -p -t porelo)
      printf '%s\n' "$screen" | grep -qF 'bypass permissions on' || exit 1
      ! printf '%s\n' "$screen" | grep -qE 'esc to interrupt|Esc to cancel'
      """
    When a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | porelo talk tell porelo-wake-wake"
    Then it exits zero
    And within 120 seconds the terminal shows "A message for you waits"
    # The guide has a message answered with a message, so the answer counts either way the agent gives it:
    # as its reply, held because this project cannot address the person, or on its screen. The kit stops
    # the wait at once when the agent loops.
    Then within 240 seconds this script exits zero:
      """
      held=$(porelo talk held porelo-wake-wake)
      for id in $(printf '%s\n' "$held" | awk '$1 ~ /\.json$/ && $2 == "held" {print $1}'); do
        porelo talk read porelo-wake-wake "$id" | grep -qE '5,?555' && exit 0
      done
      screen=$(podman exec porelo-wake-wake tmux capture-pane -p -S - -t porelo)
      printf '%s\n' "$screen" | grep -qE '5,?555' && exit 0
      printf 'held: %s\nscreen, last lines:\n' "$held"; printf '%s\n' "$screen" | grep -v '^ *$' | tail -15
      exit 1
      """
    When a script runs "porelo task remove porelo-wake-wake --force"
    And a script runs "porelo project unfollow wake --force"
    Then it exits zero

  @slow
  Scenario: a message that arrives while the agent works wakes it once it is at rest
    # The case a single look at the screen misses: told while it works, the agent is announced the message when
    # its turn has ended, not never.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    # The wake comes from the daemon's passes, which a leased machine's account does not run by itself.
    And a daemon of this scenario's own
    And a project called "busy" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "porelo task start busy --project busy --repository busy --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
    Then the "claude" agent in task "busy" of "busy" reaches work without being asked anything
    When I type "Run the shell command 'sleep 10' with your shell tool, then reply with the single word DONE."
    And I press Enter
    # Working, seen rather than assumed: its sleep runs in the task, or its tool shows it running, so the message
    # below comes while it works. A line still standing in the input box gets Enter again, as above. A try that sees
    # neither says what it saw.
    Then within 120 seconds this script exits zero:
      """
      top=$(podman top porelo-busy-busy args 2>&1)
      screen=$(podman exec porelo-busy-busy tmux capture-pane -p -t porelo 2>&1)
      printf '%s\n' "$top" | grep -qE '(^|/)sleep 10' && exit 0
      printf '%s\n' "$screen" | grep -qF 'Bash(sleep 10)' && printf '%s\n' "$screen" | grep -qF 'Running…' && exit 0
      if printf '%s\n' "$screen" | grep -A1 '^──' | grep '^❯' | grep -qF 'single word DONE'; then
        podman exec porelo-busy-busy tmux send-keys -t porelo Enter; echo 'the line still stood in the input box: Enter again'
      fi
      printf 'podman top:\n%s\nscreen, last lines:\n' "$top"; printf '%s\n' "$screen" | grep -v '^ *$' | tail -12
      exit 1
      """
    When a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | porelo talk tell porelo-busy-busy"
    Then it exits zero
    And within 240 seconds the terminal shows "A message for you waits"
    # The guide has a message answered with a message, so the answer counts either way the agent gives it:
    # as its reply, held because this project cannot address the person, or on its screen. The kit stops
    # the wait at once when the agent loops.
    Then within 240 seconds this script exits zero:
      """
      held=$(porelo talk held porelo-busy-busy)
      for id in $(printf '%s\n' "$held" | awk '$1 ~ /\.json$/ && $2 == "held" {print $1}'); do
        porelo talk read porelo-busy-busy "$id" | grep -qE '5,?555' && exit 0
      done
      screen=$(podman exec porelo-busy-busy tmux capture-pane -p -S - -t porelo)
      printf '%s\n' "$screen" | grep -qE '5,?555' && exit 0
      printf 'held: %s\nscreen, last lines:\n' "$held"; printf '%s\n' "$screen" | grep -v '^ *$' | tail -15
      exit 1
      """
    When a script runs "porelo task remove porelo-busy-busy --force"
    And a script runs "porelo project unfollow busy --force"
    Then it exits zero
