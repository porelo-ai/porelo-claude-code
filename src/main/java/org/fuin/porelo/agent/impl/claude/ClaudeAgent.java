package org.fuin.porelo.agent.impl.claude;

import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.AgentMain;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.agent.api.Finder;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.agent.api.YamlAgent;
import org.jspecify.annotations.Nullable;

/**
 * Claude Code.
 *
 * <p>Five overrides, all because the definition genuinely cannot express them, and all living here
 * rather than in Porelo:
 *
 * <ul>
 *   <li>the credential is an OAuth token nested in a JSON file Claude writes at login;
 *   <li>what it finds of itself on the host depends on several files read in Claude's own order;
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

    /**
     * Says what Claude Code finds of itself on the host; overrides the agent API's default once
     * Porelo publishes it.
     *
     * @return Finder reading Claude Code's own files.
     */
    @Override
    public Finder finder() {
        return ClaudeFindings.onThisMachine();
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
