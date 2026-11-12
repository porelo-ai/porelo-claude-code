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
        // The leg reads the pin it tells the suite to expect from the pom it is run beside.
        Files.writeString(dir.resolve("pom.xml"), "<agent.cli.version>1.0.0</agent.cli.version>\n");

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
    void theDailyCheckHoldsTheIssueHeadsAndFailsWithThem(@TempDir final Path dir) throws Exception {

        // An issue head or index that breaks the rules would otherwise turn nothing red until the
        // next push, and a [skip ci] push of issues alone starts no Build.
        final List<String> steps =
                WorkflowSteps.named("shared-rules.yml", "Every issue head holds to its schema");
        assertThat(steps).as("the issue heads' step in shared-rules.yml").hasSize(1);
        final Path bin = dir.resolve("bin");
        WorkflowSteps.stub(
                bin, "java", "echo \"$*\" >> \"$HOME/java-calls\"\nexit \"${CHECK_EXIT:-0}\"\n");
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), "tooling.jar");

        final WorkflowSteps.Run held =
                WorkflowSteps.run(steps.get(0), dir, Map.of("PATH", WorkflowSteps.pathWith(bin)));
        assertThat(held.exit()).as("exit of the step:%n%s", held.output()).isZero();
        assertThat(Files.readAllLines(dir.resolve("java-calls")))
                .as("the release tooling, called by the step on the resolved classpath")
                .containsExactly(
                        "-cp tooling.jar org.fuin.porelo.release.Main issues check issues");
        assertThat(stepBlock("shared-rules.yml", "Every issue head holds to its schema"))
                .as("the step runs on every trigger: a daily run, a pull request, a dispatch")
                .noneMatch(
                        line ->
                                line.trim().startsWith("if:")
                                        || line.trim().startsWith("continue-on-error:"));

        // The other half: a head the tooling refuses fails the step, and so the day's run.
        final WorkflowSteps.Run broken =
                WorkflowSteps.run(
                        steps.get(0),
                        dir,
                        Map.of("PATH", WorkflowSteps.pathWith(bin), "CHECK_EXIT", "1"));
        assertThat(broken.exit()).as("exit of the step:%n%s", broken.output()).isNotZero();
    }

    @Test
    void theAcceptanceLegsRunOnEveryForge() throws IOException {

        // On the host's forge a leg takes a machine of its kind from the host's pool, told where by
        // the forge; a condition naming GitHub's server would leave the host's forge untested until
        // the gathered push.
        final List<String> lines = Files.readAllLines(workflows().resolve("build.yml"));
        final int start = lines.indexOf("    acceptance:");
        assertThat(start).as("the acceptance job in build.yml").isNotNegative();
        int end = start + 1;
        while (end < lines.size() && !lines.get(end).matches("^ {4}[a-z].*:$")) {
            end++;
        }
        assertThat(lines.subList(start, end))
                .as("the acceptance job names no forge's server")
                .noneMatch(line -> line.contains("github.server_url"));

        assertThat(stepBlock("build.yml", "Acceptance on ${{ matrix.os }}"))
                .as("the leg is given the pool's run key, empty where there is no pool")
                .anyMatch(
                        line ->
                                line.trim()
                                        .equals("PORELO_POOL_SSH: ${{ secrets.PORELO_POOL_SSH }}"));

        // GitHub's artifact actions refuse every other server, so the packages travel through the
        // cache, saved and restored under the condition the leg passes --candidate on.
        final String key = "key: candidate-${{ github.run_id }}-${{ github.run_attempt }}";
        final String when = "if: github.ref_type == 'tag' || vars.PORELO_PUBLISH != 'true'";
        assertThat(lines)
                .as("the build job saves the packages under the key the legs restore")
                .filteredOn(line -> line.trim().equals(key))
                .hasSize(2);
        assertThat(stepBlock("build.yml", "The packages this run built"))
                .map(String::trim)
                .contains(
                        when,
                        key,
                        "restore-keys: candidate-${{ github.run_id }}-",
                        "fail-on-cache-miss: true")
                .noneMatch(line -> line.contains("download-artifact"));
        assertThat(stepBlock("build.yml", "Acceptance on ${{ matrix.os }}"))
                .as("the leg passes --candidate where the packages were restored")
                .anyMatch(
                        line ->
                                line.contains(
                                        "if [ \"$GITHUB_REF_TYPE\" = tag ] || [ \"$PORELO_PUBLISH\" != true ]; then FROM=\"--package porelo-agent-claude --candidate candidate\""));
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

    /**
     * The lines of one step in a workflow, from its name to the next step or the end.
     *
     * @param workflow The workflow's file name.
     * @param name The step's name.
     * @return Its lines.
     * @throws IOException If the workflow cannot be read.
     */
    private static List<String> stepBlock(final String workflow, final String name)
            throws IOException {
        final List<String> lines = Files.readAllLines(workflows().resolve(workflow));
        final int start = lines.indexOf("            - name: " + name);
        assertThat(start).as("the step %s in %s", name, workflow).isNotNegative();
        int end = start + 1;
        while (end < lines.size()
                && !lines.get(end).startsWith("            - ")
                && !lines.get(end).matches("^ {0,8}\\S.*")) {
            end++;
        }
        return lines.subList(start, end);
    }
}
