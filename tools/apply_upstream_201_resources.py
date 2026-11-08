"""Apply the two proven 2.0.1 resource changes without importing MC26.3 NBT."""
from pathlib import Path
from zipfile import ZipFile
import gzip
import hashlib
import io
import json
import struct

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT.parents[1]
BASE = PROJECT / "reference/upstream-2.0.0-20261107/mielon's-the-sift-2.0.0-mc26.2-fabric.jar"
UPSTREAM = PROJECT / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
BASE_SHA = "ac6809cc012e52f727ca6d6342c27528d57e4d59c3527e3d164e1b43ca42e240"
UPSTREAM_SHA = "b1dd0faae623797418c42197c4b66ffe2e96e08cabd1d9329ec5661c7c6195ed"
LANG = "assets/the_sift/lang/en_us.json"
STRUCTURE = "data/the_sift/structure/ichor_dome/organic_desert_2.nbt"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def parse(data):
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(data)))


props = dict(line.split("=", 1) for line in
             (ROOT / "gradle.properties").read_text(encoding="utf8").splitlines()
             if "=" in line and not line.startswith("#"))
if props.get("minecraft_version") != "26.2":
    raise SystemExit("Historical MC26.2 resource adaptation cannot run against MC26.3")
assert sha(BASE.read_bytes()) == BASE_SHA
assert sha(UPSTREAM.read_bytes()) == UPSTREAM_SHA
with ZipFile(BASE) as base, ZipFile(UPSTREAM) as upstream:
    old_structure = base.read(STRUCTURE)
    raw = gzip.decompress(old_structure)
    before, after = b"minecraft:chiseled_sulfur", b"the_sift:chiseled_soulshale"
    old_value = struct.pack(">H", len(before)) + before
    new_value = struct.pack(">H", len(after)) + after
    assert raw.count(old_value) == 1, "The audited structure had exactly one wrong palette identifier"
    new_structure = gzip.compress(raw.replace(old_value, new_value, 1), mtime=0)
    expected = parse(old_structure)
    expected["palette"][6]["Name"] = nbtlib.String(after.decode("ascii"))
    assert parse(new_structure) == expected, "The structure changed beyond the single identifier"
    assert int(expected["DataVersion"]) == 4903, "Never import the MC26.3 data version"
    assert str(parse(upstream.read(STRUCTURE))["palette"][6]["id"]) == after.decode("ascii")
    language = upstream.read(LANG)
    old_lang = json.loads(base.read(LANG))
    new_lang = json.loads(language)
    assert set(new_lang) - set(old_lang) == {
        "gamerule.the_sift.spawn_rifts", "gamerule.the_sift.spawn_rifts.description"
    }
    assert all(new_lang[key] == value for key, value in old_lang.items())
    changed = {LANG: language, STRUCTURE: new_structure}
    for name, data in changed.items():
        target = ROOT / "src/main/resources" / name
        assert target.read_bytes() in (base.read(name), data), f"Unexpected existing edits: {name}"
    for name, data in changed.items():
        (ROOT / "src/main/resources" / name).write_bytes(data)

manifest = {
    "port_version": "2.0.1-neoforge.1-alpha.1",
    "minecraft": "26.2",
    "base_version": "2.0.0",
    "base_minecraft": "26.2",
    "base_sha256": BASE_SHA,
    "delta_version": "2.0.1",
    "delta_minecraft": "26.3",
    "delta_modrinth_id": "PrUXXDu2",
    "delta_sha256": UPSTREAM_SHA,
    "prior_source_checkpoint": "c83984621c7414074af5da8e34e975af1469e6e1",
    "resource_overrides": {name: sha(data) for name, data in changed.items()},
    "structure_adaptation": "Only palette[6].Name changed to the_sift:chiseled_soulshale; MC26.2 Name/Properties/DataVersion4903 preserved",
    "gamerule": {"id": "the_sift:spawn_rifts", "default": True, "category": "SPAWNING", "scope": "Natural Rift spawning only"},
}
(ROOT / "UPSTREAM-2.0.1-DELTA.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
print(json.dumps(manifest, indent=2))
