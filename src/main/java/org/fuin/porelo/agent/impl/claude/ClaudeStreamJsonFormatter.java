package org.fuin.porelo.agent.impl.claude;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.wire.Json;
import org.jspecify.annotations.Nullable;

/**
 * Renders Claude's {@code stream-json} output as readable lines.
 *
 * <p>This is the capability the reference implementation declares in YAML and then ignores, writing
 * {@code if effective_agent == "claude"} in the viewer instead. Here it is a method on the agent,
 * so an agent added later either supplies its own formatter or gets plain text - and either way
 * says which, rather than inheriting a decision made about somebody else.
 *
 * <p>A person reads what the agent says and does, so an event of Claude Code's own is said in words
 * where it tells them something - a command in the background, a commit, a notice - and is left out
 * otherwise, an unknown one included; one that reports an error or a warning is always shown, since
 * for an unattended run this is the only record there is. A line that is not JSON, and one
 * recognised but unreadable, which gets a marker, still pass through; every line loses its terminal
 * control characters.
 */
public class ClaudeStreamJsonFormatter implements LogFormatter {

    /**
     * The key of the notice that the task image's waiver of the dangerous-rm check is set. The task
     * sets it on purpose, so the notice tells the person nothing; a notice that the waiver has no
     * effect has other words and still passes.
     */
    private static final String WAIVER_NOTICE = "bypass-removal-waiver";

    /**
     * The same notice as the CLI writes it to standard error, which the task's log holds beside the
     * stream. Standard error has no key, so it is known by its opening words.
     */
    private static final String WAIVER_NOTICE_TEXT =
            "CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS is set, so dangerous rm commands";

    /** A terminal's color and cursor sequences, which the CLI wraps the notice in. */
    private static final java.util.regex.Pattern ESCAPES =
            java.util.regex.Pattern.compile("\\u001b\\[[0-9;?]*[ -/]*[@-~]");

    /** Prefixes a line the formatter recognised but could not read, so it is visibly not prose. */
    private static final String UNREADABLE = "[unreadable] ";

    /** How much of what a failed run said the log shows, in code points. */
    private static final int SAID_LIMIT = 300;

    /** The error of a failed run whose result line says neither a reason nor an error subtype. */
    private static final String NO_REASON = "the run ended with an error and gave no reason";

    @Override
    public @Nullable String format(String line) {
        final String rendered = render(line);
        return rendered == null ? null : visible(rendered);
    }

    /**
     * Drops the ISO control characters but the line break and the tab: JSON's escapes parse back
     * into live bytes, and what the model wrote must not drive the terminal that shows it.
     */
    private static String visible(String text) {
        return text.codePoints()
                .filter(c -> !Character.isISOControl(c) || c == '\n' || c == '\t')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private @Nullable String render(String line) {

        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return ESCAPES.matcher(trimmed).replaceAll("").startsWith(WAIVER_NOTICE_TEXT)
                    ? null
                    : line;
        }

        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            // Not JSON after all, or a partial line. Show it.
            return line;
        }
        if (!(parsed instanceof Map<?, ?> event)) {
            return line;
        }

