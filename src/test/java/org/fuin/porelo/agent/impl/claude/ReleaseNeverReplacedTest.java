package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs both "Publish the packages" steps of the build workflow for a release, with stand-ins for
 * {@code curl} and {@code jf}. A release that is published already is never uploaded again, and
 * when the index cannot be read nobody knows whether it is, so nothing is uploaded either.
 */
class ReleaseNeverReplacedTest {

    private static final String DEB = "porelo-agent-claude_0.5.0_amd64.deb";

    @Test
    void anUnreadableIndexStopsThePublishBeforeAnyUpload(@TempDir final Path dir) throws Exception {

        for (final String step : steps()) {
            final Path work = Files.createTempDirectory(dir, "step");
            final WorkflowSteps.Run run = publish(step, work, "22", "");
            assertThat(run.exit())
                    .as("exit of the publish when the index cannot be read:%n%s", run.output())
                    .isNotZero();
            assertThat(work.resolve("jf-calls"))
                    .as("uploads when the index cannot be read")
                    .doesNotExist();
        }
    }

    @Test
    void aPublishedReleaseIsNotUploadedAgain(@TempDir final Path dir) throws Exception {

        for (final String step : steps()) {
            final Path work = Files.createTempDirectory(dir, "step");
            final WorkflowSteps.Run run =
                    publish(
                            step,
                            work,
                            "0",
                            "Package: porelo-agent-claude\n"
                                    + "Filename: pool/main/p/porelo-agent-claude/"
                                    + DEB
                                    + "\n");
            assertThat(run.exit())
                    .as("exit of the publish of a published release:%n%s", run.output())
                    .isNotZero();
            assertThat(work.resolve("jf-calls"))
                    .as("uploads of a published release")
                    .doesNotExist();
        }
    }

    @Test
    void aNewReleaseIsUploaded(@TempDir final Path dir) throws Exception {

        // The other half: the same steps upload a release the index does not list, so the stops
        // above are the index's and nothing else's.
        for (final String step : steps()) {
            final Path work = Files.createTempDirectory(dir, "step");
            final WorkflowSteps.Run run =
                    publish(
                            step,
                            work,
                            "0",
                            "Package: porelo-agent-claude\n"
                                    + "Filename: pool/main/p/porelo-agent-claude/"
                                    + "porelo-agent-claude_0.4.1_amd64.deb\n");
            assertThat(run.exit())
                    .as("exit of the publish of a new release:%n%s", run.output())
                    .isZero();
            assertThat(Files.readString(work.resolve("jf-calls")))
                    .as("uploads of a new release")
                    .contains(
                            "rt upload target/*.deb porelo-dist-deb/pool/main/p/porelo-agent-claude/");
        }
    }

    private static List<String> steps() throws IOException {
        final List<String> steps = WorkflowSteps.named("build.yml", "Publish the packages");
        assertThat(steps)
                .as(
                        "\"Publish the packages\" steps in build.yml, the build job's and the release job's")
                .hasSize(2);
        return steps;
    }

    /**
     * Runs a publish step for the releases channel in a directory holding the packages, with a
     * {@code curl} that exits so and answers with that index.
     */
    private static WorkflowSteps.Run publish(
            final String step, final Path dir, final String curlExit, final String index)
            throws IOException, InterruptedException {
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target").resolve(DEB), "");
        Files.writeString(dir.resolve("target/porelo-agent-claude-0.5.0-1.x86_64.rpm"), "");
        Files.writeString(dir.resolve("target/porelo-agent-claude.cdx.json"), "{}");
        Files.writeString(dir.resolve("index"), index);
        final Path bin = dir.resolve("bin");
        WorkflowSteps.stub(
                bin,
                "curl",
                """
                out=""
                while [ $# -gt 0 ]; do
                    if [ "$1" = -o ]; then out="$2"; shift; fi
                    shift
                done
                [ "$CURL_EXIT" = 0 ] || exit "$CURL_EXIT"
                if [ -n "$out" ]; then cat "$HOME/index" > "$out"; else cat "$HOME/index"; fi
                """);
        WorkflowSteps.stub(bin, "jf", "echo \"$*\" >> \"$HOME/jf-calls\"\n");
        return WorkflowSteps.run(
                step,
                dir,
                Map.of(
                        "PATH",
                        WorkflowSteps.pathWith(bin),
                        "CHANNEL",
                        "releases",
                        "JF_URL",
                        "https://artifactory.invalid",
                        "CURL_EXIT",
                        curlExit));
    }
}
