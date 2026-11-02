package org.fuin.porelo.agent.impl.claude;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Credential;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.wire.Json;
import org.fuin.porelo.wire.JsonException;
import org.jspecify.annotations.Nullable;

/**
 * Reads the OAuth token Claude writes at login.
 *
 * <p>Neither of the agent API's two shapes fits: the token sits under a nested key, and the refresh
 * token and expiry beside it are worth keeping, together with where and as whom Claude Code renews
 * them. So this is an override - and the point of the structure is that the override lives here, in
 * Claude's own module, rather than as an entry in a name-keyed registry that every other agent has
 * to be looked up in.
 *
 * <p>An API key, when the operator used one instead, is a plain field at the top level. Both are
 * handled, because the vault needs to know which it got: they go into different environment
 * variables, and sending one as the other fails in a way that looks like a bad key.
 */
public class ClaudeCredentialExtractor implements CredentialExtractor {

    /** File Claude writes its credentials into. */
    static final String CREDENTIALS_FILE = ".credentials.json";

    /** Where a Console sign-in keeps its API key, in the account file. */
    static final String CONSOLE_KEY = "primaryApiKey";

    private static final String ACCOUNT_FILE = ".claude.json";
    private static final String DEFAULT_DIRECTORY = ".claude";
    private static final String INFERENCE_SCOPE = "user:inference";

    /** The one provider what Claude Code keeps here is for. */
    private static final String PROVIDER = "anthropic";

    /** Credential type for an OAuth token. */
    static final String OAUTH = "oauth";

    /** Credential type for an API key. */
    static final String API_KEY = "api-key";

    /**
     * Where Claude Code renews a subscription login, with {@code grant_type=refresh_token}. Read
     * from the pinned 2.1.267 binary, in the block of its production endpoints beside the authorize
     * URLs; its development block names {@code localhost} instead. update.yml checks the pinned CLI
     * still names it.
     */
    static final String TOKEN_URL = "https://platform.claude.com/v1/oauth/token";

    /**
     * The OAuth client Claude Code signs in as, which a renewal has to name. From the same
     * production block of 2.1.267; the other client id in the binary belongs to its {@code
     * localhost} block. update.yml checks the pinned CLI still names it.
     */
    static final String CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";

    @Override
    public Optional<Credential> extract(Path configDirectory, @Nullable String provider) {
        if (provider == null) {
            return extract(configDirectory);
        }
        return PROVIDER.equals(provider)
                ? extract(configDirectory).map(found -> found.forProvider(PROVIDER))
                : Optional.empty();
    }

    @Override
    public Optional<Credential> extract(Path configDirectory) {

        final Path file = configDirectory.resolve(CREDENTIALS_FILE);
        if (!Files.isRegularFile(file)) {
            return consoleKey(configDirectory);
        }

        final Map<?, ?> root;
        try {
            root = readObject(file);
        } catch (AgentException ex) {
            return readableConsoleKey(configDirectory).map(Optional::of).orElseThrow(() -> ex);
        }
        // Claude Code would use the key here, so an import must not take what it ignores.
        if (!holdsSubscription(root)) {
            final Optional<Credential> consoleKey = readableConsoleKey(configDirectory);
            if (consoleKey.isPresent()) {
                return consoleKey;
            }
        }
        if (root.get("claudeAiOauth") instanceof Map<?, ?> oauth) {
            return oauth(file, oauth);
        }
        if (root.get("apiKey") instanceof String apiKey && !apiKey.isBlank()) {
            return Optional.of(Credential.of(API_KEY, apiKey));
        }
        // The file exists but holds neither. Saying so beats reporting "not logged in", which
        // would send the operator to run a login that has already succeeded.
        throw new AgentException(file + " holds neither an OAuth token nor an API key");
    }

    /**
     * Returns the file holding a Console sign-in's key: beside the default directory, inside one
     * the person moved, as Claude Code 2.1.311 was measured to read it.
     *
     * @param configDirectory Claude Code's configuration directory.
     * @return Path of its account file.
     */
    static Path accountFile(final Path configDirectory) {
        final Path directory = configDirectory.toAbsolutePath().normalize();
        final Path parent = directory.getParent();
        return parent != null && directory.endsWith(DEFAULT_DIRECTORY)
                ? parent.resolve(ACCOUNT_FILE)
                : directory.resolve(ACCOUNT_FILE);
    }

    /**
     * Tells whether Claude Code would take the credentials file's content as a subscription.
     *
     * @param root Content of the credentials file.
     * @return Whether it holds an access token with the scope that allows inference.
     */
    static boolean holdsSubscription(final Map<?, ?> root) {
        return root.get("claudeAiOauth") instanceof Map<?, ?> oauth
                && oauth.get("accessToken") instanceof String token
                && !token.isBlank()
                && oauth.get("scopes") instanceof List<?> scopes
                && scopes.contains(INFERENCE_SCOPE);
    }

    private static Optional<Credential> readableConsoleKey(final Path configDirectory) {
        try {
            return consoleKey(configDirectory);
        } catch (AgentException ex) {
            return Optional.empty();
        }
    }

    private static Optional<Credential> consoleKey(final Path configDirectory) {
        final Path file = accountFile(configDirectory);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return readObject(file).get(CONSOLE_KEY) instanceof String key && !key.isBlank()
                ? Optional.of(Credential.of(API_KEY, key))
                : Optional.empty();
    }

    private static Map<?, ?> readObject(final Path file) {
        final Object parsed;
        try {
            parsed = Json.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new AgentException("Cannot read " + file, ex);
        } catch (JsonException ex) {
            // Neither the parser's message nor the cause: it quotes what it stumbles over, and any
            // other fault is a bug, not the file's.
            throw new AgentException("Cannot parse " + file + ": it is not valid JSON");
        }
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new AgentException(file + " is not a JSON object");
        }
        return root;
    }

    private Optional<Credential> oauth(Path file, Map<?, ?> oauth) {

        if (!(oauth.get("accessToken") instanceof String token) || token.isBlank()) {
            throw new AgentException(file + " has an OAuth block with no access token");
        }

        final Map<String, String> attributes = new LinkedHashMap<>();
        if (oauth.get("refreshToken") instanceof String refresh && !refresh.isBlank()) {
            attributes.put("refreshToken", refresh);
            // With them Porelo renews the login on the host, so a task never needs the refresh
            // token
            // and a subscription outlasts its access token. They are Claude Code's, not the file's:
            // the file names neither.
            attributes.put("tokenUrl", TOKEN_URL);
            attributes.put("clientId", CLIENT_ID);
        }
        if (oauth.get("expiresAt") instanceof Number expiry) {
            // Kept so the vault can tell a stale token from a wrong one later, which are very
            // different problems wearing the same error message.
            attributes.put("expiresAt", String.valueOf(expiry.longValue()));
        }
        return Optional.of(new Credential(OAUTH, token, attributes));
    }
}
