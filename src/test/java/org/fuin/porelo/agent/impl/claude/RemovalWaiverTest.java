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

    private static List<String> section(final int subagents, final String name) {
        final Object root = Json.parse(ClaudeSettings.document(subagents));
        assertThat(root).as("the settings document").isInstanceOf(Map.class);
        final Object section = ((Map<?, ?>) root).get(name);
        assertThat(section).as("the settings' %s", name).isInstanceOf(Map.class);
        return ((Map<?, ?>) section).keySet().stream().map(String::valueOf).toList();
    }
}
