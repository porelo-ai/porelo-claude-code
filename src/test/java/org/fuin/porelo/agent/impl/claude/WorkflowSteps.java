package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs a workflow step's script as the runner does, so a test proves what the step does rather than
 * that some text is in it.
 */
final class WorkflowSteps {

    /** How long a step may take before the test stops it and fails. */
    static final Duration LIMIT = Duration.ofSeconds(30);

    private WorkflowSteps() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * What a step printed and how it exited.
     *
     * @param exit Its exit code.
     * @param output Its standard output and error, in the order written.
     */
    record Run(int exit, String output) {}

    /**
     * Returns the script of each step with this name, a block's indentation removed.
     *
     * @param workflow The file's name in {@code .github/workflows}.
     * @param name The step's name, exactly as written after {@code - name:}.
     * @return Each such step's script, in the order they appear.
     * @throws IOException If the workflow cannot be read.
     */
    static List<String> named(final String workflow, final String name) throws IOException {
        final List<String> lines =
                Files.readAllLines(
                        RepositoryDocuments.root().resolve(".github/workflows").resolve(workflow));
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).strip().equals("- name: " + name)) {
                continue;
            }
            final int step = indentOf(lines.get(i));
            int j = i + 1;
            while (j < lines.size()
                    && !lines.get(j).strip().startsWith("run: ")
                    && (lines.get(j).isBlank() || indentOf(lines.get(j)) > step)) {
                j++;
            }
            assertThat(j < lines.size() && lines.get(j).strip().startsWith("run: "))
                    .as("the step \"%s\" in %s runs a script", name, workflow)
                    .isTrue();
            if (!lines.get(j).strip().equals("run: |")) {
                result.add(plain(lines, j));
                continue;
            }
            final int key = indentOf(lines.get(j));
            final StringBuilder script = new StringBuilder();
            int body = -1;
            for (int k = j + 1;
                    k < lines.size() && (lines.get(k).isBlank() || indentOf(lines.get(k)) > key);
                    k++) {
                if (lines.get(k).isBlank()) {
                    script.append('\n');
                    continue;
                }
                if (body < 0) {
                    body = indentOf(lines.get(k));
                }
                script.append(lines.get(k).substring(body)).append('\n');
            }
            result.add(script.toString());
        }
        return result;
    }

    /** A script written on the {@code run:} line, folded as YAML folds a plain scalar. */
    private static String plain(final List<String> lines, final int at) {
        final int key = indentOf(lines.get(at));
        final StringBuilder script =
                new StringBuilder(lines.get(at).strip().substring("run: ".length()));
        for (int k = at + 1;
                k < lines.size() && !lines.get(k).isBlank() && indentOf(lines.get(k)) > key;
                k++) {
            script.append(' ').append(lines.get(k).strip());
        }
        return script.append('\n').toString();
    }

    /**
     * Runs a script with {@code bash -e}, as the runner runs a step without a {@code shell}, in a
     * directory and with only the environment given, the machine's git configuration left out.
     *
     * @param script The step's script.
     * @param dir The working directory.
     * @param env Variables beside {@code PATH} and {@code HOME}; a {@code PATH} given replaces it.
     * @param limit How long it may take.
     * @return What it printed and how it exited.
     * @throws IOException If it cannot be started.
     * @throws InterruptedException If the test is interrupted.
     */
    static Run run(
            final String script,
            final Path dir,
            final Map<String, String> env,
            final Duration limit)
            throws IOException, InterruptedException {
        final Path file = Files.createTempFile("workflow-step", ".sh");
        // To a file rather than a pipe: reading a pipe to its end would wait out a hang, not the
        // limit.
        final Path out = Files.createTempFile("workflow-step", ".out");
        try {
            Files.writeString(file, script);
            final ProcessBuilder builder =
                    new ProcessBuilder("bash", "--noprofile", "--norc", "-e", file.toString())
                            .directory(dir.toFile())
                            .redirectErrorStream(true)
                            .redirectOutput(out.toFile());
            builder.environment().clear();
            builder.environment()
                    .put("PATH", System.getenv().getOrDefault("PATH", "/usr/bin:/bin"));
            builder.environment().put("HOME", dir.toString());
            builder.environment().put("LC_ALL", "C");
            builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
            builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
            builder.environment().putAll(env);
            final Process process = builder.start();
            if (!process.waitFor(limit.toMillis(), TimeUnit.MILLISECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new AssertionError(
                        "the step did not end within "
                                + limit
                                + ":\n"
                                + Files.readString(out, StandardCharsets.UTF_8));
            }
            return new Run(process.exitValue(), Files.readString(out, StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(out);
        }
    }

    /**
     * Runs a script as {@link #run(String, Path, Map, Duration)} does, within {@link #LIMIT}.
     *
     * @param script The step's script.
     * @param dir The working directory.
     * @param env Variables beside {@code PATH} and {@code HOME}.
     * @return What it printed and how it exited.
     * @throws IOException If it cannot be started.
     * @throws InterruptedException If the test is interrupted.
     */
    static Run run(final String script, final Path dir, final Map<String, String> env)
            throws IOException, InterruptedException {
        return run(script, dir, env, LIMIT);
    }

    /**
     * Writes an executable stand-in for a command.
     *
     * @param bin The directory put first on the step's {@code PATH}.
     * @param name The command's name.
     * @param body Its bash script, after the shebang line.
     * @throws IOException If it cannot be written.
     */
    static void stub(final Path bin, final String name, final String body) throws IOException {
        Files.createDirectories(bin);
        final Path file = bin.resolve(name);
        Files.writeString(file, "#!/bin/bash\n" + body);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
    }

    /**
     * Returns a {@code PATH} that finds the stand-ins first.
     *
     * @param bin The directory holding them.
     * @return The value for the step's {@code PATH}.
     */
    static String pathWith(final Path bin) {
        return bin + ":" + System.getenv().getOrDefault("PATH", "/usr/bin:/bin");
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
