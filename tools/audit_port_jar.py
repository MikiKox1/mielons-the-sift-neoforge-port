"""Verify the produced artifact itself, not only source files or Gradle status."""
from pathlib import Path
from collections import Counter
from zipfile import ZipFile
import hashlib
import json
import tomllib
from verify_shader_delta import verify as verify_shader_delta

ROOT = Path(__file__).resolve().parents[1]
VERSION = "2.0.1-neoforge.1-mc26.3-alpha.4"
JAR = ROOT / "build/libs" / f"the_sift-{VERSION}.jar"
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
ORIGINAL_SHA = "b1dd0faae623797418c42197c4b66ffe2e96e08cabd1d9329ec5661c7c6195ed"


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


assert sha256(ORIGINAL) == ORIGINAL_SHA, "Pinned original changed"
manifest = json.loads((ROOT / "UPSTREAM-26.3-RESOURCE-MANIFEST.json").read_text(encoding="utf-8"))
assert manifest["source_jar_sha256"] == ORIGINAL_SHA
assert manifest["source_resource_count"] == 2573
with ZipFile(JAR) as output, ZipFile(ORIGINAL) as original:
    assert output.testzip() is None, "Invalid ZIP CRC in output"
    count = Counter(output.namelist())
    assert all(value == 1 for value in count.values()), "Duplicate ZIP entry"
    names = set(count)
    metadata = tomllib.loads(output.read("META-INF/neoforge.mods.toml").decode("utf-8"))
    assert metadata["modLoader"] == "javafml"
    assert metadata["loaderVersion"] == "[12,)"
    assert metadata["license"] == "MIT"
    assert len(metadata["mods"]) == 1 and metadata["mods"][0]["modId"] == "the_sift"
    assert metadata["mods"][0]["version"] == VERSION
    deps = {item["modId"]: item for item in metadata["dependencies"]["the_sift"]}
    assert deps["minecraft"]["versionRange"] == "[26.3]"
    assert deps["neoforge"]["versionRange"] == "[26.3.0.64-beta,)"
    assert deps["geckolib"]["versionRange"] == "[5.5.7,6)"
    assert not any(d["modId"] in ("easymagic", "puzzleslib") and d.get("type") == "required"
                   for d in metadata["dependencies"]["the_sift"])
    expected_resources = sorted(name for name in original.namelist()
                                if name.startswith(("assets/", "data/")) and not name.endswith("/"))
    assert len(expected_resources) == 2573
    for name in expected_resources:
        assert name in names, f"Missing original resource: {name}"
        assert output.read(name) == original.read(name), f"Target MC26.3 resource differs: {name}"
        assert hashlib.sha256(output.read(name)).hexdigest() == manifest["resource_sha256"][name]
    expected_classes = sorted(name for name in original.namelist()
                              if name.startswith("mielon/thesift/") and name.endswith(".class") and "$" not in name)
    for name in expected_classes:
        assert name in names, f"Original top-level class missing from actual JAR: {name}"
    assert "fabric.mod.json" not in names and "META-INF/mods.toml" not in names
    assert not any(n.startswith(("fuzs/easymagic/", "fuzs/puzzleslib/")) for n in names)
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
    for name in ("LICENSE", "AI_DISCLOSURE.md", "SOURCE_PROVENANCE.md", "UPSTREAM-26.3-RESOURCE-MANIFEST.json"):
        assert name in names, f"Missing original attribution / disclosure: {name}"
    assert "META-INF/accesstransformer.cfg" in names
    assert "data/neoforge/data_maps/block/transformables.json" in names
    assert "data/neoforge/data_maps/block/strippables.json" not in names
    assert "data/neoforge/data_maps/item/compostables.json" not in names
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
    "original_target_resources_byte_identical": len(expected_resources),
    "mc262_nbt_downgrades_applied": False,
    "verified_optional_easy_magic_catalyst_tag": "the_sift:charoite",
    "upstream_201_sha256": ORIGINAL_SHA,
    "original_top_level_classes_present": len(expected_classes),
    "configured_mixin_classes_present": mixin_count,
    "test_mod_excluded": True,
    "fabric_linkage_absent": True,
    "minecraft": "[26.3]",
    "neoforge": "[26.3.0.64-beta,)",
    "geckolib": "[5.5.7,6)",
    "limits": "Artifact structure/content verification only; runtime tests reported separately",
}
path = ROOT / "evidence/artifact-audit-shaders-26.3.json"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, indent=2))
