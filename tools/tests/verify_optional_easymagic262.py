#!/usr/bin/env python3
"""Static/recorded smoke verifier for isolated Easy Magic 26.2 integration.

Tests only that the correct third-party JARs were loaded on a disposable
server and client with no hard errors; NOT the in-world enchanting behavior.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
REF = SIFT / "backports/26.2-1.1.2-prep/reference/optional-easymagic-262"
SOURCES = {
    "EasyMagic-v26.2.0-mc26.2.x-NeoForge.jar":
        "85cbcd76fc33a5c3123f502efab4ed07e197b126469e009e8b223185ec3074ce",
    "puzzleslib-v26.2.5-mc26.2.x+neoforge.jar":
        "643643086780874780e7586e10b08c16cb14965c0fef18088bd1ee1b07e3cc95",
}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify(condition: bool, message: str) -> None:
    assert condition, message
    print("PASS", message)


def main() -> None:
    for name, expected in SOURCES.items():
        source = REF / name
        verify(source.is_file() and sha(source) == expected, "official pinned " + name)
        for kind in ("server", "client"):
            path = ROOT / f"run/{kind}-26.2.0.90/mods/{name}"
            verify(path.is_file() and sha(path) == expected,
                   f"the same original optional mod in disposable {kind}")
    for kind in ("server", "client"):
        log = ROOT / f"run/{kind}-26.2.0.90/logs/latest.log"
        verify(log.is_file(), f"recorded optional {kind} log")
        content = log.read_text(encoding="utf-8", errors="replace")
        verify("[the_sift/]: The Sift initialized" in content,
               f"The Sift mod initialized alongside optional mods in {kind}")
        verify("easymagic:common" in content and "puzzleslib:common" in content,
               f"both required Easy Magic components initialized in {kind}")
        verify(
            not any(token in content for token in (
                "/ERROR]", "/FATAL]", "MixinApplyError", "InjectionError",
                "InvalidInjectionException", "FatalStartupException"
            )), f"no hard error recorded in optional {kind} boot"
        )
        if kind == "server":
            verify("Done (" in content and "the_sift:the_sift" in content,
                   "optional dedicated server reached Done and saved Sift dimension")
        else:
            verify("easymagic:client" in content and "Sound engine started" in content
                   and "Loaded 5 models and 5 animations" in content,
                   "optional client initialized audio, GeckoLib and Easy Magic client")
    print("OPTIONAL_EASYMAGIC_262_SMOKE=PASS; IN_WORLD_ENCHANTING=PENDING")


if __name__ == "__main__":
    main()
