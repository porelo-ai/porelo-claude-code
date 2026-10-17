package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.ContainerFile;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;

/** Tests for {@link ClaudeContainerSetup}. */
class ClaudeContainerSetupTest {

    private static final String TOKEN = "porelo_pt_example";

    private List<ContainerFile> files(String type) {
        return withToken(TOKEN, type);
    }

    private List<ContainerFile> withToken(String token, String type) {
        return new ClaudeContainerSetup()
                .files(
                        new org.fuin.porelo.agent.api.SetupContext(
                                token, type, "/workspace", "", "anthropic"));
    }

    private List<String> paths(List<ContainerFile> files) {
        return files.stream().map(ContainerFile::path).toList();
    }

    private Map<?, ?> parse(ContainerFile file) {
        return (Map<?, ?>) Json.parse(file.content());
    }

    private ContainerFile named(String path, String type) {
        return files(type).stream().filter(f -> f.path().equals(path)).findFirst().orElseThrow();
    }

    @Test
    void answersTheQuestionsAFreshContainerWouldBeAsked() {

        // Measured: without these the CLI runs its first-run wizard, and the login menu it offers
        // there is part of the wizard rather than any check of the credential.
        final Map<?, ?> config = parse(named(ClaudeFirstRun.FILE, "oauth"));

        assertThat(config.get("hasCompletedOnboarding")).isEqualTo(Boolean.TRUE);
        assertThat(
                        ((Map<?, ?>) ((Map<?, ?>) config.get("projects")).get("/workspace"))
                                .get("hasTrustDialogAccepted"))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void answersTheBypassModeWarningWhicheverWayTheModeIsEntered() {

        // Measured: in bypass mode the CLI opens with a full-screen warning whose default answer
        // is "No, exit", whether the mode comes from the flag or from defaultMode. It is not a
        // permission prompt, so no flag answers it, and unattended nobody can.
        final Map<?, ?> settings = parse(named(ClaudeSettings.FILE, "oauth"));

        assertThat(((Map<?, ?>) settings.get("permissions")).get("defaultMode"))
                .isEqualTo("bypassPermissions");
        assertThat(settings.get("skipDangerousModePermissionPrompt")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void keepsTheCliAtThePinnedVersion() {

        // Measured on 2.1.267: without this the CLI downloaded 2.1.276 within 90 seconds and
        // announced "Update installed". DISABLE_UPDATES also refuses 'claude update'.
        final Map<?, ?> settings = parse(named(ClaudeSettings.FILE, "oauth"));

        assertThat(((Map<?, ?>) settings.get("env")).get("DISABLE_UPDATES")).isEqualTo("1");
    }

    @Test
    void letsAModelTheCliDoesNotKnowUseItsOwnWindow() {

        // Measured on 2.1.267 with z-ai/glm-5.3-flash over OpenRouter: the CLI says the model
        // "isn't
        // described by this version's model catalog" and holds the session to 200k tokens. With
        // this
        // set it says nothing and leaves the window to the provider, as before 2.1.267.
        final Map<?, ?> settings = parse(named(ClaudeSettings.FILE, "oauth"));

        assertThat(
                        ((Map<?, ?>) settings.get("env"))
                                .get("CLAUDE_CODE_DISABLE_UNKNOWN_MODEL_WINDOW_ENFORCEMENT"))
                .isEqualTo("1");
    }

    @Test
    void keepsTheSettingsReadableRatherThanSecret() {

        // Configuration, not a credential: nothing in it is worth hiding, and marking it
        // owner-only would say it was.
        assertThat(named(ClaudeSettings.FILE, "oauth").ownerOnly()).isFalse();
    }

    @Test
    void writesNoCredentialFileForEitherKind() {

        // Measured on 2.1.267: the CLI authenticates from the variable its kind names, and a
        // .credentials.json holding the same token gave no key and no token with the variable gone.
        // A file nothing reads is only a second copy of the token.
        for (final String kind : List.of("api-key", "oauth")) {
            assertThat(paths(files(kind)))
                    .containsExactlyInAnyOrder(ClaudeFirstRun.FILE, ClaudeSettings.FILE);
        }
    }

    @Test
    void approvesTheTasksOwnApiKeySoTheCliDoesNotAskAboutIt() {

        // Measured on 2.1.267: started with ANTHROPIC_API_KEY set, the CLI asks whether to use
        // "a custom API key" and recommends No - which refuses the only credential the task has.
        // Its last 20 characters under customApiKeyResponses.approved remove the dialog.
        final String token = "porelo_pt_0123456789abcdefghijklmnopqrstuvwxyz";
        final ContainerFile file = namedIn(withToken(token, "api-key"), ClaudeFirstRun.FILE);

        final Map<?, ?> responses = (Map<?, ?>) parse(file).get("customApiKeyResponses");
        assertThat(responses.get("approved"))
                .isEqualTo(List.of(token.substring(token.length() - 20)));
        // Part of a token is still part of a token.
        assertThat(file.ownerOnly()).isTrue();
    }

    @Test
    void approvesNothingWithoutAnApiKeyToApprove() {

        // No key in the environment, no dialog to answer - and nothing of a token in a file
        // that is readable.
        for (final ContainerFile file :
                List.of(
                        namedIn(withToken("", "api-key"), ClaudeFirstRun.FILE),
                        namedIn(withToken(TOKEN, "oauth"), ClaudeFirstRun.FILE))) {
            assertThat(parse(file).get("customApiKeyResponses")).isNull();
            assertThat(file.ownerOnly()).isFalse();
        }
    }

    private ContainerFile namedIn(List<ContainerFile> files, String path) {
        return files.stream().filter(f -> f.path().equals(path)).findFirst().orElseThrow();
    }

    @Test
    void placesWhatIsNotACredentialForATaskThatHasNone() {

        // Measured on a VM: none of the files were placed without a token, so a person
        // logging in inside the container met every dialog these files exist to answer.
        assertThat(paths(withToken("", "oauth")))
                .contains(ClaudeFirstRun.FILE, ClaudeSettings.FILE);
    }

    @Test
    void refusesACredentialKindThisProviderDoesNotStore() {

        // An unknown kind would reach the CLI in the default variable and fail looking exactly like
        // a wrong key, which sends whoever reads it looking at the credential, not at the mistake.
        assertThatThrownBy(() -> files("subscription"))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("subscription")
                .hasMessageContaining("api-key")
                .hasMessageContaining("oauth");
    }

    @Test
    void setsUpBothKindsThisProviderDoesStore() {

        // The other half of the refusal above: the two known kinds still set a task up, so the
        // guard cannot be satisfied by rejecting everything.
        assertThat(files("api-key")).isNotEmpty();
        assertThat(files("oauth")).isNotEmpty();
    }
}
