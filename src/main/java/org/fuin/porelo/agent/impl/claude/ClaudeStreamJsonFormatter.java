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
 * <p>Anything unrecognised is passed through, and anything recognised but unreadable is passed
 * through with a marker; either way without its terminal control characters. A log viewer that
 * swallowed lines it did not understand would hide exactly the output worth reading when something
 * has gone wrong - and for an unattended run this is the only record there is.
 */
public class ClaudeStreamJsonFormatter implements LogFormatter {

    /**
     * The only line that is dropped: the CLI announces its session at startup and the operator has
     * that information from the task itself. Matched by subtype, because "system" as a category
     * also carries warnings and errors, and those are the lines an unattended run is read for.
     */
    private static final String BENIGN = "init";

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
            return line;
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
            case "system" -> BENIGN.equals(String.valueOf(event.get("subtype"))) ? null : line;
            default -> line;
        };
    }

    private String text(Map<?, ?> event, String prefix, String line) {
        if (!(event.get("message") instanceof Map<?, ?> message)) {
            return UNREADABLE + line;
        }
        if (!(message.get("content") instanceof List<?> content)) {
            return UNREADABLE + line;
        }
        final StringBuilder out = new StringBuilder();
        for (final Object block : content) {
            if (!(block instanceof Map<?, ?> map)) {
                return UNREADABLE + line;
            }
            if (map.get("text") instanceof String value) {
                out.append(value);
            } else if ("tool_use".equals(String.valueOf(map.get("type")))) {
                out.append("[").append(map.get("name")).append("]");
            } else if (map.get("type") instanceof String type) {
                // A block shape this formatter does not render, named by what it calls itself
                // rather than by a guess: the operator sees that something was there.
                out.append("[").append(type).append("]");
            } else {
                return UNREADABLE + line;
            }
        }
        return out.isEmpty() ? UNREADABLE + line : prefix + out;
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
