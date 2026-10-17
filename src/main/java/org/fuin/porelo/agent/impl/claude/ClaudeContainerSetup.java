package org.fuin.porelo.agent.impl.claude;

import java.util.List;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.ContainerFile;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.SetupContext;

/**
 * What Claude Code needs in a fresh container before it will run.
 * <p>
 * The endpoint is not used: this agent is told where to send its requests with environment
 * variables, which is the shape it declared. An agent that can only be given a URL writes it into
 * a file here instead.
 * <p>
 * Two files with two different owners, which is why each is produced somewhere else:
 * {@link ClaudeFirstRun} is the agent's own state, {@link ClaudeSettings} is configuration a person
 * would otherwise edit. This class only says what a container needs.
 * <p>
 * <strong>No credential file.</strong> The task's token reaches the CLI in the variable the definition
 * names for its kind - {@code ANTHROPIC_API_KEY} or {@code CLAUDE_CODE_OAUTH_TOKEN} - and that is what it
 * authenticates from. Measured on 2.1.267 at a terminal: with the variable removed, a
 * {@code .credentials.json} holding the same token gave no key and no token at all, for both kinds;
 * with the file removed, the variable alone authenticated. A file the CLI does not read is only a
 * second copy of the token.
 */
public class ClaudeContainerSetup implements ContainerSetup {

    @Override
    public List<ContainerFile> files(final SetupContext context) {
        // Only an API key is shown in the dialog the approval answers; an OAuth token travels in a
        // variable the CLI does not ask about. Approved, the file holds part of the key.
        final String apiKey = credentialed(context)
                && ClaudeCredentialExtractor.API_KEY.equals(credentialKind(context))
                ? context.token() : "";
        final String firstRun = ClaudeFirstRun.document(context.workspace(), apiKey);
        return List.of(
                apiKey.isEmpty() ? ContainerFile.of(ClaudeFirstRun.FILE, firstRun)
                        : ContainerFile.secret(ClaudeFirstRun.FILE, firstRun),
                ContainerFile.of(ClaudeSettings.FILE, ClaudeSettings.document()));
    }

    /**
     * Returns the task's credential kind, refusing one this provider does not know.
     * <p>
     * <strong>Refused rather than guessed.</strong> An unknown kind would reach the CLI in the
     * definition's default variable, {@code ANTHROPIC_API_KEY}, and fail looking exactly like a wrong
     * key - which sends whoever reads it looking at the credential instead of at the mistake.
     *
     * @param context What the agent was told about the task.
     * @return The kind, one of the two this provider stores.
     * @throws AgentException If the task has a token of another kind.
     */
    private static String credentialKind(final SetupContext context) {
        final String kind = context.credentialType();
        if (!ClaudeCredentialExtractor.API_KEY.equals(kind) && !ClaudeCredentialExtractor.OAUTH.equals(kind)) {
            throw new AgentException("Cannot set up a task for credential kind '" + kind
                    + "': this provider stores '" + ClaudeCredentialExtractor.API_KEY + "' or '"
                    + ClaudeCredentialExtractor.OAUTH + "'");
        }
        return kind;
    }

    /**
     * Returns whether this task was given a credential to present.
     *
     * @param context What the agent was told about the task.
     * @return {@code true} when there is a token.
     */
    private static boolean credentialed(final SetupContext context) {
        return context.token() != null && !context.token().isBlank();
    }
}
