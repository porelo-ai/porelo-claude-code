package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The account file's shape is what Claude Code 2.1.311 writes, measured. */
class ClaudeSignInFactsTest {

    private static final String SECRET = "fixture-secret-value";
    private static final String ACCOUNT = "person@example.org";
    private static final long EXPIRES = 1_800_000_000_000L;
    private static final Instant CHANGED = Instant.parse("2026-11-01T10:00:00Z");

    @TempDir Path home;

    private Path config;

    @BeforeEach
    void createTheConfigDirectory() throws IOException {
        config = Files.createDirectory(home.resolve(".claude"));
    }

    private List<ClaudeSignInFacts> facts() {
        final ClaudeFindings findings =
                new ClaudeFindings(home.resolve("managed.json"), home.resolve("profiles"), false);
        return ClaudeSignInFacts.of(findings.find(config), config);
    }

    private void subscription(final long expiresAt) throws IOException {
        final Path file =
                Files.writeString(
                        config.resolve(".credentials.json"),
                        "{\"claudeAiOauth\":{\"accessToken\":\""
                                + SECRET
                                + "\",\"refreshToken\":\""
                                + SECRET
                                + "\",\"expiresAt\":"
                                + expiresAt
                                + ",\"scopes\":[\"user:inference\"]}}");
        Files.setLastModifiedTime(file, FileTime.from(CHANGED));
    }

    private void accountFile(final String content) throws IOException {
        Files.setLastModifiedTime(
                Files.writeString(home.resolve(".claude.json"), content), FileTime.from(CHANGED));
    }

    private static String account(final String address) {
        return "{\"oauthAccount\":{\"emailAddress\":\"" + address + "\"}}";
    }

    // Prevents the person choosing a sign-in blind: which account, how fresh, how long it holds.
    @Test
    void aSubscriptionSaysItsAccountExpiryAndLastChange() throws IOException {
        subscription(EXPIRES);
        accountFile(account(ACCOUNT));

        assertThat(facts())
                .singleElement()
                .satisfies(
                        facts -> {
                            assertThat(facts.finding().kind()).isEqualTo(Kind.SUBSCRIPTION);
                            assertThat(facts.finding().importable()).as("carriable").isTrue();
                            assertThat(facts.account()).isEqualTo(ACCOUNT);
                            assertThat(facts.expires()).isEqualTo(Instant.ofEpochMilli(EXPIRES));
                            assertThat(facts.changed()).isEqualTo(CHANGED);
                        });
    }

    // Prevents a Console key from claiming an expiry it does not have.
    @Test
    void aConsoleKeySaysItsAccountAndNoExpiry() throws IOException {
        accountFile(
                "{\"primaryApiKey\":\""
                        + SECRET
                        + "\",\"oauthAccount\":{\"emailAddress\":\""
                        + ACCOUNT
                        + "\"}}");

        assertThat(facts())
                .singleElement()
                .satisfies(
                        facts -> {
                            assertThat(facts.finding().kind()).isEqualTo(Kind.API_KEY);
                            assertThat(facts.account()).isEqualTo(ACCOUNT);
                            assertThat(facts.expires()).isNull();
                            assertThat(facts.changed()).isEqualTo(CHANGED);
                        });
    }

    // Prevents a guess: without an account file, or one naming none, there is no account.
    @Test
    void noAccountFileIsNoAccount() throws IOException {
        subscription(EXPIRES);

        assertThat(facts()).singleElement().extracting(ClaudeSignInFacts::account).isNull();
    }

    // Prevents something that is not an address from reaching the person's dialog as one.
    @Test
    void anAccountWithAControlCharacterIsNoAccount() throws IOException {
        subscription(EXPIRES);
        accountFile(account("person\\u001b[2J@example.org"));

        assertThat(facts()).singleElement().extracting(ClaudeSignInFacts::account).isNull();
    }

    // Prevents a zero or missing expiry from reading as one in 1970.
    @Test
    void noExpiryHeldIsNoExpiry() throws IOException {
        subscription(0);

        assertThat(facts()).singleElement().extracting(ClaudeSignInFacts::expires).isNull();
    }

    // Prevents a route an import cannot take from being offered with facts to carry.
    @Test
    void aSignInInTheSettingsHasNoFacts() throws IOException {
        Files.writeString(
                config.resolve("settings.json"),
                "{\"env\":{\"ANTHROPIC_API_KEY\":\"" + SECRET + "\"}}");

        assertThat(facts()).isEmpty();
    }

    // Prevents a file Claude Code cannot use from being described as a sign-in.
    @Test
    void aCredentialsFileWithoutASignInHasNoFacts() throws IOException {
        Files.writeString(config.resolve(".credentials.json"), "{\"other\":1}");

        assertThat(facts()).isEmpty();
    }

    // Prevents a credential's value leaving the adapter in what is shown to the person.
    @Test
    void noFactCarriesACredentialsValue() throws IOException {
        subscription(EXPIRES);
        accountFile(
                "{\"primaryApiKey\":\""
                        + SECRET
                        + "\",\"oauthAccount\":{\"emailAddress\":\""
                        + ACCOUNT
                        + "\"}}");

        final List<ClaudeSignInFacts> facts = facts();

        assertThat(facts).as("both routes were read").hasSize(2);
        assertThat(facts.toString()).as("no value in any field").doesNotContain(SECRET);
    }
}
