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
 * <p>
 * This is the capability the reference implementation declares in YAML and then ignores, writing
 * {@code if effective_agent == "claude"} in the viewer instead. Here it is a method on the agent,
 * so an agent added later either supplies its own formatter or gets plain text - and either way
 * says which, rather than inheriting a decision made about somebody else.
 * <p>
 * Anything unrecognised is passed through unchanged, and anything recognised but unreadable is
 * passed through with a marker. A log viewer that swallowed lines it did not understand would hide
 * exactly the output worth reading when something has gone wrong - and for an unattended run this
 * is the only record there is.
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

    @Override
    public @Nullable String format(String line) {

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

    /** A provider's refusal, as Claude Code writes it into the result: {@code API Error: 402 ...}. */
    private static final java.util.regex.Pattern PROVIDER_STATUS =
            java.util.regex.Pattern.compile("API Error: (\\d{3})\\b");

    /**
     * Reads one line for how the run ended: the {@code result} line, marked {@code is_error} or with an
     * error subtype when the run stopped before its end.
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
        final boolean failed = Boolean.TRUE.equals(event.get("is_error"))
                || String.valueOf(event.get("subtype")).startsWith("error");
        if (!failed) {
            return new AgentEnd(true, "", AgentEnd.AGENT, null);
        }
        final String said = event.get("result") instanceof String text && !text.isBlank()
                ? text.strip().replaceAll("\\s+", " ")
                : String.valueOf(event.get("subtype"));
        final java.util.regex.Matcher status = PROVIDER_STATUS.matcher(said);
        return status.find()
                ? new AgentEnd(false, said, AgentEnd.PROVIDER, Integer.valueOf(status.group(1)))
                : new AgentEnd(false, said, AgentEnd.AGENT, null);
    }

    private String result(Map<?, ?> event) {
        final Object cost = event.get("total_cost_usd");
        final Object turns = event.get("num_turns");
        // A run that failed - a provider's refusal, a turn limit - still ends with a result line, marked
        // is_error and with an error subtype. Read as "done", it looked like a success with nothing in it.
        final boolean failed = Boolean.TRUE.equals(event.get("is_error"))
                || String.valueOf(event.get("subtype")).startsWith("error");
        final StringBuilder out = new StringBuilder(failed ? "-- ended with an error" : "-- done");
        if (failed) {
            final Object subtype = event.get("subtype");
            if (subtype != null && !"success".equals(subtype)) {
                out.append(" (").append(subtype).append(')');
            }
            final Object said = event.get("result");
            if (said instanceof String text && !text.isBlank()) {
                final String line = text.strip().replaceAll("\\s+", " ");
                out.append(": ").append(line.length() > 300 ? line.substring(0, 300) + "…" : line);
            }
        }
        if (turns != null) {
            final String count = number(turns);
            out.append(", ").append(count).append("1".equals(count) ? " turn" : " turns");
        }
        if (cost != null) {
            // Claude Code prices every run at API rates, a subscription's included. Said as such, so
            // a person on a subscription does not read a bill; four places, because a double carries
            // seventeen digits nobody priced.
            out.append(", about $").append(cost instanceof Number n
                    ? BigDecimal.valueOf(n.doubleValue()).setScale(4, RoundingMode.HALF_UP)
                            .stripTrailingZeros().toPlainString()
                    : String.valueOf(cost)).append(" at API prices");
        }
        return out.toString();
    }

    private static String number(Object value) {
        return value instanceof Number n && n.doubleValue() == Math.floor(n.doubleValue())
                ? String.valueOf((long) n.doubleValue())
                : String.valueOf(value);
    }
}
