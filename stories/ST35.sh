#!/usr/bin/env bash
# Story ST35, "Refuse an incompatible protocol and a meaningless option", played as a person at the
# terminal would, on a test VM (CC66, part of ST347). Test tooling: it is proven by its run, not by
# tests of its own.
#
#   stories/ST35.sh PACKAGES RESTORE      (installs with sudo dpkg -i, the only install the VM allows)
#
# PACKAGES holds what the plays install: porelo_*.deb built with B230 (it provides
# porelo-agent-protocol at its lowest) and porelo-agent-claude_*.deb built with CC22. RESTORE holds
# the porelo and porelo-agent-claude debs the VM had before, which also serve as the day's Porelo
# without the provide; they are put back at the end, after a failure too.
#
# It prints one line per play and the story's verdict last: passed, failed at <play>, or waiting
# on <issue> where the issue a play needs is not in Review yet (WAITING names them).
set -uo pipefail

PACKAGES="${1:?the directory with the packages to play}"
RESTORE="${2:?the directory with the packages to put back}"
# The issues not yet in Review, whose plays are expected to fail; the Coordinator's word on the day.
WAITING="${WAITING:-}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# The test VM's accounts may run dpkg through sudo and nothing else that installs.
DPKG=(sudo -n dpkg)
VERDICT=passed

one() {
    local pattern="$1" dir="$2" found
    mapfile -t found < <(compgen -G "$dir/$pattern")
    [ -f "${found[0]}" ] || {
        echo "ST35: no $pattern in $dir" >&2
        exit 2
    }
    [ "${#found[@]}" -eq 1 ] || {
        echo "ST35: more than one $pattern in $dir" >&2
        exit 2
    }
    printf '%s\n' "$(cd "$dir" && pwd)/${found[0]##*/}"
}

PORELO="$(one 'porelo_*.deb' "$PACKAGES")" || exit 2
ADAPTER="$(one 'porelo-agent-claude_*.deb' "$PACKAGES")" || exit 2
OLD_PORELO="$(one 'porelo_*.deb' "$RESTORE")" || exit 2
OLD_ADAPTER="$(one 'porelo-agent-claude_*.deb' "$RESTORE")" || exit 2

# What a play prints and its outcome; a play that fails while its issue is still waiting says so.
play() {
    local name="$1" issue="$2" ok="$3"
    if [ "$ok" = yes ]; then
        echo "ST35 play $name: passed"
    elif [ -n "$issue" ] && [[ " $WAITING " == *" $issue "* ]]; then
        echo "ST35 play $name: waiting on $issue"
        [ "$VERDICT" = passed ] && VERDICT="waiting on $issue"
    else
        echo "ST35 play $name: FAILED"
        [[ "$VERDICT" == failed* ]] || VERDICT="failed at $name"
    fi
}

# A copy of B230's porelo that provides the protocol at another lowest, as a Porelo before or after
# a break would; its version says what it is, so apt takes it as a package of its own.
stand_in() {
    local lowest="$1"
    local dir="$WORK/porelo-$lowest"
    dpkg-deb -R "$PORELO" "$dir"
    sed -i -e "s/^Provides: porelo-agent-protocol (= [0-9]*)/Provides: porelo-agent-protocol (= $lowest)/" \
        -e "s/^Version: \(.*\)$/Version: \1+st35.$lowest/" "$dir/DEBIAN/control"
    grep -q "^Provides: porelo-agent-protocol (= $lowest)" "$dir/DEBIAN/control" ||
        {
            echo "ST35: B230's porelo provides no porelo-agent-protocol to repack" >&2
            return 1
        }
    dpkg-deb --root-owner-group -b "$dir" "$WORK/porelo-$lowest.deb" >/dev/null
    printf '%s\n' "$WORK/porelo-$lowest.deb"
}

installed() {
    dpkg-query -W -f='${Status}' porelo-agent-claude 2>/dev/null | grep -q 'install ok installed'
}

