#!/usr/bin/env python3
"""Migrate only the 158 original The Sift 1.1.2 Fabric 26.2 worldgen JSONs.

This script is dry-run by default. --apply updates only this isolated Git
worktree; original Fabric/NeoForge JARs and other Minecraft profiles untouched.
Run only after the pinned reference SHA-256 has been verified.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
FABRIC = SIFT / "backports/26.2-1.1.2-prep/reference/upstream-mielon-1.1.2-fabric-26.2.jar"
EXPECTED_SHA = "1013807c2dea8f366f97347ab74cc895518799caf0e9513ff19a660567d33886"
WORLDGEN = ROOT / "src/main/resources/data/the_sift/worldgen"
EVIDENCE = ROOT / "evidence/phase2-fabric-worldgen-262.json"
EXPECTED_CATEGORIES = {
    "biome": 10, "configured_carver": 3, "configured_feature": 46,
    "density_function": 21, "noise": 15, "noise_settings": 1,
    "placed_feature": 59, "processor_list": 1, "structure": 2,
}


def sha(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def normalize(p: str) -> str:
    return p.replace("configured_feature/", "feature/").replace(
        "configured_carver/", "carver/"
    )


def main() -> None:
    args = argparse.ArgumentParser()
    args.add_argument("--apply", action="store_true")
    options = args.parse_args()

    assert FABRIC.is_file() and sha(FABRIC.read_bytes()) == EXPECTED_SHA
    assert WORLDGEN.is_dir(), "Local 26.3 source worldgen not present"
    with ZipFile(FABRIC) as jar:
        assert jar.testzip() is None
        prefix = "data/the_sift/worldgen/"
        original = {
            name[len(prefix):]: jar.read(name)
            for name in jar.namelist()
            if name.startswith(prefix) and name.endswith(".json")
        }
    assert len(original) == 158
    from collections import Counter
    assert dict(Counter(k.split("/")[0] for k in original)) == EXPECTED_CATEGORIES
    ours = {
        str(file.relative_to(WORLDGEN)).replace("\\", "/"): file
        for file in WORLDGEN.rglob("*.json")
    }
    assert len(ours) == 158
    expected_ours = {normalize(key) for key in original}
    assert set(ours) == expected_ours, "Worldgen source IDs differ; review before overwriting"
    manifest = {
        "phase": "Worldgen 1.1.2 Minecraft 26.2 original format",
        "state": "APPLIED" if options.apply else "DRY_RUN",
        "upstream_sha256": EXPECTED_SHA,
        "source_26_3_files": len(ours),
        "target_26_2_files": len(original),
        "old_paths": {key: sha(file.read_bytes()) for key, file in sorted(ours.items())},
        "target_paths": {key: sha(data) for key, data in sorted(original.items())},
        "unmodified_jar": str(FABRIC),
    }
    if options.apply:
        # No files/dirs are removed outside exactly one checked worldgen subtree.
        assert WORLDGEN.resolve().is_relative_to(ROOT.resolve())
        assert not any(x.is_symlink() for x in WORLDGEN.rglob("*"))
        for file in ours.values():
            file.unlink()
        for rel, blob in original.items():
            path = (WORLDGEN / rel).resolve()
            assert path.is_relative_to(WORLDGEN.resolve())
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(blob)
        for obsolete in ("feature", "carver"):
            folder = WORLDGEN / obsolete
            if folder.is_dir() and not list(folder.iterdir()):
                folder.rmdir()
        current = {
            str(file.relative_to(WORLDGEN)).replace("\\", "/"): sha(file.read_bytes())
            for file in WORLDGEN.rglob("*.json")
        }
        assert current == manifest["target_paths"], "Copy verification failed"
        EVIDENCE.parent.mkdir(parents=True, exist_ok=True)
        EVIDENCE.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("WORLDGEN_ID_CONSERVATION=158/158")
    print("NBT_STRUCTURES=UNCHANGED")
    print("MODE=" + manifest["state"])


if __name__ == "__main__":
    main()
