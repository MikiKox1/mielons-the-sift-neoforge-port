"""Run one real-player main-portal GameTest on an isolated full Sift server.

Never attaches to the user's Modrinth process, saves, or public servers.
Expected RED and subsequent GREEN are two separate, preserved records.
"""
import argparse
import json
import socket
import time
from pathlib import Path

from probe_disposable_world import packet, receive, validate_response

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-T11-26.3.0.101-mc263-routing"
LOG = SERVER / "logs/latest.log"
TEST = "the_sift_tests:m26317_real_player_main_roundtrip"
PASS = "PRIME2_M26317_REAL_PLAYER_PASS"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("phase", choices=("red", "green"))
    args = parser.parse_args()
    expected = {"server-ip": "127.0.0.1", "server-port": "25644",
                "enable-rcon": "true", "rcon.port": "25645",
                "level-name": "prime2-mainportal-alpha2-disposable"}
    props = dict(line.split("=", 1) for line in
                 (SERVER / "server.properties").read_text(encoding="ascii").splitlines()
                 if "=" in line and not line.startswith("#"))
    if any(props.get(key) != val for key, val in expected.items()) or len(props.get("rcon.password", "")) < 32:
        raise RuntimeError("Refusing non-disposable server or missing private RCON credential")
    evidence = ROOT / "evidence" / ("M26317-PLAYER-ROUNDTRIP-" + args.phase.upper() + ".json")
    if evidence.exists():
        raise FileExistsError("Previous evidence must be preserved: " + str(evidence))
    events = []
    result = "NOT_RUN"
    for _ in range(100):
        try:
            connection = socket.create_connection(("127.0.0.1", 25645), timeout=2)
            break
        except OSError:
            time.sleep(1)
    else:
        raise RuntimeError("Isolated main-portal server never started")
    try:
        with connection:
            connection.settimeout(60)
            connection.sendall(packet(1, 3, props["rcon.password"]))
            if receive(connection)[:2] != (1, 2):
                raise RuntimeError("Private QA RCON auth failed")
            sequence = 1

            def command(text):
                nonlocal sequence
                sequence += 1
                connection.sendall(packet(sequence, 2, text))
                rid, kind, answer = receive(connection)
                if (rid, kind) != (sequence, 0):
                    raise RuntimeError("RCON sequence mismatch")
                validate_response(answer)
                events.append({"command": text, "response": answer[:1200]})
                return answer

            initial_len = LOG.stat().st_size if LOG.is_file() else 0
            response = command("test run " + TEST)
            if "Running 1 test" not in response:
                raise AssertionError("Targeted player GameTest not registered: " + response)
            for _ in range(120):
                with LOG.open("rb") as source:
                    source.seek(initial_len)
                    text = source.read().decode("utf8", errors="replace")
                if PASS in text:
                    result = "PASS"
                    break
                if "m26317_real_player_main_roundtrip failed" in text:
                    result = "FAIL"
                    break
                time.sleep(1)
            events.append({"result": result})
            command("save-all flush")
            command("stop")
    finally:
        evidence.parent.mkdir(parents=True, exist_ok=True)
        evidence.write_text(json.dumps({
            "phase": args.phase, "result": result, "test": TEST,
            "server": str(SERVER), "events": events,
            "scope": "Full Sift dimension; registered mock ServerPlayer, never owner profile",
        }, indent=2) + "\n", encoding="utf8")
    print(f"M26317_{args.phase.upper()}_RESULT={result}", flush=True)
    if args.phase == "red" and result == "PASS":
        raise AssertionError("Player bug did not reproduce in this test setup; do not claim RED")
    if args.phase == "green" and result != "PASS":
        raise AssertionError("Real player main-portal route did not pass")


if __name__ == "__main__":
    main()
