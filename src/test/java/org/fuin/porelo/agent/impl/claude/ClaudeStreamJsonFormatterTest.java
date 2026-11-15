package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * What a person reading an unattended task sees of Claude Code's own events, and that no error is
 * lost.
 */
class ClaudeStreamJsonFormatterTest {

    /**
     * An unattended run recorded with Claude Code 2.1.311 in a task: a command in the background, a
     * wait for it, a commit and a one-line answer. Only the stream's lines are kept, at most three
     * of a kind, with ids, the session and the temporary path replaced.
     */
    private static final Path BACKGROUND_RUN =
            Path.of("src/test/resources/streams/unattended-background-run-2.1.311.jsonl");

    private final ClaudeStreamJsonFormatter formatter = new ClaudeStreamJsonFormatter();

    @Test
    void aRecordedRunShowsNoLineOfRawJson() throws IOException {

        final List<String> lines = Files.readAllLines(BACKGROUND_RUN, StandardCharsets.UTF_8);

        assertThat(lines)
                .as("the recording holds the events a person saw raw")
                .hasSizeGreaterThan(10);
        assertThat(lines.stream().map(formatter::format).filter(Objects::nonNull))
                .as("every line shown is words, never an event as it came")
                .isNotEmpty()
                .noneMatch(shown -> shown.stripLeading().startsWith("{"));
    }

