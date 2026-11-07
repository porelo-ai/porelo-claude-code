package org.fuin.porelo.agent.impl.claude;

/**
 * The git hook that ends a task's commit message with the trailer naming the model that served it.
 *
 * <p><strong>Only the session knows its model.</strong> Measured on 2.1.311: the setup is told no
 * model, a run may leave it to the CLI's default, {@code attribution.commit} is written literally
 * with nothing filled in, and a {@code SessionStart} hook's input named none in a {@code -p} run.
 * The session's transcript does: every assistant record carries the model that answered, so a
 * {@code /model} change is followed too.
 *
 * <p><strong>A commit without its trailer is refused, not made bare.</strong> The forge's checks
 * would find it only after the push, and a missing attribution there is a commit to rewrite.
 *
 * <p>POSIX {@code sh} with {@code grep} and {@code sed}, since a project's own image may lack
 * {@code python3}, and git is there wherever a commit is made.
 */
final class ClaudeCommitTrailer {

    /** The folder git is told to take its hooks from. */
    static final String HOOKS = "/home/agent/.config/porelo/git-hooks";

    /** The hook itself. */
    static final String HOOK = HOOKS + "/commit-msg";

    private static final String SCRIPT =
            """
            #!/bin/sh
            # Ends a task's commit message with the trailer naming the model that served the session.
            set -eu
            msg=$1
            rc=0
            grep -v -i '^co-authored-by:.*<noreply@anthropic\\.com>' "$msg" >"$msg.porelo" || rc=$?
            if [ "$rc" -gt 1 ]; then
                exit "$rc"
            fi
            mv "$msg.porelo" "$msg"
            # What git keeps once it strips comments and the verbose diff: nothing, and git aborts.
            if ! sed '/^# -* >8 -*$/,$d; /^#/d' "$msg" | grep -q '[^[:space:]]'; then
                exit 0
            fi
            projects=${CLAUDE_CONFIG_DIR:-$HOME/.claude}/projects
            # The model of the record that answered, as 2.1.311 writes it after the content and a
            # later CLI first in the message; never one inside a tool call's input or result.
            model_in() {
                grep -a '"model":"' "$1" |
                    sed -n -e 's/"message":{"model":"\\([^"]*\\)","id":".*/\t\\1/' -e 's/^.*\t//p' \\
                        -e 's/.*}],"model":"\\([^"]*\\)","stop_reason":.*/\\1/p' |
                    grep -v '^<synthetic>$' | tail -n 1
            }
            plain() {
                case $1 in
                '' | *[!A-Za-z0-9._/:@-]*) return 1 ;;
                esac
            }
            model=
            own=
            if [ -n "${CLAUDE_CODE_SESSION_ID:-}" ]; then
                for f in "$projects"/*/"$CLAUDE_CODE_SESSION_ID".jsonl; do
                    if [ -f "$f" ]; then
                        own=$f
                        model=$(model_in "$f") || true
                        break
                    fi
                done
            fi
            if [ -z "$own" ]; then
                for f in $(ls -t "$projects"/*/*.jsonl 2>/dev/null || true); do
                    model=$(model_in "$f") || true
                    if [ -n "$model" ]; then
                        break
                    fi
                done
            fi
            why="no model found in the session's transcript under $projects"
            if [ -n "$model" ] && ! plain "$model"; then
                why="the model the session's transcript names is no plain identifier, so it is not written"
                model=
            fi
            if [ -z "$model" ]; then
                # Kept only when every Assisted-by trailer names a tool and a plain model.
                written=$(git interpret-trailers --parse "$msg" | grep '^Assisted-by: ' || true)
                if [ -n "$written" ] && ! printf '%s\\n' "$written" |
                    grep -q -v -E '^Assisted-by: [^,]+, [A-Za-z0-9._/:@-]+$'; then
                    exit 0
                fi
                echo "commit-msg: $why; end the message with 'Assisted-by: Claude Code, <model>'" \\
                    "yourself" >&2
                exit 1
            fi
            git interpret-trailers --in-place --if-exists replace \\
                --trailer "Assisted-by: Claude Code, $model" "$msg"
            """;

    private ClaudeCommitTrailer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the hook's script.
     *
     * @return File content.
     */
    static String script() {
        return SCRIPT;
    }

    /**
     * Returns the command that makes the hook one git runs.
     *
     * <p>A file the setup writes carries no mode, and git skips a hook that is not executable, so
     * the session's start makes it one before the agent's first turn.
     *
     * @return Shell command.
     */
    static String installCommand() {
        return installCommand(HOOKS);
    }

    /**
     * Returns the command that makes the hook in a given folder one git runs.
     *
     * @param hooks The folder that holds the hook.
     * @return Shell command.
     */
    static String installCommand(final String hooks) {
        return "chmod +x " + hooks + "/commit-msg && git config --global core.hooksPath " + hooks;
    }
}
