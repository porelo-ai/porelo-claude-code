package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import org.fuin.porelo.agent.api.AgentModels;
import org.junit.jupiter.api.Test;

/** The definition's models as Porelo's own reader takes them, not as a YAML parser sees them. */
class ModelsReadByPoreloTest {

    @Test
    void poreloReadsAnthropicsModelsFromTheDefinition() {

        final AgentModels models = new ClaudeAgent().definition().models();

        assertThat(models.providers().keySet()).containsExactly("anthropic");
        assertThat(models.providers().get("anthropic"))
                .extracting(AgentModels.Model::id, AgentModels.Model::name)
                .as("the aliases with their names, in the definition's order")
                .containsExactly(
                        tuple("sonnet", "Sonnet 5.5"),
                        tuple("fable", "Fable 5.1"),
                        tuple("opus", "Opus 5.5"),
                        tuple("haiku", "Haiku 5.5"));
    }
}
