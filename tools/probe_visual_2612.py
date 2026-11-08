"""Loopback-only commands to the explicitly disposable MC26.1.2 graphics world.

No launcher profiles or player worlds are inspected. No credentials are logged.
Each run records commands/replies as evidence; the caller must name a new file.
"""
from pathlib import Path
import argparse
import json
import socket
import time

from probe_disposable_world import packet, receive, validate_response

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-26.1.2.126-mc2612-visual-qa"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--command", action="append", required=True)
    parser.add_argument("--evidence", required=True)
    args = parser.parse_args()
    if Path(args.evidence).name != args.evidence or not args.evidence.endswith(".json"):
        raise ValueError("Evidence must be a plain new JSON filename")
    out = ROOT / "evidence" / args.evidence
    if out.exists():
        raise FileExistsError("Preserve previous evidence: " + str(out))
    props = dict(line.split("=", 1) for line in (SERVER / "server.properties").read_text(encoding="ascii").splitlines()
                 if "=" in line and not line.startswith("#"))
    for key, expected in {"server-ip": "127.0.0.1", "server-port": "25626",
                          "rcon.port": "25627", "enable-rcon": "true",
                          "level-name": "world-MC2612-visual-disposable"}.items():
        if props.get(key) != expected:
            raise RuntimeError("Not the disposable graphics QA server: " + key)
    if len(props.get("rcon.password", "")) < 32:
        raise RuntimeError("Missing local ephemeral credential")
    events = []
    passed = False
    with socket.create_connection(("127.0.0.1", 25627), timeout=90) as connection:
        connection.settimeout(90)
        connection.sendall(packet(1, 3, props["rcon.password"]))
        if receive(connection)[:2] != (1, 2):
            raise RuntimeError("QA RCON authentication failed")
        try:
            for index, command in enumerate(args.command, 2):
                start = time.perf_counter()
                connection.sendall(packet(index, 2, command))
                rid, kind, reply = receive(connection)
                if (rid, kind) != (index, 0):
                    raise RuntimeError("Unexpected QA response")
                validate_response(reply)
                event = {"command": command, "response": reply,
                         "round_trip_ms": round((time.perf_counter() - start) * 1000, 3)}
                events.append(event)
                print(json.dumps(event, ensure_ascii=False))
            passed = True
        finally:
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps({"commands_valid": passed, "events": events,
                                       "server": str(SERVER),
                                       "scope": "Disposable graphics QA world only; not owner gameplay acceptance"},
                                      ensure_ascii=False, indent=2) + "\n", encoding="utf8")


if __name__ == "__main__":
    main()
