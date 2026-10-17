package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowRulesTest {

    private static final String STEP = "            - ";

    @Test
    void noWorkflowBuildsThePackagesWithoutTheirTests() throws IOException {

        // -DskipTests also skips NativeLinkageCheck, so the packages would ship a binary nobody
        // held to the floor.
        final List<String> untested = new ArrayList<>();
        try (Stream<Path> files = Files.list(workflows())) {
            for (final Path file :
                    files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                for (final String line : joined(Files.readAllLines(file))) {
                    if (line.contains("-Pnative,dist") && line.contains("-DskipTests")) {
                        untested.add(file.getFileName() + ": " + line.strip());
                    }
                }
            }
        }
        assertThat(untested).as("package builds without their tests").isEmpty();
    }

    @Test
    void noAcceptanceLegRunsWithoutTheKey() throws IOException {

        // Without it the credential scenarios skip themselves, and a green leg proves less than it
        // says.
        final List<String> unguarded = new ArrayList<>();
        int legs = 0;
        for (final String step : steps(workflows().resolve("build.yml"))) {
            if (step.contains("machines.Main acceptance")) {
                legs++;
                if (!step.contains("-n \"${PORELO_E2E_OPENROUTER_API_KEY:-}\"")) {
                    unguarded.add(step.lines().findFirst().orElse("").strip());
                }
            }
        }
        assertThat(legs).as("acceptance steps in build.yml").isPositive();
        assertThat(unguarded)
                .as("acceptance steps in build.yml that do not refuse an empty key")
                .isEmpty();

        assertThat(steps(workflows().resolve("update.yml")))
                .as("a step in update.yml that stops on an empty key")
                .anyMatch(
                        step ->
                                step.contains("secrets.OPEN_ROUTER_API_KEY")
                                        && step.contains("-n \"$KEY\""));
    }

    private static Path workflows() {
        return RepositoryDocuments.root().resolve(".github/workflows");
    }

    /** A line continued with a trailing backslash, read as one. */
    private static List<String> joined(final List<String> lines) {
        final List<String> result = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        for (final String line : lines) {
            if (line.endsWith("\\")) {
                current.append(line, 0, line.length() - 1).append(' ');
            } else {
                result.add(current.append(line).toString());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    /** Each step of a job, from its dash to the next one. */
    private static List<String> steps(final Path file) throws IOException {
        final List<String> result = new ArrayList<>();
        StringBuilder current = null;
        for (final String line : Files.readAllLines(file)) {
            if (line.startsWith(STEP) || !line.startsWith(" ") && !line.isEmpty()) {
                if (current != null) {
                    result.add(current.toString());
                }
                current = line.startsWith(STEP) ? new StringBuilder() : null;
            }
            if (current != null) {
                current.append(line).append('\n');
            }
        }
        if (current != null) {
            result.add(current.toString());
        }
        return result;
    }

    @Test
    void noFoldedPackageBuildSkipsItsTests() throws IOException {

        // A command folded onto more-indented lines, with or without a backslash, is still one
        // command.
        final List<String> untested = new ArrayList<>();
        try (Stream<Path> files =
                Files.list(RepositoryDocuments.root().resolve(".github/workflows"))) {
            for (final Path file :
                    files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                final List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).contains("-Pnative,dist")) {
                        continue;
                    }
                    final int indent = indentOf(lines.get(i));
                    for (int j = i;
                            j < lines.size()
                                    && (j == i
                                            || !lines.get(j).isBlank()
                                                    && indentOf(lines.get(j)) > indent);
                            j++) {
                        if (lines.get(j).contains("-DskipTests")) {
                            untested.add(file.getFileName() + ":" + (j + 1));
                        }
                    }
                }
            }
        }
        assertThat(untested).as("package builds that skip their tests on a folded line").isEmpty();
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
