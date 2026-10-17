Feature: The published package on a clean machine

  Everything before this proved the code was right; this proves that what an operator gets is.
  The packages were installed from the repository by the run that rented this machine, the way an
  operator installs them - and this agent's binary was built in another repository against a
  published contract, so porelo has never heard of it.

  Background:
    Given the suite runs as an unprivileged user

  Scenario: porelo is installed and discovers the agent it was never linked against
    When a script runs "porelo --version"
    Then it exits zero
    And a script running "porelo agents" mentions "claude"

  Scenario: a person sees the agent by its name and its label
    Given a terminal on the machine
    When I run "porelo agents"
    Then the terminal shows "claude"
    And the terminal shows "Claude Code"

  Scenario: the hooks register for this user
    When a script runs "porelo setup"
    Then it exits zero

  Scenario: doctor says what this machine can do
    Given a terminal on the machine
    When I run "porelo doctor"
    Then the terminal shows "hooks registered"

  Scenario: the installed version is a snapshot the next build supersedes
    # A flat snapshot is the same version every build, so 'apt upgrade' has nothing to do and
    # whoever installed yesterday stays there. Asked of dpkg or rpm, not reasoned about.
    Then the installed package "porelo-agent-claude" is a snapshot that the next build supersedes

  Scenario: the package carries a bill naming what it installs
    # Checked on a machine that installed the package rather than in the build that made it: an
    # update gate diffs this against the published one, and a missing bill breaks it silently.
    Then the bill at "/usr/share/porelo/sbom/porelo-agent-claude.cdx.json" names "claude-code"

  Scenario: the adapter installs the Claude Code its bill names
    # Two facts written by two different steps of the build; an image installs one and the
    # update gate compares the other.
    Then the "claude" agent installs the version the bill at "/usr/share/porelo/sbom/porelo-agent-claude.cdx.json" names for "claude-code"

  Scenario: an update run installs the version it was asked to test
    # Set only by the update job, which tests its candidate packages rather than the published ones.
    Given the environment variable "PORELO_E2E_EXPECT_CLI" is set
    Then the "claude" agent installs the version in the environment variable "PORELO_E2E_EXPECT_CLI"
