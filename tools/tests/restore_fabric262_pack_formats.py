#!/usr/bin/env python3
"""Restore target-26.2 version-sensitive resources from pinned original 1.1.2.

Scope: 78 advancement, 53 loot, 1 recipe, 2 vanilla advancements,
3 block models and 10 shaders DIFFERING from our Minecraft 26.3 port.
Do not replace sound tables (only formatting), mod-specific NeoForge tags,
textures, 1.1.2 source Java or separately handled worldgen.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from collections import Counter
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
PREP = SIFT / "backports/26.2-1.1.2-prep"
FAB = PREP / "reference/upstream-mielon-1.1.2-fabric-26.2.jar"
NEO = PREP / "reference/our-port-1.1.2-neoforge-26.3.jar"
CSV = PREP / "research/SHARED-RESOURCES-26.2-VS-26.3.csv"
EVIDENCE = ROOT / "evidence/phase2-pack-262.json"
EXPECTED = {
    "assets/the_sift/models": 3,
    "assets/the_sift/shaders": 10,
    "data/the_sift/advancement": 78,
    "data/the_sift/loot_table": 53,
    "data/the_sift/recipe": 1,
    "misc": 2,
}
ALLOWED = ("data/the_sift/advancement/", "data/the_sift/loot_table/",
           "data/the_sift/recipe/", "data/minecraft/advancement/adventure/",
           "assets/the_sift/models/", "assets/the_sift/shaders/")


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()
    assert sha(FAB.read_bytes()) == "1013807c2dea8f366f97347ab74cc895518799caf0e9513ff19a660567d33886"
    assert sha(NEO.read_bytes()) == "d3bd9a6bf718e755e1da42190ff726a48d3fe6708245ed879a00883434e27cee"
    with CSV.open(encoding="utf-8-sig", newline="") as file:
        rows = list(csv.DictReader(file))
    changed = [r for r in rows if r["match"] == "different" and
               r["path"].startswith(ALLOWED)]
    counts = dict(Counter(r["category"] for r in changed))
    assert counts == EXPECTED, f"Unexpected target resources: {counts}"
    assert len(changed) == 147

    with ZipFile(FAB) as f, ZipFile(NEO) as n:
        assert f.testzip() is None and n.testzip() is None
        checklist = []
        for row in changed:
            name = row["path"]
            blob = f.read(name)
            earlier = n.read(name)
            assert blob != earlier
            paths = [ROOT / root / name for root in ("src/main/resources", "src/client/resources")]
            candidates = [p for p in paths if p.is_file()]
            assert len(candidates) == 1, f"Unexpected local source paths: {name}, {candidates}"
            target = candidates[0].resolve()
            assert target.is_relative_to(ROOT.resolve()) and not target.is_symlink()
            old = target.read_bytes()
            # Dev source resources can use Windows CRLF while the JAR uses LF.
            # Only allow a difference in line endings; changed meanings abort.
            assert old.replace(b"\r\n", b"\n") == earlier.replace(b"\r\n", b"\n"), (
                f"Current source differs from pinned NeoForge release; do not overwrite: {name}"
            )
            checklist.append((name, target, blob, old))
        manifest = {
            "mode": "APPLIED" if args.apply else "DRY_RUN",
            "target_mc": "26.2",
            "source": "Mielon's official Fabric 1.1.2 MC26.2",
            "restored_files": len(checklist),
            "categories": counts,
            "resources": [{
                "name": name,
                "original_sha256": sha(old),
                "target_sha256": sha(blob),
            } for name, _, blob, old in checklist],
            "kept": ["source Java", "neoforge-specific tags", "sound JSON", "NBT structure templates"],
        }
        if args.apply:
            for name, target, blob, old in checklist:
                target.write_bytes(blob)
                assert target.read_bytes() == blob, f"Copy failed: {name}"
            EVIDENCE.parent.mkdir(parents=True, exist_ok=True)
            EVIDENCE.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
                                encoding="utf-8")
        print("FABRIC_262_RESOURCE_RESTORE=" + manifest["mode"])
        print("FILES=" + str(len(checklist)))
        print("GROUPS=" + json.dumps(counts))
        print("KEPT_NEOFORGE_INTEGRATIONS=PASS")


if __name__ == "__main__":
    main()
