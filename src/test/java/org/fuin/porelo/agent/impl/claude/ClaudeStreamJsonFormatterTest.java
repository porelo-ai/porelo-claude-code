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
}
