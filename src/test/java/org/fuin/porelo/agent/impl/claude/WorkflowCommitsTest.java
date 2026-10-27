package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * The commit messages of every push, every pull request and main's last day are held to the shared
 * rule, so no way onto main passes it around.
 */
class WorkflowCommitsTest {

    private static final String PUSH_STEP = "The pushed commits' messages";
    private static final String PULL_REQUEST_STEP = "The pull request's commit messages";
    private static final String DAY_STEP = "Main's commits of the last day";
    private static final String CLASSPATH = "tooling.jar";
    private static final String NAME = "porelo-claude-code";
    private static final String REPOSITORY = "porelo-ai/" + NAME;
    private static final String SHARED_RESOLVE_STEP = "Resolve the release tooling";
    private static final String CHECKOUT = "checkout";
    private static final String[] INSIDE = {
        "-cp", "tooling.jar", "org.fuin.porelo.release.Main", "check-commits", "6666666..7777777"
    };
    private static final String[] OUTSIDE = {
        "-cp",
        "tooling.jar",
        "org.fuin.porelo.release.Main",
        "check-commits",
        "6666666..7777777",
        "--outside"
    };
    private static final String RESOLVE_STEP = "Resolve the release and machine tooling";
    private static final String GITHUB = "https://github.com";
    private static final String FORGE = "https://10.73.0.1:3000";

    @TempDir Path dir;

    // Prevents the check from being skipped on a push or fed a range a workflow expression built.
    @Test
    void theBuildChecksThePushedRangeFromItsOwnEnvironment() throws IOException {
        final Map<String, Object> step = step(job("build.yml", "build"), PUSH_STEP);
        assertThat(step.get("if"))
                .as("the step runs on a push")
                .isEqualTo("github.event_name == 'push'");
        assertThat(env(step).get("PUSH_BEFORE"))
                .as("the range starts where the push began")
                .isEqualTo("${{ github.event.before }}");
        assertThat((String) step.get("run"))
                .as("the range reaches the script through its environment alone")
                .doesNotContain("${{");
    }

    // Prevents a range the checkout cannot read, and a check before the tooling is resolved.
    @Test
    void theBuildJobChecksOutTheWholeHistoryAndChecksAfterTheTooling() throws IOException {
        final Map<String, Object> job = job("build.yml", "build");
        assertThat(checkoutDepth(job))
                .as("the build job's checkout holds every commit of a pushed range")
                .isEqualTo(0);
        final List<String> names = names(job);
        assertThat(names.indexOf(PUSH_STEP))
                .as("the check runs after the release tooling is resolved")
                .isGreaterThan(names.indexOf(RESOLVE_STEP));
        assertThat(names.indexOf(RESOLVE_STEP))
                .as("the build job resolves the release tooling after its checkout")
                .isGreaterThan(names.indexOf(CHECKOUT));
        assertThat(names.indexOf(CHECKOUT)).as("the build job checks out").isNotNegative();
    }

    // Prevents a commit check that finds no tooling and so checks nothing.
    @Test
    void theSharedRulesJobChecksCommitsAfterItsCheckoutAndTheTooling() throws IOException {
        final List<String> names = names(job("shared-rules.yml", "check-shared"));
        assertThat(names.indexOf(CHECKOUT)).as("the job checks out first").isZero();
        assertThat(names.indexOf(SHARED_RESOLVE_STEP))
                .as("the job resolves the release tooling")
                .isPositive();
        assertThat(names.indexOf(PULL_REQUEST_STEP))
                .as("the pull request's check runs after the tooling is resolved")
                .isGreaterThan(names.indexOf(SHARED_RESOLVE_STEP));
        assertThat(names.indexOf(DAY_STEP))
                .as("the day's check runs after the tooling is resolved")
                .isGreaterThan(names.indexOf(SHARED_RESOLVE_STEP));
    }

