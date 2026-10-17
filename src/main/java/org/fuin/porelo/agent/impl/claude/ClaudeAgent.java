package org.fuin.porelo.agent.impl.claude;

import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.AgentMain;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.agent.api.YamlAgent;
import org.jspecify.annotations.Nullable;

/**
 * Claude Code.
 *
 * <p>Four overrides, all because the definition genuinely cannot express them, and all living here
 * rather than in Porelo:
 *
 * <ul>
 *   <li>the credential is an OAuth token nested in a JSON file Claude writes at login;
 *   <li>a fresh container needs the files Claude reads at its first run and its settings;
 *   <li>the output is a stream of JSON objects rather than lines of text;
 *   <li>how the run ended is read from the last of those objects, the result line.
 * </ul>
 *
 * Everything else is {@code claude.yaml}.
 */
public final class ClaudeAgent extends YamlAgent {

    /** Constructor. */
    public ClaudeAgent() {
        super("claude");
    }

    @Override
    public CredentialExtractor credentialExtractor() {
        return new ClaudeCredentialExtractor();
    }

    @Override
    public ContainerSetup containerSetup() {
        return new ClaudeContainerSetup();
    }

    @Override
    public LogFormatter logFormatter() {
        return new ClaudeStreamJsonFormatter();
    }

    @Override
    public @Nullable AgentEnd ended(String line) {
        return ClaudeStreamJsonFormatter.ended(line);
    }

    /**
     * Entry point of the {@code porelo-agent-claude} binary.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        AgentMain.run(new ClaudeAgent(), args);
    }
}
