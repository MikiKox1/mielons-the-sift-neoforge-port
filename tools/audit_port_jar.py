"""Verify the produced artifact itself, not only source files or Gradle status."""
from pathlib import Path
from collections import Counter
from zipfile import ZipFile
import hashlib
import json
import tomllib
from verify_shader_delta import verify as verify_shader_delta

ROOT = Path(__file__).resolve().parents[1]
VERSION = "2.0.1-neoforge.1-alpha.8"
JAR = ROOT / "build/libs" / f"the_sift-{VERSION}.jar"
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.0-20261107/mielon's-the-sift-2.0.0-mc26.2-fabric.jar"
ORIGINAL_SHA = "ac6809cc012e52f727ca6d6342c27528d57e4d59c3527e3d164e1b43ca42e240"


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


assert sha256(ORIGINAL) == ORIGINAL_SHA, "Pinned original changed"
delta = json.loads((ROOT / "UPSTREAM-2.0.1-DELTA.json").read_text(encoding="utf-8"))
overrides = delta["resource_overrides"]
assert set(overrides) == {"assets/the_sift/lang/en_us.json", "data/the_sift/structure/ichor_dome/organic_desert_2.nbt"}
lair = json.loads((ROOT / "SCULKERS-LAIR-MC26.2-PALETTE-FIX.json").read_text(encoding="utf-8"))
lair_overrides = lair["resource_sha256"]
assert len(lair_overrides) == 79
assert all(n.startswith("data/the_sift/structure/sculkers_lair/parts/") and n.endswith(".nbt")
           for n in lair_overrides)
with ZipFile(JAR) as output, ZipFile(ORIGINAL) as original:
    assert output.testzip() is None, "Invalid ZIP CRC in output"
    count = Counter(output.namelist())
    assert all(value == 1 for value in count.values()), "Duplicate ZIP entry"
    names = set(count)
    metadata = tomllib.loads(output.read("META-INF/neoforge.mods.toml").decode("utf-8"))
    assert metadata["modLoader"] == "javafml"
    assert metadata["license"] == "MIT"
    assert len(metadata["mods"]) == 1 and metadata["mods"][0]["modId"] == "the_sift"
    assert metadata["mods"][0]["version"] == VERSION
    deps = {item["modId"]: item for item in metadata["dependencies"]["the_sift"]}
    assert deps["minecraft"]["versionRange"] == "[26.2]"
    assert deps["neoforge"]["versionRange"] == "[26.2.0.90,)"
    assert deps["geckolib"]["versionRange"] == "[5.5.6,6)"
    expected_resources = sorted(name for name in original.namelist()
                                if name.startswith(("assets/", "data/")) and not name.endswith("/"))
    eol_equivalent = 0
    for name in expected_resources:
        assert name in names, f"Missing original resource: {name}"
        if name in overrides:
            assert hashlib.sha256(output.read(name)).hexdigest() == overrides[name], f"Wrong 2.0.1 adapted resource: {name}"
        elif name in lair_overrides:
            assert hashlib.sha256(output.read(name)).hexdigest() == lair_overrides[name], f"Wrong lair palette fix: {name}"
        else:
            current, pinned = output.read(name), original.read(name)
            if current != pinned:
                assert name.endswith((".json", ".mcmeta", ".txt")) and current.replace(b"\r\n", b"\n") == pinned.replace(b"\r\n", b"\n"), f"Changed original 26.2 resource: {name}"
                eol_equivalent += 1
    expected_classes = sorted(name for name in original.namelist()
                              if name.startswith("mielon/thesift/") and name.endswith(".class") and "$" not in name)
    for name in expected_classes:
        assert name in names, f"Original top-level class missing from actual JAR: {name}"
    assert "fabric.mod.json" not in names and "META-INF/mods.toml" not in names
    assert not any("gametest/Sift" in name or "PortCoreContractTest" in name or name.startswith("evidence/") for name in names), "Development tests/evidence leaked into installable JAR"
    for name in names:
        if name.endswith(".class"):
            body = output.read(name)
            assert body.startswith(b"\xca\xfe\xba\xbe"), f"Not a Java class: {name}"
            assert int.from_bytes(body[6:8], "big") <= 69, f"Requires newer than Java25: {name}"
            assert b"net/fabricmc/" not in body, f"Fabric API linkage remains: {name}"
    mixin_count = 0
    for config in ("the_sift.mixins.json", "the_sift.client.mixins.json"):
        data = json.loads(output.read(config))
        assert data["required"] is True, "Do not silence required native mixin failures"
        for key in ("mixins", "client", "server"):
            for class_name in data.get(key, []):
                target = (data["package"] + "." + class_name).replace(".", "/") + ".class"
                assert target in names, f"Configured mixin absent: {target}"
                mixin_count += 1
    for name in ("LICENSE", "AI_DISCLOSURE.md", "SOURCE_PROVENANCE.md", "UPSTREAM-2.0.1-DELTA.json"):
        assert name in names, f"Missing original attribution / disclosure: {name}"
    assert "META-INF/accesstransformer.cfg" in names
    assert "data/neoforge/data_maps/block/strippables.json" in names
    assert "data/neoforge/data_maps/item/compostables.json" in names
    catalyst_tag = "data/easymagic/tags/item/enchanting_catalysts.json"
    assert catalyst_tag in names, "Easy Magic Charoite overlay missing from final JAR"
    assert json.loads(output.read(catalyst_tag)) == {
        "replace": False, "values": ["the_sift:charoite"]
    }, "Incorrect optional Easy Magic catalyst tag"
    assert "mielon/thesift/mixin/compat/EasyMagicEnchantmentMenuMixin.class" in names
    assert "mielon/thesift/compat/easymagic/CharoiteEnchantmentMenuBridge.class" in names

shader_delta = verify_shader_delta(JAR)

result = {
    "shader_integration_delta": shader_delta,
    "status": "PASS",
    "artifact": JAR.name,
    "sha256": sha256(JAR),
    "bytes": JAR.stat().st_size,
    "original_sha256": ORIGINAL_SHA,
    "original_resources_total": len(expected_resources),
    "original_resources_equivalent_in_content": len(expected_resources) - len(overrides) - len(lair_overrides),
    "line_ending_only_resource_differences": eol_equivalent,
    "verified_upstream_201_resource_adaptations": overrides,
    "verified_community_lair_nbt_overrides": len(lair_overrides),
    "verified_optional_easy_magic_catalyst_tag": "the_sift:charoite",
    "upstream_201_sha256": delta["delta_sha256"],
    "original_top_level_classes_present": len(expected_classes),
    "configured_mixin_classes_present": mixin_count,
    "test_mod_excluded": True,
    "fabric_linkage_absent": True,
    "minecraft": "[26.2]",
    "neoforge": "[26.2.0.90,)",
    "geckolib": "[5.5.6,6)",
    "limits": "Artifact structure/content verification only; runtime tests reported separately",
}
path = ROOT / "evidence/artifact-audit-shaders-26.2.json"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, indent=2))
