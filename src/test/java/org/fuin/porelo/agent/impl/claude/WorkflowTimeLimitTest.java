package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkflowTimeLimitTest {

    private static final Pattern JOB = Pattern.compile("^    ([A-Za-z_][A-Za-z0-9_-]*):\\s*$");

    /**
     * The jobs a scan read, and those of them without a limit, each as its file and its id.
     *
     * @param jobs Every job read.
     * @param unlimited The jobs without {@code timeout-minutes}.
     */
    record Scan(List<String> jobs, List<String> unlimited) {}

    @Test
    void everyJobStopsAfterALimit() throws IOException {

        // GitHub's default is six hours, and a leg on a rented machine pays for every minute of a
        // hang.
        final Scan scan = scan(RepositoryDocuments.root().resolve(".github/workflows"));

        assertThat(scan.jobs())
                .as("the jobs read")
                .contains(
                        "build.yml build",
                        "build.yml acceptance",
                        "build.yml release",
                        "update.yml update",
                        "shared-rules.yml check-shared");
        assertThat(scan.unlimited()).as("jobs without timeout-minutes").isEmpty();
    }

    @Test
    void seesAJobWithoutALimitWhateverItsFileOrIdIsCalled(@TempDir final Path dir)
            throws IOException {

        Files.writeString(
                dir.resolve("other.yaml"),
                "jobs:\n\n    Build:\n        runs-on: ubuntu-24.04\n        steps:\n"
                        + "            - run: true\n");
        Files.writeString(
                dir.resolve("limited.yml"),
                "jobs:\n\n    limited:\n        timeout-minutes: 5\n        steps:\n"
                        + "            - run: true\n");

        final Scan scan = scan(dir);

        assertThat(scan.jobs())
                .as("the jobs read")
                .containsExactly("limited.yml limited", "other.yaml Build");
        assertThat(scan.unlimited())
                .as("jobs without timeout-minutes")
                .containsExactly("other.yaml Build");
    }

    /**
     * Reads every workflow in a directory for its jobs and their limits.
     *
     * @param dir The workflows' directory.
     * @return What was read and what has no limit.
     * @throws IOException If a workflow cannot be read.
     */
    static Scan scan(final Path dir) throws IOException {
        final List<String> jobs = new ArrayList<>();
        final List<String> unlimited = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (final Path file : files.filter(WorkflowRulesTest::isWorkflow).sorted().toList()) {
                boolean inJobs = false;
                String job = null;
                boolean limited = false;
                for (final String line : Files.readAllLines(file)) {
                    if (line.equals("jobs:")) {
                        inJobs = true;
                        continue;
                    }
                    final Matcher m = JOB.matcher(line);
                    if (inJobs && m.matches()) {
                        if (job != null && !limited) {
                            unlimited.add(file.getFileName() + " " + job);
                        }
                        job = m.group(1);
                        jobs.add(file.getFileName() + " " + job);
                        limited = false;
                    } else if (line.matches("^        timeout-minutes: [1-9][0-9]*\\s*(#.*)?$")) {
                        limited = true;
                    }
                }
                if (job != null && !limited) {
                    unlimited.add(file.getFileName() + " " + job);
                }
            }
        }
        return new Scan(jobs, unlimited);
    }
}
