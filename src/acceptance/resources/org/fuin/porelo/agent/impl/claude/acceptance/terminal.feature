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
    And the terminal shows "raw.githubusercontent.com"

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
