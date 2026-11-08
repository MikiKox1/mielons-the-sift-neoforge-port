"""Regression check: a visible Singer must register its dynamic light source.

This inspects the compiled renderer, because the previous .2 release built
successfully while moving light registration behind the !visible branch.
"""

from __future__ import annotations

import os
from pathlib import Path
import re
import shutil
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[2]
JARS = sorted((ROOT / "build" / "libs").glob("the_sift-*.jar"))
DEFAULT_JAR = next((jar for jar in reversed(JARS) if "-sources" not in jar.name), None)
MOD_JAR = Path(os.environ["THE_SIFT_JAR"]) if "THE_SIFT_JAR" in os.environ else DEFAULT_JAR


def javap_executable() -> str:
    java_home = os.environ.get("JAVA_HOME")
    options = [shutil.which("javap")]
    if java_home:
        options.append(str(Path(java_home) / "bin" / "javap.exe"))
    for candidate in options:
        if candidate and Path(candidate).is_file():
            return candidate
    raise RuntimeError("JDK 25 javap is required")


def method_instructions(disassembly: str) -> list[tuple[int, str, str]]:
    method = re.search(
        r"(?m)^  public boolean shouldRender\(mielon\.thesift\.entity\.SingerEntity,",
        disassembly,
    )
    if method is None:
        raise AssertionError("Typed SingerRenderer.shouldRender method is absent")
    method_text = disassembly[method.end():].split("\n  public ", 1)[0]
    return [
        (int(offset), op, args)
        for offset, op, args in re.findall(
            r"(?m)^\s+(\d+):\s+([a-z_][\w]*)\s*(.*)$", method_text
        )
    ]


class SingerLightRegistrationTest(unittest.TestCase):
    def test_both_visible_and_culled_paths_register_light(self) -> None:
        self.assertIsNotNone(MOD_JAR, "No built production JAR found")
        self.assertTrue(MOD_JAR.is_file(), f"Missing {MOD_JAR}")
        result = subprocess.run(
            [
                javap_executable(),
                "-classpath",
                str(MOD_JAR),
                "-c",
                "-p",
                "mielon.thesift.client.entity.SingerRenderer",
            ],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=True,
        )
        instructions = method_instructions(result.stdout)
        self.assertTrue(instructions)
        offsets = {offset: i for i, (offset, _, _) in enumerate(instructions)}
        pending = [(0, False)]
        seen: set[tuple[int, bool]] = set()
        return_paths = 0

        while pending:
            index, registered = pending.pop()
            if (index, registered) in seen:
                continue
            seen.add((index, registered))
            offset, op, args = instructions[index]
            registered |= "SingerHeadLightBridge.update:" in args
            if op in ("ireturn", "areturn", "return"):
                return_paths += 1
                self.assertTrue(
                    registered,
                    f"Renderer may return from offset {offset} without registering Singer light",
                )
                continue
            if op.startswith("if"):
                match = re.search(r"^(-?\d+)", args)
                self.assertIsNotNone(match, f"Unparseable branch at offset {offset}")
                pending.append((offsets[int(match.group(1))], registered))
            elif op in ("goto", "goto_w"):
                pending.append((offsets[int(args.split()[0])], registered))
                continue
            if index + 1 < len(instructions):
                pending.append((index + 1, registered))

        self.assertGreaterEqual(return_paths, 1, "No shouldRender return path found")
        self.assertEqual(
            result.stdout.count("SingerHeadLightBridge.update:"),
            1,
            "Renderer must not also move the same light from a deferred locator callback",
        )


if __name__ == "__main__":
    unittest.main()