        return switch (String.valueOf(event.get("type"))) {
            case "assistant" -> text(event, "", line);
            case "user" -> text(event, "> ", line);
            case "result" -> result(event);
            case "system" -> system(event);
            default -> trouble(event);
        };
    }

    /** A system line in words, or {@code null} when it tells the person nothing. */
    private static @Nullable String system(Map<?, ?> event) {
        final String trouble = trouble(event);
        if (trouble != null) {
            return trouble;
        }
        return switch (String.valueOf(event.get("subtype"))) {
            case "task_started" ->
                    Boolean.TRUE.equals(event.get("is_backgrounded"))
                            ? words(event, "description", "[background] ", " started")
                            : null;
            // Measured on 2.1.311: a job in the background writes its output to a file, a call in
            // the foreground, which shows as its own tool call, names none.
            case "task_notification" ->
                    event.get("output_file") instanceof String file && !file.isBlank()
                            ? words(event, "summary", "[background] ", "")
                            : null;
            case "vcs_state_changed" ->
                    event.get("kind") instanceof String kind && !kind.isBlank()
                            ? "[git] "
                                    + cut(oneLine(kind))
                                    + (text(event.get("branch")) instanceof String branch
                                            ? " on " + cut(oneLine(branch))
                                            : "")
                            : null;
            case "notification" ->
                    WAIVER_NOTICE.equals(event.get("key"))
                            ? null
                            : words(event, "text", "[notice] ", "");
            default -> null;
        };
    }

    /** A field's text between a prefix and a suffix, or {@code null} when it holds none. */
    private static @Nullable String words(
            Map<?, ?> event, String key, String prefix, String suffix) {
        final String text = text(event.get(key));
        return text == null ? null : prefix + cut(oneLine(text)) + suffix;
    }

    /** The words a status or outcome uses for a run or a step that went wrong. */
    private static final java.util.Set<String> FAILED =
            java.util.Set.of(
                    "failed", "failure", "error", "killed", "rejected", "denied", "aborted");

    /**
     * An event that reports an error or a warning, said as such with its message on one line, or
     * {@code null} when it reports neither. An error is named by the event's type or subtype, its
     * level, a status or outcome of failure (a job's {@code patch} included), an error mark, or an
     * error it carries; a warning by its type, subtype or level.
     */
    private static @Nullable String trouble(Map<?, ?> event) {
        final boolean error =
                names(event.get("type"), "error", "fail", "fatal", "exception")
                        || names(event.get("subtype"), "error", "fail", "fatal", "exception")
                        || names(event.get("level"), "error", "fatal")
                        || failure(event.get("status"))
                        || failure(event.get("outcome"))
                        || event.get("patch") instanceof Map<?, ?> patch
                                && failure(patch.get("status"))
                        || marked(event.get("is_error"))
                        || marked(event.get("isError"))
                        || carries(event.get("error"));
        final boolean warning =
                names(event.get("type"), "warn")
                        || names(event.get("subtype"), "warn")
                        || names(event.get("level"), "warn");
        if (!error && !warning) {
            return null;
        }
        return (error ? "[error] " : "[warning] ") + cut(oneLine(troubleText(event)));
    }

    private static boolean names(@Nullable Object value, String... words) {
        if (!(value instanceof String text)) {
            return false;
        }
        final String lower = text.toLowerCase(java.util.Locale.ROOT);
        for (final String word : words) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean failure(@Nullable Object status) {
        return status instanceof String text
                && FAILED.contains(text.strip().toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean marked(@Nullable Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    /**
     * Whether a field holds an error: any text, a flag that is set, a number, a list or an object.
     */
    private static boolean carries(@Nullable Object error) {
        return text(error) != null
                || Boolean.TRUE.equals(error)
                || error instanceof Number
                || error instanceof List<?> list && !list.isEmpty()
                || error instanceof Map<?, ?> map && !map.isEmpty();
    }

    /**
     * What an error says: its own text or message, the event's message, summary, standard error or
     * text, the error itself, or what the event is.
     */
    private static String troubleText(Map<?, ?> event) {
        final Object error = event.get("error");
        if (text(error) instanceof String own) {
            return own;
        }
        if (error instanceof Map<?, ?> nested
                && text(nested.get("message")) instanceof String message) {
            return message;
        }
        for (final String key : List.of("message", "summary", "stderr", "text")) {
            if (text(event.get(key)) instanceof String said) {
                return said;
            }
        }
        if (error != null && !Boolean.TRUE.equals(error)) {
            return String.valueOf(error);
        }
        return event.get("subtype") instanceof String subtype
                ? subtype
                : String.valueOf(event.get("type"));
    }

    private static @Nullable String text(@Nullable Object value) {
        return value instanceof String text && !text.isBlank() ? text.strip() : null;
    }

    /**
     * Folds what an event says onto one line, so a line break in it - a Unicode separator included
     * - cannot pass for a line of the log, and drops the invisible marks that turn text around.
     */
    private static String oneLine(String text) {
        return text.replaceAll("\\p{Cf}", "")
                .replaceAll("[\\s\\u0085\\u2028\\u2029]+", " ")
                .strip();
    }

    private @Nullable String text(Map<?, ?> event, String prefix, String line) {
        if (!(event.get("message") instanceof Map<?, ?> message)) {
            return UNREADABLE + line;
        }
        if (!(message.get("content") instanceof List<?> content)) {
            return UNREADABLE + line;
        }
        final StringBuilder out = new StringBuilder();
        boolean thought = false;
        for (final Object block : content) {
            if (!(block instanceof Map<?, ?> map)) {
                return UNREADABLE + line;
            }
            if (map.get("text") instanceof String value) {
                out.append(value);
                continue;
            }
            final String part;
            switch (String.valueOf(map.get("type"))) {
                case "tool_use" -> part = call(map);
                case "tool_result" -> part = outcome(map);
                // The model's reasoning before it acts; the call it leads to says what it did.
                case "thinking", "redacted_thinking" -> {
                    thought = true;
                    continue;
                }
                default -> {
                    if (!(map.get("type") instanceof String type)) {
                        return UNREADABLE + line;
                    }
                    // A block shape this formatter does not render, named by what it calls
                    // itself rather than by a guess: the operator sees that something was there.
                    part = "[" + type + "]";
                }
            }
            if (!out.isEmpty() && !Character.isWhitespace(out.charAt(out.length() - 1))) {
                out.append(' ');
            }
            out.append(part);
        }
        if (out.isEmpty()) {
            return thought ? null : UNREADABLE + line;
        }
        return prefix + out;
    }

    /** The input field each of Claude Code's own tools is known by, in its order of preference. */
    private static final Map<String, List<String>> KEY_INPUT =
            Map.ofEntries(
                    Map.entry("Bash", List.of("command")),
                    Map.entry("Read", List.of("file_path")),
                    Map.entry("Edit", List.of("file_path")),
                    Map.entry("MultiEdit", List.of("file_path")),
                    Map.entry("Write", List.of("file_path")),
                    Map.entry("NotebookEdit", List.of("notebook_path", "file_path")),
                    Map.entry("Grep", List.of("pattern")),
                    Map.entry("Glob", List.of("pattern")),
                    Map.entry("WebFetch", List.of("url")),
                    Map.entry("WebSearch", List.of("query")),
                    Map.entry("Task", List.of("description")),
                    Map.entry("Agent", List.of("description")));

    /**
     * The fields that tell most of what any other tool was called on, in that order; a field that
     * holds what it writes or asks at length, such as a content or a prompt, is not among them.
     */
    private static final List<String> TELLING =
            List.of(
                    "description",
                    "file_path",
                    "path",
                    "pattern",
                    "query",
                    "url",
                    "command",
                    "subject",
                    "name");

    /**
     * A tool call by its name and the one input that says what it was called on, on one line and
     * cut: a command by its first line, a todo list by its number of items.
     */
    private static String call(Map<?, ?> block) {
        final String name = String.valueOf(block.get("name"));
        final Map<?, ?> input = block.get("input") instanceof Map<?, ?> map ? map : Map.of();
        final @Nullable String said;
        if ("TodoWrite".equals(name)) {
            said = input.get("todos") instanceof List<?> todos ? count(todos.size(), "item") : null;
        } else {
            said = keyInput(input, KEY_INPUT.getOrDefault(name, TELLING));
        }
        final String head = "[" + cut(oneLine(name)) + "]";
        return said == null ? head : head + " " + cut(oneLine(said));
    }

    /** The first of the fields that holds text, by its first line that is not blank. */
    private static @Nullable String keyInput(Map<?, ?> input, List<String> keys) {
        for (final String key : keys) {
            if (text(input.get(key)) instanceof String value) {
                return firstLine(value);
            }
        }
        return null;
    }

    /**
     * A tool's result by whether it failed and its first line that is not blank, with its number of
     * lines when it has more; one that holds no text by what it holds. Never the whole output.
     */
    private static String outcome(Map<?, ?> block) {
        final String failed =
                marked(block.get("is_error")) || marked(block.get("isError")) ? "[failed] " : "";
        final StringBuilder text = new StringBuilder();
        final java.util.Map<String, Integer> other = new java.util.LinkedHashMap<>();
        if (block.get("content") instanceof String value) {
            text.append(value);
        } else if (block.get("content") instanceof List<?> parts) {
            for (final Object part : parts) {
                if (part instanceof Map<?, ?> map && map.get("text") instanceof String value) {
                    text.append(text.isEmpty() ? "" : "\n").append(value);
                } else if (part instanceof Map<?, ?> map
                        && map.get("type") instanceof String type) {
                    other.merge(type, 1, Integer::sum);
                }
            }
        }
        if (text(text.toString()) instanceof String said) {
            final long lines = said.lines().count();
            return failed
                    + cut(oneLine(firstLine(said)))
                    + (lines > 1 ? " (" + count(lines, "line") + ")" : "");
        }
        if (!other.isEmpty()) {
            final List<String> held = new java.util.ArrayList<>();
            other.forEach((type, n) -> held.add(count(n, cut(oneLine(type)))));
            return failed + String.join(", ", held);
        }
        return failed + "no output";
    }

    /** The first line of a text that is not blank; the text itself when it has none. */
    private static String firstLine(String text) {
        return text.lines().filter(l -> !l.isBlank()).findFirst().orElse(text).strip();
    }

    private static String count(long n, String thing) {
        return n + " " + thing + (n == 1 ? "" : "s");
    }

    /**
     * A provider's refusal, as Claude Code writes it into the result: {@code API Error: 402 ...}.
     */
    private static final java.util.regex.Pattern PROVIDER_STATUS =
            java.util.regex.Pattern.compile("API Error: (\\d{3})\\b");

    /**
     * Reads one line for how the run ended: the {@code result} line, marked {@code is_error} or
     * with an error subtype when the run stopped before its end.
     *
     * @param line One raw line of output.
     * @return How the run ended, or {@code null} for a line that is not its end.
     */
    static @Nullable AgentEnd ended(String line) {
        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            return null;
        }
        if (!(parsed instanceof Map<?, ?> event) || !"result".equals(event.get("type"))) {
            return null;
        }
        if (!failed(event)) {
            return new AgentEnd(true, "", AgentEnd.AGENT, null);
        }
        final String said = said(event);
        final String error = visible(said.isEmpty() ? errorSubtype(event) : said);
        final java.util.regex.Matcher status = PROVIDER_STATUS.matcher(error);
        return status.find()
                ? new AgentEnd(false, error, AgentEnd.PROVIDER, Integer.valueOf(status.group(1)))
                : new AgentEnd(false, error, AgentEnd.AGENT, null);
    }

    /**
     * Whether a result line reports a run that stopped before its end: a provider's refusal or a
     * turn limit still ends with one, marked {@code is_error} or with an error subtype.
     */
    private static boolean failed(Map<?, ?> event) {
        return Boolean.TRUE.equals(event.get("is_error"))
                || String.valueOf(event.get("subtype")).startsWith("error");
    }

    /** What the result line says, on one line, or empty when it says nothing. */
    private static String said(Map<?, ?> event) {
        return event.get("result") instanceof String text && !text.isBlank()
                ? text.strip().replaceAll("\\s+", " ")
                : "";
    }

    /**
     * The subtype, when it names something other than success; otherwise that the line gave no
     * reason, since "success" or a missing subtype is no error text.
     */
    private static String errorSubtype(Map<?, ?> event) {
        return event.get("subtype") instanceof String subtype && !"success".equals(subtype)
                ? subtype
                : NO_REASON;
    }

    /** Cuts what a failed run said on a code point, so no half of a pair reaches the log. */
    private static String cut(String text) {
        return text.codePointCount(0, text.length()) > SAID_LIMIT
                ? text.substring(0, text.offsetByCodePoints(0, SAID_LIMIT)) + "…"
                : text;
    }

    private String result(Map<?, ?> event) {
        final Object cost = event.get("total_cost_usd");
        final Object turns = event.get("num_turns");
        final boolean failed = failed(event);
        final StringBuilder out = new StringBuilder(failed ? "-- ended with an error" : "-- done");
        if (failed) {
            final Object subtype = event.get("subtype");
            if (subtype != null && !"success".equals(subtype)) {
                out.append(" (").append(subtype).append(')');
            }
            final String said = said(event);
            if (!said.isEmpty()) {
                out.append(": ").append(cut(said));
            }
        }
        if (turns != null) {
            final String count = number(turns);
            out.append(", ").append(count).append("1".equals(count) ? " turn" : " turns");
        }
        if (cost != null) {
            // Claude Code prices every run at API rates, a subscription's included. Said as such,
            // so a person on a subscription does not read a bill; four places, because a double
            // carries seventeen digits nobody priced.
            out.append(", about $")
                    .append(
                            cost instanceof Number n
                                    ? BigDecimal.valueOf(n.doubleValue())
                                            .setScale(4, RoundingMode.HALF_UP)
                                            .stripTrailingZeros()
                                            .toPlainString()
                                    : String.valueOf(cost))
                    .append(" at API prices");
        }
        return out.toString();
    }

    private static String number(Object value) {
        return value instanceof Number n && n.doubleValue() == Math.floor(n.doubleValue())
                ? String.valueOf((long) n.doubleValue())
                : String.valueOf(value);
    }
}
