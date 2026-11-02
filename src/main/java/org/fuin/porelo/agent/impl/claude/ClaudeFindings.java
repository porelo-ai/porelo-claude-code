package org.fuin.porelo.agent.impl.claude;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.fuin.porelo.agent.api.Finder;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.fuin.porelo.wire.Json;
import org.fuin.porelo.wire.JsonException;
import org.jspecify.annotations.Nullable;

/**
 * What Claude Code finds of itself on the host, each route in the order Claude Code takes it.
 *
 * <p>Only reads files: it runs no helper, starts nothing, reaches no network, and reads no
 * environment, since an agent the daemon spawns does not share the person's shell. What each file
 * means is measured on 2.1.311 with {@code claude auth status}.
 */
final class ClaudeFindings implements Finder {

    private static final String PROVIDER = "anthropic";
    private static final String SETTINGS_FILE = "settings.json";

    /** Each names a provider that is not this agent's, whichever one. */
    private static final List<String> PROVIDER_SWITCHES =
            List.of(
                    "CLAUDE_CODE_USE_BEDROCK",
                    "CLAUDE_CODE_USE_VERTEX",
                    "CLAUDE_CODE_USE_FOUNDRY",
                    "CLAUDE_CODE_USE_ANTHROPIC_AWS",
                    "CLAUDE_CODE_USE_ANTHROPIC_GOOGLE_CLOUD",
                    "CLAUDE_CODE_USE_MANTLE");

    private static final Set<String> SWITCH_ON = Set.of("1", "true", "yes", "on");

    private final Path managedSettings;
    private final Path profiles;
    private final boolean keychain;

    ClaudeFindings(final Path managedSettings, final Path profiles, final boolean keychain) {
        this.managedSettings = managedSettings;
        this.profiles = profiles;
        this.keychain = keychain;
    }

    /** Returns the findings of the machine this runs on. */
    static ClaudeFindings onThisMachine() {
        final boolean macOs =
                System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
        return new ClaudeFindings(
                Path.of(
                        macOs
                                ? "/Library/Application Support/ClaudeCode/managed-settings.json"
                                : "/etc/claude-code/managed-settings.json"),
                Path.of(System.getProperty("user.home", "/"), ".config", "anthropic"),
                macOs);
    }

    @Override
    public List<Finding> find(final Path configDirectory) {

        final Path directory = configDirectory.toAbsolutePath();
        final List<Finding> findings = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            findings.add(
                    new Finding(What.CONFIGURATION, directory.toString(), null, null, null, null));
        }

        final JsonFile settings = JsonFile.read(directory.resolve(SETTINGS_FILE));
        final Map<?, ?> env = settings.get("env") instanceof Map<?, ?> map ? map : Map.of();
        final Routes routes = routes(settings, env);

