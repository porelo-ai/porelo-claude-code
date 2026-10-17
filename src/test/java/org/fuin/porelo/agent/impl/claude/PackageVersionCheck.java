package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Whether the packages the build made carry the project's version, read from the packages.
 *
 * <p>jdeb and the rpm plugin each take their version from their own configuration, and a plugin may
 * take it from a property that means something else - a test of the pom would not see that, a look
 * into the built package does. The version expected is the project's, mapped as the packaging
 * promises: {@code 0.4.1-SNAPSHOT} built as run 7 is {@code 0.4.1~snapshot.7}, so a snapshot sorts
 * below its release and each run above the last; a release is its own version. The rpm's release is
 * always {@code 1}, so the rpm and the deb say the same version.
 *
 * <p>Not a unit test: the packages exist only after jdeb and the rpm plugin, which run at {@code
 * verify}. The {@code dist} profile runs this class by name after both, so neither the unit tests
 * nor the acceptance suite pick it up.
 */
class PackageVersionCheck {

    private static final String NAME = "porelo-agent-claude";

    private static final Path TARGET = Path.of(System.getProperty("porelo.target", "target"));

    @Test
    void theDebCarriesTheProjectsVersion() throws IOException, InterruptedException {

        final Path deb = newest(NAME + "_", ".deb");

        assertThat(run("dpkg-deb", "-f", deb.toString(), "Package").strip()).isEqualTo(NAME);
        assertThat(run("dpkg-deb", "-f", deb.toString(), "Version").strip())
                .as("the Version of %s", deb)
                .isEqualTo(expected());
    }

    @Test
    void theRpmCarriesTheProjectsVersion() throws IOException, InterruptedException {

        final Path rpm = newest(NAME + "-", ".rpm");

        assertThat(
                        run(
                                "rpm",
                                "-qp",
                                "--queryformat",
                                "%{NAME} %{VERSION} %{RELEASE}",
                                rpm.toString()))
                .as("the name, version and release of %s", rpm)
                .isEqualTo(NAME + " " + expected() + " 1");
    }

    @Test
    void aSnapshotSortsBelowItsReleaseAndAboveTheRunBefore()
            throws IOException, InterruptedException {

        // A snapshot that sorted above its release would keep apt from ever installing the
        // release, and one that sorted below the run before would never be installed.
        assumeTrue(onPath("dpkg"), "dpkg is not installed here, so nothing can sort the versions");
        final String before = packageVersion("0.4.0", "5");
        final String run6 = packageVersion("0.4.1-SNAPSHOT", "6");
        final String run7 = packageVersion("0.4.1-SNAPSHOT", "7");
        final String release = packageVersion("0.4.1", "7");

        assertThat(dpkgSorts(run7, "lt", release))
                .as("dpkg sorts %s below %s", run7, release)
                .isTrue();
        assertThat(dpkgSorts(run6, "lt", run7)).as("dpkg sorts %s below %s", run6, run7).isTrue();
        assertThat(dpkgSorts(before, "lt", run6))
                .as("dpkg sorts %s below %s", before, run6)
                .isTrue();
    }

    /** Whether {@code dpkg --compare-versions} holds the relation between two versions. */
    private static boolean dpkgSorts(final String left, final String relation, final String right)
            throws IOException, InterruptedException {
        final Process process =
                new ProcessBuilder("dpkg", "--compare-versions", left, relation, right)
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("dpkg --compare-versions did not finish within 30 seconds");
        }
        final int exit = process.exitValue();
        assertThat(exit)
                .as("dpkg --compare-versions %s %s %s: 0 or 1", left, relation, right)
                .isIn(0, 1);
        return exit == 0;
    }

    /** Whether a command is found on the {@code PATH}. */
    private static boolean onPath(final String command) {
        return Stream.of(System.getenv().getOrDefault("PATH", "").split(":"))
                .filter(dir -> !dir.isEmpty())
                .anyMatch(dir -> Files.isExecutable(Path.of(dir, command)));
    }

    /** The version the packages must carry, from what Maven built and the run it was given. */
    private static String expected() {
        return packageVersion(
                System.getProperty("project.version", ""),
                System.getProperty("agent.snapshot.run", ""));
    }

    /**
     * Maps a project version to the version its packages carry.
     *
     * @param project The project's version, as Maven has it.
     * @param run The build's run number.
     * @return The package version.
     */
    static String packageVersion(final String project, final String run) {
        assertThat(project).as("project.version, passed by the dist profile").isNotBlank();
        return project.endsWith("-SNAPSHOT")
                ? project.replaceFirst("-SNAPSHOT$", "~snapshot." + run)
                : project;
    }

    /**
     * The newest file in the target directory whose name starts and ends so - a build may leave
     * older ones.
     */
    private static Path newest(final String prefix, final String suffix) throws IOException {
        try (Stream<Path> files = Files.list(TARGET)) {
            return files.filter(
                            f ->
                                    f.getFileName().toString().startsWith(prefix)
                                            && f.getFileName().toString().endsWith(suffix))
                    .max(
                            (a, b) ->
                                    Long.compare(
                                            a.toFile().lastModified(), b.toFile().lastModified()))
                    .orElseThrow(
                            () ->
                                    new AssertionError(
                                            "no "
                                                    + prefix
                                                    + "*"
                                                    + suffix
                                                    + " in "
                                                    + TARGET
                                                    + " - built by -Pnative,dist"));
        }
    }

    /**
     * Runs a command and returns what it printed; what it says on stderr only shows when it fails.
     */
    private static String run(final String... command) throws IOException, InterruptedException {
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("LC_ALL", "C");
        // To files rather than pipes: reading a pipe to its end would wait out a hang, not the
        // timeout.
        // rpm warns on stderr about a database a query of a package file does not need.
        final Path out = Files.createTempFile("package-version", ".out");
        final Path err = Files.createTempFile("package-version", ".err");
        try {
            final Process process =
                    builder.redirectOutput(out.toFile()).redirectError(err.toFile()).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError(List.of(command) + " did not finish within 30 seconds");
            }
            final String output = Files.readString(out, StandardCharsets.UTF_8);
            assertThat(process.exitValue())
                    .as(
                            "%s: %s%s",
                            List.of(command), output, Files.readString(err, StandardCharsets.UTF_8))
                    .isZero();
            return output;
        } finally {
            Files.deleteIfExists(out);
            Files.deleteIfExists(err);
        }
    }
}