    // Prevents a pushed range from going unchecked, and a refusal from passing.
    @Test
    void aPushedRangeReachesCheckCommitsAndItsRefusalFailsTheStep() throws Exception {
        final Result passed = push("refs/heads/main", "1111111", "1111111", 0);
        assertThat(passed.exit()).as("a clean range: %s", passed.output()).isZero();
        assertThat(passed.java())
                .as("check-commits reads exactly the pushed range, with the release tooling")
                .containsExactly(
                        "-cp",
                        CLASSPATH,
                        "org.fuin.porelo.release.Main",
                        "check-commits",
                        "1111111..2222222");

        final Result refused = push("refs/heads/main", "1111111", "1111111", 1);
        assertThat(refused.exit())
                .as("a refused range fails the step: %s", refused.output())
                .isNotZero();
    }

    // Prevents a new branch, whose start is all zeros, from checking nothing.
    @Test
    void aNewBranchIsCheckedFromMain() throws Exception {
        final Result run = push("refs/heads/topic", "0".repeat(40), "", 0);
        assertThat(run.exit()).as("a new branch: %s", run.output()).isZero();
        assertThat(run.java()).as("its range starts at main").endsWith("origin/main..2222222");
    }

    // Prevents a branch pushed over, whose old tip the checkout lacks, from failing a clean push.
    @Test
    void aBranchPushedOverIsCheckedFromMain() throws Exception {
        final Result run = push("refs/heads/topic", "3333333", "1111111", 0);
        assertThat(run.exit()).as("a branch pushed over: %s", run.output()).isZero();
        assertThat(run.java()).as("its range starts at main").endsWith("origin/main..2222222");
    }

    // Prevents a tag, whose range from main is empty, from checking nothing.
    @Test
    void aTagChecksTheCommitItNames() throws Exception {
        final Result run = push("refs/tags/v1.2.3", "0".repeat(40), "", 0);
        assertThat(run.exit()).as("a tag: %s", run.output()).isZero();
        assertThat(run.java())
                .as("its range is the commit it names")
                .endsWith("2222222~1..2222222");
    }

    // Prevents main moved from a commit nobody holds from passing as an empty range.
    @Test
    void mainFromACommitTheCheckoutLacksFailsLoudly() throws Exception {
        final Result run = push("refs/heads/main", "3333333", "1111111", 0);
        assertThat(run.exit()).as("main from an unknown commit: %s", run.output()).isNotZero();
        assertThat(run.output())
                .as("the refusal is a visible workflow error")
                .contains("::error::");
        assertThat(run.java()).as("check-commits is not started for it").isEmpty();
    }

    // Prevents a missing start of the range from silently checking nothing.
    @Test
    void aMissingStartFailsLoudly() throws Exception {
        // On a branch, where nothing after this refusal would stop the step.
        final Result run = push("refs/heads/topic", "", "1111111", 0);
        assertThat(run.exit()).as("no start of the range: %s", run.output()).isNotZero();
        assertThat(run.output())
                .as("the refusal is a visible workflow error")
                .contains("::error::");
        assertThat(run.java()).as("check-commits is not started without a range").isEmpty();
    }

    // Prevents a push the Build never starts from going unchecked: the schedule reads main's last
    // day.
    @Test
    void theScheduleChecksMainsLastDay() throws Exception {
        final Map<String, Object> job = job("shared-rules.yml", "check-shared");
        final Map<String, Object> step = step(job, DAY_STEP);
        assertThat(step.get("if"))
                .as("the step runs on the schedule")
                .isEqualTo("github.event_name == 'schedule'");
        assertThat((String) step.get("run"))
                .as("the range comes from git, not a workflow expression")
                .doesNotContain("${{");
        assertThat(checkoutDepth(job))
                .as("the scheduled checkout holds main's last day")
                .isEqualTo(0);

        final Result run = execute(DAY_STEP, Map.of("DAY_START", "4444444"), 0);
        assertThat(run.exit()).as("a clean day: %s", run.output()).isZero();
        assertThat(run.java())
                .as("check-commits reads main's commits since the last day began")
                .containsExactly(
                        "-cp",
                        CLASSPATH,
                        "org.fuin.porelo.release.Main",
                        "check-commits",
                        "4444444..HEAD");
        assertThat(execute(DAY_STEP, Map.of("DAY_START", "4444444"), 1).exit())
                .as("a refused day fails the step")
                .isNotZero();
    }

