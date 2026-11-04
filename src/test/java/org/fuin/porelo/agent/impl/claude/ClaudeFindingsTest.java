package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The cases are what Claude Code 2.1.311 itself reports for these files, measured. */
class ClaudeFindingsTest {

    private static final String SECRET = "fixture-secret-value";
    private static final String SUBSCRIPTION =
            "{\"claudeAiOauth\":{\"accessToken\":\""
                    + SECRET
                    + "\",\"refreshToken\":\""
                    + SECRET
                    + "\",\"expiresAt\":1,\"scopes\":[\"user:inference\",\"user:profile\"]}}";

    @TempDir Path home;

    private Path config;

    @BeforeEach
    void createTheConfigDirectory() throws IOException {
        config = Files.createDirectory(home.resolve(".claude"));
    }

    private List<Finding> find() throws IOException {
        return new ClaudeFindings(home.resolve("managed.json"), home.resolve("profiles"), false)
                .find(config);
    }

    private List<Finding> signIns(final List<Finding> findings) {
        return findings.stream().filter(finding -> finding.what() == What.SIGN_IN).toList();
    }

    private List<Finding> signIns() throws IOException {
        return signIns(find());
    }

    private void credentials(final String content) throws IOException {
        Files.writeString(config.resolve(".credentials.json"), content);
    }

    private void settings(final String content) throws IOException {
        Files.writeString(config.resolve("settings.json"), content);
    }

    private void settingsEnv(final String name, final String value) throws IOException {
        settings("{\"env\":{\"" + name + "\":\"" + value + "\"}}");
    }

    private String path(final String file) {
        return config.resolve(file).toAbsolutePath().toString();
    }

    private static Finding signedIn(
            final String path, final boolean importable, final String provider, final Kind kind) {
        return new Finding(What.SIGN_IN, path, SignedIn.SIGNED_IN, importable, provider, kind);
    }

    private static Finding notSignedIn(final String path) {
        return new Finding(What.SIGN_IN, path, SignedIn.NOT_SIGNED_IN, null, null, null);
    }

    private static Finding cannotSay(final String path) {
        return new Finding(What.SIGN_IN, path, SignedIn.CANNOT_SAY, null, null, null);
    }

    // Prevents a start from offering nothing to the person whose sign-in is right there.
    @Test
    void aSubscriptionIsImportableAndAnthropicsEvenPastItsExpiry() throws IOException {
        credentials(SUBSCRIPTION);

        assertThat(signIns())
                .as("Claude Code treats an expired token as signed in, since it renews it")
                .containsExactly(
                        signedIn(path(".credentials.json"), true, "anthropic", Kind.SUBSCRIPTION));
    }

