"""Compile and run real pure-logic contracts against original and migrated classes.

Uses the existing JDK and exact target libraries, no game launch or mock API.
"""
from pathlib import Path
import hashlib
import json
import os
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
EXPECTED_SHA = "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72"
JAVA_HOME = Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Java\jdk-25.0.4.1"))
LIBS = (ROOT / "build/port-compile-classpath.txt").read_text(encoding="utf-8-sig").splitlines()
TEST = ROOT / "src/coreTest/java/mielon/thesift/worldgen/PortCoreContractTest.java"
CLASS = "mielon.thesift.worldgen.PortCoreContractTest"
EVIDENCE = ROOT / "evidence/2.0.1-mc2612-core-contract"
EVIDENCE.mkdir(parents=True, exist_ok=True)
with ORIGINAL.open("rb") as stream:
    if hashlib.file_digest(stream, "sha256").hexdigest() != EXPECTED_SHA:
        raise RuntimeError("Original 26.2 JAR does not match the accepted input pin")
for tool in ("java.exe", "javac.exe"):
    if not (JAVA_HOME / "bin" / tool).is_file():
        raise FileNotFoundError(JAVA_HOME / "bin" / tool)


def run(command: list[str], log: Path) -> str:
    result = subprocess.run(command, cwd=ROOT, text=True, encoding="utf-8", errors="replace",
                            capture_output=True, timeout=120)
    output = result.stdout + result.stderr
    log.write_text(output, encoding="utf-8")
    if result.returncode:
        raise RuntimeError(f"Contract command failed ({result.returncode}): {log}\n{output[-7000:]}")
    return output


outputs = {}
for mode in ("original", "port"):
    classes = ROOT / "build/core-contract" / mode
    classes.mkdir(parents=True, exist_ok=True)
    sources = [str(TEST)]
    classpath = os.pathsep.join(([str(ORIGINAL)] if mode == "original" else []) + LIBS)
    if mode == "port":
        sources += [str(ROOT / "src/main/java/mielon/thesift" / path) for path in (
            "world/SiftTides.java", "world/SiftTideTransition.java", "map/SiftMapEncoding.java",
            "compat/SoulshaleIds.java", "worldgen/SiftPackedLongSet.java",
        )]
    run([str(JAVA_HOME / "bin/javac.exe"), "-encoding", "UTF-8", "-cp", classpath,
         "-d", str(classes), *sources], EVIDENCE / f"{mode}-compile.log")
    outputs[mode] = run([str(JAVA_HOME / "bin/java.exe"), "-ea", "-cp",
                         str(classes) + os.pathsep + classpath, CLASS], EVIDENCE / f"{mode}-run.log")
    print(f"{mode.upper()}: {outputs[mode].strip()}")
if outputs["original"] != outputs["port"]:
    raise AssertionError("Original and port core-contract results differ")
(EVIDENCE / "RESULT.json").write_text(json.dumps({
    "original_sha256": EXPECTED_SHA,
    "core_contract": "PASS",
    "original_and_port_output_identical": True,
    "tested_classes": ["SiftTides", "SiftTideTransition", "SiftMapEncoding", "SoulshaleIds", "SiftPackedLongSet"],
    "game_runtime": "NOT RUN; pure production-class execution only",
}, indent=2) + "\n", encoding="utf-8")
print("DIFFERENTIAL CORE CONTRACT: PASS")
