package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.porelo.agent.api.AgentEnd;
import org.junit.jupiter.api.Test;

/** Tests for how {@link ClaudeAgent} reads the end of its run. */
class ClaudeAgentEndTest {

    private final ClaudeAgent agent = new ClaudeAgent();

    @Test
    void readsAProvidersRefusalWithItsStatus() {

        assertThat(
                        agent.ended(
                                "{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":true,"
                                        + "\"result\":\"API Error: 402 This request requires more credits\"}"))
                .isEqualTo(
                        new AgentEnd(
                                false,
                                "API Error: 402 This request requires more credits",
                                AgentEnd.PROVIDER,
                                402));
    }

    @Test
    void readsATurnLimitAsTheAgentsOwnStop() {

        assertThat(
                        agent.ended(
                                "{\"type\":\"result\",\"subtype\":\"error_max_turns\",\"is_error\":true}"))
                .isEqualTo(new AgentEnd(false, "error_max_turns", AgentEnd.AGENT, null));
    }

    @Test
    void readsAFinishedRun() {

        assertThat(
                        agent.ended(
                                "{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,"
                                        + "\"result\":\"Done.\"}"))
                .isEqualTo(new AgentEnd(true, "", AgentEnd.AGENT, null));
    }

    @Test
    void answersNothingForALineThatIsNotTheEnd() {

        assertThat(agent.ended("{\"type\":\"assistant\",\"message\":{\"content\":[]}}")).isNull();
        assertThat(agent.ended("not json")).isNull();
    }
}
