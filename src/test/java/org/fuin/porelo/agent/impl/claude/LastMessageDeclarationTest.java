package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.fuin.porelo.agent.api.Waiting;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;

/**
 * What a finished unattended run said: the harm is a reader whose findings stand in its answer and
 * whose run is judged on an empty one.
 *
 * <p>The records are a whole run of the pinned Claude Code 2.1.311, recorded unattended in a Porelo
 * task on 2026-11-12, as the agent wrote them; only the lists in its first record are cut short,
 * and the two lines it wrote that are not records are left out. The run reasoned, wrote a sentence,
 * ran {@code ls -a}, and replied {@code ALL DONE}.
 */
class LastMessageDeclarationTest {

    private static final Path RUN =
            Path.of("src/test/resources/streams/unattended-run-2.1.311.jsonl");

    /**
     * A run the API refused for an invalid key, recorded with Claude Code 2.1.322 on the host on
     * 2026-11-12, as it wrote it; its first record's lists are cut and its directory named {@code
     * /workspace}.
     */
    private static final Path REFUSED =
            Path.of("src/test/resources/streams/refused-run-2.1.322.jsonl");

    private static List<Object> records() throws IOException {
        return records(RUN);
    }

    private static List<Object> records(final Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .filter(line -> !line.isBlank())
                .map(line -> (Object) Json.parse(line))
                .toList();
    }

    private static Waiting.LastMessage lastMessage() {
        final Waiting waiting = new ClaudeAgent().definition().waiting();
        assertThat(waiting).as("claude.yaml declares session.waiting").isNotNull();
        final Waiting.LastMessage last = waiting.lastMessage();
        assertThat(last).as("claude.yaml declares session.waiting.last_message").isNotNull();
        return last;
    }

    @Test
    void answersTheLastReplyAlone() throws IOException {

        // Before it stand a reasoning, an earlier reply, a tool call and its output; the answer is
        // none of those.
        assertThat(lastMessage().of(records())).as("the run's last reply").isEqualTo("ALL DONE");
    }

    @Test
    void aRunThatEndedWithoutAResultAnswersNothing() throws IOException {

        // A run cut off before its result has no answer to give, which is not an empty one.
        final List<Object> cut = records();

        assertThat(lastMessage().of(cut.subList(0, cut.size() - 1))).isNull();
    }

    @Test
    void aRunTheApiRefusedAnswersNothing() throws IOException {

        // Its result is of subtype success, marked is_error, its text the API's error; read as a
        // reply, a reader's failed run would pass for a finished one.
        final List<Object> refused = records(REFUSED);
        assertThat(refused.getLast().toString())
                .as("the recorded result")
                .contains("Invalid API key", "api_error");

        assertThat(lastMessage().of(refused)).isNull();
    }

    @Test
    void theDeclarationAddsNoScreenRule() {

        // Porelo wakes an agent by its screen rules; the answer of a finished run must not change
        // which screens read as waiting.
        assertThat(new ClaudeAgent().definition().waiting().screen())
                .containsExactly(new Waiting.Rule("Esc to cancel", 5, "an answer to its question"));
    }
}