    // Prevents a repository younger than a day from checking nothing, or leaving out its first
    // commit.
    @Test
    void aRepositoryYoungerThanADayIsReadWhole() throws Exception {
        final Result run = execute(DAY_STEP, Map.of("DAY_START", ""), 0);
        assertThat(run.exit()).as("a young repository: %s", run.output()).isZero();
        assertThat(run.java()).as("it is read whole").endsWith("check-commits", "HEAD");
    }

    // Prevents a second fetch, which a private forge copy refuses to a checkout without
    // credentials.
    @Test
    void aMembersPullRequestIsCheckedFromTheCheckoutAlone() throws Exception {
        final Result run = pullRequest(REPOSITORY, "MEMBER", "someone");
        assertThat(run.exit())
                .as("a member's pull request, nothing fetched: %s", run.output())
                .isZero();
        assertThat(run.java())
                .as("its own commits are read, as a member's")
                .containsExactly(
                        "-cp",
                        CLASSPATH,
                        "org.fuin.porelo.release.Main",
                        "check-commits",
                        "6666666..7777777");
    }

    // Prevents the pull request's facts from reaching the script any way but its environment, where
    // a workflow expression in the script would run what a pull request's title or branch carries.
    @Test
    void thePullRequestsFactsReachTheScriptThroughItsEnvironment() throws IOException {
        final Map<String, Object> step =
                step(job("shared-rules.yml", "check-shared"), PULL_REQUEST_STEP);
        assertThat(step.get("if"))
                .as("the step runs on a pull request")
                .isEqualTo("github.event_name == 'pull_request'");
        assertThat(env(step))
                .as("the pull request's facts")
                .containsEntry("PR_BASE", "${{ github.event.pull_request.base.sha }}")
                .containsEntry("PR_HEAD", "${{ github.event.pull_request.head.sha }}")
                .containsEntry("PR_AUTHOR", "${{ github.event.pull_request.user.login }}")
                .containsEntry(
                        "PR_REPOSITORY", "${{ github.event.pull_request.head.repo.full_name }}")
                .containsEntry(
                        "PR_ASSOCIATION", "${{ github.event.pull_request.author_association }}")
                .containsEntry("PR_SERVER", "${{ github.server_url }}");
        assertThat((String) step.get("run"))
                .as("the script reads its environment alone")
                .doesNotContain("${{");
    }

    // Prevents the repository's own update workflow's pull request from being refused for want of a
    // sign-off, though GitHub names its author a contributor.
    @Test
    void theOwnWorkflowsPullRequestIsFromInside() throws Exception {
        final Result run = pullRequest(REPOSITORY, "CONTRIBUTOR", "github-actions[bot]");
        assertThat(run.exit()).as("the own workflow's pull request: %s", run.output()).isZero();
        assertThat(run.java()).as("its commits need no sign-off").containsExactly(INSIDE);
    }

    // Prevents an owner's pull request from being refused for want of a sign-off.
    @Test
    void anOwnersPullRequestIsFromInside() throws Exception {
        final Result run = pullRequest(REPOSITORY, "OWNER", "someone");
        assertThat(run.exit()).as("an owner's pull request: %s", run.output()).isZero();
        assertThat(run.java()).as("its commits need no sign-off").containsExactly(INSIDE);
    }

    // Prevents a pull request whose commits are refused from passing its check.
    @Test
    void aRefusedPullRequestFailsTheStep() throws Exception {
        final Result run = pullRequest(REPOSITORY, "MEMBER", "someone", 1);
        assertThat(run.exit()).as("a refused pull request: %s", run.output()).isNotZero();
    }

