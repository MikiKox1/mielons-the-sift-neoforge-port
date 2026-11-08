"""Verify 2.0.0 -> 2.0.1 save upgrade and persisted gamerule on the disposable server."""
from pathlib import Path
import argparse
import json
import socket
import time

from probe_disposable_world import packet, receive, validate_response

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-26.2.0.93-port-2.0.1"
RULE = "the_sift:spawn_rifts"
DIMENSION = "execute in the_sift:the_sift "


def main(mode):
    props = dict(line.split("=", 1) for line in (SERVER / "server.properties").read_text(encoding="utf-8").splitlines()
                 if "=" in line and not line.startswith("#"))
    for key, value in {"server-ip": "127.0.0.1", "server-port": "25576", "enable-rcon": "true",
                       "rcon.port": "25586", "level-name": "world-2.0.1-upgrade", "level-seed": "2622000"}.items():
        if props.get(key) != value:
            raise RuntimeError(f"Not the intended disposable 2.0.1 server: {key}")
    if len(props.get("rcon.password", "")) < 32:
        raise RuntimeError("Missing private loopback RCON credential")
    result = ROOT / "evidence/2.0.1-server" / f"{mode}-gamerule.json"
    result.parent.mkdir(parents=True, exist_ok=True)
    if result.exists():
        raise RuntimeError("Retain the previous evidence before rerunning")
    events = []
    passed = False
    with socket.create_connection(("127.0.0.1", 25586), timeout=20) as connection:
        connection.settimeout(30)
        connection.sendall(packet(1, 3, props["rcon.password"]))
        request_id, kind, _ = receive(connection)
        if (request_id, kind) != (1, 2):
            raise RuntimeError("Loopback RCON authentication failed")
        sequence = 1

        def command(text, expected=None):
            nonlocal sequence
            sequence += 1
            connection.sendall(packet(sequence, 2, text))
            request_id, kind, response = receive(connection)
            if (request_id, kind) != (sequence, 0):
                raise RuntimeError("Unexpected command response")
            events.append({"command": text, "response": response})
            print(text + " => " + response.strip(), flush=True)
            validate_response(response, expected)
            return response

        try:
            command("seed", "2622000")
            value = "true" if mode == "initial" else "false"
            command(f"gamerule {RULE}", value)
            command(DIMENSION + "run forceload add 0 0")
            for _ in range(40):
                response = command(DIMENSION + "if entity @e[type=the_sift:blub,tag=prime2_persist]")
                if "Test passed. Count: 1" in response:
                    break
                time.sleep(0.25)
            else:
                raise AssertionError("The old 2.0.0 Blub did not load exactly once after upgrade")
            command(DIMENSION + "if block 8 220 8 the_sift:soulshale_bricks", "Test passed")
            command(DIMENSION + "run data get entity @e[type=the_sift:blub,tag=prime2_persist,limit=1] Tags", "prime2_persist")
            requested = "false" if mode == "initial" else "true"
            command(f"gamerule {RULE} {requested}", requested)
            command(f"gamerule {RULE}", requested)
            command("save-all flush", "Saved the game")
            command(DIMENSION + "run forceload remove 0 0", "Unmarked")
            command("save-all flush", "Saved the game")
            passed = True
        finally:
            result.write_text(json.dumps({
                "pass": passed, "mode": mode, "gamerule": RULE,
                "input_world": "Copy of the isolated 2.0.0 world; no user save",
                "initial_expected_value": "true" if mode == "initial" else "false",
                "events": events,
                "limits": "Command/default/persistence and one saved block/entity; not manual traversal or natural-spawn timing acceptance",
            }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            try:
                command("stop")
            except (OSError, ConnectionError):
                print("Server closed RCON during stop; verify the shutdown log")
    if not passed:
        raise RuntimeError("2.0.1 save/gamerule verification failed")
    print("PASS: 2.0.1 gamerule and upgrade persistence", mode)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("initial", "reload"))
    main(parser.parse_args().mode)
