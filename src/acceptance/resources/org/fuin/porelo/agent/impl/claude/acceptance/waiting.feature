@credential
Feature: Porelo shows the agent waiting for a person, read by the agent's own declaration

  Claude Code declares in its YAML what a question looks like on its attached screen. This drives it to one
  at the version this package pins and asks Porelo, never the screen, what it makes of it - so a release
  that words its questions differently fails here, instead of going quiet in the field.

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "PORELO_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "PORELO_E2E_MODEL" is set

  @slow
  Scenario: an attached agent is shown not waiting at its prompt, and waiting once it asks
    # Typed, then Enter on its own, as a person does. Measured on 2.1.267: a line feed is a new line
    # in its prompt, and text with its carriage return in one write reads as a paste, whose carriage
    # return does not submit either - so neither 'I type' nor 'I enter' alone asks anything. Only once
    # it is at work, when its screen is in raw mode and the carriage return reaches it as one.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "asks" of class "guarded" with a file in it
    And a terminal on the machine
    # --model: the model the suite pays for answers, not the agent's own default.
    When I run "porelo task start asks --project asks --repository asks --agent claude --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
    Then the "claude" agent in task "asks" of "asks" reaches work without being asked anything
    And porelo shows the "claude" agent in task "asks" of "asks" not waiting for a person
    When I type "Use your AskUserQuestion tool to ask me whether I prefer red or blue. Do nothing else."
    # Enter only once the text is on its screen. Over a slow link the text and an Enter right behind it
    # arrive as one burst, which it takes as a paste and does not submit - measured in CI.
    Then the screen of task "asks" of "asks" shows "Do nothing else."
    When I press Enter
    # Its question on its own screen first, with the kit's patience: how long the model takes to ask is
    # not what this proves, and when it never asks, this step prints the screen it drew instead.
    Then the screen of task "asks" of "asks" shows "Esc to cancel"
    And porelo shows the "claude" agent in task "asks" of "asks" waiting for a person
    When a script runs "porelo project unfollow asks --force"
    Then it exits zero