    // Prevents code from outside from passing without its author's sign-off.
    @Test
    void aPullRequestFromElsewhereStaysFromOutside() throws Exception {
        assertOutside(
                pullRequest("someone/" + NAME, "CONTRIBUTOR", "github-actions[bot]"),
                "the bot's pull request from another repository");
        assertOutside(
                pullRequest(REPOSITORY, "CONTRIBUTOR", "someone"),
                "a contributor's pull request from this repository");
        assertOutside(
                pullRequest("someone/" + NAME, "MEMBER", "someone"),
                "a member's pull request from another repository");
    }

    // Prevents the host's forge, which names no author association, from refusing every pull
    // request of the repository's own branches for want of a sign-off.
    @Test
    void aForgePullRequestFromTheSameRepositoryIsFromInside() throws Exception {
        final Result run = pullRequest(REPOSITORY, "", "agent-machines", FORGE);
        assertThat(run.exit()).as("the forge's own pull request: %s", run.output()).isZero();
        assertThat(run.java()).as("its commits need no sign-off").containsExactly(INSIDE);
    }

    // Prevents code from another repository on the forge from passing without its author's
    // sign-off.
    @Test
    void aForgePullRequestFromElsewhereStaysFromOutside() throws Exception {
        assertOutside(
                pullRequest("someone/" + NAME, "", "someone", FORGE),
                "a forge pull request from another repository");
    }

    private static void assertOutside(final Result run, final String which) {
        assertThat(run.exit()).as("%s: %s", which, run.output()).isZero();
        assertThat(run.java()).as("%s needs its author's sign-off", which).containsExactly(OUTSIDE);
    }

    /**
     * What a step did with git and java stubbed.
     *
     * @param exit Its exit code.
     * @param output What it printed.
     * @param java The arguments java was called with, one per line.
     */
    private record Result(int exit, String output, List<String> java) {}

    private Result pullRequest(
            final String repository, final String association, final String author)
            throws Exception {
        return pullRequest(repository, association, author, 0);
    }

    private Result pullRequest(
            final String repository,
            final String association,
            final String author,
            final int checkExit)
            throws Exception {
        return pullRequest(repository, association, author, GITHUB, checkExit);
    }

    private Result pullRequest(
            final String repository,
            final String association,
            final String author,
            final String server)
            throws Exception {
        return pullRequest(repository, association, author, server, 0);
    }

    private Result pullRequest(
            final String repository,
            final String association,
            final String author,
            final String server,
            final int checkExit)
            throws Exception {
        return execute(
                PULL_REQUEST_STEP,
                Map.of(
                        "PR_BASE", "6666666",
                        "PR_HEAD", "7777777",
                        "PR_REPOSITORY", repository,
                        "GITHUB_REPOSITORY", REPOSITORY,
                        "PR_ASSOCIATION", association,
                        "PR_AUTHOR", author,
                        "PR_SERVER", server),
                checkExit);
    }

    /**
     * Runs the Build's step for a push of {@code ref} from {@code before}; git holds {@code known}.
     */
    private Result push(
            final String ref, final String before, final String known, final int checkExit)
            throws Exception {
        return execute(
                PUSH_STEP,
                Map.of(
                        "PUSH_BEFORE",
                        before,
                        "GITHUB_SHA",
                        "2222222",
                        "GITHUB_REF",
                        ref,
                        "GITHUB_REF_TYPE",
                        ref.startsWith("refs/tags/") ? "tag" : "branch",
                        "KNOWN",
                        known.isEmpty() ? "none" : known),
                checkExit);
    }

