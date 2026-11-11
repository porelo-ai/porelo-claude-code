package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.ToolEvent.Kind;
import org.fuin.porelo.agent.api.ToolEvent.Phase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests how {@link ClaudeAgent} reads the tool calls out of Claude Code's {@code stream-json}, from
 * lines as Claude Code 2.1.311 wrote them in an unattended task, trimmed to what each test needs.
 */
class ClaudeToolEventsTest {

    private final ClaudeAgent agent = new ClaudeAgent();

    private static final String SHELL_START =
            """
            {"type":"assistant","message":{"model":"claude-opus-5-5","id":"msg_01","type":"message",\
            "role":"assistant","content":[{"type":"tool_use","id":"toolu_01D2","name":"Bash",\
            "input":{"command":"head -5 README.md; ls /porelo/files 2>/dev/null",\
            "description":"Read README head"},"caller":{"type":"direct"}}]},\
            "parent_tool_use_id":null,"session_id":"72d1","uuid":"a1",\
            "timestamp":"2026-11-11T15:45:46.457Z"}""";

    private static final String SHELL_END =
            """
            {"type":"user","message":{"role":"user","content":[{"tool_use_id":"toolu_01D2",\
            "type":"tool_result","content":"# porelo-message-sluice","is_error":false}]},\
            "parent_tool_use_id":null,"session_id":"72d1","uuid":"a2",\
            "timestamp":"2026-11-11T15:45:46.529Z"}""";

    private static final String BACKGROUND_START =
            """
            {"type":"assistant","message":{"role":"assistant","content":[{"type":"tool_use",\
            "id":"toolu_01Gh","name":"Bash","input":{"command":"./mvnw -B test > /tmp/build.log 2>&1",\
            "description":"Run Maven tests in background","run_in_background":true},\
            "caller":{"type":"direct"}}]},"parent_tool_use_id":null,"session_id":"72d1",\
            "uuid":"b1","timestamp":"2026-11-11T15:45:45.304Z"}""";

    private static final String BACKGROUND_RETURNED =
            """
            {"type":"user","message":{"role":"user","content":[{"tool_use_id":"toolu_01Gh",\
            "type":"tool_result","content":"Command running in background with ID: b1ig.",\
            "is_error":false}]},"parent_tool_use_id":null,"session_id":"72d1","uuid":"b2",\
            "timestamp":"2026-11-11T15:45:45.403Z"}""";

    private static final String JOB_STARTED =
            """
            {"type":"system","subtype":"task_started","task_id":"b1ig","run_id":"0mwc-5bd5",\
            "tool_use_id":"toolu_01Gh","description":"Run Maven tests in background",\
            "is_backgrounded":true,"task_type":"local_bash","uuid":"b3","session_id":"72d1"}""";

    private static final String JOB_COMPLETED =
            """
            {"type":"system","subtype":"task_updated","task_id":"b1ig","run_id":"0mwc-5bd5",\
            "patch":{"status":"completed","end_time":1794411996678},"uuid":"b4",\
            "session_id":"72d1"}""";

    private static final String MOVED_TO_BACKGROUND =
            """
            {"type":"system","subtype":"task_updated","task_id":"bqv3","run_id":"0mwc-340b",\
            "patch":{"is_backgrounded":true},"uuid":"c1","session_id":"72d1"}""";

    private static final String HEARTBEAT =
            """
            {"type":"tool_progress","tool_use_id":"toolu_01Px","tool_name":"Bash",\
            "parent_tool_use_id":null,"elapsed_time_seconds":30,"heartbeat":true,\
            "session_id":"72d1","uuid":"c2"}""";

    private static final String SUB_AGENT_START =
            """
            {"type":"assistant","message":{"role":"assistant","content":[{"type":"tool_use",\
            "id":"toolu_019X","name":"Agent","input":{"description":"ISSUE-7",\
            "subagent_type":"general-purpose","run_in_background":false,\
            "prompt":"Your name is ISSUE-7. Count the Java files holding Sluice."},\
            "caller":{"type":"direct"}}]},"parent_tool_use_id":null,"session_id":"41e9",\
            "uuid":"d1","timestamp":"2026-11-11T16:13:57.292Z"}""";

    private static final String SUB_AGENT_JOB_STARTED =
            """
            {"type":"system","subtype":"task_started","task_id":"aebe","run_id":"0mwc-3d84",\
            "tool_use_id":"toolu_019X","description":"ISSUE-7","subagent_type":"general-purpose",\
            "is_backgrounded":false,"spawn_depth":1,"task_type":"local_agent",\
            "prompt":"Your name is ISSUE-7.","uuid":"d2","session_id":"41e9"}""";

    private static final String CALL_UNDER_SUB_AGENT =
            """
            {"type":"assistant","message":{"role":"assistant","content":[{"type":"tool_use",\
            "id":"toolu_0183","name":"Bash","input":{"command":"grep -rl --include='*.java' Sluice . | wc -l",\
            "description":"Count Java files containing Sluice"}}]},\
            "parent_tool_use_id":"toolu_019X","session_id":"41e9","uuid":"d3",\
            "timestamp":"2026-11-11T16:13:59.928Z"}""";

