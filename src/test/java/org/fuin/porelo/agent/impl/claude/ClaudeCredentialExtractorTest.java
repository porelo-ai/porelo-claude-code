package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClaudeCredentialExtractorTest {

    private static final String TOKEN = "porelo_pt_round-trip";

    @TempDir
    Path home;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "oauth   | {\"claudeAiOauth\":{\"accessToken\":\"%s\",\"refreshToken\":\"r\",\"expiresAt\":1,\"scopes\":[\"user:inference\"]}}",
            "api-key | {\"apiKey\":\"%s\"}"})
    void readsBothKindsAsALoginWritesThem(final String kind, final String shape) throws IOException {

        // What 'claude' writes when a person logs in on the machine, which is what the vault imports.
        Files.writeString(home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE), shape.formatted(TOKEN));

        final Credential credential = new ClaudeCredentialExtractor().extract(home).orElseThrow();

        assertThat(credential.type()).isEqualTo(kind);
        assertThat(credential.secret()).isEqualTo(TOKEN);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{not json                                   | Cannot parse",
            "[1, 2]                                      | is not a JSON object",
            "{\"claudeAiOauth\": {\"refreshToken\": \"r\"}}  | has an OAuth block with no access token",
            "{\"claudeAiOauth\": {\"accessToken\": \" \"}}   | has an OAuth block with no access token",
            "{\"apiKey\": \"  \"}                          | holds neither an OAuth token nor an API key",
            "{}                                          | holds neither an OAuth token nor an API key"})
    void saysWhatIsWrongWithAFileItCannotUse(final String content, final String reason) throws IOException {

        // Each is "logged in but unreadable", which must not read as "not logged in".
        Files.writeString(home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE), content.strip());

        assertThatThrownBy(() -> new ClaudeCredentialExtractor().extract(home))
                .isInstanceOf(AgentException.class).hasMessageContaining(reason.strip());
    }

    @Test
    void carriesWhatARenewalOnTheHostNeeds() throws IOException {

        // A subscription login: Porelo renews it from the refresh token at Claude Code's token URL, as
        // its client, so the task never holds the refresh token.
        Files.writeString(home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"claudeAiOauth\":{\"accessToken\":\"t\",\"refreshToken\":\"r\",\"expiresAt\":1790000000000}}");

        assertThat(new ClaudeCredentialExtractor().extract(home).orElseThrow().attributes())
                .containsEntry("refreshToken", "r")
                .containsEntry("expiresAt", "1790000000000")
                .containsEntry("tokenUrl", "https://platform.claude.com/v1/oauth/token")
                .containsEntry("clientId", "9d1c250a-e61b-44d9-88ed-5944d1962f5e");
    }

    @Test
    void namesNoRenewalWithoutARefreshToken() throws IOException {

        // Nothing to renew with, so no endpoint either: an access token alone is used until it ends.
        Files.writeString(home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"claudeAiOauth\":{\"accessToken\":\"t\"}}");

        assertThat(new ClaudeCredentialExtractor().extract(home).orElseThrow().attributes())
                .doesNotContainKeys("tokenUrl", "clientId");
    }

    @Test
    void ignoresAnExpiryThatIsNotANumber() throws IOException {

        Files.writeString(home.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE),
                "{\"claudeAiOauth\": {\"accessToken\": \"t\", \"expiresAt\": \"soon\"}}");

        assertThat(new ClaudeCredentialExtractor().extract(home).orElseThrow().attributes())
                .doesNotContainKey("expiresAt");
    }
}
