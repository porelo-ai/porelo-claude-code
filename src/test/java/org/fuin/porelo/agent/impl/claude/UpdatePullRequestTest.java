package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the update job's "Open the pull request" and "Merge, or say why not" steps against a real
 * git repository and a stand-in for {@code gh}.
 *
 * <p>Every gate before them reports by writing STOP; a step ended by its limit, the job's limit or
 * a cancel writes none. Read on STOP alone, such a run asked for a merge of a pin nobody verified.
 */
class UpdatePullRequestTest {

    private static final String TARGET = "2.1.300";

    @Test
    void aJobThatDidNotSucceedIsNeitherCalledGreenNorMerged(@TempDir final Path dir)
            throws Exception {

        for (final String status : List.of("failure", "cancelled")) {
            final Path work = Files.createTempDirectory(dir, status);
            final WorkflowSteps.Run opened = open(work, status);
            assertThat(opened.exit())
                    .as("exit of opening the request:%n%s", opened.output())
                    .isZero();
            assertThat(Files.readString(work.resolve("body-1")))
                    .as("the request's text after a job that ended as %s", status)
                    .doesNotContain("Green")
                    .contains("not ready to publish");

            final WorkflowSteps.Run merged = merge(work, status);
            assertThat(merged.exit()).as("exit of the merge step:%n%s", merged.output()).isZero();
            assertThat(Files.readAllLines(work.resolve("gh-calls")))
                    .as("gh calls after a job that ended as %s", status)
                    .noneMatch(call -> call.startsWith("pr merge"));
        }
    }

    @Test
    void aJobThatSucceededIsCalledGreenAndMerged(@TempDir final Path dir) throws Exception {

        // The other half: the same steps merge a job that succeeded, so the refusals above are
        // the job's status and nothing else's.
        final WorkflowSteps.Run opened = open(dir, "success");
        assertThat(opened.exit()).as("exit of opening the request:%n%s", opened.output()).isZero();
        assertThat(Files.readString(dir.resolve("body-1")))
                .as("the request's text after a job that succeeded")
                .contains("**Green.**");

        final WorkflowSteps.Run merged = merge(dir, "success");
        assertThat(merged.exit()).as("exit of the merge step:%n%s", merged.output()).isZero();
        assertThat(Files.readAllLines(dir.resolve("gh-calls")))
                .as("gh calls after a job that succeeded")
                .contains("pr merge --merge --delete-branch update/claude-code-" + TARGET);
    }

    @Test
    void onlyThePushHoldsTheTokenAndNoArgumentListSaysIt(@TempDir final Path dir) throws Exception {

        // The checkout keeps no token, so the push must get it; any other git call, and any
        // command line, would carry it where the job's later steps or a process list can read it.
        final Path bin = dir.resolve("bin");
        WorkflowSteps.stub(
                bin,
                "git",
                """
                echo "$*" >> "$CALLS/git-calls"
                header="$("$REAL_GIT" config --get-all http.https://github.com/.extraheader || true)"
                if [ -n "$header" ]; then echo "$1 $header" >> "$CALLS/git-headers"; fi
                exec "$REAL_GIT" "$@"
                """);
        final Path repo = repository(dir);
        final Map<String, String> env = env(dir, "success");
        env.put("TARGET", TARGET);
        env.put("WAS", "2.1.267");
        env.put("REAL_GIT", onPath("git").toString());

        final WorkflowSteps.Run opened =
                WorkflowSteps.run(step("Open the pull request"), repo, env);

        assertThat(opened.exit()).as("exit of opening the request:%n%s", opened.output()).isZero();
        final String token = env.get("GH_TOKEN");
        final String basic =
                Base64.getEncoder()
                        .encodeToString(
                                ("x-access-token:" + token).getBytes(StandardCharsets.UTF_8));
        assertThat(linesOf(dir.resolve("git-headers")))
                .as("the git calls that held the token")
                .containsExactly("push AUTHORIZATION: basic " + basic);
        assertThat(
                        Files.readString(dir.resolve("git-calls"))
                                + Files.readString(dir.resolve("gh-calls")))
                .as("the argument lists of git and gh")
                .contains("push --force origin update/claude-code-" + TARGET)
                .doesNotContain(token)
                .doesNotContain(basic);
    }

