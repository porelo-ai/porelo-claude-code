package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * How an attended Claude Code session takes its opening text: the harm is a discussion that is
 * refused, or one that runs in print mode and ends after its first answer instead of waiting for
 * the person.
 */
class OpeningTextDeclarationTest {

    @Test
    void declaresThatTheOpeningTextIsPositional() {

        assertThat(new ClaudeAgent().definition().openingFlag())
                .as(
                        "claude.yaml declares session.opening_flag, positional for an interactive claude")
                .isEqualTo("");
    }

    @Test
    void startsTheSessionWithTheTextLastAndWithoutPrintMode() {

        final List<String> command =
                new ClaudeAgent()
                        .definition()
                        .attendedCommand("cheap-model", "Name the file you read first");

        assertThat(command.getLast())
                .as("the opening text is the last argument: %s", command)
                .isEqualTo("Name the file you read first");
        assertThat(command)
                .as("-p would run once and end instead of keeping the session: %s", command)
                .doesNotContain("-p", "--print");
    }
}
