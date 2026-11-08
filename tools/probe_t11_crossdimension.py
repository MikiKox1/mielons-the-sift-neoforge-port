"""Validate the Rift pair in the isolated normal server (with the Sift dimension).

The vanilla GameTestServer has no Sift dimension, so this strictly localhost
probe runs one explicit dev-only test in the real serverT11 configuration.
"""
from pathlib import Path
import json
import socket
import time

from probe_disposable_world import packet, receive, validate_response

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-T11-26.1.2.126-mc2612-routing"
LOG = SERVER / "logs/latest.log"
EVIDENCE = ROOT / "evidence/MC2612-ALPHA1-five-routes-underground.json"
TESTS = (
    ("the_sift_tests:t11_actual_rift_pair_sift", "PRIME2_T11_CROSSDIM_PASS"),
    ("the_sift_tests:t11_real_main_return", "PRIME2_T11_MAIN_RETURN_PASS"),
    ("the_sift_tests:t11_vehicle_rider_portal_return", "PRIME2_T11_RIDER_RETURN_PASS"),
    ("the_sift_tests:t11_real_sift_rift_out", "PRIME2_T11_BIDIR_PASS"),
    ("the_sift_tests:t11_real_underground_portal", "PRIME2_T11_UNDERGROUND_PASS"),
)


def run() -> None:
    props = dict(
        line.split("=", 1) for line in (SERVER / "server.properties").read_text(encoding="utf-8").splitlines()
        if "=" in line and not line.startswith("#")
    )
    for key, value in {
        "server-ip": "127.0.0.1", "server-port": "25624", "rcon.port": "25625",
        "enable-rcon": "true", "level-name": "world-MC2612-T11-routing",
    }.items():
        if props.get(key) != value:
            raise RuntimeError(f"Refusing a non-disposable server: {key}")
    if len(props.get("rcon.password", "")) < 32 or EVIDENCE.exists():
        raise RuntimeError("Missing secret or existing evidence; refusing overwrite")

    events, passed = [], False
    for _ in range(90):
        try:
            conn = socket.create_connection(("127.0.0.1", 25625), timeout=2)
            break
        except OSError:
            time.sleep(1)
    else:
        raise RuntimeError("T11 server RCON unavailable")

    with conn:
        conn.settimeout(30)
        conn.sendall(packet(1, 3, props["rcon.password"]))
        if receive(conn)[:2] != (1, 2):
            raise RuntimeError("Loopback RCON auth failed")
        sequence = 1

        def command(text: str) -> str:
            nonlocal sequence
            sequence += 1
            conn.sendall(packet(sequence, 2, text))
            rid, kind, response = receive(conn)
            if (rid, kind) != (sequence, 0):
                raise RuntimeError("Unexpected RCON reply")
            validate_response(response)
            events.append({"command": text, "response": response[:2000]})
            return response

        try:
            for test_id, pass_marker in TESTS:
                reply = command("test run " + test_id)
                if "Running 1 test" not in reply:
                    raise AssertionError(f"Exact test not registered: {test_id}: {reply}")
                for _ in range(90):
                    text = LOG.read_text(encoding="utf-8-sig", errors="replace")
                    if pass_marker in text:
                        events.append({"test": test_id, "observed": pass_marker})
                        break
                    if test_id.split(":")[1] + " failed" in text:
                        raise AssertionError(f"Cross-dimension test failed: {test_id}")
                    time.sleep(1)
                else:
                    raise AssertionError(f"No independently logged PASS for {test_id}")
            passed = True
            command("save-all flush")
        finally:
            EVIDENCE.parent.mkdir(parents=True, exist_ok=True)
            EVIDENCE.write_text(json.dumps({
                "pass": passed, "tests": [name for name, _ in TESTS], "server": str(SERVER),
                "source_log": str(LOG), "events": events,
                "scope": "Actual Sift Rift spawned/reused and a Cow teleported Sift-to-Overworld via main entrance; no human player traversal",
            }, indent=2) + "\n", encoding="utf-8")
            try:
                command("stop")
            except OSError:
                pass
    if not passed:
        raise RuntimeError("Cross-dimension Rift pair not confirmed")
    print("PASS: real Sift Rift pairing and main-portal return, disposable server")


if __name__ == "__main__":
    run()