    private static final String FAILED_END =
            """
            {"type":"user","message":{"role":"user","content":[{"type":"tool_result",\
            "tool_use_id":"toolu_01Ff","content":"Exit code 2","is_error":true}]},\
            "parent_tool_use_id":null,"session_id":"72d1","uuid":"e1",\
            "timestamp":"2026-11-11T15:46:01.000Z"}""";

    private static final String WRITE_START =
            """
            {"type":"assistant","message":{"role":"assistant","content":[\
            {"type":"text","text":"Writing the file."},{"type":"tool_use","id":"toolu_01Wr",\
            "name":"Write","input":{"file_path":"/workspace/notes.txt","content":"secret plan"}}]},\
            "parent_tool_use_id":null,"session_id":"72d1","uuid":"f1",\
            "timestamp":"2026-11-11T15:47:00.000Z"}""";

    @Test
    void aShellCallStartsWithItsCommandLineAndTheStreamsTime() {

        assertThat(agent.toolEvents(SHELL_START))
                .as("a Bash tool_use is the start of a shell call, its line and time as given")
                .containsExactly(
                        new ToolEvent(
                                0,
                                "toolu_01D2",
                                Phase.START,
                                "Bash",
                                Kind.SHELL,
                                "head -5 README.md; ls /porelo/files 2>/dev/null",
                                false,
                                null,
                                "2026-11-11T15:45:46.457Z",
                                null,
                                null));
    }

    @Test
    void aToolResultEndsTheCallWithTheSameId() {

        assertThat(agent.toolEvents(SHELL_END))
                .as("a tool_result ends the call it names, at its line's time, without its output")
                .containsExactly(
                        new ToolEvent(
                                0,
                                "toolu_01D2",
                                Phase.END,
                                "",
                                Kind.OTHER,
                                null,
                                false,
                                null,
                                "2026-11-11T15:45:46.529Z",
                                null,
                                null));
    }

    @Test
    void aFailedCallStillEnds() {

        assertThat(agent.toolEvents(FAILED_END))
                .as("a failed call ends like any other, so its time is not lost")
                .extracting(ToolEvent::id, ToolEvent::phase)
                .containsExactly(tuple("toolu_01Ff", Phase.END));
    }

    @Test
    void aBackgroundCallIsMarkedAndItsJobIsJoinedByItsId() {

        assertThat(agent.toolEvents(BACKGROUND_START))
                .as("run_in_background marks the call as one whose end is its job's")
                .extracting(ToolEvent::id, ToolEvent::background, ToolEvent::jobId)
                .containsExactly(tuple("toolu_01Gh", true, null));
        assertThat(agent.toolEvents(JOB_STARTED))
                .as("task_started names the job of the call it started")
                .containsExactly(
                        new ToolEvent(
                                0,
                                "toolu_01Gh",
                                Phase.START,
                                "Bash",
                                Kind.SHELL,
                                null,
                                true,
                                "b1ig",
                                null,
                                null,
                                null));
        assertThat(agent.toolEvents(JOB_COMPLETED))
                .as("the job ends when task_updated says so, at its own end time")
                .containsExactly(
                        new ToolEvent(
                                0,
                                "b1ig",
                                Phase.END,
                                "",
                                Kind.OTHER,
                                null,
                                true,
                                "b1ig",
                                "2026-11-11T15:46:36.678Z",
                                null,
                                null));
    }

    @Test
    void theReturnOfABackgroundCallIsAnOrdinaryEnd() {

        assertThat(agent.toolEvents(BACKGROUND_RETURNED))
                .as("the call's own end is passed; Porelo ends a background call with its job")
                .extracting(ToolEvent::id, ToolEvent::phase, ToolEvent::background)
                .containsExactly(tuple("toolu_01Gh", Phase.END, false));
    }

    @Test
    void aSubAgentCarriesItsNameAndItsCallsCarryItAsParent() {

        assertThat(agent.toolEvents(SUB_AGENT_START))
                .as("an Agent call is a sub-agent named by its description")
                .containsExactly(
                        new ToolEvent(
                                0,
                                "toolu_019X",
                                Phase.START,
                                "Agent",
                                Kind.SUB_AGENT,
                                null,
                                false,
                                null,
                                "2026-11-11T16:13:57.292Z",
                                "ISSUE-7",
                                null));
        // Porelo moves a call whose job is named to that job: a foreground sub-agent ends when
        // its Agent call returns, so its task names no job.
        assertThat(agent.toolEvents(SUB_AGENT_JOB_STARTED))
                .as("the task of a sub-agent in the foreground")
                .isEmpty();
        assertThat(
                        agent.toolEvents(
                                SUB_AGENT_JOB_STARTED.replace(
                                        "\"is_backgrounded\":false", "\"is_backgrounded\":true")))
                .as("the job of a sub-agent in the background keeps its name and joins the call")
                .extracting(
                        ToolEvent::id,
                        ToolEvent::kind,
                        ToolEvent::jobId,
                        ToolEvent::name,
                        ToolEvent::background)
                .containsExactly(tuple("toolu_019X", Kind.SUB_AGENT, "aebe", "ISSUE-7", true));
        assertThat(agent.toolEvents(CALL_UNDER_SUB_AGENT))
                .as("a call the sub-agent makes names the Agent call it ran under")
                .extracting(ToolEvent::id, ToolEvent::parent)
                .containsExactly(tuple("toolu_0183", "toolu_019X"));
    }

