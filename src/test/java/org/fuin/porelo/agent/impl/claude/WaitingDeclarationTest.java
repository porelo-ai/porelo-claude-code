package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.porelo.agent.api.Waiting;
import org.junit.jupiter.api.Test;

/**
 * The waiting declaration read against the screens it was written from.
 *
 * <p>Each screen is what the attached agent drew on 2.1.267, captured at a terminal - so a change
 * to the declaration that stops fitting what was measured fails here. Whether the agent still draws
 * these at the version it pins is the acceptance suite's question, not this one's.
 */
class WaitingDeclarationTest {

    /** Asked to put a question, it asked with its question tool: the list, and how to answer it. */
    private static final String ASKING =
            """
             ▐▛███▛█   Claude Code v2.1.267
            ▝▜██████▀  Opus 5 (1M context) · API Usage Billing
              ▝▝ ▝▝    /workspace
            ❯ Use your AskUserQuestion tool to ask me whether I prefer red or blue. Do nothing else.
            ────────────────────────────────────────────────────────────────────────────────
             ☐ Color
            Do you prefer red or blue?
            ❯ 1. Red
                 You prefer red.
              2. Blue
                 You prefer blue.
              3. Type something.
            ────────────────────────────────────────────────────────────────────────────────
              4. Chat about this
            Enter to select · ↑/↓ to navigate · Esc to cancel
            """;

    /** At its prompt, having asked nothing - what the ready marker waits for. */
    private static final String AT_REST =
            """
             ▐▛███▛█   Claude Code v2.1.267
            ▝▜██████▀  Opus 5 (1M context) · API Usage Billing
              ▝▝ ▝▝    /workspace
            ────────────────────────────────────────────────────────────────────────────────
            ❯
            ────────────────────────────────────────────────────────────────────────────────
              ⏵⏵ bypass permissions on (shift+tab to cycle)
            """;

    private Waiting waiting() {
        final Waiting waiting = new ClaudeAgent().definition().waiting();
        assertThat(waiting).as("claude.yaml declares what waiting looks like").isNotNull();
        return waiting;
    }

    @Test
    void readsAQuestionAsWaitingForAnAnswer() {

        final Waiting.Reading reading = waiting().read(ASKING);

        assertThat(reading.seen()).isEqualTo(Waiting.Seen.WAITING);
        assertThat(reading.waitingFor()).isEqualTo("an answer to its question");
    }

    @Test
    void readsItsPromptAsNotWaiting() {

        // The other half: a rule that matched everything would pass the test above.
        assertThat(waiting().read(AT_REST).seen()).isEqualTo(Waiting.Seen.NOT_WAITING);
    }

    @Test
    void readsTheWorkingLineAsNotWaiting() {

        // At work its last lines say "esc to interrupt"; the rule is matched without regard to
        // case, so
        // it must not be "esc to" alone.
        assertThat(
                        waiting()
                                .read(
                                        AT_REST.replace(
                                                "⏵⏵ bypass permissions on (shift+tab to cycle)",
                                                "✻ Thinking… (esc to interrupt)"))
                                .seen())
                .isEqualTo(Waiting.Seen.NOT_WAITING);
    }

    @Test
    void readsAnAnswerQuotedHigherUpAsNotWaiting() {

        // The same words in the conversation, above the last lines, are not a question on screen.
        final String quoted =
                "It said: Enter to select · ↑/↓ to navigate · Esc to cancel\n" + AT_REST;

        assertThat(waiting().read(quoted).seen()).isEqualTo(Waiting.Seen.NOT_WAITING);
    }
}
