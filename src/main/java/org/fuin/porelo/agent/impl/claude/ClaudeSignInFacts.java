package org.fuin.porelo.agent.impl.claude;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.fuin.porelo.wire.Json;
import org.fuin.porelo.wire.JsonException;
import org.jspecify.annotations.Nullable;

/**
 * What a sign-in Claude Code keeps in a file says of itself beside its finding, for a person who
 * chooses which one to carry to a host: the account, when it expires and when it last changed.
 * Never its value; the value goes to Porelo through {@link ClaudeCredentialExtractor} alone.
 *
 * <p>The account is the one Claude Code names in its account file, {@code oauthAccount}'s {@code
 * emailAddress}, measured on 2.1.311. The expiry is the subscription's {@code expiresAt}; a Console
 * key has none. The last change is the file's own time, which a renewal moves.
 *
 * @param finding The finding these facts belong to.
 * @param account The account the sign-in is for, or {@code null} where no file names one.
 * @param expires When the sign-in's access token expires, or {@code null} where none is held.
 * @param changed When its file was last written, or {@code null} where that cannot be read.
 */
record ClaudeSignInFacts(
        Finding finding,
        @Nullable String account,
        @Nullable Instant expires,
        @Nullable Instant changed) {

    /** Longest account name kept, in characters; a longer one is not taken as one. */
    static final int LONGEST_ACCOUNT = 320;

    /**
     * Returns the facts of each finding that is a sign-in in one of Claude Code's own files, in the
     * findings' order. A sign-in in the settings is left out: its value is the person's to give,
     * and an import does not take it.
     *
     * @param findings What {@link ClaudeFindings} found in the directory.
     * @param configDirectory Claude Code's configuration directory.
     * @return The facts, one per sign-in found in a file.
     */
    static List<ClaudeSignInFacts> of(final List<Finding> findings, final Path configDirectory) {
        final Path directory = configDirectory.toAbsolutePath();
        final Path credentials = directory.resolve(ClaudeCredentialExtractor.CREDENTIALS_FILE);
        final Path accountFile = ClaudeCredentialExtractor.accountFile(directory);
        final @Nullable String account = account(read(accountFile));
        return findings.stream()
                .filter(finding -> finding.what() == What.SIGN_IN)
                .filter(finding -> finding.signedIn() == SignedIn.SIGNED_IN)
                .filter(finding -> finding.path() != null)
                .<ClaudeSignInFacts>mapMulti(
                        (finding, out) -> {
                            final Path path = Path.of(finding.path()).normalize();
                            if (path.equals(credentials.normalize())) {
                                out.accept(
                                        new ClaudeSignInFacts(
                                                finding,
                                                account,
                                                expires(read(credentials)),
                                                changed(credentials)));
                            } else if (path.equals(accountFile.normalize())) {
                                out.accept(
                                        new ClaudeSignInFacts(
                                                finding, account, null, changed(accountFile)));
                            }
                        })
                .toList();
    }

    private static Map<?, ?> read(final Path file) {
        try {
            return Json.parse(Files.readString(file, StandardCharsets.UTF_8))
                            instanceof Map<?, ?> map
                    ? map
                    : Map.of();
        } catch (IOException | JsonException ex) {
            return Map.of();
        }
    }

    private static @Nullable String account(final Map<?, ?> accountFile) {
        if (accountFile.get("oauthAccount") instanceof Map<?, ?> oauth
                && oauth.get("emailAddress") instanceof String address
                && !address.isBlank()
                && address.length() <= LONGEST_ACCOUNT
                && address.codePoints().noneMatch(Character::isISOControl)) {
            return address;
        }
        return null;
    }

    private static @Nullable Instant expires(final Map<?, ?> credentials) {
        if (credentials.get("claudeAiOauth") instanceof Map<?, ?> oauth
                && oauth.get("expiresAt") instanceof Number millis
                && millis.longValue() > 0) {
            return Instant.ofEpochMilli(millis.longValue());
        }
        return null;
    }

    private static @Nullable Instant changed(final Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException ex) {
            return null;
        }
    }
}