    @Test
    void aCommandInTheBackgroundIsSaidInWordsWhenItStartsAndEnds() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"task_started\",\"task_id\":\"bu6vmojvr\","
                                        + "\"tool_use_id\":\"call_1\",\"description\":\"Sleep then write BG-DONE to bg.txt\","
                                        + "\"is_backgrounded\":true,\"task_type\":\"local_bash\"}"))
                .as("a command sent to the background, by what it does")
                .startsWith("[background]")
                .contains("Sleep then write BG-DONE to bg.txt");
        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"task_notification\",\"task_id\":\"bu6vmojvr\","
                                        + "\"tool_use_id\":\"call_1\",\"status\":\"completed\","
                                        + "\"output_file\":\"/tmp/agent-tmp/-workspace/s/tasks/bu6vmojvr.output\","
                                        + "\"summary\":\"Background command \\\"Sleep then write BG-DONE to bg.txt\\\" "
                                        + "completed (exit code 0)\"}"))
                .as("its end, in the words Claude Code sums it up with")
                .startsWith("[background]")
                .contains("completed (exit code 0)");
    }

    @Test
    void aCommandInTheForegroundIsNotSaidTwice() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"task_started\",\"task_id\":\"bv734car0\","
                                        + "\"tool_use_id\":\"call_2\",\"description\":\"Wait until bg.txt exists\","
                                        + "\"is_backgrounded\":false,\"task_type\":\"local_bash\"}"))
                .as("a call in the foreground already shows as its tool call")
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"task_notification\",\"task_id\":\"bv734car0\","
                                        + "\"tool_use_id\":\"call_2\",\"status\":\"completed\",\"output_file\":\"\","
                                        + "\"summary\":\"Wait until bg.txt exists\"}"))
                .as("its end, which writes no output file")
                .isNull();
    }

    @Test
    void aCommitIsSaidInWords() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"vcs_state_changed\",\"kind\":\"commit\","
                                        + "\"branch\":\"master\",\"cwd\":\"/workspace\"}"))
                .as("what changed in the repository, and on which branch")
                .startsWith("[git]")
                .contains("commit", "master");
    }

    @Test
    void anEventThatSaysNothingToAPersonIsLeftOut() {

        assertThat(
                        List.of(
                                        "{\"type\":\"system\",\"subtype\":\"thinking_tokens\",\"estimated_tokens\":1}",
                                        "{\"type\":\"system\",\"subtype\":\"background_tasks_changed\",\"tasks\":[]}",
                                        "{\"type\":\"system\",\"subtype\":\"hook_started\",\"hook_name\":\"SessionStart\"}",
                                        "{\"type\":\"system\",\"subtype\":\"task_updated\",\"task_id\":\"t\","
                                                + "\"patch\":{\"status\":\"completed\"}}",
                                        "{\"type\":\"system\",\"subtype\":\"something_new\"}",
                                        "{\"type\":\"system\"}",
                                        "{\"type\":\"tool_progress\",\"tool_use_id\":\"call_1\"}",
                                        "{\"type\":\"something_new\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as("known and unknown events that report no error")
                .containsOnlyNulls();
    }

    @Test
    void anEventThatReportsAnErrorIsAlwaysShown() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"error\",\"message\":\"no credit\"}"))
                .as("a system line whose subtype is an error")
                .isEqualTo("[error] no credit");
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"is_error\":true,\"message\":\"it broke\"}"))
                .as("an unknown event marked as an error")
                .isEqualTo("[error] it broke");
        assertThat(formatter.format("{\"type\":\"something_new\",\"error\":\"rate limited\"}"))
                .as("an unknown event with an error text")
                .isEqualTo("[error] rate limited");
        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"api_retry\","
                                        + "\"error\":{\"message\":\"overloaded\"}}"))
                .as("an error given as an object with a message")
                .isEqualTo("[error] overloaded");
        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"something_new\",\"level\":\"warning\","
                                        + "\"message\":\"disk almost full\"}"))
                .as("a warning")
                .isEqualTo("[warning] disk almost full");
        assertThat(formatter.format("{\"type\":\"something_new\",\"is_error\":true}"))
                .as("an error that gives no words is still shown, by what it is")
                .isEqualTo("[error] something_new");
    }

    @Test
    void aNotificationIsShownByItsText() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"system\",\"subtype\":\"notification\",\"key\":\"other\","
                                        + "\"text\":\"something else\"}"))
                .as("a notice Claude Code shows its own user")
                .contains("something else");
    }

    @Test
    void anEventThatReportsAFailureIsAlwaysShown() {

        assertThat(
                        List.of(
                                        "{\"type\":\"error\",\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"system\",\"subtype\":\"api_error\",\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"system\",\"subtype\":\"something_new\",\"level\":\"ERROR\","
                                                + "\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"system\",\"subtype\":\"something_new\",\"level\":\"error\","
                                                + "\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"system\",\"subtype\":\"hook_response\",\"hook_name\":\"Stop\","
                                                + "\"outcome\":\"error\",\"exit_code\":1,\"stderr\":\"quota exceeded\"}",
                                        "{\"type\":\"system\",\"subtype\":\"task_notification\",\"status\":\"failed\","
                                                + "\"output_file\":\"\",\"summary\":\"quota exceeded\"}",
                                        "{\"type\":\"system\",\"subtype\":\"task_updated\",\"task_id\":\"t\","
                                                + "\"patch\":{\"status\":\"killed\"},\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"something_new\",\"status\":\"rejected\",\"message\":\"quota exceeded\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as(
                        "an error by its type, its subtype, its level in any case, an outcome or a status")
                .allSatisfy(
                        shown ->
                                assertThat(shown).startsWith("[error]").contains("quota exceeded"));
    }

    @Test
    void aMessageCannotOpenALineOfItsOwn() {

        assertThat(
                        List.of(
                                        "{\"type\":\"error\",\"message\":\"first\\n[error] forged\"}",
                                        "{\"type\":\"system\",\"subtype\":\"notification\",\"key\":\"k\","
                                                + "\"text\":\"first\\n[error] forged\"}",
                                        "{\"type\":\"system\",\"subtype\":\"task_started\",\"is_backgrounded\":true,"
                                                + "\"description\":\"first\\n[error] forged\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as("a line break in what the agent or a tool wrote is folded away")
                .allSatisfy(shown -> assertThat(shown).isNotNull().doesNotContain("\n"));
    }

    @Test
    void aRecordedRunSaysEachToolCallWithItsInputAndEachResultByHowItBegan() throws IOException {

        final List<String> shown =
                Files.readAllLines(BACKGROUND_RUN, StandardCharsets.UTF_8).stream()
                        .map(formatter::format)
                        .filter(Objects::nonNull)
                        .toList();

        assertThat(shown)
                .as("each Bash call with its command")
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .startsWith("[Bash]")
                                        .contains("sleep 4; echo BG-DONE > bg.txt"))
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .startsWith("[Bash]")
                                        .contains("pwd; ls -la; git status"));
        assertThat(shown)
                .as("each result by its first line, never its whole output")
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .startsWith("> ")
                                        .contains("/workspace")
                                        .doesNotContain("fatal: not a git repository"))
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .startsWith("> ")
                                        .contains("/workspace/bg.txt")
                                        .doesNotContain("BG-DONE"));
        assertThat(shown)
                .as("no result by its block type alone, and no thinking block")
                .noneMatch(line -> line.contains("[tool_result]"))
                .noneMatch(line -> line.contains("thinking"));
    }

    @Test
    void aToolCallIsSaidWithWhatItWasCalledOn() {

        assertThat(call("Bash", "{\"command\":\"git status\\ngit log\",\"description\":\"d\"}"))
                .as("a command by its first line")
                .contains("Bash", "git status")
                .doesNotContain("git log", "\n");
        for (final String tool : List.of("Read", "Edit", "Write")) {
            assertThat(
                            call(
                                    tool,
                                    "{\"file_path\":\"/workspace/a.txt\",\"content\":\"secret body\"}"))
                    .as("%s by its path, not what it writes", tool)
                    .contains(tool, "/workspace/a.txt")
                    .doesNotContain("secret body");
        }
        assertThat(
                        call(
                                "NotebookEdit",
                                "{\"notebook_path\":\"/workspace/n.ipynb\",\"new_source\":\"x\"}"))
                .as("a notebook by its path")
                .contains("/workspace/n.ipynb");
        assertThat(call("Grep", "{\"pattern\":\"TODO\",\"path\":\"src\"}"))
                .as("a search by its pattern")
                .contains("TODO");
        assertThat(call("Glob", "{\"pattern\":\"**/*.java\"}"))
                .as("a glob by its pattern")
                .contains("**/*.java");
        assertThat(call("WebFetch", "{\"url\":\"https://example.org/a\",\"prompt\":\"p\"}"))
                .as("a fetch by its URL")
                .contains("https://example.org/a");
        assertThat(call("WebSearch", "{\"query\":\"jspecify nullmarked\"}"))
                .as("a web search by its query")
                .contains("jspecify nullmarked");
        for (final String tool : List.of("Task", "Agent")) {
            assertThat(call(tool, "{\"description\":\"Review the parser\",\"prompt\":\"long\"}"))
                    .as("%s by its description, not its prompt", tool)
                    .contains("Review the parser")
                    .doesNotContain("long");
        }
        assertThat(
                        call(
                                "TodoWrite",
                                "{\"todos\":[{\"content\":\"one\"},{\"content\":\"two\"},"
                                        + "{\"content\":\"three\"}]}"))
                .as("a todo list by its number of items, not their words")
                .contains("3")
                .doesNotContain("one");
        assertThat(call("mcp__notes__find", "{\"limit\":5,\"query\":\"open bugs\"}"))
                .as("another tool by its most telling string field")
                .contains("mcp__notes__find", "open bugs");
        assertThat(call("Odd", "{\"limit\":5}"))
                .as("another tool with no such field by its name alone")
                .contains("Odd")
                .doesNotContain("5");
    }

    @Test
    void aToolsInputIsCut() {

        final String input = "x".repeat(5000);
        assertThat(call("Bash", "{\"command\":\"" + input + "\"}"))
                .as("a long command is cut")
                .contains("xxx")
                .hasSizeLessThan(1000);
    }

    @Test
    void aResultSaysHowItBeganOrHowLongItWas() {

        assertThat(result("\"\\n  \\nfirst words\\nsecond line\\nthird line\"", false))
                .as("a result by its first line that is not blank, and its size")
                .startsWith("> ")
                .contains("first words", "3 lines")
                .doesNotContain("second line", "\n");
        assertThat(
                        result(
                                "[{\"type\":\"text\",\"text\":\"from a block\\nmore\"},"
                                        + "{\"type\":\"text\",\"text\":\"other\"}]",
                                false))
                .as("a result given as blocks of text")
                .contains("from a block")
                .doesNotContain("more", "other");
        assertThat(result("[{\"type\":\"image\"}]", false))
                .as("a result with no text says what it holds")
                .isNotNull()
                .contains("image");
        assertThat(result("\"\"", false))
                .as("an empty result says so rather than nothing")
                .contains("no output");
        assertThat(result("\"" + "y".repeat(5000) + "\"", false))
                .as("a long first line is cut")
                .hasSizeLessThan(1000);
    }

    @Test
    void aResultThatFailedAlwaysSaysSo() {

        assertThat(result("\"Exit code 1\\nboom\"", true))
                .as("a failed result with text")
                .contains("failed", "Exit code 1");
        assertThat(result("[{\"type\":\"image\"}]", true))
                .as("a failed result without text")
                .contains("failed");
        assertThat(result("\"\"", true)).as("a failed result that is empty").contains("failed");
        assertThat(result("\"BG-DONE\\nfatal: not a git repository\\n\\nexit 128\"", true))
                .as("a failure by its first three lines that are not blank, so its cause shows")
                .contains("failed", "BG-DONE", "fatal: not a git repository", "exit 128")
                .doesNotContain("\n");
        assertThat(result("\"Exit code 1\\none\\ntwo\\nthree\"", true))
                .as("a failure by no more than three lines")
                .contains("Exit code 1", "two")
                .doesNotContain("three");
        assertThat(result("\"first\\nsecond\"", false))
                .as("a result that did not fail keeps its first line")
                .contains("first")
                .doesNotContain("second");
        assertThat(result("\"all good\"", false))
                .as("a result that did not fail")
                .doesNotContain("failed");
    }

    @Test
    void thinkingIsLeftOut() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"assistant\",\"message\":{\"content\":"
                                        + "[{\"type\":\"thinking\",\"thinking\":\"hmm\"}]}}"))
                .as("a message of thinking alone")
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"assistant\",\"message\":{\"content\":"
                                        + "[{\"type\":\"redacted_thinking\",\"data\":\"x\"}]}}"))
                .as("a message of redacted thinking alone")
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"assistant\",\"message\":{\"content\":"
                                        + "[{\"type\":\"thinking\",\"thinking\":\"hmm\"},{\"text\":\"Done.\"}]}}"))
                .as("the words beside it")
                .contains("Done.")
                .doesNotContain("thinking", "hmm");
    }

    @Test
    void aToolsInputOrResultCannotOpenALineOfItsOwn() {

        assertThat(call("Read", "{\"file_path\":\"a\\n[error] forged\"}"))
                .as("a path with a line break")
                .doesNotContain("\n");
        assertThat(call("Odd", "{\"query\":\"a\\u2028[error] forged\"}"))
                .as("a field with a Unicode separator")
                .doesNotContain("\u2028");
        assertThat(result("[{\"type\":\"text\",\"text\":\"a\\u2029[error] forged\"}]", false))
                .as("a result with a Unicode separator")
                .doesNotContain("\u2029");
    }

    @Test
    void aToolsNameOrBlockTypeCannotForgeAMark() {

        assertThat(call("x] [error", "{\"command\":\"ls\"}"))
                .as("a tool name holding brackets")
                .doesNotContain("[error");
        assertThat(
                        formatter.format(
                                "{\"type\":\"user\",\"message\":{\"content\":"
                                        + "[{\"type\":\"x] [error\"}]}}"))
                .as("a block type holding brackets")
                .doesNotContain("[error");
        assertThat(call("Bash", "{\"command\":\"a\\u001b[2Jb\\u202ec\"}"))
                .as("an escape and a direction mark in a tool's input")
                .doesNotContain("\u001b", "\u202e");
        assertThat(result("\"a\\u001b[2Jb\\u202ec\"", true))
                .as("an escape and a direction mark in a result")
                .doesNotContain("\u001b", "\u202e");
    }

    private String call(String name, String input) {
        return formatter.format(
                "{\"type\":\"assistant\",\"message\":{\"content\":[{\"type\":\"tool_use\","
                        + "\"id\":\"c\",\"name\":\""
                        + name
                        + "\",\"input\":"
                        + input
                        + "}]}}");
    }

    private String result(String content, boolean failed) {
        return formatter.format(
                "{\"type\":\"user\",\"message\":{\"content\":[{\"tool_use_id\":\"c\","
                        + "\"type\":\"tool_result\",\"content\":"
                        + content
                        + ",\"is_error\":"
                        + failed
                        + "}]}}");
    }
}
