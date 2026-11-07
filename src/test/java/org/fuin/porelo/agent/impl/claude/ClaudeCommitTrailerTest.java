package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ClaudeCommitTrailer}: the hook as git runs it, on a real commit.
 *
 * <p>The harm guarded: a task's commit that leaves with Claude's own {@code Co-Authored-By:} or
 * without the {@code Assisted-by:} trailer the shared rules ask for, or with a trailer that names a
 * model the session's transcript did not.
 */
class ClaudeCommitTrailerTest {

    private static final String SESSION = "0b6a3c3e-1f0e-4d55-9a57-2a8f0c3b7d11";

    @TempDir Path home;

    private Path repository;
    private Path projects;

    @BeforeEach
    void prepare() throws Exception {
        final Path hooks = Files.createDirectories(home.resolve("hooks"));
        final Path hook = hooks.resolve("commit-msg");
        Files.writeString(hook, ClaudeCommitTrailer.script());
        assertThat(hook.toFile().setExecutable(true))
                .as("the hook can be made executable")
                .isTrue();
        projects = Files.createDirectories(home.resolve(".claude/projects/-workspace"));
        repository = Files.createDirectories(home.resolve("workspace"));
        git(List.of("init", "-q", "-b", "main"), null);
        git(List.of("config", "core.hooksPath", hooks.toString()), null);
        git(List.of("config", "user.name", "Agent Example"), null);
        git(List.of("config", "user.email", "agent@localhost"), null);
        Files.writeString(repository.resolve("a.txt"), "a\n");
        git(List.of("add", "a.txt"), null);
    }

    @Test
    void replacesClaudesOwnTrailerWithTheModelTheSessionLastRanAs() throws Exception {

        transcript(
                SESSION,
                Instant.parse("2026-11-07T10:00:00Z"),
                "claude-sonnet-5-5",
                "claude-opus-5-5",
                "<synthetic>");

        final Result result =
                commit(
                        "Change a\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\n",
                        SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("Claude's own trailer goes, and the last real model of the session is named")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void replacesATrailerTheAgentWroteWithTheModelTheTranscriptNames() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "claude-opus-5-5");

        final Result result =
                commit("Change a\n\nAssisted-by: Claude Code, wrong model!\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("a trailer guessed by hand or kept by an amend gives way to the session's own")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void keepsTheAgentsTrailerWhenNoTranscriptNamesAModel() throws Exception {

        final Result result =
                commit("Change a\n\nAssisted-by: Claude Code, claude-sonnet-5-5\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("the way the refusal names, writing the trailer by hand, stays open")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-sonnet-5-5\n");
    }

    @Test
    void addsTheTrailerWhenTheBodyOnlyTalksAboutOne() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "claude-opus-5-5");

