"""Two-process Rift persistence regression on the disposable full Sift server."""
from pathlib import Path
import argparse
import json
import socket
import time

from probe_disposable_world import packet, receive, validate_response

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-T11-26.2.0.93-routing"
TESTS = {
    "prepare": ("the_sift_tests:t11_pair_restart_prepare", "PRIME2_T11_PERSIST_PREP_PASS"),
    "verify": ("the_sift_tests:t11_pair_restart_verify", "PRIME2_T11_PERSIST_VERIFY_PASS"),
}


def run(mode: str) -> None:
    props = dict(
        line.split("=", 1) for line in (SERVER / "server.properties").read_text(encoding="utf-8").splitlines()
        if "=" in line and not line.startswith("#")
    )
    for key, expected in {
        "server-ip": "127.0.0.1", "server-port": "25577", "rcon.port": "25587",
        "enable-rcon": "true", "level-name": "world-T11-routing",
    }.items():
        if props.get(key) != expected:
            raise RuntimeError(f"Refusing to control a non-disposable server: {key}")
    if len(props.get("rcon.password", "")) < 32:
        raise RuntimeError("Missing isolated RCON credential")
    evidence = ROOT / "evidence" / f"T11-persisted-pair-{mode}.json"
    if evidence.exists():
        raise RuntimeError("Do not overwrite prior persistence evidence")
    test, marker = TESTS[mode]
    events = []
    success = False

    for _ in range(90):
        try:
            connection = socket.create_connection(("127.0.0.1", 25587), timeout=2)
            break
        except OSError:
            time.sleep(1)
    else:
        raise RuntimeError("Isolated T11 full server never started RCON")

    with connection:
        connection.settimeout(30)
        connection.sendall(packet(1, 3, props["rcon.password"]))
        if receive(connection)[:2] != (1, 2):
            raise RuntimeError("Local RCON login failed")
        sequence = 1

        def command(text: str) -> str:
            nonlocal sequence
            sequence += 1
            connection.sendall(packet(sequence, 2, text))
            request, kind, response = receive(connection)
            if (request, kind) != (sequence, 0):
                raise RuntimeError("Unexpected RCON response")
            validate_response(response)
            events.append({"command": text, "response": response[:1200]})
            return response

        try:
            reply = command("test run " + test)
            if "Running 1 test" not in reply:
                raise AssertionError(f"Test not available in real server: {test}: {reply}")
            for _ in range(100):
                text = (SERVER / "logs/latest.log").read_text(encoding="utf-8-sig", errors="replace")
                if marker in text:
                    success = True
                    events.append({"observed": marker})
                    break
                if test.split(":")[1] + " failed" in text:
                    raise AssertionError(f"Real server GameTest failed; inspect latest.log: {test}")
                time.sleep(1)
            if not success:
                raise AssertionError(f"No actual PASS marker from {test}")
            command("save-all flush")
        finally:
            evidence.parent.mkdir(parents=True, exist_ok=True)
            evidence.write_text(json.dumps({
                "pass": success,
                "phase": mode,
                "test": test,
                "observed_marker": marker if success else None,
                "server": str(SERVER),
                "events": events,
                "limitations": "Disposable full server; automatic source/target Rift validation, not manual player travel",
            }, indent=2) + "\n", encoding="utf-8")
            try:
                command("stop")
            except OSError:
                pass
    if not success:
        raise RuntimeError(f"Pair {mode} was not verified")
    print(f"PASS persisted Rift pair {mode} on real full server")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=TESTS)
    run(parser.parse_args().mode)
