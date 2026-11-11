package org.fuin.porelo.agent.impl.claude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.ToolEvent.Kind;
import org.fuin.porelo.agent.api.ToolEvent.Phase;
import org.fuin.porelo.wire.Json;
import org.jspecify.annotations.Nullable;

/**
 * Reads the tool calls out of one line of Claude Code's {@code stream-json}.
 *
 * <p>A call starts with a {@code tool_use} block in an {@code assistant} line and ends with the
 * {@code tool_result} of the same id in a {@code user} line, both at the line's own {@code
 * timestamp}. A job in the background starts with {@code system/task_started}, which names the
 * call, and ends with {@code system/task_updated} at its {@code end_time}. A call a sub-agent makes
 * names the {@code Agent} call it ran under in {@code parent_tool_use_id}. Nothing of a tool's
 * output and no input but a shell call's command line and a sub-agent's name is passed on.
 */
final class ClaudeToolEvents {

    /** What {@code task_updated} says of a job that has ended. */
    private static final Set<String> ENDED = Set.of("completed", "failed", "killed");

    /** The tool name the result and job lines do not carry: Porelo joins them by id. */
    private static final String UNNAMED = "";

    private ClaudeToolEvents() {}

    /**
     * Reads one line.
     *
     * @param line One raw line of output.
     * @return The line's tool events in their order; none for a line that holds none or cannot be
     *     read.
     */
    static List<ToolEvent> of(final String line) {
        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return List.of();
        }
        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            // A partial line or not JSON after all: it holds no call, and the run goes on.
            return List.of();
        }
        if (!(parsed instanceof Map<?, ?> event)) {
            return List.of();
        }
        return switch (String.valueOf(event.get("type"))) {
            case "assistant" -> blocks(event, "tool_use");
            case "user" -> blocks(event, "tool_result");
            case "system" -> job(event);
            default -> List.of();
        };
    }

    private static List<ToolEvent> blocks(final Map<?, ?> event, final String wanted) {
        if (!(event.get("message") instanceof Map<?, ?> message)
                || !(message.get("content") instanceof List<?> content)) {
            return List.of();
        }
        final @Nullable String at = text(event.get("timestamp"));
        final @Nullable String parent = text(event.get("parent_tool_use_id"));
        final List<ToolEvent> events = new ArrayList<>();
        for (final Object item : content) {
            if (item instanceof Map<?, ?> block && wanted.equals(block.get("type"))) {
                final ToolEvent read =
                        "tool_use".equals(wanted)
                                ? started(block, at, parent)
                                : ended(block, at, parent);
                if (read != null) {
                    events.add(read);
                }
            }
        }
        return events;
    }

    private static @Nullable ToolEvent started(
            final Map<?, ?> block, final @Nullable String at, final @Nullable String parent) {
        final String id = text(block.get("id"));
        final String tool = text(block.get("name"));
        if (id == null || tool == null) {
            return null;
        }
        final Map<?, ?> input = block.get("input") instanceof Map<?, ?> given ? given : Map.of();
        final Kind kind = kind(tool);
        return new ToolEvent(
                0,
                id,
                Phase.START,
                tool,
                kind,
                kind == Kind.SHELL ? text(input.get("command")) : null,
                Boolean.TRUE.equals(input.get("run_in_background")),
                null,
                at,
                kind == Kind.SUB_AGENT ? text(input.get("description")) : null,
                parent);
    }

    private static @Nullable ToolEvent ended(
            final Map<?, ?> block, final @Nullable String at, final @Nullable String parent) {
        final String id = text(block.get("tool_use_id"));
        if (id == null) {
            return null;
        }
        return new ToolEvent(
                0, id, Phase.END, UNNAMED, Kind.OTHER, null, false, null, at, null, parent);
    }

    private static List<ToolEvent> job(final Map<?, ?> event) {
        final String job = text(event.get("task_id"));
        if (job == null) {
            return List.of();
        }
        return switch (String.valueOf(event.get("subtype"))) {
            case "task_started" -> jobStarted(event, job);
            case "task_updated" -> jobEnded(event, job);
            default -> List.of();
        };
    }

    private static List<ToolEvent> jobStarted(final Map<?, ?> event, final String job) {
        final String id = text(event.get("tool_use_id"));
        if (id == null) {
            return List.of();
        }
        final boolean agent = "local_agent".equals(event.get("task_type"));
        return List.of(
                new ToolEvent(
                        0,
                        id,
                        Phase.START,
                        agent ? "Agent" : "Bash",
                        agent ? Kind.SUB_AGENT : Kind.SHELL,
                        null,
                        Boolean.TRUE.equals(event.get("is_backgrounded")),
                        job,
                        null,
                        agent ? text(event.get("description")) : null,
                        null));
    }

    private static List<ToolEvent> jobEnded(final Map<?, ?> event, final String job) {
        if (!(event.get("patch") instanceof Map<?, ?> patch)
                || !(patch.get("status") instanceof String status)
                || !ENDED.contains(status)) {
            return List.of();
        }
        // The job's line names no call, only the job: Porelo joins it by the job's id.
        return List.of(
                new ToolEvent(
                        0,
                        job,
                        Phase.END,
                        UNNAMED,
                        Kind.OTHER,
                        null,
                        true,
                        job,
                        patch.get("end_time") instanceof Number millis
                                ? Instant.ofEpochMilli(millis.longValue()).toString()
                                : null,
                        null,
                        null));
    }

    /**
     * Tells a tool's kind from its name.
     *
     * @param tool Claude Code's name for the tool.
     * @return Its kind; {@link Kind#OTHER} for one not named here.
     */
    static Kind kind(final String tool) {
        if (tool.startsWith("mcp__")) {
            return Kind.MCP;
        }
        return switch (tool) {
            case "Bash" -> Kind.SHELL;
            case "Edit", "MultiEdit", "Write", "NotebookEdit" -> Kind.EDIT;
            case "Read" -> Kind.READ;
            case "Grep", "Glob" -> Kind.SEARCH;
            case "WebFetch", "WebSearch" -> Kind.WEB;
            case "Agent", "Task" -> Kind.SUB_AGENT;
            default -> Kind.OTHER;
        };
    }

    private static @Nullable String text(final @Nullable Object value) {
        return value instanceof String given && !given.isEmpty() ? given : null;
    }
}
