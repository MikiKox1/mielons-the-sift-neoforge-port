"""Backport only the entity-predicate codec shape, never the gameplay conditions.

MC26.2 uses namespaced EntitySubPredicate keys; MC26.1.2 silently ignores them.
Pinned original data plus native RED tests establish the conversion boundary.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SIFT = ROOT.parents[1]
ORIGINAL = SIFT / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
PIN = "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72"
ADVANCEMENTS = ("story/fallen_civilization", "the_sift/certified_sifter", "the_sift/lookalike",
                "the_sift/choir_conductor", "the_sift/regicide", "the_sift/sift_slayer")
LOOT = ("nester", "nuzzle", "pollinator", "seedling", "slabber")
PATHS = tuple("data/the_sift/advancement/" + n + ".json" for n in ADVANCEMENTS) + tuple(
    "data/the_sift/loot_table/entities/" + n + ".json" for n in LOOT)
FIELDS = {"minecraft:location": "location", "minecraft:equipment": "equipment",
          "minecraft:entity_type": "type", "minecraft:flags": "flags",
          "minecraft:components": "components"}


def convert(document):
    result = copy.deepcopy(document)
    converted = 0

    def walk(value):
        nonlocal converted
        if isinstance(value, dict):
            if value.get("condition") == "minecraft:entity_properties" and isinstance(value.get("predicate"), dict):
                old = value["predicate"]
                new = {}
                for field, condition in old.items():
                    if field == "minecraft:type_specific/sheep":
                        assert isinstance(condition, dict) and set(condition) == {"sheared"}
                        new["type_specific"] = {"type": "minecraft:sheep", **condition}
                    else:
                        assert field in FIELDS, "Unreviewed original predicate field: " + field
                        new[FIELDS[field]] = condition
                value["predicate"] = new
                converted += 1
            for nested in value.values():
                walk(nested)
        elif isinstance(value, list):
            for nested in value:
                walk(nested)

    walk(result)
    return result, converted


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    assert ROOT.name == "neo-26.1.2-2.0.1-port-alpha1"
    with ORIGINAL.open("rb") as stream:
        assert hashlib.file_digest(stream, "sha256").hexdigest() == PIN
    total = 0
    entries = {}
    with ZipFile(ORIGINAL) as archive:
        for name in PATHS:
            original_bytes = archive.read(name)
            original = json.loads(original_bytes)
            expected, count = convert(original)
            target = ROOT / "src/main/resources" / name
            current = json.loads(target.read_text(encoding="utf8"))
            assert current in (original, expected), "Unexpected independent changes: " + name
            if args.apply:
                target.write_text(json.dumps(expected, ensure_ascii=False, indent=2) + "\n", encoding="utf8")
            assert json.loads(target.read_text(encoding="utf8")) == expected, "Target codec adaptation not applied: " + name
            entries[name] = {
                "original_sha256": hashlib.sha256(original_bytes).hexdigest(),
                "target_sha256": hashlib.sha256(target.read_bytes()).hexdigest(),
                "entity_predicates": count,
                "reason": "Exact MC26.1.2 EntityPredicate codec: unnamespaced record fields and typed sheep sub-predicate; original conditions, rewards, quantities and IDs unchanged",
            }
            total += count
    assert len(entries) == 11 and total == 51
    if args.apply:
        (ROOT / "PREDICATE-2612-ADAPTATIONS.json").write_text(json.dumps({
            "source_sha256": PIN, "target": "26.1.2", "converted_objects": total,
            "resources": entries,
        }, ensure_ascii=False, indent=2) + "\n", encoding="utf8")
    print("MC2612_PREDICATE_CODEC_CONVERSION_PASS files=11 objects=51 gameplay_conditions_preserved=True")


if __name__ == "__main__":
    main()
