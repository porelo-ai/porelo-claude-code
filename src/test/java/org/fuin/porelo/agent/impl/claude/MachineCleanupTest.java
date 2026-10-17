package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs every step that deletes what a run rented, with a machine tooling that could not delete
 * everything. A cleanup that fails quietly leaves rented machines running and paid for, with a
 * green run that says nothing of them.
 */
class MachineCleanupTest {

    @Test
    void aCleanupThatLeavesSomethingFailsItsStep(@TempDir final Path dir) throws Exception {

        final List<String> steps = new ArrayList<>();
        steps.addAll(WorkflowSteps.named("build.yml", "Delete what this run created"));
        steps.addAll(WorkflowSteps.named("update.yml", "Delete what this run created"));
        steps.addAll(
                WorkflowSteps.named("update.yml", "Delete what this run created - fedora leg"));
        assertThat(steps)
                .as("cleanup steps: the acceptance job's and the update job's two legs")
                .hasSize(3);
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), "tooling.jar");
        final Path bin = dir.resolve("bin");
        WorkflowSteps.stub(bin, "java", "echo \"left server acc-ubuntu-1\" >&2\nexit 1\n");

        for (final String step : steps) {
            final WorkflowSteps.Run run =
                    WorkflowSteps.run(step, dir, Map.of("PATH", WorkflowSteps.pathWith(bin)));
            assertThat(run.exit())
                    .as("exit of %s when the sweep left something:%n%s", step.strip(), run.output())
                    .isNotZero();
        }
    }
}
