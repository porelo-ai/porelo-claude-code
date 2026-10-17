package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests that no tracked file of this repository says the product's old name, but where it must.
 * <p>
 * The repository was renamed by a script; a word it left behind, or one written later out of habit, names something
 * that is no longer there. The old name is spelled here in two pieces, so that this file does not say it.
 */
@Tag("documents")
class NoOldNameTest {

    private static final String OLD = "so" + "kar";

    /** The changelog file, whose released sections stay as they were released. */
    private static final String CHANGELOG = "CHANGELOG.md";

    /** The first heading of a released section; everything from it on was released. */
    private static final Pattern RELEASED = Pattern.compile("## \\[\\d+\\.\\d+\\.\\d+\\].*");

    private static List<String> tracked() throws IOException, InterruptedException {
        final Process git = new ProcessBuilder("git", "ls-files", "-z").directory(RepositoryDocuments.root().toFile())
                .redirectErrorStream(true).start();
        final String out = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(git.waitFor()).as("git ls-files: %s", out).isZero();
        return List.of(out.split("\0"));
    }

    private static List<String> lines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (final MalformedInputException ex) {
            // Not text: nothing in it is read as a word.
            return List.of();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static int unreleasedEnd(String file, List<String> lines) {
        if (!file.equals(CHANGELOG)) {
            return lines.size();
        }
        for (int i = 0; i < lines.size(); i++) {
            if (RELEASED.matcher(lines.get(i)).matches()) {
                return i;
            }
        }
        return lines.size();
    }

    @Test
    void noTrackedFileSaysTheOldName() throws Exception {
        final List<String> files = tracked();
        final List<String> found = new ArrayList<>();
        int changelogReleased = 0;
        for (final String file : files) {
            if (file.toLowerCase(Locale.ROOT).contains(OLD)) {
                found.add(file + ": its path");
            }
            final List<String> lines = lines(RepositoryDocuments.root().resolve(file));
            final int end = unreleasedEnd(file, lines);
            if (file.equals(CHANGELOG)) {
                changelogReleased = lines.size() - end;
            }
            for (int i = 0; i < end; i++) {
                final String line = lines.get(i);
                if (line.toLowerCase(Locale.ROOT).contains(OLD)) {
                    found.add(file + ":" + (i + 1) + ": " + line.strip());
                }
            }
        }

        // A guard that reads nothing passes forever: assert what it read, then what it checks.
        assertThat(files).as("the tracked files read").contains("pom.xml", "AGENTS.md", "issues/README.md", CHANGELOG);
        assertThat(changelogReleased).as("the changelog's released lines left as released").isPositive();
        assertThat(found).as("tracked files that say the old name").isEmpty();
    }
}