        if (mayExist(managedSettings)) {
            findings.add(cannotSay(managedSettings));
        }
        if (settings.state() == JsonFile.State.UNREADABLE) {
            findings.add(cannotSay(settings.path()));
        }
        if (switchedAway(env)) {
            findings.add(routes.fromSettings(settings.path(), null));
        }
        findSettingsRoutes(settings, env, routes, findings);
        findCredentialsFile(directory, routes, findings);
        findConsoleKey(directory, routes, findings);
        final Path profileConfigs = profiles.resolve("configs");
        if (holdsAnything(profileConfigs)) {
            findings.add(cannotSay(profileConfigs));
        }
        return List.copyOf(findings);
    }

    private Routes routes(final JsonFile settings, final Map<?, ?> env) {
        final boolean elsewhere = switchedAway(env) || isSet(env.get("ANTHROPIC_BASE_URL"));
        final boolean open =
                settings.state() == JsonFile.State.UNREADABLE || mayExist(managedSettings);
        return new Routes(elsewhere, open);
    }

    private static boolean switchedAway(final Map<?, ?> env) {
        return PROVIDER_SWITCHES.stream().anyMatch(name -> isOn(env.get(name)));
    }

    private static void findSettingsRoutes(
            final JsonFile settings,
            final Map<?, ?> env,
            final Routes routes,
            final List<Finding> findings) {

        if (isSet(env.get("ANTHROPIC_AUTH_TOKEN"))) {
            findings.add(routes.fromSettings(settings.path(), null));
        }
        if (isSet(env.get("ANTHROPIC_API_KEY")) || isSet(settings.get("apiKeyHelper"))) {
            findings.add(routes.fromSettings(settings.path(), Kind.API_KEY));
        }
        if (isSet(env.get("CLAUDE_CODE_OAUTH_TOKEN"))) {
            findings.add(routes.fromSettings(settings.path(), Kind.SUBSCRIPTION));
        }
    }

    private void findCredentialsFile(
            final Path directory, final Routes routes, final List<Finding> findings) {

        final JsonFile file =
                JsonFile.read(directory.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE));
        switch (file.state()) {
            case ABSENT -> {}
            case UNREADABLE -> findings.add(cannotSay(file.path()));
            case BROKEN, OBJECT -> {
                if (ClaudeCredentialExtractor.holdsSubscription(file.root())) {
                    findings.add(routes.fromFile(file.path(), Kind.SUBSCRIPTION));
                    return;
                }
                findings.add(
                        new Finding(
                                What.SIGN_IN,
                                file.path().toString(),
                                SignedIn.NOT_SIGNED_IN,
                                null,
                                null,
                                null));
            }
        }
        if (keychain) {
            findings.add(new Finding(What.SIGN_IN, null, SignedIn.CANNOT_SAY, null, null, null));
        }
    }

    private static void findConsoleKey(
            final Path directory, final Routes routes, final List<Finding> findings) {

        final JsonFile file = JsonFile.read(ClaudeCredentialExtractor.accountFile(directory));
        switch (file.state()) {
            case ABSENT -> {}
            case UNREADABLE, BROKEN -> findings.add(cannotSay(file.path()));
            case OBJECT -> {
                if (isSet(file.get(ClaudeCredentialExtractor.CONSOLE_KEY))) {
                    findings.add(routes.fromFile(file.path(), Kind.API_KEY));
                }
            }
        }
    }

    /** A path that cannot be looked at may be there, so only one known absent counts as absent. */
    private static boolean mayExist(final Path path) {
        return !Files.notExists(path);
    }

    /** A directory that cannot be listed may hold something, so it counts as holding it. */
    private static boolean holdsAnything(final Path directory) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findAny().isPresent();
        } catch (NoSuchFileException ex) {
            return false;
        } catch (IOException | UncheckedIOException ex) {
            return true;
        }
    }

    private static Finding cannotSay(final Path path) {
        return new Finding(
                What.SIGN_IN,
                path.toAbsolutePath().toString(),
                SignedIn.CANNOT_SAY,
                null,
                null,
                null);
    }

    private static boolean isSet(final @Nullable Object value) {
        return value instanceof String text && !text.isBlank();
    }

    private static boolean isOn(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.doubleValue() == 1;
        }
        return value != null && SWITCH_ON.contains(value.toString().toLowerCase(Locale.ROOT));
    }

    /**
     * What every route shares: whether requests go somewhere that is no provider of this agent, and
     * whether a file that may say so could not be read.
     */
    private record Routes(boolean elsewhere, boolean open) {

        Finding fromSettings(final Path path, final @Nullable Kind kind) {
            return signedIn(path, false, kind);
        }

        Finding fromFile(final Path path, final Kind kind) {
            return signedIn(path, !elsewhere, kind);
        }

        private Finding signedIn(
                final Path path, final boolean importable, final @Nullable Kind kind) {
            return new Finding(
                    What.SIGN_IN,
                    path.toString(),
                    SignedIn.SIGNED_IN,
                    importable,
                    elsewhere || open ? null : PROVIDER,
                    kind);
        }
    }

    /** A JSON file, read once; a value it holds never leaves this class. */
    private record JsonFile(Path path, State state, Map<?, ?> root) {

        enum State {
            ABSENT,
            UNREADABLE,
            BROKEN,
            OBJECT
        }

        static JsonFile read(final Path path) {
            final String content;
            try {
                content = Files.readString(path, StandardCharsets.UTF_8);
            } catch (NoSuchFileException ex) {
                return new JsonFile(path, State.ABSENT, Map.of());
            } catch (IOException ex) {
                return new JsonFile(path, State.UNREADABLE, Map.of());
            }
            try {
                return Json.parse(content) instanceof Map<?, ?> map
                        ? new JsonFile(path, State.OBJECT, map)
                        : new JsonFile(path, State.BROKEN, Map.of());
            } catch (JsonException ex) {
                return new JsonFile(path, State.BROKEN, Map.of());
            }
        }

        @Nullable Object get(final String key) {
            return root.get(key);
        }
    }
}
