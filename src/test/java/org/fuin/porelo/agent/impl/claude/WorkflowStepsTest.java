package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for how a workflow step is run by the tests that run one. */
class WorkflowStepsTest {

    @Test
    void aStepThatHangsIsStoppedAtItsLimit(@TempDir final Path dir) {

        // A step that never ends would otherwise hang the whole suite, not fail one test.
        final long start = System.nanoTime();
        assertThatThrownBy(
                        () -> WorkflowSteps.run("sleep 20\n", dir, Map.of(), Duration.ofSeconds(1)))
                .as("a step that sleeps past its limit")
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("did not end within");
        assertThat(Duration.ofNanos(System.nanoTime() - start))
                .as("how long the hanging step was waited for")
                .isLessThan(Duration.ofSeconds(10));
    }
}
