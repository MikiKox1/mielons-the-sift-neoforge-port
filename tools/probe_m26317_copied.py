"""Isolated world-geometry M263-17 RED/GREEN regression; never opens owner saves."""
from pathlib import Path
import argparse
import json
import socket
import time

from probe_disposable_world import packet, receive, validate_response

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-T11-copied-26.3.0.101-mainportal-alpha2"
TEST = "the_sift_tests:m26317_fractional_underground_return"
MARKER = "PRIME2_M26317_FRACTIONAL_RETURN_PASS"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("phase", choices=("red", "green", "release"))
    args = parser.parse_args()
    props = dict(
        line.split("=", 1)
        for line in (SERVER / "server.properties").read_text(encoding="ascii").splitlines()
        if "=" in line and not line.startswith("#")
    )
    for key, expected in {
        "server-ip": "127.0.0.1", "server-port": "25648",
        "enable-rcon": "true", "rcon.port": "25649",
        "level-name": "prime2-m26317-copied-region",
    }.items():
        if props.get(key) != expected:
            raise RuntimeError("Refusing a non-disposable or unknown target: " + key)
    if len(props.get("rcon.password", "")) < 32:
        raise RuntimeError("Isolated RCON credential missing")
    output = ROOT / "evidence" / f"M26317-FRACTIONAL-{args.phase.upper()}.json"
    if output.exists():
        raise FileExistsError("Preserve earlier regression evidence: " + str(output))
    events = []
    status = "NOT_RUN"
    for _ in range(95):
        try:
            connection = socket.create_connection(("127.0.0.1", 25649), timeout=2)
            break
        except OSError:
            time.sleep(1)
    else:
        raise RuntimeError("Isolated server did not accept local connections")
    try:
        with connection:
            connection.settimeout(60)
            connection.sendall(packet(1, 3, props["rcon.password"]))
            if receive(connection)[:2] != (1, 2):
                raise RuntimeError("QA RCON authorization failed")
            request = 1

            def command(text):
                nonlocal request
                request += 1
                connection.sendall(packet(request, 2, text))
                rid, kind, response = receive(connection)
                if (rid, kind) != (request, 0):
                    raise RuntimeError("Unexpected RCON response sequence")
                validate_response(response)
                events.append({"command": text, "response": response[:500]})
                return response

            log = SERVER / "logs/latest.log"
            start = log.stat().st_size
            reply = command("test run " + TEST)
            if "Running 1 test" not in reply:
                raise AssertionError("Native GameTest missing: " + reply)
            for _ in range(110):
                with log.open("rb") as source:
                    source.seek(start)
                    text = source.read().decode("utf8", errors="replace")
                if MARKER in text:
                    status = "PASS"
                    break
                if "m26317_fractional_underground_return failed" in text:
                    status = "FAIL"
                    break
                time.sleep(1)
            events.append({"status": status})
            command("save-all flush")
            command("stop")
    finally:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps({
            "phase": args.phase, "status": status, "test": TEST,
            "copied_world_only": True, "owner_world_modified": False,
            "events": events, "game_server": str(SERVER),
        }, indent=2) + "\n", encoding="utf8")
    print(f"M26317_{args.phase.upper()}_FRACTIONAL_RESULT={status}", flush=True)
    if args.phase == "red" and status != "FAIL":
        raise AssertionError("Expected original geometry regression RED, got " + status)
    if args.phase in ("green", "release") and status != "PASS":
        raise AssertionError("Expected corrected return GREEN, got " + status)


if __name__ == "__main__":
    main()