    // Prevents a start from choosing an agent whose tool would itself refuse to run.
    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"apiKey\":\"fixture-secret-value\"}",
                "{\"claudeAiOauth\":{\"refreshToken\":\"fixture-secret-value\"}}",
                "{\"claudeAiOauth\":{\"accessToken\":\"\",\"scopes\":[\"user:inference\"]}}",
                "{\"claudeAiOauth\":{\"accessToken\":\"fixture-secret-value\"}}",
                "{\"claudeAiOauth\":{\"accessToken\":\"x\",\"scopes\":[\"user:profile\"]}}",
                "{\"other\":1}",
                "[1]",
                "not json"
            })
    void aCredentialsFileClaudeCannotUseIsAFindingNotAFailure(final String content)
            throws IOException {
        credentials(content);

        assertThat(signIns())
                .as("what Claude Code itself calls not signed in, with the file named")
                .containsExactly(notSignedIn(path(".credentials.json")));
    }

    // Prevents an empty account from reading as an agent found.
    @Test
    void nothingThereIsNoSignInFinding() throws IOException {
        assertThat(find())
                .as("only the directory itself is found")
                .containsExactly(
                        new Finding(
                                What.CONFIGURATION,
                                config.toAbsolutePath().toString(),
                                null,
                                null,
                                null,
                                null));
    }

    // Prevents an account that never ran Claude Code from reading as one that has.
    @Test
    void noDirectoryIsNoFindingAtAll() throws IOException {
        Files.delete(config);

        assertThat(find()).as("nothing is there").isEmpty();
    }

    // Prevents a Console key from being missed, the route that leaves no credentials file.
    @Test
    void aConsoleKeyBesideTheDirectoryIsAnImportableApiKey() throws IOException {
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"" + SECRET + "\"}");

        assertThat(signIns())
                .containsExactly(
                        signedIn(
                                home.resolve(".claude.json").toAbsolutePath().toString(),
                                true,
                                "anthropic",
                                Kind.API_KEY));
    }

    // Prevents the wrong route from deciding: Porelo takes the first one signed in.
    @Test
    void aSubscriptionComesBeforeAConsoleKey() throws IOException {
        credentials(SUBSCRIPTION);
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"" + SECRET + "\"}");

        assertThat(signIns())
                .extracting(Finding::kind)
                .as("Claude Code prefers the subscription")
                .containsExactly(Kind.SUBSCRIPTION, Kind.API_KEY);
    }

    // Prevents reading the account's file when the person moved the directory: it does not count.
    @Test
    void aMovedDirectoryKeepsItsAccountFileInside() throws IOException {
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"x\"}");
        final Path moved = Files.createDirectory(home.resolve("work"));
        Files.writeString(moved.resolve(".claude.json"), "{\"primaryApiKey\":\"y\"}");

        final List<Finding> found =
                signIns(
                        new ClaudeFindings(home.resolve("m"), home.resolve("p"), false)
                                .find(moved));

        assertThat(found)
                .extracting(Finding::path)
                .as("the file inside the moved directory, not the one in the home")
                .containsExactly(moved.resolve(".claude.json").toAbsolutePath().toString());
    }

    // Prevents an import being offered for a route it cannot take, and the wrong kind being named.
    @Test
    void aKeyInTheSettingsComesFirstAndCannotBeImported() throws IOException {
        credentials(SUBSCRIPTION);
        settingsEnv("ANTHROPIC_API_KEY", SECRET);

        assertThat(signIns())
                .as("the key wins over the subscription in Claude Code")
                .containsExactly(
                        signedIn(path("settings.json"), false, "anthropic", Kind.API_KEY),
                        signedIn(path(".credentials.json"), true, "anthropic", Kind.SUBSCRIPTION));
    }

    // Prevents a token in the settings from reading as not signed in.
    @Test
    void aTokenInTheSettingsIsSignedIn() throws IOException {
        settingsEnv("CLAUDE_CODE_OAUTH_TOKEN", SECRET);
        assertThat(signIns())
                .containsExactly(
                        signedIn(path("settings.json"), false, "anthropic", Kind.SUBSCRIPTION));

        settingsEnv("ANTHROPIC_AUTH_TOKEN", SECRET);
        assertThat(signIns())
                .as("a bearer token says nothing of its kind")
                .containsExactly(signedIn(path("settings.json"), false, "anthropic", null));
    }

    // Prevents Find from running a command the person's settings name, at every start.
    @Test
    void aHelperIsReportedAndNeverRun() throws IOException {
        final Path marker = home.resolve("helper-ran");
        settings("{\"apiKeyHelper\":\"touch " + marker + "\"}");

        assertThat(signIns())
                .containsExactly(signedIn(path("settings.json"), false, "anthropic", Kind.API_KEY));
        assertThat(marker).as("the helper's command must not have run").doesNotExist();
    }

    // Prevents a task being started against Anthropic for a person who works through another.
    @ParameterizedTest
    @ValueSource(
            strings = {
                "CLAUDE_CODE_USE_BEDROCK",
                "CLAUDE_CODE_USE_VERTEX",
                "CLAUDE_CODE_USE_FOUNDRY",
                "CLAUDE_CODE_USE_ANTHROPIC_AWS",
                "CLAUDE_CODE_USE_ANTHROPIC_GOOGLE_CLOUD",
                "CLAUDE_CODE_USE_MANTLE"
            })
    void aProviderSwitchIsNotAnthropic(final String name) throws IOException {
        credentials(SUBSCRIPTION);
        settingsEnv(name, "1");

        assertThat(signIns())
                .as("no provider of this agent, and the subscription beneath it not offered")
                .containsExactly(
                        signedIn(path("settings.json"), false, null, null),
                        signedIn(path(".credentials.json"), false, null, Kind.SUBSCRIPTION));
    }

    // Prevents a switch turned off from hiding the sign-in that is really used.
    @ParameterizedTest
    @ValueSource(strings = {"0", "false", "no", "off", "2", "x", ""})
    void aSwitchOffIsNoSwitch(final String value) throws IOException {
        credentials(SUBSCRIPTION);
        settingsEnv("CLAUDE_CODE_USE_BEDROCK", value);

        assertThat(signIns())
                .containsExactly(
                        signedIn(path(".credentials.json"), true, "anthropic", Kind.SUBSCRIPTION));
    }

    // Prevents a switch on from being missed for the way it is written.
    @ParameterizedTest
    @ValueSource(strings = {"1", "true", "TRUE", "yes", "on"})
    void aSwitchOnIsASwitch(final String value) throws IOException {
        settingsEnv("CLAUDE_CODE_USE_BEDROCK", value);

        assertThat(signIns()).containsExactly(signedIn(path("settings.json"), false, null, null));
    }

    // Prevents the same miss for the number, which the settings also take.
    @Test
    void aSwitchAsTheNumberOneIsASwitch() throws IOException {
        settings("{\"env\":{\"CLAUDE_CODE_USE_VERTEX\":1}}");

        assertThat(signIns()).containsExactly(signedIn(path("settings.json"), false, null, null));
    }

    // Prevents a name that only looks like a switch from turning a subscription away.
    @ParameterizedTest
    @ValueSource(strings = {"CLAUDE_CODE_USE_GATEWAY", "ANTHROPIC_BEDROCK_BASE_URL"})
    void aNameClaudeDoesNotTakeAsASwitchIsNone(final String name) throws IOException {
        credentials(SUBSCRIPTION);
        settingsEnv(name, "1");

        assertThat(signIns())
                .containsExactly(
                        signedIn(path(".credentials.json"), true, "anthropic", Kind.SUBSCRIPTION));
    }

    // Prevents sending a task to Anthropic when the person's requests go elsewhere, which Claude
    // Code's own status does not show.
    @Test
    void anotherBaseUrlIsNotAnthropic() throws IOException {
        credentials(SUBSCRIPTION);
        settingsEnv("ANTHROPIC_BASE_URL", "https://gateway.example.org");

        assertThat(signIns())
                .containsExactly(
                        signedIn(path(".credentials.json"), false, null, Kind.SUBSCRIPTION));
    }

    // Prevents broken settings from hiding a sign-in that works: Claude Code ignores them.
    @ParameterizedTest
    @ValueSource(strings = {"not json", "[1]", "{\"env\":\"x\"}"})
    void settingsClaudeIgnoresChangeNothing(final String content) throws IOException {
        credentials(SUBSCRIPTION);
        settings(content);

        assertThat(signIns())
                .containsExactly(
                        signedIn(path(".credentials.json"), true, "anthropic", Kind.SUBSCRIPTION));
    }

    // Prevents "anthropic" being claimed while a file that may say otherwise could not be read.
    @Test
    void settingsThatCannotBeReadLeaveTheProviderOpen() throws IOException {
        credentials(SUBSCRIPTION);
        Files.createDirectory(config.resolve("settings.json"));

        assertThat(signIns())
                .containsExactly(
                        cannotSay(path("settings.json")),
                        signedIn(path(".credentials.json"), true, null, Kind.SUBSCRIPTION));
    }

    // Prevents "not signed in" when a credentials file could not be read.
    @Test
    void aCredentialsFileThatCannotBeReadCannotSay() throws IOException {
        Files.createDirectory(config.resolve(".credentials.json"));

        assertThat(signIns()).containsExactly(cannotSay(path(".credentials.json")));
    }

    // Prevents "not signed in" when the file that may hold a key is broken.
    @Test
    void aBrokenAccountFileCannotSay() throws IOException {
        Files.writeString(home.resolve(".claude.json"), "not json");

        assertThat(signIns())
                .containsExactly(
                        cannotSay(home.resolve(".claude.json").toAbsolutePath().toString()));
    }

    // Prevents "anthropic" being claimed where the machine's managed settings may say otherwise.
    @Test
    void managedSettingsLeaveTheProviderOpen() throws IOException {
        credentials(SUBSCRIPTION);
        final Path managed = Files.writeString(home.resolve("managed.json"), "{}");

        assertThat(signIns())
                .containsExactly(
                        cannotSay(managed.toAbsolutePath().toString()),
                        signedIn(path(".credentials.json"), true, null, Kind.SUBSCRIPTION));
    }

    // Prevents "not signed in" for a Console sign-in without a key, kept as a profile.
    @Test
    void aProfileCannotSay() throws IOException {
        final Path profiles = Files.createDirectories(home.resolve("profiles/configs"));
        Files.writeString(profiles.resolve("default.json"), "{}");

        assertThat(signIns()).containsExactly(cannotSay(profiles.toAbsolutePath().toString()));
    }

    // Prevents an account with nothing from reading as "cannot say" for an empty directory.
    @Test
    void anEmptyProfileDirectoryIsNothing() throws IOException {
        Files.createDirectories(home.resolve("profiles/configs"));

        assertThat(signIns()).as("no profile is there").isEmpty();
    }

    // Prevents "not signed in" on a machine whose login is in the Keychain, never read here.
    @Test
    void withAKeychainNoUsableFileCannotSay() throws IOException {
        final ClaudeFindings onKeychain =
                new ClaudeFindings(home.resolve("m"), home.resolve("p"), true);

        assertThat(signIns(onKeychain.find(config))).containsExactly(cannotSay(null));

        credentials("{}");
        assertThat(signIns(onKeychain.find(config)))
                .as("the login may still be in the Keychain")
                .containsExactly(notSignedIn(path(".credentials.json")), cannotSay(null));

        credentials(SUBSCRIPTION);
        assertThat(signIns(onKeychain.find(config)))
                .as("the fallback file holds it")
                .containsExactly(
                        signedIn(path(".credentials.json"), true, "anthropic", Kind.SUBSCRIPTION));
    }

    private void withoutAccess(final Path directory, final IoAction action) throws IOException {
        final Set<PosixFilePermission> before = Files.getPosixFilePermissions(directory);
        Files.setPosixFilePermissions(directory, Set.of());
        try {
            // The forge's runner is root, which reads through any mode.
            assumeFalse(Files.isReadable(directory), "this account reads through a mode");
            action.run();
        } finally {
            Files.setPosixFilePermissions(directory, before);
        }
    }

    private interface IoAction {
        void run() throws IOException;
    }

    // Prevents "not signed in" when the directory holding the login could not be entered.
    @Test
    void aDirectoryThatCannotBeEnteredCannotSay() throws IOException {
        credentials(SUBSCRIPTION);

        withoutAccess(
                config,
                () ->
                        assertThat(signIns())
                                .as("a denied read is not an absent file")
                                .contains(cannotSay(path(".credentials.json")))
                                .extracting(Finding::signedIn)
                                .containsOnly(SignedIn.CANNOT_SAY));
    }

    // Prevents "anthropic" being claimed when managed settings may sit where they cannot be seen.
    @Test
    void managedSettingsThatCannotBeSeenLeaveTheProviderOpen() throws IOException {
        credentials(SUBSCRIPTION);
        final Path shut = Files.createDirectory(home.resolve("shut"));
        final Path managed = shut.resolve("managed.json");

        withoutAccess(
                shut,
                () ->
                        assertThat(
                                        signIns(
                                                new ClaudeFindings(
                                                                managed, home.resolve("p"), false)
                                                        .find(config)))
                                .containsExactly(
                                        cannotSay(managed.toAbsolutePath().toString()),
                                        signedIn(
                                                path(".credentials.json"),
                                                true,
                                                null,
                                                Kind.SUBSCRIPTION)));
    }

    // Prevents "not signed in" when the profiles could not be listed.
    @Test
    void profilesThatCannotBeListedCannotSay() throws IOException {
        final Path profiles = Files.createDirectory(home.resolve("profiles"));
        final Path configs = profiles.resolve("configs");

        withoutAccess(
                profiles,
                () ->
                        assertThat(signIns())
                                .containsExactly(cannotSay(configs.toAbsolutePath().toString())));
    }

    // Prevents a credential's value leaving the adapter, the one thing Find must never do.
    @Test
    void noFindingCarriesACredentialsValue() throws IOException {
        credentials(SUBSCRIPTION);
        Files.writeString(home.resolve(".claude.json"), "{\"primaryApiKey\":\"" + SECRET + "\"}");
        settings(
                "{\"apiKeyHelper\":\""
                        + SECRET
                        + "\",\"env\":{\"ANTHROPIC_API_KEY\":\""
                        + SECRET
                        + "\",\"ANTHROPIC_AUTH_TOKEN\":\""
                        + SECRET
                        + "\",\"CLAUDE_CODE_OAUTH_TOKEN\":\""
                        + SECRET
                        + "\",\"ANTHROPIC_BASE_URL\":\""
                        + SECRET
                        + "\"}}");

        final List<Finding> found = find();

        assertThat(signIns(found)).as("the guard must have read every route").hasSize(5);
        assertThat(found.toString()).as("no value in any field").doesNotContain(SECRET);
    }

    // Prevents the adapter from answering "cannot say" through Porelo's default for ever.
    @Test
    void theAgentAnswersWithItsOwnFindings() throws IOException {
        credentials(SUBSCRIPTION);

        final List<Finding> found = new ClaudeAgent().finder().find(config);

        assertThat(signIns(found))
                .filteredOn(finding -> finding.signedIn() == SignedIn.SIGNED_IN)
                .first()
                .as("what the file holds, whatever else the machine may add")
                .extracting(Finding::kind)
                .isEqualTo(Kind.SUBSCRIPTION);
    }
}
