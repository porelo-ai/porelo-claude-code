package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkflowRulesTest {

    /** A profile list given to Maven, with or without a space or quotes. */
    private static final Pattern PROFILES =
            Pattern.compile("(?:^|\\s)(?:-P|--activate-profiles[= ])\\s*['\"]?([^\\s'\"]+)");

    /** What makes Maven skip the tests, or not even compile them. */
    private static final Pattern SKIPS_TESTS =
            Pattern.compile("-DskipTests\\b|-Dmaven\\.test\\.skip\\b");

    /**
     * The package builds a scan read, and those of them that skip their tests, each as its file and
     * line.
     *
     * @param builds Every command that builds the packages.
     * @param untested Those that skip the tests.
     */
    record Scan(List<String> builds, List<String> untested) {}

    @Test
    void noWorkflowBuildsThePackagesWithoutTheirTests() throws IOException {

        // -DskipTests also skips NativeLinkageCheck and PackageVersionCheck, so the packages would
        // ship a binary nobody held to the floor, with a version nobody read.
        final Scan scan = scan(workflows());

        assertThat(scan.builds().stream().map(build -> build.substring(0, build.indexOf(':'))))
                .as("the package builds read")
                .contains("build.yml", "update.yml");
        assertThat(scan.untested()).as("package builds that skip their tests").isEmpty();
    }

    @Test
    void seesAPackageBuildThatSkipsItsTestsHoweverItIsWritten(@TempDir final Path dir)
            throws IOException {

        Files.writeString(
                dir.resolve("spaced.yml"),
                "            - run: ./mvnw -B -s settings.xml -P native,dist verify -DskipTests\n");
        Files.writeString(
                dir.resolve("folded.yaml"),
                "            - run: ./mvnw -B -s settings.xml -Pnative,dist verify\n"
                        + "                  -DskipTests\n");
        Files.writeString(
                dir.resolve("tested.yml"),
                "            - run: ./mvnw -B -s settings.xml -Pnative,dist verify\n");

        final Scan scan = scan(dir);

        assertThat(scan.builds())
                .as("the package builds read")
                .containsExactly("folded.yaml:1", "spaced.yml:1", "tested.yml:1");
        assertThat(scan.untested())
                .as("package builds that skip their tests")
                .containsExactly("folded.yaml:1", "spaced.yml:1");
    }

    /**
     * Reads every workflow in a directory for the commands that build the packages.
     *
     * @param dir The workflows' directory.
     * @return What was read and what skips the tests.
     * @throws IOException If a workflow cannot be read.
     */
    static Scan scan(final Path dir) throws IOException {
        final List<String> builds = new ArrayList<>();
        final List<String> untested = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (final Path file : files.filter(WorkflowRulesTest::isWorkflow).sorted().toList()) {
                final List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (!buildsThePackages(lines.get(i))) {
                        continue;
                    }
                    final String at = file.getFileName() + ":" + (i + 1);
                    builds.add(at);
                    if (SKIPS_TESTS.matcher(command(lines, i)).find()) {
                        untested.add(at);
                    }
                }
            }
        }
        return new Scan(builds, untested);
    }

    @Test
    void anAcceptanceLegWithAnEmptyKeyStopsBeforeItRents(@TempDir final Path dir) throws Exception {

        // Without the key the credential scenarios skip themselves, and a green leg proves less
        // than it says.
        final List<String> legs =
                WorkflowSteps.named("build.yml", "Acceptance on ${{ matrix.os }}");
        assertThat(legs).as("acceptance steps in build.yml").hasSize(1);
        final String leg = legs.get(0).replace("${{ matrix.os }}", "ubuntu");
        final Path bin = dir.resolve("bin");
        WorkflowSteps.stub(bin, "java", "echo \"$*\" >> \"$HOME/java-calls\"\n");
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), "tooling.jar");

        final WorkflowSteps.Run empty =
                WorkflowSteps.run(
                        leg,
                        dir,
                        Map.of(
                                "PATH", WorkflowSteps.pathWith(bin),
                                "GITHUB_REF_TYPE", "branch",
                                "PORELO_E2E_OPENROUTER_API_KEY", ""));
        assertThat(empty.exit())
                .as("exit of the leg with an empty key:%n%s", empty.output())
                .isNotZero();
        assertThat(dir.resolve("java-calls"))
                .as("the machine tooling, called with an empty key")
                .doesNotExist();

        // The other half: with a key the same step reaches the tooling, so the stop above is the
        // empty key's.
        final WorkflowSteps.Run keyed =
                WorkflowSteps.run(
                        leg,
                        dir,
                        Map.of(
                                "PATH", WorkflowSteps.pathWith(bin),
                                "GITHUB_REF_TYPE", "branch",
                                "PORELO_E2E_OPENROUTER_API_KEY", "a-key"));
        assertThat(keyed.exit()).as("exit of the leg with a key:%n%s", keyed.output()).isZero();
        assertThat(Files.readString(dir.resolve("java-calls")))
                .as("the machine tooling, called with a key")
                .contains("acceptance --os ubuntu");
    }

    @Test
    void theUpdateJobStopsOnAnEmptyKey(@TempDir final Path dir) throws Exception {

        // The update job's legs run only while nothing said STOP; a leg without the key would
        // prove less and still let the pin through.
        final List<String> gates = WorkflowSteps.named("update.yml", "The acceptance key is there");
        assertThat(gates).as("the key's gate in update.yml").hasSize(1);

        final Path env = dir.resolve("github-env");
        Files.writeString(env, "");
        final WorkflowSteps.Run empty =
                WorkflowSteps.run(
                        gates.get(0), dir, Map.of("GITHUB_ENV", env.toString(), "KEY", ""));
        assertThat(empty.exit()).as("exit of the gate:%n%s", empty.output()).isZero();
        assertThat(Files.readString(env))
                .as("what the gate wrote with an empty key")
                .startsWith("STOP=");

        Files.writeString(env, "");
        final WorkflowSteps.Run keyed =
                WorkflowSteps.run(
                        gates.get(0), dir, Map.of("GITHUB_ENV", env.toString(), "KEY", "a-key"));
        assertThat(keyed.exit()).as("exit of the gate:%n%s", keyed.output()).isZero();
        assertThat(Files.readString(env)).as("what the gate wrote with a key").isEmpty();
    }

    @Test
    void aBranchNameIsNeverRunAsShell(@TempDir final Path dir) throws Exception {

        // Whoever may push a branch would otherwise run a command on the runner by naming it.
        final String ref = "refs/heads/x$(touch${IFS}ran)";
        final List<String> steps = WorkflowSteps.named("build.yml", "Not publishing");
        assertThat(steps).as("\"Not publishing\" steps in build.yml").hasSize(1);
        // Interpolated as the runner does before the shell sees the script.
        final String step =
                steps.get(0)
                        .replace("${{ github.event_name }}", "push")
                        .replace("${{ github.ref }}", ref);

        final WorkflowSteps.Run run =
                WorkflowSteps.run(
                        step, dir, Map.of("GITHUB_EVENT_NAME", "push", "GITHUB_REF", ref));

        assertThat(run.exit()).as("exit of the step:%n%s", run.output()).isZero();
        assertThat(dir.resolve("ran")).as("what the branch name ran").doesNotExist();
        assertThat(run.output()).as("what the step said").contains(ref);
    }

    private static Path workflows() {
        return RepositoryDocuments.root().resolve(".github/workflows");
    }

    /** A workflow file, whichever of its two extensions it has. */
    static boolean isWorkflow(final Path file) {
        final String name = file.getFileName().toString();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    /** Whether a line activates both the native and the dist profile, however it spells them. */
    private static boolean buildsThePackages(final String line) {
        final Matcher matcher = PROFILES.matcher(line);
        while (matcher.find()) {
            final List<String> profiles =
                    Stream.of(matcher.group(1).split(","))
                            .map(profile -> profile.strip().replaceFirst("^\\+", ""))
                            .toList();
            if (profiles.contains("native") && profiles.contains("dist")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The command a line starts: it and the lines that continue it, after a trailing backslash or
     * folded onto more-indented lines.
     */
    private static String command(final List<String> lines, final int start) {
        final StringBuilder command = new StringBuilder(lines.get(start));
        final int indent = indentOf(lines.get(start));
        for (int j = start + 1;
                j < lines.size()
                        && !lines.get(j).isBlank()
                        && (indentOf(lines.get(j)) > indent || lines.get(j - 1).endsWith("\\"));
                j++) {
            command.append('\n').append(lines.get(j));
        }
        return command.toString();
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