    @Test
    void aBranchLeftByAClosedRequestIsDeletedBeforeThePush(@TempDir final Path dir)
            throws Exception {

        // Measured on 2026-10-18: such a branch sits on an older main, so moving it moves the
        // workflows it holds, which GitHub refuses the job's token, and the update opens no
        // request at all.
        leaveBranch(dir);
        final WorkflowSteps.Run opened = open(dir, "success");

        assertThat(opened.exit()).as("exit of opening the request:%n%s", opened.output()).isZero();
        assertThat(Files.readAllLines(dir.resolve("gh-calls")))
                .as("gh calls with a branch left by a closed request")
                .contains(
                        "api -X DELETE repos/porelo-ai/porelo-claude-code/git/refs/heads/update/claude-code-"
                                + TARGET)
                .anyMatch(call -> call.startsWith("pr create"));
    }

    @Test
    void anOpenRequestOnAnOlderMainIsClosedAndReplaced(@TempDir final Path dir) throws Exception {

        // The same refusal under an open request: its branch cannot be moved to this main, so a
        // comment on it would report a run whose pin nobody could see there.
        leaveBranch(dir);
        final Path repo = repository(dir);
        final Map<String, String> env = env(dir, "success");
        env.put("TARGET", TARGET);
        env.put("WAS", "2.1.267");
        env.put("OPEN_PR", "7");
        env.put("BEHIND_BY", "3");
        final WorkflowSteps.Run opened =
                WorkflowSteps.run(step("Open the pull request"), repo, env);

        assertThat(opened.exit()).as("exit of opening the request:%n%s", opened.output()).isZero();
        assertThat(Files.readAllLines(dir.resolve("gh-calls")))
                .as("gh calls with a request open on an older main")
                .anyMatch(call -> call.startsWith("pr close 7"))
                .contains(
                        "api -X DELETE repos/porelo-ai/porelo-claude-code/git/refs/heads/update/claude-code-"
                                + TARGET)
                .anyMatch(call -> call.startsWith("pr create"))
                .noneMatch(call -> call.startsWith("pr comment"));
        assertThat(Files.readString(dir.resolve("body-1")))
                .as("the fresh request names the one it replaces")
                .contains("#7");
    }

    /**
     * Opens the request in a fresh clone whose pom the pin changed, as the job does after a run
     * that ended as given.
     */
    private static WorkflowSteps.Run open(final Path dir, final String status)
            throws IOException, InterruptedException {
        final Path repo = repository(dir);
        final Map<String, String> env = env(dir, status);
        env.put("TARGET", TARGET);
        env.put("WAS", "2.1.267");
        return WorkflowSteps.run(step("Open the pull request"), repo, env);
    }

    private static WorkflowSteps.Run merge(final Path dir, final String status)
            throws IOException, InterruptedException {
        final Map<String, String> env = env(dir, status);
        env.put("AUTO", "true");
        env.put("HAVE_TOKEN", "true");
        env.put("TARGET", TARGET);
        return WorkflowSteps.run(step("Merge, or say why not"), dir.resolve("repo"), env);
    }

