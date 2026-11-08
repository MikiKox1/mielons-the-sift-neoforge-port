"""Read-only full-server RCON probe of actual naturally generated main portal.

Creates no world. Uses only a strictly validated disposable localhost server.
Stops that server after test; preserves an immutable result record.
"""
from pathlib import Path
import argparse
import json
import socket
import time
from probe_disposable_world import packet, receive, validate_response

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "run/server-T11-natural-26.3.0.101-mainportal-alpha2"
TEST = "the_sift_tests:m26317_natural_sift_main_roundtrip"
MARKER = "PRIME2_M26317_NATURAL_RETURN_PASS"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("phase", choices=["red","green"])
    args = parser.parse_args()
    props = dict(s.split("=",1) for s in (SERVER/"server.properties").read_text(encoding="ascii").splitlines()
                 if "=" in s and not s.startswith("#"))
    for name, value in {"server-ip":"127.0.0.1","server-port":"25646","enable-rcon":"true",
                        "rcon.port":"25647","level-name":"prime2-natural-portal-test"}.items():
        if props.get(name) != value:
            raise RuntimeError("Refusing untrusted or non-disposable target: "+name)
    if len(props.get("rcon.password","")) < 32:
        raise RuntimeError("No ephemeral RCON credential")
    output=ROOT/"evidence"/f"M26317-NATURAL-{args.phase.upper()}.json"
    if output.exists():
        raise FileExistsError("Refusing to overwrite prior evidence")
    events=[]
    result="NOT_RUN"
    for _ in range(110):
        try:
            connection=socket.create_connection(("127.0.0.1",25647),timeout=2)
            break
        except OSError:
            time.sleep(1)
    else:
        raise RuntimeError("Own disposable server did not start")
    try:
        with connection:
            connection.settimeout(80)
            connection.sendall(packet(1,3,props["rcon.password"]))
            if receive(connection)[:2] != (1,2):
                raise RuntimeError("Private RCON authentication failed")
            sequence=1
            def cmd(text):
                nonlocal sequence
                sequence+=1
                connection.sendall(packet(sequence,2,text))
                rid,kind,response=receive(connection)
                if (rid,kind)!=(sequence,0):
                    raise RuntimeError("RCON sequence mismatch")
                validate_response(response)
                events.append({"command":text,"response":response[:1200]})
                return response
            log=SERVER/"logs/latest.log"
            size=log.stat().st_size
            reply=cmd("test run "+TEST)
            if "Running 1 test" not in reply:
                raise AssertionError("Natural portal GameTest not registered: "+reply)
            for i in range(120):
                with log.open("rb") as f:
                    f.seek(size)
                    tail=f.read().decode("utf8","replace")
                if MARKER in tail:
                    result="PASS";break
                if "m26317_natural_sift_main_roundtrip failed" in tail:
                    result="FAIL";break
                time.sleep(1)
            events.append({"result":result})
            cmd("save-all flush")
            cmd("stop")
    finally:
        output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(json.dumps({"phase":args.phase,"result":result,"events":events,
                        "scope":"New disposable real Sift main portal with registered GameTest ServerPlayer",
                        "server":str(SERVER)},indent=2)+"\n",encoding="utf8")
    print("NATURAL_"+args.phase.upper()+"_RESULT="+result,flush=True)
    if args.phase=="red" and result=="PASS":
        raise AssertionError("Natural frame does not reproduce bug under these conditions")
    if args.phase=="green" and result!="PASS":
        raise AssertionError("Natural Sift main portal failed to return a registered test player")

if __name__ == "__main__":
    main()
