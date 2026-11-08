"""Verify the produced artifact itself, not only source files or Gradle status."""
from pathlib import Path
from collections import Counter
from zipfile import ZipFile
from verify_mixin_compile_floor import verify as verify_minimum_annotations
import hashlib
import json
import tomllib
from verify_shader_delta import verify as verify_shader_delta

ROOT = Path(__file__).resolve().parents[1]
VERSION = "2.0.1-neoforge.1-mc26.1.2-alpha.4"
JAR = ROOT / "build/libs" / f"the_sift-{VERSION}.jar"
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
ORIGINAL_SHA = "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72"
BASE = ROOT.parents[1] / "artifacts/builds/TheSift-2.0.1-MC26.2-NeoForge-ALPHA5-EASYMAGIC/mielon's-the-sift-2.0.1-mc26.2-neoforge-port-alpha5.jar"
BASE_SHA = "7371911c66d5b41515fe62f691f7e6623695a790c38d2dfa8207bd4c6e59a487"


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


minimum_annotations = verify_minimum_annotations(JAR)
assert sha256(ORIGINAL) == ORIGINAL_SHA, "Pinned original changed"
assert sha256(BASE) == BASE_SHA, "Accepted reference binary changed"
target = json.loads((ROOT / "TARGET-2612-PROVENANCE.json").read_text(encoding="utf8"))
overrides = target["target_resource_changes_from_accepted_base"]
additions = target.get("target_resource_additions", {})
removals = target.get("target_resource_removals", {})
predicate_delta = json.loads((ROOT / "PREDICATE-2612-ADAPTATIONS.json").read_text(encoding="utf8"))
assert len(predicate_delta["resources"]) == 11 and predicate_delta["converted_objects"] == 51
assert {"assets/the_sift/shaders/core/restore_tide_sky.vsh", *predicate_delta["resources"]} <= set(overrides)
assert not set(overrides) & (set(additions) | set(removals)), "Ambiguous target resource change classes"
lair = json.loads((ROOT / "SCULKERS-LAIR-MC26.2-PALETTE-FIX.json").read_text(encoding="utf-8"))
lair_overrides = lair["resource_sha256"]
assert len(lair_overrides) == 79
assert all(n.startswith("data/the_sift/structure/sculkers_lair/parts/") and n.endswith(".nbt")
           for n in lair_overrides)
with ZipFile(JAR) as output, ZipFile(ORIGINAL) as original, ZipFile(BASE) as accepted:
    assert output.testzip() is None, "Invalid ZIP CRC in output"
    count = Counter(output.namelist())
    assert all(value == 1 for value in count.values()), "Duplicate ZIP entry"
    names = set(count)
    metadata = tomllib.loads(output.read("META-INF/neoforge.mods.toml").decode("utf-8"))
    assert metadata["modLoader"] == "javafml"
    assert metadata["loaderVersion"] == "[11,)"
    assert metadata["license"] == "MIT"
    assert len(metadata["mods"]) == 1 and metadata["mods"][0]["modId"] == "the_sift"
    assert metadata["mods"][0]["version"] == VERSION
    deps = {item["modId"]: item for item in metadata["dependencies"]["the_sift"]}
    assert deps["minecraft"]["versionRange"] == "[26.1.2]"
    assert deps["neoforge"]["versionRange"] == "[26.1.2.100,)"
    assert deps["geckolib"]["versionRange"] == "[5.5.2,6)"
    assert not any(d["modId"] in ("easymagic", "puzzleslib") and d.get("type") == "required"
                   for d in metadata["dependencies"]["the_sift"])
    expected_resources = sorted(name for name in original.namelist()
                                if name.startswith(("assets/", "data/")) and not name.endswith("/"))
    eol_equivalent = 0
    assert len(expected_resources) == 2555
    for name in expected_resources:
        if name in removals:
            assert name not in names, "Retired target-incompatible resource still packaged: " + name
            assert hashlib.sha256(accepted.read(name)).hexdigest() == removals[name]["before_sha256"]
            continue
        assert name in names, f"Missing original resource: {name}"
        if name in overrides:
            assert hashlib.sha256(accepted.read(name)).hexdigest() == overrides[name]["before_sha256"]
            assert hashlib.sha256(output.read(name)).hexdigest() == overrides[name]["after_sha256"], f"Wrong reviewed MC26.1.2 adaptation: {name}"
        elif name in lair_overrides:
            assert hashlib.sha256(output.read(name)).hexdigest() == lair_overrides[name], f"Wrong lair palette fix: {name}"
        else:
            current, pinned = output.read(name), original.read(name)
            if current != pinned:
                assert name.endswith((".json", ".mcmeta", ".txt")) and current.replace(b"\r\n", b"\n") == pinned.replace(b"\r\n", b"\n"), f"Changed original 26.2 resource: {name}"
                eol_equivalent += 1
    accepted_resources = {n for n in accepted.namelist() if n.startswith(("assets/", "data/")) and not n.endswith("/")}
    produced_resources = {n for n in names if n.startswith(("assets/", "data/")) and not n.endswith("/")}
    assert len(accepted_resources) == 2558
    shader_additions = json.loads((ROOT / "SHADER-INTEGRATION-RESOURCES.json").read_text(encoding="utf8"))["added_resources"]
    assert produced_resources == (accepted_resources - set(removals)) | set(additions) | set(shader_additions), "Unreviewed added/removed asset or data path"
    for name, entry in additions.items():
        assert name not in accepted_resources and hashlib.sha256(output.read(name)).hexdigest() == entry["after_sha256"]
        assert entry.get("reason"), "Every additional target resource needs a source-based rationale"
    for name in accepted_resources - set(overrides) - set(removals):
        assert output.read(name) == accepted.read(name), f"Changed accepted gameplay resource: {name}"
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
                target_class = (data["package"] + "." + class_name).replace(".", "/") + ".class"
                assert target_class in names, f"Configured mixin absent: {target_class}"
                mixin_count += 1
    for name in ("LICENSE", "AI_DISCLOSURE.md", "SOURCE_PROVENANCE.md", "TARGET-2612-PROVENANCE.json"):
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
    "original_resources_equivalent_in_content": len(expected_resources) - len(set(overrides) & set(expected_resources)) - len(lair_overrides) - len(removals),
    "accepted_resources_byte_identical_except_reviewed_target_adaptations": len(accepted_resources) - len(overrides) - len(removals),
    "line_ending_only_resource_differences": eol_equivalent,
    "reviewed_target_resource_adaptations": overrides,
    "reviewed_target_resource_additions": additions,
    "reviewed_target_resource_removals": removals,
    "entity_predicates_converted_without_changing_conditions": 51,
    "verified_community_lair_nbt_overrides": len(lair_overrides),
    "verified_optional_easy_magic_catalyst_tag": "the_sift:charoite",
    "upstream_201_sha256": ORIGINAL_SHA,
    "official_neoforge_1211_sha256": target["official_neoforge1211_201_sha256"],
    "accepted_base_sha256": BASE_SHA,
    "original_top_level_classes_present": len(expected_classes),
    "configured_mixin_classes_present": mixin_count,
    "test_mod_excluded": True,
    "fabric_linkage_absent": True,
    "minecraft": "[26.1.2]",
    "neoforge": "[26.1.2.100,)",
    "geckolib": "[5.5.2,6)",
    "minimum_mixin_annotation_abi": minimum_annotations,
    "limits": "Artifact structure/content verification only; runtime tests reported separately",
}
path = ROOT / "evidence/artifact-audit-shaders-26.1.2.json"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, indent=2))
