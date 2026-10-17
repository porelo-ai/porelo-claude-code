package org.fuin.porelo.agent.impl.claude;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.porelo.agent.api.SessionIds;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;

/**
 * Where Claude Code names its session, read against what 2.1.267 wrote.
 * <p>
 * That the id is still where this says at the version the package pins is the acceptance suite's
 * question; this one fails when the declaration stops fitting what was measured.
 */
class SessionIdDeclarationTest {

    private static final String ID = "7eb09386-82be-4aec-b8c3-d3f6484f5581";

    /** The first records of an unattended run, as it wrote them, trimmed of fields that do not matter. */
    private static final List<Object> RECORDS = List.of(
            Json.parse("{\"type\":\"system\",\"subtype\":\"init\",\"cwd\":\"/workspace\",\"session_id\":\"" + ID + "\"}"),
            Json.parse("{\"type\":\"assistant\",\"session_id\":\"" + ID + "\"}"),
            Json.parse("{\"type\":\"result\",\"subtype\":\"success\",\"session_id\":\"" + ID + "\"}"));

    private SessionIds ids() {
        final SessionIds ids = new ClaudeAgent().definition().sessionIds();
        assertThat(ids).as("claude.yaml declares where its session id is").isNotNull();
        return ids;
    }

    @Test
    void readsTheSessionAnUnattendedRunOpened() {

        assertThat(ids().of(RECORDS)).isEqualTo(ID);
    }

    @Test
    void findsNoSessionWhereNoRecordNamesOne() {

        // The other half: a declaration that took any record's id would pass the test above.
        assertThat(ids().of(List.of(Json.parse("{\"type\":\"assistant\",\"message\":{}}")))).isNull();
    }

    @Test
    void looksForAnAttendedSessionWhereItsFileIsNamedAfterTheSameId() {

        // Measured: the run above left ~/.claude/projects/-workspace/<id>.jsonl, the same id.
        assertThat(ids().directory()).isEqualTo(".claude/projects/-workspace");
        assertThat(ids().suffix()).isEqualTo(".jsonl");
        assertThat(SessionIds.isId(ID)).isTrue();
    }
}
