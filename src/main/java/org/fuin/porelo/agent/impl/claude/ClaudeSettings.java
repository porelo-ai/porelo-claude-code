package org.fuin.porelo.agent.impl.claude;

import java.util.LinkedHashMap;
import java.util.Map;
import org.fuin.porelo.wire.Json;

/**
 * The settings that stop the CLI asking to be allowed to do what it was started to do.
 *
 * <p><strong>Entering bypass mode raises a dialog, whichever way it is entered.</strong> The CLI
 * opens with a full-screen warning whose default answer is "No, exit" and which asks the operator
 * to accept responsibility for actions taken in bypass mode. It is not a permission prompt, so no
 * flag answers it - and unattended there is nobody to answer it at all, which is a task that starts
 * and then waits at a menu.
 *
 * <p><strong>Each key does one thing, measured on 2.1.267 at a terminal, a fresh task per
 * run:</strong> {@code defaultMode} alone puts the session in bypass mode and raises the warning,
 * with or without {@code --dangerously-skip-permissions}; {@code skipDangerousModePermissionPrompt}
 * alone silences the warning but leaves manual mode unless the flag is given; both together reach
 * bypass mode with no warning, and the flag then changes nothing, so it is not passed. Headless the
 * same holds: without {@code defaultMode} a {@code -p} run stopped to ask permission for a shell
 * command, a question nobody answers there; with it the command ran.
 *
 * <p><strong>Answering it here is defensible in a way it would not be on a person's
 * machine.</strong> The responsibility the dialog asks about is the container's: an unreviewed
 * egress policy, a workspace that is a clone rather than the operator's checkout, and a gate
 * between anything the agent commits and the real upstream. The question was answered when somebody
 * chose to run the agent in a box.
 *
 * <p><strong>It also keeps the CLI at the version that was installed.</strong> See {@code
 * doc/decisions.md}, "The CLI does not update itself in a task".
 *
 * <p>Kept apart from {@link ClaudeFirstRun}, which answers the wizard, because the two files are
 * different: this is configuration a person would edit, that is state the CLI writes itself.
 */
final class ClaudeSettings {

    /** Where the CLI reads its settings. */
    static final String FILE = "/home/agent/.claude/settings.json";

    private ClaudeSettings() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the settings document for a container.
     *
     * @return File content.
     */
    static String document() {

        final Map<String, Object> permissions = new LinkedHashMap<>();
        // What puts the session in bypass mode, flag or not. Alone it also raises the warning.
        permissions.put("defaultMode", "bypassPermissions");

        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("permissions", permissions);
        // What silences the warning. Alone it leaves manual mode; with the key above, bypass mode
        // is
        // reached without it.
        root.put("skipDangerousModePermissionPrompt", true);

        // The version that runs is the pinned one. Left alone, the CLI fetched 2.1.276 within 90
        // seconds of starting 2.1.267 and would run it on the next start - unpinned and unchecked.
        // DISABLE_UPDATES rather than DISABLE_AUTOUPDATER: it also refuses 'claude update', which
        // the agent could otherwise run through its own shell. 'autoUpdates: false' is no answer,
        // the CLI ignores it for a native install.
        final Map<String, Object> env = new LinkedHashMap<>();
        env.put("DISABLE_UPDATES", "1");
        // A model this version's catalog does not describe - other vendors' models over OpenRouter
        // - would be
        // held to the 200k tokens the CLI assumes, with a notice at every start. Its real window is
        // the
        // provider's to know, not the adapter's: with this the CLI compacts when the provider says
        // the context
        // is full, as before 2.1.267. By the CLI's own check it applies only to a model outside its
        // catalog.
        env.put("CLAUDE_CODE_DISABLE_UNKNOWN_MODEL_WINDOW_ENFORCEMENT", "1");
        // No memory of its own: what holds across tasks is the repository's reviewed AGENTS.md.
        // The variable, not 'autoMemoryEnabled': the /memory menu cannot switch it back on.
        env.put("CLAUDE_CODE_DISABLE_AUTO_MEMORY", "1");
        root.put("env", env);
        return Json.write(root);
    }
}
