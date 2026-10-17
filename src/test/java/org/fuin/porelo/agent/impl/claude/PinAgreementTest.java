package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The pinned Claude Code version agrees everywhere the build writes it.
 *
 * <p>Reads the FILTERED definition from the test classpath - the file the binary answers {@code
 * describe} with - and not the source template, which would check the template and prove nothing
 * about the build. Whether the digest is the one Anthropic publishes needs the network, so that
 * question stays with the {@code check-pin} of Porelo's release tool, which still runs on every
 * push.
 *
 * <p>Each refusal below is reproduced from the real files rather than from a hand-written sample,
 * so a change to the definition's shape breaks the test instead of passing it.
 */
class PinAgreementTest {

    private static final String DEFINITION = "agent/claude.yaml";

    private static final Pattern VERSION =
            Pattern.compile("^\\s*version:\\s*\"?([^\"\\n]+)\"?\\s*$", Pattern.MULTILINE);

    private static final Pattern URL =
            Pattern.compile("^\\s*-?\\s*url:\\s*(\\S+)\\s*$", Pattern.MULTILINE);

    private static final Pattern SHA256 =
            Pattern.compile("^\\s*sha256:\\s*\"?([0-9a-f]{64})\"?\\s*$", Pattern.MULTILINE);

    private static final Pattern PINNED =
            Pattern.compile("<agent\\.cli\\.version>([^<]+)</agent\\.cli\\.version>");

    @Test
    void theBuiltDefinitionAgreesWithThePom() throws IOException {

        assertThat(disagreements(filtered(), pom())).isEmpty();
    }

    @Test
    void refusesADefinitionThatWasNeverFiltered() throws IOException {

        // The source template is exactly what a build that skipped filtering would ship.
        final String template = Files.readString(Path.of("src/main/resources", DEFINITION));

        assertThat(disagreements(template, pom()))
                .singleElement()
                .asString()
                .contains("filtering did not run");
    }

    @Test
    void refusesAPomThatPinsAnotherVersion() throws IOException {

        final String pom =
                PINNED.matcher(pom()).replaceFirst("<agent.cli.version>0.0.1</agent.cli.version>");

        assertThat(disagreements(filtered(), pom))
                .singleElement()
                .asString()
                .contains("pom.xml pins 0.0.1");
    }

    @Test
    void refusesADownloadUrlThatNamesAnotherVersion() throws IOException {

        final String definition = filtered();
        final String version = first(VERSION, definition);
        final String moved = definition.replace("/" + version + "/", "/0.0.1/");

        assertThat(disagreements(moved, pom()))
                .singleElement()
                .asString()
                .contains("the download URL does not name " + version);
    }

    @Test
    void refusesADefinitionWithoutACompletePinnedArtifact() throws IOException {

        final String withoutDigest = SHA256.matcher(filtered()).replaceFirst("");

        assertThat(disagreements(withoutDigest, pom()))
                .singleElement()
                .asString()
                .contains("no complete pinned artifact");
    }

    /**
     * Returns every way the definition and the pom disagree about the pin.
     *
     * @param definition Definition text.
     * @param pom Content of {@code pom.xml}.
     * @return One sentence per disagreement; empty when they agree.
     */
    static List<String> disagreements(final String definition, final String pom) {

        final Matcher version = VERSION.matcher(definition);
        final Matcher url = URL.matcher(definition);
        final Matcher digest = SHA256.matcher(definition);
        if (!(version.find() && url.find() && digest.find())) {
            return List.of("the definition has no complete pinned artifact");
        }
        final String installed = version.group(1).strip();
        if (installed.contains("${") || url.group(1).contains("${")) {
            return List.of(
                    "resource filtering did not run - the definition still reads " + installed);
        }

        final List<String> problems = new ArrayList<>();
        final Matcher pinned = PINNED.matcher(pom);
        if (!pinned.find()) {
            problems.add("pom.xml declares no agent.cli.version");
        } else if (!pinned.group(1).strip().equals(installed)) {
            problems.add(
                    "pom.xml pins "
                            + pinned.group(1).strip()
                            + ", the definition installs "
                            + installed);
        }
        if (!url.group(1).contains("/" + installed + "/")) {
            problems.add("the download URL does not name " + installed + ": " + url.group(1));
        }
        return problems;
    }

    private static String first(final Pattern pattern, final String text) {
        final Matcher matcher = pattern.matcher(text);
        assertThat(matcher.find()).as("%s in the definition", pattern).isTrue();
        return matcher.group(1).strip();
    }

    private static String filtered() throws IOException {
        try (InputStream in =
                PinAgreementTest.class.getClassLoader().getResourceAsStream(DEFINITION)) {
            assertThat(in).as("%s on the classpath - build first", DEFINITION).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String pom() throws IOException {
        return Files.readString(Path.of("pom.xml"));
    }
}