    /** Runs a step's own script with git and java stubbed, in the given environment. */
    private Result execute(
            final String name, final Map<String, String> environment, final int checkExit)
            throws Exception {
        final Map<String, Object> job =
                name.equals(PUSH_STEP)
                        ? job("build.yml", "build")
                        : job("shared-rules.yml", "check-shared");
        final Path bin = Files.createDirectories(dir.resolve("bin"));
        final Path javaArguments = dir.resolve("java-arguments");
        Files.writeString(javaArguments, "");
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), CLASSPATH);
        stub(
                bin.resolve("java"),
                "printf '%s\\n' \"$@\" >> \"$JAVA_ARGUMENTS\"\nexit " + checkExit + "\n");
        // Answers only the calls the steps make, as git would; any other call fails.
        stub(
                bin.resolve("git"),
                "case \"$*\" in\n"
                        + "'cat-file -e '*) [ \"$#\" -eq 3 ] && [ \"$3\" = \"$KNOWN^{commit}\" ] ;;\n"
                        + "'rev-list -1 --before=26 hours ago HEAD')"
                        + " [ -z \"$DAY_START\" ] || printf '%s\\n' \"$DAY_START\" ;;\n"
                        + "*) echo \"unexpected git $*\" >&2; exit 99 ;;\n"
                        + "esac\n");
        final Path output = dir.resolve("output");
        // bash -e, as the runner runs a step without a shell of its own.
        final ProcessBuilder command =
                new ProcessBuilder(
                                "bash",
                                "--noprofile",
                                "--norc",
                                "-e",
                                "-o",
                                "pipefail",
                                "-c",
                                (String) step(job, name).get("run"))
                        .directory(dir.toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(output.toFile());
        command.environment().clear();
        command.environment().put("PATH", bin + ":/usr/bin:/bin");
        command.environment().put("KNOWN", "none");
        command.environment().put("DAY_START", "");
        command.environment().putAll(environment);
        command.environment().put("JAVA_ARGUMENTS", javaArguments.toString());
        final Process process = command.start();
        try {
            assertThat(process.waitFor(10, TimeUnit.SECONDS))
                    .as("the step ends in %s", environment)
                    .isTrue();
            return new Result(
                    process.exitValue(),
                    Files.readString(output),
                    Files.readAllLines(javaArguments));
        } finally {
            process.destroyForcibly();
        }
    }

    private static void stub(final Path file, final String body) throws IOException {
        Files.writeString(file, "#!/bin/sh\n" + body);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> job(final String workflow, final String name)
            throws IOException {
        try (Reader reader =
                Files.newBufferedReader(
                        RepositoryDocuments.root()
                                .resolve(".github/workflows")
                                .resolve(workflow))) {
            final Map<String, Object> jobs =
                    (Map<String, Object>)
                            new Yaml(noDuplicateKeys())
                                    .<Map<String, Object>>load(reader)
                                    .get("jobs");
            assertThat(jobs).as("%s's jobs", workflow).containsKey(name);
            return (Map<String, Object>) jobs.get(name);
        }
    }

    /** A key written twice would silently hide one of its values from every assertion. */
    private static LoaderOptions noDuplicateKeys() {
        final LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        return options;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> steps(final Map<String, Object> job) {
        return (List<Map<String, Object>>) job.get("steps");
    }

    private static Map<String, Object> step(final Map<String, Object> job, final String name) {
        final List<Map<String, Object>> found =
                steps(job).stream().filter(step -> name.equals(step.get("name"))).toList();
        assertThat(found).as("steps named \"%s\"", name).hasSize(1);
        return found.get(0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> env(final Map<String, Object> step) {
        return (Map<String, Object>) step.getOrDefault("env", Map.of());
    }

    private static List<String> names(final Map<String, Object> job) {
        final List<String> names = new ArrayList<>();
        for (final Map<String, Object> step : steps(job)) {
            names.add(
                    String.valueOf(step.get("uses")).startsWith("actions/checkout@")
                            ? CHECKOUT
                            : String.valueOf(step.get("name")));
        }
        return names;
    }

    /** The {@code fetch-depth} of the job's one checkout, as YAML reads it. */
    @SuppressWarnings("unchecked")
    private static Object checkoutDepth(final Map<String, Object> job) {
        final List<Map<String, Object>> checkouts =
                steps(job).stream()
                        .filter(
                                step ->
                                        String.valueOf(step.get("uses"))
                                                .startsWith("actions/checkout@"))
                        .toList();
        assertThat(checkouts).as("checkouts in the job").hasSize(1);
        return ((Map<String, Object>) checkouts.get(0).getOrDefault("with", Map.of()))
                .get("fetch-depth");
    }
}
