package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;

/**
 * The waiver that keeps Claude Code's dangerous-rm check from stopping a task, where the CLI reads
 * it.
 *
 * <p>Prevents a task that hangs on a question nobody answers: measured on 2.1.311, the check asks
 * even in bypass mode, and only this variable in the process environment waives it. That the CLI
 * still honors it is the acceptance suite's question; this one fails when the line is dropped or
 * moved to where it does nothing.
 */
class RemovalWaiverTest {

    private static final String WAIVER = "CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS";

    @Test
    void theTaskImageSetsTheWaiverInEveryStartsEnvironment() {

        assertThat(new ClaudeAgent().definition().installAsRoot())
                .as("an ENV line of the image reaches every start of the CLI in a task")
                .contains("ENV " + WAIVER + "=1");
    }

    @Test
    void theSettingsHoldNoHookThatClearsTheWaiver() {

        // Measured in the binary: a PermissionRequest hook makes the waiver have no effect.
        for (final int subagents : new int[] {0, 2}) {
            assertThat(section(subagents, "hooks"))
                    .as("the settings' hooks, with %d subagents allowed", subagents)
                    .doesNotContain("PermissionRequest");
        }
    }

    @Test
    void theSettingsDoNotCarryTheWaiverWhereItDoesNothing() {

        // Measured on 2.1.311: in the settings' env the check still asked, so a copy there only
        // looks like the waiver.
        for (final int subagents : new int[] {0, 2}) {
            assertThat(section(subagents, "env"))
                    .as("the settings' env, with %d subagents allowed", subagents)
                    .doesNotContain(WAIVER);
        }
    }

    /** What 2.1.318 wrote to standard error in a headless run with the waiver set. */
    private static final String NOTICE =
            WAIVER
                    + " is set, so dangerous rm commands can run without a prompt in bypass"
                    + " permissions mode once managed settings and plugin hooks have loaded. If you"
                    + " didn't set it, check your shell profile.";

    @Test
    void theNoticeThatTheWaiverIsSetIsNotShownInATasksLog() {

        final var formatter = new ClaudeAgent().logFormatter();

        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"notification\","
                                        + "\"key\":\"bypass-removal-waiver\",\"text\":\""
                                        + NOTICE
                                        + "\",\"priority\":\"high\",\"color\":\"warning\"}"))
                .as("the notice's stream line, known by its key")
                .isNull();
        assertThat(formatter.format(NOTICE)).as("the notice on standard error").isNull();
        assertThat(formatter.format("\u001b[33m" + NOTICE + "\u001b[39m"))
                .as("the notice on standard error in yellow")
                .isNull();
        assertThat(formatter.format("  " + NOTICE + "\r"))
                .as("the notice with a margin and a carriage return")
                .isNull();
    }

    @Test
    void everyOtherSystemLineStillReachesTheLog() {

        final var formatter = new ClaudeAgent().logFormatter();

        final String other =
                "{\"type\":\"system\",\"subtype\":\"notification\",\"key\":\"other\","
                        + "\"text\":\"something else\"}";
        assertThat(formatter.format(other)).as("a notification with another key").isEqualTo(other);
        final String retry = "{\"type\":\"system\",\"subtype\":\"api_retry\",\"attempt\":1}";
        assertThat(formatter.format(retry)).as("another system subtype").isEqualTo(retry);
        // That the waiver does nothing is what a task's log must still say.
        final String lost =
                WAIVER
                        + " has no effect in this session: dangerous rm commands keep their"
                        + " usual checks.";
        assertThat(formatter.format(lost)).as("the warning that the waiver is off").isEqualTo(lost);
        final String quoted = "the log said: " + NOTICE;
        assertThat(formatter.format(quoted)).as("the words inside a line").isEqualTo(quoted);
        assertThat(
                        formatter.format(
                                "{\"type\":\"assistant\",\"message\":{\"content\":[{\"text\":\""
                                        + NOTICE
                                        + "\"}]}}"))
                .as("the agent's own reply quoting the words")
                .isEqualTo(NOTICE);
    }

    private static List<String> section(final int subagents, final String name) {
        final Object root = Json.parse(ClaudeSettings.document(subagents));
        assertThat(root).as("the settings document").isInstanceOf(Map.class);
        final Object section = ((Map<?, ?>) root).get(name);
        assertThat(section).as("the settings' %s", name).isInstanceOf(Map.class);
        return ((Map<?, ?>) section).keySet().stream().map(String::valueOf).toList();
    }
}