# The adapter beside the porelo given: installs, or is refused naming what is missing. A refused
# dpkg -i leaves the adapter unpacked and not configured, so it is taken off again.
adapter_beside() {
    local porelo="$1" status
    "${DPKG[@]}" -r porelo-agent-claude >/dev/null 2>&1
    "${DPKG[@]}" -i "$porelo" >"$WORK/porelo.log" 2>&1 ||
        {
            echo "    could not install $(basename "$porelo"):"
            sed 's/^/      /' "$WORK/porelo.log"
            return 2
        }
    "${DPKG[@]}" -i "$ADAPTER" >"$WORK/adapter.log" 2>&1
    status=$?
    grep -E 'porelo-agent-protocol' "$WORK/adapter.log" | sed 's/^/    /'
    installed || "${DPKG[@]}" -r porelo-agent-claude >/dev/null 2>&1
    return "$status"
}

restore() {
    echo "-- putting back $(basename "$OLD_PORELO") and $(basename "$OLD_ADAPTER")"
    "${DPKG[@]}" -r porelo-agent-claude >/dev/null 2>&1
    if "${DPKG[@]}" -i "$OLD_PORELO" >/dev/null 2>&1 && "${DPKG[@]}" -i "$OLD_ADAPTER" >/dev/null 2>&1; then
        dpkg-query -W -f='    ${Package} ${Version}\n' porelo porelo-agent-claude
    else
        echo "ST35: COULD NOT PUT BACK the VM's packages; dpkg -l 'porelo*' shows what is there"
    fi
}

trap 'restore; rm -rf "$WORK"' EXIT

echo "-- ST35 on $(. /etc/os-release && echo "$PRETTY_NAME"), as $(id -un); waiting: ${WAITING:-none}"

# 1. Compatible: the adapter installs beside a Porelo it speaks with, and Porelo lists it.
echo "-- play compatible: $(basename "$PORELO"), then $(basename "$ADAPTER")"
ok=no
# Listed from the package's own directory: a copy under ~/.local/share/porelo/agents would shadow it.
if adapter_beside "$PORELO" && installed; then
    porelo agents | awk '$1 == "claude" { print "    listed: " $0 }'
    porelo agents | awk '$1 == "claude" && $NF ~ /^\/usr\/libexec\/porelo\/agents\// { f = 1 } END { exit !f }' && ok=yes
fi
play compatible CC22 "$ok"

# 2. and 3. A Porelo whose lowest has passed the adapter, and one older than the adapter's build.
for case in "6 <=" "2 >="; do
    set -- $case
    echo "-- play lowest-$1: a porelo providing the protocol at $1, then the adapter"
    ok=no
    if standin="$(stand_in "$1")"; then
        adapter_beside "$standin"
        status=$?
        [ "$status" -ne 0 ] && [ "$status" -ne 2 ] &&
            grep -q "porelo-agent-protocol ($2 [0-9]*)" "$WORK/adapter.log" &&
            ! installed && ok=yes
    fi
    play "lowest-$1" B230 "$ok"
done

# 4. The day's Porelo, from before the provide: refused by name.
echo "-- play no-provide: $(basename "$OLD_PORELO"), then the adapter"
ok=no
adapter_beside "$OLD_PORELO"
status=$?
[ "$status" -ne 0 ] && [ "$status" -ne 2 ] && grep -q 'porelo-agent-protocol' "$WORK/adapter.log" &&
    ! installed && ok=yes
play no-provide CC22 "$ok"

# 5. --now: refused as an option task start does not have, not accepted without an effect.
echo "-- play now: porelo task start --now --detach, in an empty directory"
ok=no
out="$(cd "$WORK" && porelo task start --now --detach 2>&1)"
status=$?
echo "$out" | sed 's/^/    /' | head -3
[ "$status" -ne 0 ] && echo "$out" | grep -q -- "'--now' is not an option" && ok=yes
play now B201 "$ok"

echo "ST35: $VERDICT"
[ "$VERDICT" = passed ]
