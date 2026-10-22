Feature: A subscription is renewed only on the host

    Background:
        Given the suite runs as an unprivileged user

    @slow
    Scenario: a subscription's refresh token stays outside its running task
        # A subscription's refresh token must never reach a task, which could spend or steal the login.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And a project called "subscription" of class "guarded" with a file in it
        When a script runs:
            """
            set -eu
            umask 077
            mkdir "$PORELO_VAULT.subscription"
            cd "$PORELO_VAULT.subscription"
            python3 - <<'PY'
            import json, pathlib, secrets, time
            suffix = secrets.token_hex(24)
            fixture = {name: name + "-" + suffix for name in ("access", "refresh", "rotated", "renewed")}
            pathlib.Path("fixture.json").write_text(json.dumps(fixture))
            pathlib.Path("credentials").mkdir()
            pathlib.Path("credentials/.credentials.json").write_text(json.dumps({"claudeAiOauth": {
                "accessToken": fixture["access"], "refreshToken": fixture["refresh"],
                "expiresAt": int(time.time() * 1000) - 60000}}))
            pathlib.Path("requests.jsonl").touch()
            PY
            cat > server.py <<'PY'
            import http.server, json, pathlib, time, urllib.parse

            root = pathlib.Path(__file__).resolve().parent
            fixture = json.loads((root / "fixture.json").read_text())

            class Endpoint(http.server.BaseHTTPRequestHandler):
                expected = fixture["refresh"]
                timeout = 5

                def log_message(self, *args):
                    pass

                def do_POST(self):
                    size = int(self.headers.get("Content-Length", "0"))
                    if not 0 < size <= 8192:
                        self.send_error(400)
                        return
                    form = urllib.parse.parse_qs(self.rfile.read(size).decode("utf-8"))
                    grant = form.get("grant_type", [""])[0]
                    matched = form.get("refresh_token", [""])[0] == Endpoint.expected
                    client_id_matched = form.get("client_id", [""])[0] == "9d1c250a-e61b-44d9-88ed-5944d1962f5e"
                    accepted = self.path == "/token" and grant == "refresh_token" and matched and client_id_matched
                    with (root / "requests.jsonl").open("a") as log:
                        log.write(json.dumps({"time": time.time(),
                            "grant_type": "refresh_token" if grant == "refresh_token" else "other",
                            "accepted": accepted, "client_id_matched": client_id_matched,
                            "matched": matched, "loopback": self.client_address[0] == "127.0.0.1"}) + "\n")
                    answer = {"access_token": fixture["renewed"], "refresh_token": fixture["rotated"],
                              "expires_in": 3600} if accepted else {"error": "invalid_grant"}
                    if accepted:
                        Endpoint.expected = fixture["rotated"]
                    body = json.dumps(answer).encode()
                    self.send_response(200 if accepted else 400)
                    self.send_header("Content-Type", "application/json")
                    self.send_header("Content-Length", str(len(body)))
                    self.end_headers()
                    self.wfile.write(body)

            # The vault hook removes this directory on failure; no listener may outlive that cleanup.
            with http.server.HTTPServer(("127.0.0.1", 0), Endpoint) as server:
                server.timeout = 1
                (root / "port").write_text(str(server.server_port))
                deadline = time.monotonic() + 1200
                while root.exists() and not (root / "stop").exists() and time.monotonic() < deadline:
                    server.handle_request()
            if (root / "stop").exists():
                (root / "stopped").touch()
            PY
            cat > check.py <<'PY'
            import io, json, os, pathlib, subprocess, sys, tarfile

            root = pathlib.Path(__file__).resolve().parent
            fixture = json.loads((root / "fixture.json").read_text())

            def run(*args, include_stderr=False):
                result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
                assert result.returncode == 0, "command " + repr(args) + " exited " + str(result.returncode)
                return result.stdout + result.stderr if include_stderr else result.stdout

            def absent(label, data):
                joined = data.replace(b"\r", b"").replace(b"\n", b"")
                for name, kind in (("refresh", "original refresh"), ("rotated", "rotated refresh"),
                                   ("access", "original access"), ("renewed", "renewed access")):
                    token = fixture[name].encode()
                    assert token not in data and token not in joined, kind + " token found in " + label

            def renewal():
                rows = [json.loads(line) for line in (root / "requests.jsonl").read_text().splitlines()]
                assert len(rows) == 1, "expected exactly one host renewal, got " + str(len(rows))
                row = rows[0]
                assert row["grant_type"] == "refresh_token" and row["matched"] and row["loopback"], (
                    "expected a loopback refresh with the original token, got " + json.dumps(row))
                assert row["accepted"] and row["client_id_matched"], "renewal was not accepted for Claude Code"
                assert isinstance(row["time"], (float, int)), "renewal has no request time"
                print("exactly one host renewal with the expected refresh token")

            def scan(task, state):
                absent("the vault file", pathlib.Path(os.environ["PORELO_VAULT"]).read_bytes())
                inspected = run("podman", "inspect", task)
                absent("container inspection", inspected)
                container = json.loads(inspected)[0]
                assert container["State"]["Running"], "expected a running task"
                configured = dict(item.split("=", 1) for item in container["Config"]["Env"])
                token = configured.get("CLAUDE_CODE_OAUTH_TOKEN", "")
                assert token and token not in fixture.values(), "expected the task's own OAuth token"
                environment = run("podman", "exec", task, "env")
                assert b"CLAUDE_CODE_OAUTH_TOKEN=" in environment, "OAuth environment was not read"
                absent("environment inside the task", environment)
                archive = run("podman", "exec", task, "tar", "-cf", "-", "-C", "/home/agent", ".")
                count = 0
                with tarfile.open(fileobj=io.BytesIO(archive)) as home:
                    for entry in home:
                        absent("home file name", entry.name.encode())
                        absent("home link target name", entry.linkname.encode())
                        if entry.isfile():
                            count += 1
                            absent("a file under /home/agent", home.extractfile(entry).read())
                assert count > 0, "expected files under /home/agent, got none"
                absent("podman logs", run("podman", "logs", task, include_stderr=True))
                # task.log is not there in every task: whatever the state directory holds is searched.
                logs = pathlib.Path(state)
                count_logs = 0
                for directory, dirs, files in os.walk(logs, onerror=lambda error: sys.exit(str(error))):
                    for name in dirs + files:
                        absent("a task state file name", (pathlib.Path(directory) / name).as_posix().encode())
                    for name in files:
                        path = pathlib.Path(directory) / name
                        if path.is_file():
                            count_logs += 1
                            absent("a task state file or log", path.read_bytes())
                assert count_logs > 0, "expected task state files and logs, got none"
                print("all subscription tokens absent from the vault file, inspection, environment, "
                      + str(count) + " home files, podman logs and " + str(count_logs) + " task state files")

            if sys.argv[1] == "renewal":
                renewal()
            elif sys.argv[1] == "scan":
                scan(sys.argv[2], sys.argv[3])
            elif sys.argv[1] == "login":
                absent("vault login output", (root / "login.log").read_bytes())
            else:
                raise AssertionError("unknown subscription check")
            PY
            python3 - <<'PY'
            import pathlib, subprocess
            with pathlib.Path("server.log").open("wb") as log:
                subprocess.Popen(["python3", str(pathlib.Path("server.py").resolve())],
                    stdin=subprocess.DEVNULL, stdout=log, stderr=log, start_new_session=True)
            PY
            """
        Then it exits zero
        And within 10 seconds this script exits zero:
            """
            python3 - <<'PY'
            import os, pathlib, socket
            root = pathlib.Path(os.environ["PORELO_VAULT"] + ".subscription")
            port = int((root / "port").read_text())
            with socket.create_connection(("127.0.0.1", port), timeout=2):
                pass
            assert not (root / "requests.jsonl").read_text(), "unexpected request before login"
            PY
            """
        When a script runs:
            """
            set -eu
            code=0
            porelo vault login claude --credentials-from "$PORELO_VAULT.subscription/credentials" --token-url http://127.0.0.1:$(cat "$PORELO_VAULT.subscription/port")/token > "$PORELO_VAULT.subscription/login.log" 2>&1 || code=$?
            python3 "$PORELO_VAULT.subscription/check.py" login
            cat "$PORELO_VAULT.subscription/login.log"
            exit "$code"
            """
        Then it exits zero
        And its output contains "(oauth,"
        And its output contains "renewable against http://127.0.0.1:"
        When a task is started in "subscription" for the "claude" agent and left running
        # The broker authenticates before refusing a grant, triggering host renewal without a model call.
        And the task's container runs:
            """
            set -eu
            command -v curl >/dev/null
            test -S "$ANTHROPIC_UNIX_SOCKET"
            test -n "$CLAUDE_CODE_OAUTH_TOKEN"
            for request in 1 2; do
                reply=$(printf 'header = "Authorization: Bearer %s"\n' "$CLAUDE_CODE_OAUTH_TOKEN" | curl --config - --silent --show-error --max-time 10 --unix-socket "$ANTHROPIC_UNIX_SOCKET" --data 'grant_type=refresh_token&refresh_token=not-a-token' --write-out '\n%{http_code}' http://platform.claude.com/v1/oauth/token)
                status=$(printf '%s\n' "$reply" | tail -1)
                test "$status" = 403 && printf '%s\n' "$reply" | grep -qF 'the broker holds this credential and buys or renews its tokens itself' || { echo "broker request $request: HTTP=$status"; exit 1; }
            done
            echo 'the broker refused renewal inside the task'
            """
        Then it exits zero
        And its output contains "the broker refused renewal"
        And a script runs "python3 \"$PORELO_VAULT.subscription/check.py\" renewal"
        Then it exits zero
        And its output contains "exactly one host renewal"
        When a script runs "python3 \"$PORELO_VAULT.subscription/check.py\" scan {task} {state}" about the task
        Then it exits zero
        And its output contains "all subscription tokens absent"
        # Claude Code's own token URL is not probed: the CLI quits without its host, so a task reaches
        # it. What keeps a renewal out of the task there is that it holds no refresh token, as searched.
        When a script runs:
            """
            set -eu
            cat > "$PORELO_VAULT.subscription/probe.sh" <<'SH'
            set -eu
            command -v curl >/dev/null
            for host in 127.0.0.1 169.254.1.2; do
                code=0
                status=$(curl --silent --show-error --noproxy '*' --max-time 5 --output /dev/null --write-out '%{http_code}' --data 'grant_type=refresh_token&refresh_token=not-a-token' "http://$host:$1/token") || code=$?
                test "$code" -ne 0 && test "$status" = 000 || { echo "host loopback endpoint $host answered: exit=$code HTTP=$status"; exit 1; }
            done
            echo 'host loopback token endpoint unreachable from the task'
            SH
            """
        Then it exits zero
        When a script runs "podman exec -i {task} sh -s -- $(cat \"$PORELO_VAULT.subscription/port\") < \"$PORELO_VAULT.subscription/probe.sh\"" about the task
        Then it exits zero
        And its output contains "host loopback token endpoint unreachable"
        When a script runs "python3 \"$PORELO_VAULT.subscription/check.py\" renewal && python3 \"$PORELO_VAULT.subscription/check.py\" scan {task} {state}" about the task
        Then it exits zero
        And its output contains "exactly one host renewal"
        And its output contains "all subscription tokens absent"
        When a script runs "porelo task remove {task} --force && (podman container exists {task}; test $? -eq 1)" about the task
        Then it exits zero
        When a script runs "porelo project unfollow subscription --force"
        Then it exits zero
        When a script runs "touch \"$PORELO_VAULT.subscription/stop\""
        Then it exits zero
        And within 10 seconds this script exits zero:
            """
            python3 - <<'PY'
            import os, pathlib, socket
            root = pathlib.Path(os.environ["PORELO_VAULT"] + ".subscription")
            assert (root / "stopped").is_file(), "stand-in has not stopped"
            port = int((root / "port").read_text())
            try:
                connection = socket.create_connection(("127.0.0.1", port), timeout=2)
            except ConnectionRefusedError:
                pass
            else:
                connection.close()
                raise AssertionError("stand-in still accepts connections")
            PY
            """
        When a script runs "rm -r \"$PORELO_VAULT.subscription\" && test ! -e \"$PORELO_VAULT.subscription\" && porelo project list"
        Then it exits zero
        And its output does not contain "subscription"
