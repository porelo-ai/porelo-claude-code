package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * How Claude Code takes a reasoning effort, as its definition declares it for Porelo's Start.
 *
 * <p>Prevents a start that offers an effort the CLI does not take, or passes a picked one where the
 * CLI does not read it: Porelo offers exactly the declared values and puts the picked one where
 * {@code {effort}} stands. Read from the definition itself, since an older Porelo's reader skips
 * the section.
 */
class EffortDeclarationTest {

    private static Map<?, ?> effort() throws IOException {
        try (InputStream in =
                EffortDeclarationTest.class
                        .getClassLoader()
                        .getResourceAsStream("agent/claude.yaml")) {
            assertThat(in).as("the definition on the classpath").isNotNull();
            final Map<?, ?> definition = new Yaml().load(in);
            assertThat(definition.get("effort"))
                    .as("claude.yaml declares an effort section")
                    .isInstanceOf(Map.class);
            return (Map<?, ?>) definition.get("effort");
        }
    }

    @Test
    void passesThePickedEffortByItsFlag() throws IOException {

        assertThat(effort().get("arguments"))
                .as("'claude --help' names --effort <level>")
                .isEqualTo(java.util.List.of("--effort", "{effort}"));
    }

    @Test
    void offersTheLevelsTheCliTakes() throws IOException {

        assertThat(effort().get("values"))
                .as("the levels 'claude --help' lists, in its words and order")
                .isEqualTo(java.util.List.of("low", "medium", "high", "xhigh", "max"));
    }
}
