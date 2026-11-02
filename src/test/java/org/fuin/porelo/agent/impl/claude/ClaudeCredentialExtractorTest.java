package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClaudeCredentialExtractorTest {

    private static final String TOKEN = "porelo_pt_round-trip";

    @TempDir Path home;

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "oauth   | {\"claudeAiOauth\":{\"accessToken\":\"%s\",\"refreshToken\":\"r\",\"expiresAt\":1,\"scopes\":[\"user:inference\"]}}",
                "api-key | {\"apiKey\":\"%s\"}"
            })
    void readsBothKindsAsALoginWritesThem(final String kind, final String shape)
            throws IOException {

        // What 'claude' writes when a person logs in on the machine, which is what the vault
        // imports.
        Files.writeString(
                home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE), shape.formatted(TOKEN));

        final Credential credential = new ClaudeCredentialExtractor().extract(home).orElseThrow();

        assertThat(credential.type()).isEqualTo(kind);
        assertThat(credential.secret()).isEqualTo(TOKEN);
    }

    @Test
    void answersForAnthropicWhenAnthropicIsAsked() throws IOException {

        // Prevents the import a start offers being refused: Porelo stores nothing from an answer
        // that names no provider when one was asked for.
        Files.writeString(
                home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"apiKey\":\"%s\"}".formatted(TOKEN));

        final Credential credential =
                new ClaudeCredentialExtractor().extract(home, "anthropic").orElseThrow();

        assertThat(credential.provider()).isEqualTo("anthropic");
        assertThat(credential.secret()).isEqualTo(TOKEN);
    }

    @Test
    void holdsNothingForAnotherProvider() throws IOException {

        // Prevents a subscription's token reaching a provider it is not for.
        Files.writeString(
                home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"apiKey\":\"%s\"}".formatted(TOKEN));

        assertThat(new ClaudeCredentialExtractor().extract(home, "openrouter")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "{not json                                   | Cannot parse",
                "[1, 2]                                      | is not a JSON object",
                "{\"claudeAiOauth\": {\"refreshToken\": \"r\"}}  | has an OAuth block with no access token",
                "{\"claudeAiOauth\": {\"accessToken\": \" \"}}   | has an OAuth block with no access token",
                "{\"apiKey\": \"  \"}                          | holds neither an OAuth token nor an API key",
                "{}                                          | holds neither an OAuth token nor an API key"
            })
    void saysWhatIsWrongWithAFileItCannotUse(final String content, final String reason)
            throws IOException {

        // Each is "logged in but unreadable", which must not read as "not logged in".
        Files.writeString(
                home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE), content.strip());

        assertThatThrownBy(() -> new ClaudeCredentialExtractor().extract(home))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining(reason.strip());
    }

    @Test
    void carriesWhatARenewalOnTheHostNeeds() throws IOException {

        // A subscription login: Porelo renews it from the refresh token at Claude Code's token URL,
        // as
        // its client, so the task never holds the refresh token.
        Files.writeString(
                home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"claudeAiOauth\":{\"accessToken\":\"t\",\"refreshToken\":\"r\",\"expiresAt\":1790000000000}}");

        assertThat(new ClaudeCredentialExtractor().extract(home).orElseThrow().attributes())
                .containsEntry("refreshToken", "r")
                .containsEntry("expiresAt", "1790000000000")
                .containsEntry("tokenUrl", "https://platform.claude.com/v1/oauth/token")
                .containsEntry("clientId", "9d1c250a-e61b-44d9-88ed-5944d1962f5e");
    }

    @Test
    void namesNoRenewalWithoutARefreshToken() throws IOException {

        // Nothing to renew with, so no endpoint either: an access token alone is used until it
        // ends.
        Files.writeString(
                home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"claudeAiOauth\":{\"accessToken\":\"t\"}}");

        assertThat(new ClaudeCredentialExtractor().extract(home).orElseThrow().attributes())
                .doesNotContainKeys("tokenUrl", "clientId");
    }

    @Test
    void ignoresAnExpiryThatIsNotANumber() throws IOException {

        Files.writeString(
                home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"claudeAiOauth\": {\"accessToken\": \"t\", \"expiresAt\": \"soon\"}}");

        assertThat(new ClaudeCredentialExtractor().extract(home).orElseThrow().attributes())
                .doesNotContainKey("expiresAt");
    }

    // Prevents a Console sign-in, found and offered for import, from failing as "not signed in".
    @Test
    void importsAConsoleKeyFromBesideTheDirectory() throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"" + TOKEN + "\"}");

        final Credential credential = new ClaudeCredentialExtractor().extract(config).orElseThrow();

        assertThat(credential.type()).as("a Console key is an API key").isEqualTo("api-key");
        assertThat(credential.secret()).as("the key itself").isEqualTo(TOKEN);
    }

    // Prevents importing another account's key when the person moved the directory.
    @Test
    void aMovedDirectoryReadsOnlyItsOwnAccountFile() throws IOException {
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"other\"}");
        final Path moved = Files.createDirectory(home.resolve("work"));

        assertThat(new ClaudeCredentialExtractor().extract(moved))
                .as("the home's file does not count for a moved directory")
                .isEmpty();

        Files.writeString(moved.resolve(".claude.json"), "{\"primaryApiKey\":\"" + TOKEN + "\"}");

        assertThat(new ClaudeCredentialExtractor().extract(moved).orElseThrow().secret())
                .as("the file inside it does")
                .isEqualTo(TOKEN);
    }

    // Prevents importing a key Claude Code itself would not use over the subscription it does.
    @Test
    void aSubscriptionWinsOverAConsoleKey() throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"other\"}");
        Files.writeString(
                config.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"claudeAiOauth\":{\"accessToken\":\""
                        + TOKEN
                        + "\",\"scopes\":[\"user:inference\"]}}");

        assertThat(new ClaudeCredentialExtractor().extract(config).orElseThrow().secret())
                .as("the subscription, as Claude Code prefers it")
                .isEqualTo(TOKEN);
    }

    // Prevents a key that works from failing the import over a credentials file that holds nothing.
    @Test
    void aFileHoldingNeitherLeavesAConsoleKeyImportable() throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(config.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE), "{}");
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"" + TOKEN + "\"}");

        assertThat(new ClaudeCredentialExtractor().extract(config).orElseThrow().secret())
                .as("the key Claude Code would use")
                .isEqualTo(TOKEN);
    }

    // Prevents an import from reading "not signed in" when the file that may hold the key is
    // broken.
    @Test
    void aBrokenAccountFileIsAnError() throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(home.resolve(".claude.json"), "not json");

        assertThatThrownBy(() -> new ClaudeCredentialExtractor().extract(config))
                .as("said, rather than reported as nothing there")
                .isInstanceOf(AgentException.class)
                .hasMessageContaining(".claude.json");
    }

    // Prevents an import offered for a Console key from failing or taking what Claude Code ignores.
    @ParameterizedTest
    @ValueSource(
            strings = {
                "not json",
                "[1]",
                "{\"apiKey\":\"ignored\"}",
                "{\"claudeAiOauth\":{\"refreshToken\":\"r\"}}",
                "{\"claudeAiOauth\":{\"accessToken\":\"ignored\"}}",
                "{\"claudeAiOauth\":{\"accessToken\":\"ignored\",\"scopes\":[\"user:profile\"]}}"
            })
    void aFileClaudeCannotUseGivesWayToAConsoleKey(final String content) throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(config.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE), content);
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"" + TOKEN + "\"}");

        final Credential credential = new ClaudeCredentialExtractor().extract(config).orElseThrow();

        assertThat(credential.secret()).as("the key Claude Code would use").isEqualTo(TOKEN);
        assertThat(credential.type()).as("an API key").isEqualTo("api-key");
    }

    // Prevents a broken account file from costing an import that worked before it was read at all.
    @Test
    void aBrokenAccountFileLeavesWhatTheCredentialsFileHolds() throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(
                config.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"apiKey\":\"" + TOKEN + "\"}");
        Files.writeString(home.resolve(".claude.json"), "not json");

        assertThat(new ClaudeCredentialExtractor().extract(config).orElseThrow().secret())
                .as("what the file holds, as before")
                .isEqualTo(TOKEN);
    }

    // Prevents a path written with a trailing dot from reading another account file.
    @Test
    void theDefaultDirectoryIsKnownHoweverItIsWritten() throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"" + TOKEN + "\"}");

        assertThat(
                        new ClaudeCredentialExtractor()
                                .extract(config.resolve("."))
                                .orElseThrow()
                                .secret())
                .as("the file beside the directory")
                .isEqualTo(TOKEN);
    }

    // Prevents part of a key reaching an error message: the parser quotes what it stumbles over.
    @Test
    void aParseErrorQuotesNothingOfTheFile() throws IOException {
        final Path config = Files.createDirectory(home.resolve(".claude"));
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"\\uSECR\"}");

        assertThatThrownBy(() -> new ClaudeCredentialExtractor().extract(config))
                .isInstanceOf(AgentException.class)
                .as("the file is named, its content is not, in the message or its cause")
                .hasStackTraceContaining(".claude.json")
                .satisfies(
                        thrown -> {
                            final StringWriter trace = new StringWriter();
                            thrown.printStackTrace(new PrintWriter(trace));
                            assertThat(trace.toString()).doesNotContain("SECR");
                        });
    }
}
