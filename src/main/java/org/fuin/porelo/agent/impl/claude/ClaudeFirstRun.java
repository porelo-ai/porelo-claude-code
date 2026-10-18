package org.fuin.porelo.agent.impl.claude;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.wire.Json;

/**
 * The answers a fresh container would otherwise be asked for.
 *
 * <p>Measured, not documented. A container has never been logged in, so the CLI runs its first-run
 * wizard: it asks for a theme, then offers a login menu, then asks whether the workspace is
 * trusted. The login menu belongs to that wizard rather than to any check of the credential - with
 * the wizard marked done, the task's token is used without question, phantom or not.
 *
 * <p><strong>This is the agent's own state, not the provider's.</strong> It would look the same
 * whoever served the model, which is why it is kept apart from the settings beside it.
 */
final class ClaudeFirstRun {

    /** Where the CLI keeps the answers to its first-run questions. */
    static final String FILE = "/home/agent/.claude.json";

    private ClaudeFirstRun() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** How much of a key the CLI remembers an answer by - measured on 2.1.267. */
    private static final int KEY_TAIL = 20;

    /**
     * Returns the document that marks the first run as done.
     *
     * @param workspace Directory the agent works in.
     * @param apiKey Key the task puts in {@code ANTHROPIC_API_KEY}, or empty when it puts none.
     * @return File content.
     */
    static String document(String workspace, String apiKey) {

        final Map<String, Object> project = new LinkedHashMap<>();
        // Asked once per directory. In a container that directory is new every time, so without
        // this every task begins by asking whether the operator trusts their own project.
        project.put("hasTrustDialogAccepted", true);
        project.put("allowedTools", List.of());

        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("hasCompletedOnboarding", true);
        // From 2.1.292 the CLI offers once to make auto mode the default, and a task's agent would
        // wait at that question. Seen, it is not asked again; the task keeps bypass mode, which
        // the container makes safe (ClaudeSettings).
        root.put("hasSeenAutoDefaultNudge", true);
        root.put("theme", "dark");
        root.put("projects", Map.of(workspace, project));
        if (!apiKey.isBlank()) {
            // A key in the environment makes the CLI ask whether to use it, recommending No. The
            // key is the phantom token Porelo minted for this task, so No refuses the only
            // credential there is; see doc/decisions.md.
            final String tail = apiKey.substring(Math.max(0, apiKey.length() - KEY_TAIL));
            root.put("customApiKeyResponses", Map.of("approved", List.of(tail)));
        }
        return Json.write(root);
    }
}
