"""Command ONLY the explicitly configured loopback development server.

Reuses the earlier 1.x QA workflow (RCON + copied-world save/reopen checks),
implemented with the standard library rather than installing a new dependency.
No access to launcher profiles, real player saves or remote server credentials.
"""
from pathlib import Path
import argparse
import json
import re
import socket
import struct
import time

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-26.2.0.93-port-2.0.0"
DIMENSION = "execute in the_sift:the_sift "
PREFIX = DIMENSION + "run "


def packet(request_id: int, kind: int, text: str) -> bytes:
    body = struct.pack("<ii", request_id, kind) + text.encode("utf-8") + b"\0\0"
    return struct.pack("<i", len(body)) + body


def receive(connection: socket.socket) -> tuple[int, int, str]:
    def exactly(count: int) -> bytes:
        result = bytearray()
        while len(result) < count:
            part = connection.recv(count - len(result))
            if not part:
                raise ConnectionError("Local RCON connection closed")
            result.extend(part)
        return bytes(result)
    size = struct.unpack("<i", exactly(4))[0]
    if size < 10 or size > 1024 * 1024:
        raise ValueError("Invalid RCON packet length")
    body = exactly(size)
    if not body.endswith(b"\0\0"):
        raise ValueError("Invalid RCON packet terminators")
    request_id, kind = struct.unpack("<ii", body[:8])
    return request_id, kind, body[8:-2].decode("utf-8", errors="strict")


def validate_response(response: str, expected: str | None = None) -> None:
    if any(error in response for error in ("<--[HERE]", "Incorrect argument", "Unknown or incomplete command")):
        raise AssertionError(f"The probe sent an invalid command: {response}")
    if expected is not None and expected not in response:
        raise AssertionError(f"Expected {expected!r}, got {response!r}")


def main(mode: str) -> None:
    settings = dict(line.split("=", 1) for line in (SERVER / "server.properties").read_text(encoding="utf-8").splitlines()
                    if "=" in line and not line.startswith("#"))
    required = {"server-ip": "127.0.0.1", "server-port": "25576", "enable-rcon": "true",
                "rcon.port": "25586", "level-name": "world-2.0-startup", "level-seed": "2622000"}
    for key, expected in required.items():
        if settings.get(key) != expected:
            raise RuntimeError(f"Refusing a non-disposable server configuration: {key}")
    if len(settings.get("rcon.password", "")) < 32:
        raise RuntimeError("Expected the newly generated private loopback RCON secret")
    evidence = ROOT / "evidence/server-world"
    evidence.mkdir(parents=True, exist_ok=True)
    result = evidence / f"{mode}.json"
    if result.exists():
        raise RuntimeError(f"Evidence exists; preserve it before rerunning: {result}")
    events = []
    passed = False
    with socket.create_connection(("127.0.0.1", 25586), timeout=90) as connection:
        connection.settimeout(90)
        connection.sendall(packet(1, 3, settings["rcon.password"]))
        response_id, response_type, _ = receive(connection)
        if response_id != 1 or response_type != 2:
            raise RuntimeError("Local RCON authentication failed")
        sequence = 1

        def command(text: str, expected: str | None = None) -> str:
            # shortcut: these replies are short; add multi-packet handling before requesting full entity NBT.
            nonlocal sequence
            sequence += 1
            connection.sendall(packet(sequence, 2, text))
            response_id, response_type, response = receive(connection)
            if response_id != sequence or response_type != 0:
                raise RuntimeError("Unexpected RCON command response")
            events.append({"command": text, "response": response})
            print(text + " => " + response[:420], flush=True)
            validate_response(response, expected)
            return response

        try:
            command("seed", "2622000")
            command(PREFIX + "time query gametime")
            command(PREFIX + "forceload add 0 0")
            # Reopening a saved forced chunk legitimately reports that no NEW
            # chunks were marked. Query the state instead of interpreting that as failure.
            forced = command(PREFIX + "forceload query 0 0").lower()
            if "marked for force loading" not in forced or "not marked" in forced:
                raise AssertionError("Disposable Sift chunk is not force loaded")
            if mode == "initial":
                command(PREFIX + "setblock 8 220 8 the_sift:soulshale_bricks", "Changed")
                command(PREFIX + 'summon the_sift:blub 8.5 221 8.5 {NoAI:1b,PersistenceRequired:1b,Tags:["prime2_persist"]}', "Summoned")
                for biome in ("sift_wastes", "overgrown_clearing", "overgrown_forest", "ichor_snowy_peaks",
                              "corrupted_sift", "coral_meadow", "coral_forest", "organic_desert",
                              "overgrown_caves", "siftslate_caves", "soulshale_caves"):
                    y = 32 if biome.endswith("_caves") else 96
                    response = command(f"execute in the_sift:the_sift positioned 0 {y} 0 run locate biome the_sift:{biome}")
                    if not re.search(r"at \[-?\d+,\s*-?\d+,\s*-?\d+\]", response):
                        raise AssertionError(f"Biome not located in this bounded seed sample: {biome}")
            # Terminal `execute if` returns command feedback directly. `say`
            # broadcasts to players/log, not necessarily to this RCON client.
            command(DIMENSION + "if block 8 220 8 the_sift:soulshale_bricks", "Test passed")
            # forceload initiates asynchronous entity-section loading; wait for
            # the saved entity to become selectable rather than assuming it is immediate.
            for attempt in range(40):
                found = command(DIMENSION + "if entity @e[type=the_sift:blub,tag=prime2_persist]")
                if "Test passed" in found:
                    break
                time.sleep(0.25)
            else:
                raise AssertionError("Saved test entity did not become available in the force-loaded chunk")
            command(PREFIX + "data get entity @e[type=the_sift:blub,tag=prime2_persist,limit=1] Tags", "entity data")
            command("save-all flush", "Saved")
            command(PREFIX + "forceload remove 0 0", "Unmarked")
            command("save-all flush", "Saved")
            passed = True
        finally:
            result.write_text(json.dumps({
                "mode": mode, "pass": passed, "seed": 2622000,
                "server": str(SERVER), "commands": events,
                "limits": "Disposable server-only dimension/biome sample and block/entity save persistence, not full player gameplay or visual QA",
            }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            try:
                command("stop")
            except (OSError, ConnectionError):
                print("Server closed RCON during stop; verify saved-world shutdown in log")
    if not passed:
        raise RuntimeError("Server world probe failed")
    print(f"PASS: disposable world {mode}; evidence={result}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("initial", "reload"))
    main(parser.parse_args().mode)