    @Test
    void anEditCarriesNeitherItsPathNorItsContent() {

        final List<ToolEvent> events = agent.toolEvents(WRITE_START);

        assertThat(events)
                .as("only the tool_use block is a call, the text beside it is not")
                .hasSize(1);
        assertThat(events.get(0).kind()).as("Write changes a file").isEqualTo(Kind.EDIT);
        assertThat(events.get(0).command())
                .as("no file content reaches Porelo through the command field")
                .isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "Bash, SHELL",
        "Edit, EDIT",
        "MultiEdit, EDIT",
        "Write, EDIT",
        "NotebookEdit, EDIT",
        "Read, READ",
        "Grep, SEARCH",
        "Glob, SEARCH",
        "WebFetch, WEB",
        "WebSearch, WEB",
        "Agent, SUB_AGENT",
        "Task, SUB_AGENT",
        "mcp__github__get_issue, MCP",
        "ToolSearch, OTHER",
        "TaskStop, OTHER"
    })
    void eachToolHasItsKind(final String tool, final Kind kind) {

        final String line =
                "{\"type\":\"assistant\",\"message\":{\"content\":[{\"type\":\"tool_use\","
                        + "\"id\":\"toolu_1\",\"name\":\""
                        + tool
                        + "\",\"input\":{}}]},\"timestamp\":\"2026-11-11T15:00:00Z\"}";

        assertThat(agent.toolEvents(line))
                .as("%s is of kind %s", tool, kind)
                .extracting(ToolEvent::kind)
                .containsExactly(kind);
    }

    @Test
    void aLineThatHoldsNoCallYieldsNothingAndNeverThrows() {

        assertThat(agent.toolEvents(HEARTBEAT)).as("a heartbeat is no moment of a call").isEmpty();
        assertThat(agent.toolEvents(MOVED_TO_BACKGROUND))
                .as("a job moved to the background has not ended")
                .isEmpty();
        assertThat(
                        agent.toolEvents(
                                "CLAUDE_CODE_EVEN_MORE_DANGEROUSLY_SKIP_PERMISSIONS is set, so"
                                        + " dangerous rm commands can run"))
                .as("a line that is not JSON")
                .isEmpty();
        assertThat(agent.toolEvents("{\"type\":\"assistant\",\"message\":{\"content\":[{\"type\""))
                .as("a line cut off in the middle")
                .isEmpty();
        assertThat(agent.toolEvents("[1,2,3]")).as("JSON that is no object").isEmpty();
        assertThat(
                        agent.toolEvents(
                                "{\"type\":\"assistant\",\"message\":{\"content\":[{\"type\":\"tool_use\"}]}}"))
                .as("a tool_use without id or name")
                .isEmpty();
        assertThat(
                        agent.toolEvents(
                                "{\"type\":\"system\",\"subtype\":\"task_updated\",\"task_id\":\"x\","
                                        + "\"patch\":{\"status\":\"completed\",\"end_time\":\"soon\"}}"))
                .as("an end time that is no number is not invented")
                .extracting(ToolEvent::at)
                .containsExactly((String) null);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"completed", "failed", "killed"})
    void aJobEndsOnEachFinalStatus(final String status) {

        assertThat(
                        agent.toolEvents(
                                JOB_COMPLETED.replace(
                                        "\"status\":\"completed\"",
                                        "\"status\":\"" + status + "\"")))
                .as("a job that ended %s ends its call", status)
                .extracting(ToolEvent::jobId, ToolEvent::phase)
                .containsExactly(tuple("b1ig", Phase.END));
    }

    @Test
    void aJobStillRunningHasNotEnded() {

        assertThat(
                        agent.toolEvents(
                                JOB_COMPLETED.replace(
                                        "\"status\":\"completed\"", "\"status\":\"running\"")))
                .as("a status that is not final ends nothing")
                .isEmpty();
    }

    @Test
    void anEmptyIdNamesNoCall() {

        assertThat(agent.toolEvents(SHELL_START.replace("\"toolu_01D2\"", "\"\"")))
                .as("a start without an id would join every other one without")
                .isEmpty();
        assertThat(agent.toolEvents(SHELL_END.replace("\"toolu_01D2\"", "\"\"")))
                .as("an end without an id")
                .isEmpty();
        assertThat(agent.toolEvents(JOB_STARTED.replace("\"b1ig\"", "\"\"")))
                .as("a job without an id")
                .isEmpty();
        assertThat(agent.toolEvents(JOB_COMPLETED.replace("\"b1ig\"", "\"\"")))
                .as("a job's end without an id")
                .isEmpty();
    }
}
