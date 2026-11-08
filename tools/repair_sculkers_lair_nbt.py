"""Convert only pinned Sculkers' Lair NBT palettes to Minecraft 26.2 format.

The official MC26.2 Fabric 2.0.1 JAR inadvertently contains 77 palettes
written in the newer id/properties format. MC26.2 NbtUtils.readBlockState
requires Name/Properties and otherwise silently resolves the block to AIR.
The remaining two monarch-room templates use the older compatible form.

No geometry, block entity, loot, entity or palette entries may be changed.
Run with --apply on a NEW independent worktree, never on preserved ALPHA3.
"""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path
from zipfile import ZipFile

import nbtlib


ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
ORIGINAL_SHA = "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72"
PREFIX = "data/the_sift/structure/sculkers_lair/parts/"
MANIFEST = ROOT / "SCULKERS-LAIR-MC26.2-PALETTE-FIX.json"


def parse(blob: bytes):
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(blob) if blob.startswith(b"\x1f\x8b") else blob))


def pack(file):
    output = io.BytesIO()
    file.write(output)
    return gzip.compress(output.getvalue(), mtime=0)


def main(apply: bool):
    with ORIGINAL.open("rb") as stream:
        assert hashlib.file_digest(stream, "sha256").hexdigest() == ORIGINAL_SHA, "Original upstream JAR changed"
    with ZipFile(ORIGINAL) as archive:
        names = sorted(name for name in archive.namelist() if name.startswith(PREFIX) and name.endswith(".nbt"))
        assert len(names) == 79, f"Upstream template count changed: {len(names)}"

        resources = {}
        source_mixed = 0
        for name in names:
            original_bytes = archive.read(name)
            source = ROOT / "src/main/resources" / name
            assert source.is_file() and source.read_bytes() == original_bytes, f"Unexpected source edits: {name}"
            data = parse(original_bytes)
            assert int(data["DataVersion"]) == 5023, f"Unexpected upstream format: {name}"
            assert len(data["palette"]) > 0 and len(data["blocks"]) > 0

            for item in data["palette"]:
                if "id" in item:
                    assert "Name" not in item
                    item["Name"] = item.pop("id")
                    source_mixed += 1
                if "properties" in item:
                    assert "Properties" not in item
                    item["Properties"] = item.pop("properties")
                assert "Name" in item
            data["DataVersion"] = nbtlib.Int(4903)
            changed = pack(data)
            assert parse(changed) == data
            resources[name] = {
                "sha256": hashlib.sha256(changed).hexdigest(),
                "source_sha256": hashlib.sha256(original_bytes).hexdigest(),
                "block_count": len(data["blocks"]),
                "palette_count": len(data["palette"]),
            }
            if apply:
                temp = source.with_suffix(source.suffix + ".temp")
                temp.write_bytes(changed)
                temp.replace(source)

    manifest = {
        "purpose": "Community-only repair of Sculkers' Lair templates for Minecraft 26.2",
        "source": "Official 2.0.1 Fabric MC26.2 unchanged JAR",
        "source_jar_sha256": ORIGINAL_SHA,
        "minecraft": "26.2",
        "expected_data_version": 4903,
        "fixed_template_count": 79,
        "original_bad_palettes": 77,
        "transformation": "Rename palette id->Name and properties->Properties; set DataVersion 5023->4903. No other logical NBT changes.",
        "resource_sha256": {name: info["sha256"] for name, info in resources.items()},
        "resource_evidence": resources,
    }
    if apply:
        assert source_mixed > 77, "Incorrect source palette entry count"
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"MODE={'APPLY' if apply else 'DRY_RUN'} TEMPLATES={len(names)} BAD_PALETTE_ENTRIES={source_mixed} "
          f"MANIFEST={'CREATED' if apply else 'NOT_WRITTEN'}")


if __name__ == "__main__":
    args = argparse.ArgumentParser()
    args.add_argument("--apply", action="store_true")
    main(args.parse_args().apply)