    /** What both steps are given: the stand-ins first on the path, no STOP, the job's status. */
    private static Map<String, String> env(final Path dir, final String status) throws IOException {
        final Path bin = dir.resolve("bin");
        WorkflowSteps.stub(
                bin,
                "gh",
                """
                echo "$*" >> "$CALLS/gh-calls"
                previous=""
                for arg in "$@"; do
                    if [ "$previous" = --body-file ]; then
                        n=$(ls "$CALLS" | grep -c '^body-' || true)
                        cp "$arg" "$CALLS/body-$((n + 1))"
                    fi
                    previous="$arg"
                done
                # The request open on the branch, if any, and how far its branch is behind main.
                if [ "$1 $2" = "pr list" ]; then echo "${OPEN_PR:-}"; fi
                if [ "$1" = api ] && [ "$2" != -X ]; then echo "${BEHIND_BY:-0}"; fi
                # No request is open yet.
                [ "$1 $2" != "pr view" ]
                """);
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", WorkflowSteps.pathWith(bin));
        env.put("CALLS", dir.toString());
        env.put("GH_TOKEN", "the-workflow-token");
        env.put("GITHUB_OUTPUT", dir.resolve("github-output").toString());
        env.put("JOB_STATUS", status);
        env.put("GITHUB_REPOSITORY", "porelo-ai/porelo-claude-code");
        env.put("GITHUB_SHA", "the-checked-out-commit");
        env.put("GITHUB_RUN_NUMBER", "42");
        return env;
    }

    /** A clone of a bare origin, with the pin's change not yet committed. */
    private static Path repository(final Path dir) throws IOException, InterruptedException {
        final Path origin = dir.resolve("origin.git");
        final Path repo = dir.resolve("repo");
        Files.createDirectories(repo.resolve("target"));
        Files.writeString(repo.resolve(".gitignore"), "target\n");
        Files.writeString(
                repo.resolve("pom.xml"), "<agent.cli.version>2.1.267</agent.cli.version>\n");
        final WorkflowSteps.Run setup =
                WorkflowSteps.run(
                        "git init -q --bare -b main \"$ORIGIN\"\n"
                                + "git init -q -b main .\n"
                                + "git remote add origin \"$ORIGIN\"\n"
                                + "git add .gitignore pom.xml\n"
                                + "git -c user.name=t -c user.email=t@example.invalid commit -q -m base\n"
                                + "git push -q origin main\n"
                                + "echo '<agent.cli.version>"
                                + TARGET
                                + "</agent.cli.version>' > pom.xml\n",
                        repo,
                        Map.of("ORIGIN", origin.toString()));
        assertThat(setup.exit()).as("setting up the repository:%n%s", setup.output()).isZero();
        return repo;
    }

    /** Leaves the update's branch in the origin, as a closed request's run left it. */
    private static void leaveBranch(final Path dir) throws IOException, InterruptedException {
        final Path origin = dir.resolve("origin.git");
        final WorkflowSteps.Run left =
                WorkflowSteps.run(
                        "git init -q --bare -b main \"$ORIGIN\"\n"
                                + "git init -q -b main old\n"
                                + "cd old\n"
                                + "echo old > pom.xml\n"
                                + "git add pom.xml\n"
                                + "git -c user.name=t -c user.email=t@example.invalid commit -q -m old\n"
                                + "git push -q \"$ORIGIN\" main:refs/heads/update/claude-code-"
                                + TARGET
                                + "\n",
                        dir,
                        Map.of("ORIGIN", origin.toString()));
        assertThat(left.exit()).as("leaving the branch:%n%s", left.output()).isZero();
    }

    /** The lines a stand-in wrote, none when it wrote nothing. */
    private static List<String> linesOf(final Path file) throws IOException {
        return Files.exists(file) ? Files.readAllLines(file) : List.of();
    }

    /** Where a command is found on the test's own {@code PATH}, before the stand-ins. */
    private static Path onPath(final String command) {
        return Stream.of(System.getenv().getOrDefault("PATH", "").split(":"))
                .filter(entry -> !entry.isEmpty())
                .map(entry -> Path.of(entry, command))
                .filter(Files::isExecutable)
                .findFirst()
                .orElseThrow(() -> new AssertionError(command + " is not on the PATH"));
    }

    private static String step(final String name) throws IOException {
        final List<String> steps = WorkflowSteps.named("update.yml", name);
        assertThat(steps).as("steps named \"%s\" in update.yml", name).hasSize(1);
        return steps.get(0);
    }
}
