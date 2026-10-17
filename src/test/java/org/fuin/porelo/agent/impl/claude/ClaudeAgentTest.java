package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fuin.porelo.agent.api.Agent;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.AgentRegistry;
import org.fuin.porelo.agent.api.Credential;
import org.fuin.porelo.agent.api.ProviderDefinition;
import org.fuin.porelo.agent.api.RunRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the Claude agent module.
 */
class ClaudeAgentTest {

    private final Agent agent = new ClaudeAgent();

    @Test
    void declaresWhatItShowsOnceAtWork() {

        // The kit's check waits for this text and types nothing; a dialog before the prompt hides it.
        assertThat(agent.definition().ready()).isNotNull();
        assertThat(agent.definition().ready().text()).isEqualTo("bypass permissions on");
    }

    @Test
    void pointsItsLoginAtTheAuthenticationPage() throws IOException {

        // Read as text: the published agent API has no accessor for the field yet, and a reader
        // that knows nothing of a key ignores it, so nothing else would notice it going missing.
        final String definition;
        try (var in = ClaudeAgentTest.class.getClassLoader().getResourceAsStream("agent/claude.yaml")) {
            assertThat(in).isNotNull();
            definition = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        assertThat(agent.definition().loginArguments()).isEmpty();
        assertThat(definition).contains("documentation: \"https://code.claude.com/docs/en/authentication\"");
    }

    @Test
    void isDiscoveredThroughTheServiceLoader() {

        // Nothing names ClaudeAgent for this to work: it is found through
        // META-INF/services, which is what lets Porelo stay ignorant of it.
        final AgentRegistry registry = AgentRegistry.discover();

        assertThat(registry.names()).contains("claude");
        assertThat(registry.require("claude").definition().binary()).isEqualTo("claude");
    }

    @Test
    void readsItsOwnDefinition() {

        assertThat(agent.definition().label()).isEqualTo("Claude Code");
        assertThat(agent.definition().gitIdentity().email()).isEqualTo("noreply@anthropic.com");
        // api.anthropic.com is deliberately NOT here: the provider declares its own host, and
        // this agent no longer restates it.
        assertThat(agent.definition().allowedDomains())
                .contains("platform.claude.com")
                .doesNotContain("api.anthropic.com");
    }

    @Test
    void namesTheVariablesClaudeCodeItselfReads() {

        // Which variable a credential is read from is a fact about THIS CLI, not about whoever
        // serves it. Porelo falls back to the provider's variable when an agent names none -
        // right for a provider-agnostic agent, wrong here: pointed at OpenRouter this agent was
        // handed OPENROUTER_API_KEY and reported 'Not logged in'. An earlier version of this
        // test asserted the opposite, and the acceptance suite is what disproved it.
        //
        // Sending an OAuth token as an API key fails in a way that looks like a bad key, so the
        // two still have to differ.
        assertThat(agent.definition().tokenVariable("oauth")).isEqualTo("CLAUDE_CODE_OAUTH_TOKEN");
        assertThat(agent.definition().tokenVariable("api-key")).isEqualTo("ANTHROPIC_API_KEY");
    }

    @Test
    void declaresHowItMustBePointedAtAProvider() {

        assertThat(agent.definition().provider()).isNotNull();
        assertThat(agent.definition().provider().dialect()).isEqualTo("anthropic-messages");
        assertThat(agent.definition().provider().defaultProvider()).isEqualTo("anthropic");
        assertThat(agent.definition().provider().socketEnvironment())
                .isEqualTo("ANTHROPIC_UNIX_SOCKET");
    }

    @Test
    void refusesTheHostTheCliUpdatesItselfFrom() {

        // Allowing claude.ai admits every name under it, and the update came from one of them. The
        // settings file stops the update; refusing the host is the layer behind it.
        assertThat(agent.definition().refusedDomains()).contains("downloads.claude.ai");
        assertThat(agent.definition().allowedDomains()).doesNotContain("downloads.claude.ai");
    }

    @Test
    void buildsAHeadlessCommandFromTheSharedBuilder() {

        assertThat(agent.headlessCommand(
                new RunRequest("fix the bug", "sonnet", Integer.valueOf(3), null, false, true)))
                // No permission flag: the settings file puts the session in bypass mode, and
                // the flag changed nothing beside it (measured).
                .containsExactly("claude",
                        "--model", "sonnet", "--max-turns", "3",
                        // --verbose is part of what machine-readable output costs for Claude:
                        // it refuses stream-json in print mode without it. Found by running it.
                        "--output-format", "stream-json", "--verbose", "-p", "fix the bug");
    }

    @Test
    void extractsAnOauthTokenWithItsRefreshAndExpiry(@TempDir Path dir) throws IOException {

        Files.writeString(dir.resolve(".credentials.json"), """
                {"claudeAiOauth":{"accessToken":"sk-ant-oat-xyz",
                 "refreshToken":"sk-ant-ort-abc","expiresAt":1788000000}}
                """);

        assertThat(agent.credentialExtractor().extract(dir)).hasValueSatisfying(credential -> {
            assertThat(credential.type()).isEqualTo("oauth");
            assertThat(credential.secret()).isEqualTo("sk-ant-oat-xyz");
            assertThat(credential.attributes()).containsEntry("refreshToken", "sk-ant-ort-abc");
            assertThat(credential.attributes()).containsEntry("expiresAt", "1788000000");
        });
    }

    @Test
    void extractsAnApiKey(@TempDir Path dir) throws IOException {

        Files.writeString(dir.resolve(".credentials.json"), "{\"apiKey\":\"sk-ant-api-123\"}");

        assertThat(agent.credentialExtractor().extract(dir)).hasValueSatisfying(credential ->
                assertThat(credential.type()).isEqualTo("api-key"));
    }

    @Test
    void reportsNotLoggedInAsEmpty(@TempDir Path dir) {

        assertThat(agent.credentialExtractor().extract(dir)).isEmpty();
    }

    @Test
    void refusesAFileThatHoldsNeither(@TempDir Path dir) throws IOException {

        // Reporting "not logged in" here would send the operator to run a login that has already
        // succeeded.
        Files.writeString(dir.resolve(".credentials.json"), "{\"somethingElse\":true}");

        assertThatThrownBy(() -> agent.credentialExtractor().extract(dir))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("neither an OAuth token nor an API key");
    }

    @Test
    void keepsTheCredentialOutOfItsOwnToString() {

        // Porelo's type, tested here on purpose: this adapter hands it the token, and a toString that
        // printed it would put the credential in whatever log caught the object.

        assertThat(Credential.of("oauth", "sk-ant-secret").toString())
                .doesNotContain("sk-ant-secret");
    }

    @Test
    void rendersStreamJsonAsReadableLines() {

        final var formatter = agent.logFormatter();

        assertThat(formatter.format(
                "{\"type\":\"assistant\",\"message\":{\"content\":[{\"text\":\"Looking at it.\"}]}}"))
                .isEqualTo("Looking at it.");
        assertThat(formatter.format(
                "{\"type\":\"assistant\",\"message\":{\"content\":"
                        + "[{\"type\":\"tool_use\",\"name\":\"Bash\"}]}}"))
                .isEqualTo("[Bash]");
        assertThat(formatter.format("{\"type\":\"result\",\"num_turns\":4,\"total_cost_usd\":0.12}"))
                .isEqualTo("-- done, 4 turns, about $0.12 at API prices");
        // What a real subscription run reported: a double's full expansion, and one turn.
        assertThat(formatter.format("{\"type\":\"result\",\"num_turns\":1,\"total_cost_usd\":0.15322799999999998}"))
                .isEqualTo("-- done, 1 turn, about $0.1532 at API prices");
        // Every JSON number arrives as a double, which prints 0.0004 as 4.0E-4 and 2 as 2.0.
        assertThat(formatter.format("{\"type\":\"result\",\"total_cost_usd\":0.0004}"))
                .isEqualTo("-- done, about $0.0004 at API prices");
        assertThat(formatter.format("{\"type\":\"result\",\"total_cost_usd\":2}"))
                .isEqualTo("-- done, about $2 at API prices");
        assertThat(formatter.format("{\"type\":\"system\",\"subtype\":\"init\"}")).isNull();
    }

    @Test
    void saysARunEndedWithAnErrorRatherThanDone() {

        // A provider's refusal still ends the stream with a result line; read as "done" it hid why
        // nothing happened.
        final var formatter = agent.logFormatter();
        assertThat(formatter.format("{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":true,"
                + "\"num_turns\":1,\"result\":\"API Error: 402 This request requires more credits\"}"))
                .isEqualTo("-- ended with an error: API Error: 402 This request requires more credits, 1 turn");
        assertThat(formatter.format("{\"type\":\"result\",\"subtype\":\"error_max_turns\",\"is_error\":true,"
                + "\"num_turns\":12}"))
                .isEqualTo("-- ended with an error (error_max_turns), 12 turns");
        assertThat(formatter.format("{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,"
                + "\"num_turns\":2}"))
                .isEqualTo("-- done, 2 turns");
    }

    @Test
    void showsAnythingItDoesNotUnderstand() {

        // A viewer that swallowed unrecognised lines would hide exactly the output worth reading
        // when something has gone wrong.
        final var formatter = agent.logFormatter();

        assertThat(formatter.format("plain text from a crash")).isEqualTo("plain text from a crash");
        assertThat(formatter.format("{not valid json")).isEqualTo("{not valid json");
        assertThat(formatter.format("{\"type\":\"something_new\"}")).isEqualTo("{\"type\":\"something_new\"}");
    }

    @Test
    void showsEverySystemEventExceptTheStartupLine() {

        // "system" is a category, not a severity: a warning, an error and a schema change after an
        // upstream bump arrive under it. Only the startup line is noise, and it is matched by its
        // subtype so that everything else stays visible.
        final var formatter = agent.logFormatter();

        final String error = "{\"type\":\"system\",\"subtype\":\"error\",\"message\":\"no credit\"}";
        assertThat(formatter.format(error)).isEqualTo(error);

        final String unknown = "{\"type\":\"system\",\"subtype\":\"something_new\"}";
        assertThat(formatter.format(unknown)).isEqualTo(unknown);

        final String noSubtype = "{\"type\":\"system\"}";
        assertThat(formatter.format(noSubtype)).isEqualTo(noSubtype);
    }

    @Test
    void marksARecognisedEventItCannotRead() {

        // The alternative is a run that looks quieter than it was, exactly when somebody is
        // reading the log to find out why it failed.
        final var formatter = agent.logFormatter();

        final String noContent = "{\"type\":\"assistant\",\"message\":{}}";
        assertThat(formatter.format(noContent)).isEqualTo("[unreadable] " + noContent);

        final String noMessage = "{\"type\":\"assistant\"}";
        assertThat(formatter.format(noMessage)).isEqualTo("[unreadable] " + noMessage);

        final String contentNotAList = "{\"type\":\"user\",\"message\":{\"content\":\"plain\"}}";
        assertThat(formatter.format(contentNotAList)).isEqualTo("[unreadable] " + contentNotAList);

        final String blockWithoutType = "{\"type\":\"assistant\",\"message\":{\"content\":[{\"x\":1}]}}";
        assertThat(formatter.format(blockWithoutType)).isEqualTo("[unreadable] " + blockWithoutType);
    }

    @Test
    void namesABlockShapeItDoesNotRender() {

        // Named by what the block calls itself rather than by a guess at its contents: this
        // formatter has been measured against a single-turn run, and a shape it has never seen
        // should still leave a trace.
        final var formatter = agent.logFormatter();

        assertThat(formatter.format(
                "{\"type\":\"user\",\"message\":{\"content\":[{\"type\":\"tool_result\"}]}}"))
                .isEqualTo("> [tool_result]");
        assertThat(formatter.format(
                "{\"type\":\"assistant\",\"message\":{\"content\":"
                        + "[{\"text\":\"Here: \"},{\"type\":\"thinking\"}]}}"))
                .isEqualTo("Here: [thinking]");
    }

    @Test
    void installsAPinnedAndVerifiedBinary() {

        // Not 'curl | bash'. The version and digest are what make an image build reproducible
        // and make "which version ran" answerable from the definition.
        final var definition = agent.definition();

        // No literal version: written down here, it was a fourth place every bump had to be applied.
        assertThat(definition.version()).matches("\\d+\\.\\d+\\.\\d+");
        assertThat(definition.artifacts()).singleElement().satisfies(artifact -> {
            assertThat(artifact.url()).startsWith("https://downloads.claude.ai/");
            assertThat(artifact.url()).contains(definition.version());
            assertThat(artifact.sha256()).matches("[a-f0-9]{64}");
            assertThat(artifact.unverified()).isFalse();
            assertThat(artifact.target()).isEqualTo("/home/agent/.local/bin/claude");
        });
        assertThat(definition.unverifiedArtifacts()).isEmpty();
    }

    @Test
    void contributesAnImageLayerThatChecksBeforeItInstalls() {

        final var lines = agent.imageLayer().asAgent();

        assertThat(agent.imageLayer().isEmpty()).isFalse();
        assertThat(lines).anyMatch(line -> line.contains("sha256sum -c -"));

        final int check = indexOf(lines, "sha256sum -c -");
        final int install = indexOf(lines, "install -D");
        assertThat(install).isGreaterThan(check);
    }

    private static int indexOf(java.util.List<String> lines, String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(text)) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void keepsThoseVariablesWhateverProviderServesIt() {

        // The point of the pair: OpenRouter declares OPENROUTER_API_KEY, and this agent must
        // still be handed ANTHROPIC_API_KEY, because that is what it reads.
        final ProviderDefinition openrouter =
                new ProviderDefinition(
                        "openrouter", "OpenRouter", "https://openrouter.ai",
                        java.util.Map.of("anthropic-messages", "/api"),
                        java.util.Map.of("_default", "Authorization"),
                        java.util.Map.of("_default", "Bearer "),
                        java.util.Map.of(),
                        java.util.Map.of("_default", "OPENROUTER_API_KEY"));

        assertThat(agent.definition().tokenVariable("api-key", openrouter))
                .isEqualTo("ANTHROPIC_API_KEY");
        assertThat(agent.definition().tokenVariable("oauth", openrouter))
                .isEqualTo("CLAUDE_CODE_OAUTH_TOKEN");
    }

    @Test
    void isToldWhatPoreloPutsInItsTaskThroughItsSystemPrompt() {

        // Porelo puts these behind the sandboxed arguments in every task Porelo makes, {file} standing for the
        // path of its guide to the task; the CLI reads the file, so the text never rides on the command line.
        assertThat(agent.definition().instructionArguments()).containsExactly("--append-system-prompt-file", "{file}");
    }

    @Test
    void saysWhenItIsAtRestAsMeasuredAtATerminal() {

        // Porelo types its wake line only where this matches the last lines of the screen. The screens are
        // cut from ones measured on the VM: after an answer, and during a 'sleep 90' tool call.
        final var atRest = agent.definition().atRest();
        assertThat(atRest).isNotNull();
        assertThat(atRest.shows()).containsExactly("bypass permissions on");
        assertThat(atRest.lacks()).containsExactly("esc to interrupt", "Esc to cancel");
        assertThat(atRest.matches("● PONG\n\n✻ Baked for 3s · done 7:39 AM\n────\n❯ \n────\n⏵⏵ bypass permissions on (shift+tab to cycle) · ← for agents")).isTrue();
        assertThat(atRest.matches("  Bash(sleep 90)\n  ⎿  Running…\n❯ \n────\n⏵⏵ bypass permissions on (shift+tab to cycle) · esc to interrupt · ← for agents")).isFalse();
        // A question open: Enter would answer it, so never at rest.
        assertThat(atRest.matches("❯ 1. Yes\n  2. No\nEnter to select · ↑/↓ to navigate · Esc to cancel\n⏵⏵ bypass permissions on (shift+tab to cycle)")).isFalse();
    }
}