        final Result result =
                commit(
                        "Explain the trailer check\n\nAssisted-by: lines must stand at the end.\n\n"
                                + "More words.\n",
                        SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("a line in the body is no trailer, so it stays and the message ends with one")
                .isEqualTo(
                        "Explain the trailer check\n\nAssisted-by: lines must stand at the end.\n\n"
                                + "More words.\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void leavesAMessageOfCommentsAloneSoGitAbortsTheCommit() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "claude-opus-5-5");
        final Path written = home.resolve("only-comments.txt");
        Files.writeString(
                written,
                "# Please enter the commit message for your changes.\n"
                        + "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\n");

        final Result result =
                git(List.of("commit", "-q"), SESSION, Map.of("GIT_EDITOR", "cp " + written + " "));

        assertThat(result.exit())
                .as(
                        "an empty message aborts the commit, and the trailer does not fill it: %s",
                        result.output())
                .isNotZero();
        assertThat(result.output())
                .as("git aborts it, not the hook")
                .doesNotContain("commit-msg:")
                .contains("empty");
        assertThat(commits()).as("nothing was committed").isZero();
    }

    @Test
    void takesTheModelThatAnsweredNotOneAToolWasGiven() throws Exception {

        final Path file = projects.resolve(SESSION + ".jsonl");
        Files.writeString(
                file,
                "{\"type\":\"assistant\",\"message\":{\"model\":\"claude-opus-5-5\",\"id\":\"m1\","
                        + "\"content\":[{\"type\":\"tool_use\",\"name\":\"Agent\",\"input\":{\"model\":\"haiku\"}}]}}\n");

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("a model named in a tool call's input did not serve the session")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void findsTheModelBehindTheContentAsThePinnedCliWritesIt() throws Exception {

        // Measured on 2.1.311 over OpenRouter: the assistant record names its model after the
        // content, so a tool's input with a model of its own comes first on the line.
        final Path file = projects.resolve(SESSION + ".jsonl");
        Files.writeString(
                file,
                "{\"type\":\"assistant\",\"message\":{\"id\":\"m1\",\"role\":\"assistant\",\"content\":"
                        + "[{\"type\":\"tool_use\",\"name\":\"Agent\",\"input\":{\"model\":\"haiku\"}}],"
                        + "\"model\":\"z-ai/glm-5.3-flash\",\"stop_reason\":\"tool_use\"}}\n");

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, z-ai/glm-5.3-flash\n");
    }

    @Test
    void readsATranscriptThatHoldsABinaryByte() throws Exception {

        final Path file = projects.resolve(SESSION + ".jsonl");
        Files.writeString(
                file,
                "{\"type\":\"user\",\"toolUseResult\":\"a\u0000b\"}\n"
                        + "{\"type\":\"assistant\",\"message\":{\"model\":\"claude-opus-5-5\",\"id\":\"m1\"}}\n");

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void passesOverANewerTranscriptThatNamesNoModel() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "claude-opus-5-5");
        transcript("5d1e2f3a-0000-4000-8000-000000000000", Instant.parse("2026-11-07T11:00:00Z"));

        final Result result = commit("Change a\n", null);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void makesTheHookExecutableAndOneGitRuns() throws Exception {

        // The setup writes files without a mode; this is what makes the hook count.
        final Path hooks = Files.createDirectories(home.resolve("installed"));
        final Path hook = Files.writeString(hooks.resolve("commit-msg"), "#!/bin/sh\n");
        final Path config = home.resolve("global-config");
        final ProcessBuilder builder =
                new ProcessBuilder("sh", "-c", ClaudeCommitTrailer.installCommand(hooks.toString()))
                        .redirectErrorStream(true);
        builder.environment().put("GIT_CONFIG_GLOBAL", config.toString());
        final Process process = builder.start();
        final String output =
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("the command ended").isTrue();

        assertThat(process.exitValue()).as("the command succeeds: %s", output).isZero();
        assertThat(Files.isExecutable(hook)).as("git runs only an executable hook").isTrue();
        final ProcessBuilder read =
                new ProcessBuilder("git", "config", "--global", "--get", "core.hooksPath")
                        .redirectErrorStream(true);
        read.environment().put("GIT_CONFIG_GLOBAL", config.toString());
        final Process asked = read.start();
        final String hooksPath =
                new String(asked.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertThat(asked.waitFor(30, TimeUnit.SECONDS)).as("git ended").isTrue();
        assertThat(hooksPath).as("git is pointed at the hook's folder").isEqualTo(hooks.toString());
    }

    @Test
    void installsTheHookTheSetupWrites() {

        assertThat(ClaudeCommitTrailer.HOOK)
                .as("the hook lies in the agent's home, where the setup writes it")
                .isEqualTo("/home/agent/.config/porelo/git-hooks/commit-msg");
        assertThat(ClaudeCommitTrailer.installCommand())
                .as("the session's start installs the folder the hook lies in")
                .isEqualTo(
                        ClaudeCommitTrailer.installCommand("/home/agent/.config/porelo/git-hooks"));
    }

    @Test
    void takesNoModelOfAnArrayInAToolsInputWhenTheModelComesFirst() throws Exception {

        Files.writeString(
                projects.resolve(SESSION + ".jsonl"),
                "{\"type\":\"assistant\",\"message\":{\"model\":\"claude-opus-5-5\",\"id\":\"m1\",\"content\":"
                        + "[{\"type\":\"tool_use\",\"input\":{\"tasks\":[{\"id\":1}],\"model\":\"haiku\","
                        + "\"message\":{\"model\":\"haiku\",\"id\":\"x\"}}}]}}\n");

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as(
                        "a tool's input, even one shaped like a message, is not the record that answered")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void takesNoModelOfAToolsInputOrResultWhenTheModelComesLast() throws Exception {

        Files.writeString(
                projects.resolve(SESSION + ".jsonl"),
                "{\"type\":\"assistant\",\"message\":{\"id\":\"m2\",\"content\":"
                        + "[{\"type\":\"tool_use\",\"input\":{\"tasks\":[{\"id\":1}],\"model\":\"haiku\"}}],"
                        + "\"model\":\"claude-opus-5-5\",\"stop_reason\":\"end_turn\"}}\n"
                        + "{\"type\":\"user\",\"toolUseResult\":{\"rows\":[{\"a\":1}],\"model\":\"haiku\"}}\n");

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("a tool's input or result is not the record of the model that answered")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void neverTakesAnotherSessionsModelWhenItsOwnNamesNone() throws Exception {

        transcript(
                "5d1e2f3a-0000-4000-8000-000000000000",
                Instant.parse("2026-11-07T09:00:00Z"),
                "claude-haiku-5-5");
        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "<synthetic>");

        final Result result =
                commit("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("another session's model did not make this commit")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void keepsTheAgentsTrailerWhenTheTranscriptsModelIsNoPlainIdentifier() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "bad model!");

        final Result result =
                commit("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n", SESSION);

        assertThat(result.exit())
                .as("the way the refusal names stays open: %s", result.output())
                .isZero();
        assertThat(lastMessage())
                .as("the agent's own trailer stands")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void refusesAHandWrittenTrailerThatNamesNoModel() throws Exception {

        for (final String trailer :
                List.of(
                        "Assisted-by: Claude Code, <model>",
                        "Assisted-by: Claude Code, ",
                        "Assisted-by: someone",
                        "Assisted-by: , claude-opus-5-5",
                        "Assisted-by: Claude Code, <model>\n"
                                + "Assisted-by: Claude Code, claude-opus-5-5")) {
            final Result result = commit("Change a\n\n" + trailer + "\n", SESSION);

            assertThat(result.exit())
                    .as("'%s' names no tool and model, and nothing in the transcript does", trailer)
                    .isNotZero();
            assertThat(result.output())
                    .as("the hook refuses it, saying what to write")
                    .contains("end the message with");
        }
        assertThat(commits()).as("nothing was committed").isZero();
    }

    @Test
    void takesTheSessionsOwnTranscriptOverANewerOne() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "claude-opus-5-5");
        transcript(
                "5d1e2f3a-0000-4000-8000-000000000000",
                Instant.parse("2026-11-07T11:00:00Z"),
                "claude-haiku-5-5");

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .as("the model is the one of the session that commits, not of another one")
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void takesTheNewestTranscriptWhenTheSessionIsNotNamed() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "claude-sonnet-5-5");
        transcript(
                "5d1e2f3a-0000-4000-8000-000000000000",
                Instant.parse("2026-11-07T11:00:00Z"),
                "claude-opus-5-5");

        final Result result = commit("Change a\n", null);

        assertThat(result.exit()).as("the commit is made: %s", result.output()).isZero();
        assertThat(lastMessage())
                .isEqualTo("Change a\n\nAssisted-by: Claude Code, claude-opus-5-5\n");
    }

    @Test
    void refusesTheCommitWhenNoTranscriptNamesAModel() throws Exception {

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit()).as("a commit without its trailer is not made").isNotZero();
        assertThat(result.output())
                .as("the refusal says what was missing and what to write instead")
                .contains("no model")
                .contains("Assisted-by: Claude Code, <model>");
        assertThat(commits()).as("nothing was committed").isZero();
    }

    @Test
    void refusesAModelNameThatIsNoPlainIdentifier() throws Exception {

        transcript(SESSION, Instant.parse("2026-11-07T10:00:00Z"), "x\\nSigned-off-by: someone");

        final Result result = commit("Change a\n", SESSION);

        assertThat(result.exit())
                .as("what the transcript says is not written into a trailer unchecked")
                .isNotZero();
        assertThat(result.output()).as("the refusal says why").contains("no plain identifier");
        assertThat(commits()).as("nothing was committed").isZero();
    }

    private void transcript(String session, Instant modified, String... models) throws IOException {
        final List<String> lines = new ArrayList<>();
        lines.add("{\"type\":\"user\",\"message\":{\"role\":\"user\",\"content\":\"hi\"}}");
        for (final String model : models) {
            lines.add(
                    "{\"parentUuid\":null,\"message\":{\"model\":\""
                            + model
                            + "\",\"id\":\"m1\",\"role\":\"assistant\"},\"type\":\"assistant\"}");
        }
        final Path file = projects.resolve(session + ".jsonl");
        Files.write(file, lines);
        Files.setLastModifiedTime(file, FileTime.from(modified));
    }

    private Result commit(String message, String session) throws Exception {
        final Path file = home.resolve("message.txt");
        Files.writeString(file, message);
        return git(List.of("commit", "-q", "--cleanup=verbatim", "-F", file.toString()), session);
    }

    private String lastMessage() throws Exception {
        return git(List.of("log", "-1", "--format=%B"), null).output().stripTrailing() + "\n";
    }

    private int commits() throws Exception {
        return Integer.parseInt(git(List.of("rev-list", "--all", "--count"), null).output().trim());
    }

    private Result git(List<String> arguments, String session) throws Exception {
        return git(arguments, session, Map.of());
    }

    private Result git(List<String> arguments, String session, Map<String, String> extra)
            throws Exception {
        final List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(arguments);
        final ProcessBuilder builder =
                new ProcessBuilder(command)
                        .directory(repository.toFile())
                        .redirectErrorStream(true);
        final Map<String, String> env = builder.environment();
        env.keySet().removeIf(k -> k.startsWith("CLAUDE") || k.startsWith("GIT_"));
        env.put("HOME", home.toString());
        env.put("GIT_CONFIG_GLOBAL", "/dev/null");
        env.put("GIT_CONFIG_NOSYSTEM", "1");
        env.put("LC_ALL", "C");
        if (session != null) {
            env.put("CLAUDE_CODE_SESSION_ID", session);
        }
        env.putAll(extra);
        final Process process = builder.start();
        final String output =
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("git ended").isTrue();
        return new Result(process.exitValue(), output);
    }

    private record Result(int exit, String output) {}
}
