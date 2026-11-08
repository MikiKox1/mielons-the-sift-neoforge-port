#!/usr/bin/env python3
"""Read-only MC26.1.2 data adaptation audit; writes only its manifest/report.

There is no invented 26.1.2 upstream 2.0.1 data set. The accepted MC26.2
ALPHA5 resources are preserved unless a target 26.1.2 decoder incompatibility
is demonstrated. Run with --write to refresh the manifest and evidence report,
or without options to verify without writing.
"""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
from zipfile import ZipFile

import nbtlib

WORK = Path(__file__).resolve().parents[1]
PROJECT = WORK.parents[1]
ORIGINAL_262 = PROJECT / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
OFFICIAL_1211 = PROJECT / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc1.21.1-neo.jar"
ACCEPTED_BASE = PROJECT / "artifacts/builds/TheSift-2.0.1-MC26.2-NeoForge-ALPHA5-EASYMAGIC/mielon's-the-sift-2.0.1-mc26.2-neoforge-port-alpha5.jar"
TARGET_SOURCES = (
    WORK.parent / "neo-26.1.2-1.1.2.1-release/build/moddev/artifacts/minecraft-patched-26.1.2.126-sources.jar"
)
TARGET_MINECRAFT = (
    WORK.parent / "neo-26.1.2-1.1.2.1-release/build/moddev/artifacts/minecraft-patched-26.1.2.126.jar"
)
NEOFORGE_CACHE = (
    Path.home() / ".gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.126"
)
MANIFEST = WORK / "RESOURCE-2612-ADAPTATIONS.json"
REPORT = PROJECT / "reports/research/port-26.1.2-2.0.1-20261108/DATA-2612-REVIEW.md"

BASE_COMMIT = "22176ab7e71ad9a5c2c624cfc72015702bb98f22"
ORIGINAL_262_SHA = "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72"
OFFICIAL_1211_SHA = "0da27285ae442fc6bb27e6f4c703800c2ce90ea93ef6bc64c40da6d135ef23fb"
ACCEPTED_BASE_SHA = "7371911c66d5b41515fe62f691f7e6623695a790c38d2dfa8207bd4c6e59a487"
RESOURCE_PREFIXES = ("src/main/resources/data/", "src/client/resources/data/", "src/main/resources/assets/")
LAIR_PREFIX = "data/the_sift/structure/sculkers_lair/parts/"
SULFUR_REPAIRED = "data/the_sift/structure/ichor_dome/organic_desert_2.nbt"


def sha(blob: bytes) -> str:
    return hashlib.sha256(blob).hexdigest()


def artifact_sha(path: Path, expected: str) -> str:
    assert path.is_file(), f"Missing immutable reference: {path}"
    with path.open("rb") as stream:
        found = hashlib.file_digest(stream, "sha256").hexdigest()
    assert found == expected, f"Immutable reference changed: {path} SHA={found}"
    return found


def parse_nbt(data: bytes) -> nbtlib.File:
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(data) if data.startswith(b"\x1f\x8b") else data))


def owned_resources() -> dict[str, bytes]:
    resources: dict[str, bytes] = {}
    for prefix in RESOURCE_PREFIXES:
        directory = WORK / prefix
        if not directory.exists():
            continue
        for file in directory.rglob("*"):
            if not file.is_file():
                continue
            relative = file.relative_to(WORK).as_posix()
            if relative.startswith("src/main/resources/assets/the_sift/shaders/"):
                continue  # Client/shader worker owns this tree.
            resources[relative] = file.read_bytes()
    return dict(sorted(resources.items()))


def load_target_sources():
    assert TARGET_SOURCES.is_file() and TARGET_MINECRAFT.is_file(), "Missing exact MC26.1.2.126 patched source or binary"
    with ZipFile(TARGET_SOURCES) as sources, ZipFile(TARGET_MINECRAFT) as game:
        expected = {
            "net/minecraft/nbt/NbtUtils.java",
            "net/minecraft/world/level/levelgen/structure/templatesystem/loader/TemplateSource.java",
            "net/minecraft/world/level/block/Blocks.java",
            "net/minecraft/world/item/Items.java",
        }
        assert expected.issubset(sources.namelist())
        nbt = sources.read("net/minecraft/nbt/NbtUtils.java").decode("utf-8")
        template = sources.read(
            "net/minecraft/world/level/levelgen/structure/templatesystem/loader/TemplateSource.java"
        ).decode("utf-8")
        blocks_src = sources.read("net/minecraft/world/level/block/Blocks.java").decode("utf-8")
        items_src = sources.read("net/minecraft/world/item/Items.java").decode("utf-8")
        assert 'tag.read("Name", BLOCK_NAME_CODEC)' in nbt
        assert 'tag.getCompound("Properties")' in nbt
        assert "DataFixTypes.STRUCTURE.updateToCurrentVersion" in template
        version = json.loads(game.read("version.json"))
        assert version["id"] == "26.1.2" and version["world_version"] == 4790
        vanilla_data = {
            name: json.loads(game.read(name))
            for name in game.namelist()
            if name in {
                "data/minecraft/worldgen/noise_settings/overworld.json",
                "data/minecraft/worldgen/noise/aquifer_barrier.json",
                "data/minecraft/worldgen/biome/wooded_badlands.json",
                "data/minecraft/recipe/acacia_boat.json",
            }
        }
        assert len(vanilla_data) == 4
        assert {"aquifers_enabled", "noise", "noise_router", "default_block"} <= set(
            vanilla_data["data/minecraft/worldgen/noise_settings/overworld.json"]
        )
        assert {"firstOctave", "amplitudes"} == set(
            vanilla_data["data/minecraft/worldgen/noise/aquifer_barrier.json"]
        )
        assert {"attributes", "carvers", "features"} <= set(
            vanilla_data["data/minecraft/worldgen/biome/wooded_badlands.json"]
        )
        assert "id" in vanilla_data["data/minecraft/recipe/acacia_boat.json"]["result"]
        blocks = set(re.findall(r'\bregister[A-Za-z0-9_]*\(\s*"([a-z0-9_]+)"', blocks_src))
        # The special dirt static initialization uses BlockIds.DIRT, not a literal.
        assert "register(BlockIds.DIRT," in blocks_src
        blocks.add("dirt")
        items = set(re.findall(r'\bregister[A-Za-z0-9_]*\(\s*"([a-z0-9_]+)"', items_src))
        return {
            "jar": str(TARGET_MINECRAFT),
            "source_jar": str(TARGET_SOURCES),
            "patched_jar_sha256": sha(TARGET_MINECRAFT.read_bytes()),
            "patched_sources_sha256": sha(TARGET_SOURCES.read_bytes()),
            "world_version": version["world_version"],
            "pack_version": version["pack_version"],
            "blocks": blocks,
            "items": items,
            "blocks_src": blocks_src,
            "items_src": items_src,
        }


def validate_native_maps(resources: dict[str, bytes]) -> dict:
    modprefix = "src/main/resources/data/"

    def get(path: str):
        full = modprefix + path
        assert full in resources, f"Missing 2612 native overlay: {full}"
        return json.loads(resources[full])

    compost = get("neoforge/data_maps/item/compostables.json")
    stripping = get("neoforge/data_maps/block/strippables.json")
    catalyst = get("easymagic/tags/item/enchanting_catalysts.json")
    assert compost.get("replace") is False and stripping.get("replace") is False
    assert catalyst == {"replace": False, "values": ["the_sift:charoite"]}
    # Exact Fabric MC26.2 2.0.1 has 19 ModBlocks + 2 ModItems registrations.
    # Ash Willow sapling was added to the 26.3 original and is not part of 262.
    assert len(compost["values"]) == 21, f"Expected the 21 original MC26.2 compostables; found {len(compost['values'])}"
    assert len(stripping["values"]) == 4
    assert {v["chance"] for v in compost["values"].values()} == {0.3, 0.5, 0.65}
    assert set(stripping["values"]) == {
        "the_sift:overgrown_willow_log", "the_sift:overgrown_willow_wood",
        "the_sift:ash_willow_log", "the_sift:ash_willow_wood",
    }
    assert all(
        isinstance(v.get("stripped_block"), str) and v["stripped_block"].startswith("the_sift:stripped_")
        for v in stripping["values"].values()
    )
    assert not any("data_maps/block/transformables.json" in key for key in resources)
    jars = sorted(NEOFORGE_CACHE.rglob("*universal.jar"))
    assert len(jars) == 1, f"Cannot pin exact NF26.1.2.126 JAR in cache: {jars}"
    with ZipFile(jars[0]) as jar:
        known = set(jar.namelist())
        assert "net/neoforged/neoforge/registries/datamaps/builtin/Compostable.class" in known
        assert "net/neoforged/neoforge/registries/datamaps/builtin/Strippable.class" in known
        for name, field in [
            ("data/neoforge/data_maps/item/compostables.json", "chance"),
            ("data/neoforge/data_maps/block/strippables.json", "stripped_block"),
        ]:
            vanilla = json.loads(jar.read(name))["values"]
            assert vanilla and all(field in entry for entry in vanilla.values())
    return {
        "neoforge_jar": str(jars[0]),
        "neoforge_jar_sha256": sha(jars[0].read_bytes()),
        "compostables": len(compost["values"]),
        "strippables": len(stripping["values"]),
        "catalyst": catalyst["values"],
        "native_data_maps": ["neoforge:item/compostables", "neoforge:block/strippables"],
    }


def validate_json(resources: dict[str, bytes], vanilla: dict) -> dict:
    parsed: dict[str, object] = {}
    for path, data in resources.items():
        if path.endswith((".json", ".mcmeta")):
            parsed[path] = json.loads(data.decode("utf-8-sig"))
    data = {
        p.removeprefix("src/main/resources/"): o
        for p, o in parsed.items() if p.startswith("src/main/resources/data/")
    }
    biomes = {p: obj for p, obj in data.items() if p.startswith("data/the_sift/worldgen/biome/")}
    noise = {p: obj for p, obj in data.items() if p.startswith("data/the_sift/worldgen/noise/")}
    recipes = {p: obj for p, obj in data.items() if p.startswith("data/the_sift/recipe/")}
    configured = {p: obj for p, obj in data.items() if p.startswith("data/the_sift/worldgen/configured_feature/")}
    placed = {p: obj for p, obj in data.items() if p.startswith("data/the_sift/worldgen/placed_feature/")}
    carvers = {p: obj for p, obj in data.items() if p.startswith("data/the_sift/worldgen/configured_carver/")}
    density = {p: obj for p, obj in data.items() if p.startswith("data/the_sift/worldgen/density_function/")}
    loot = {p: obj for p, obj in data.items() if "/loot_table/" in p}
    assert len(biomes) == 11 and len(recipes) == 164
    assert len(noise) > 0 and len(configured) > 0 and len(placed) > 0 and len(density) > 0 and carvers
    for path, biome in biomes.items():
        assert {"attributes", "features", "carvers"} <= set(biome), path
        assert isinstance(biome["features"], list) and isinstance(biome["carvers"], list), path
        for feature in [name for step in biome["features"] for name in step if name.startswith("the_sift:")]:
            f = feature.partition(":")[2]
            assert f"data/the_sift/worldgen/placed_feature/{f}.json" in data, f"{path}: unknown placed_feature {feature}"
        for name in biome["carvers"]:
            if name.startswith("the_sift:"):
                assert f"data/the_sift/worldgen/configured_carver/{name.partition(':')[2]}.json" in data, (
                    f"{path}: missing carver {name}"
                )
    for path, value in noise.items():
        assert set(value) == {"firstOctave", "amplitudes"}, f"MC26.1.2 incompatible noise schema: {path}"
        assert isinstance(value["amplitudes"], list) and value["amplitudes"], path
    for path, value in configured.items():
        assert isinstance(value, dict) and {"type", "config"} <= set(value), path
    for path, value in recipes.items():
        assert isinstance(value, dict) and "type" in value, path
        assert value["type"].startswith("minecraft:"), f"Recipe type needs independent registration: {path}"
    settings = data["data/the_sift/worldgen/noise_settings/the_sift.json"]
    assert {"aquifers_enabled", "noise", "noise_router", "default_block", "default_fluid"} <= set(settings)
    assert {"min_y", "height", "size_horizontal", "size_vertical"} <= set(settings["noise"])
    assert settings["default_block"]["Name"].startswith("the_sift:")
    assert all("Name" in settings[field] for field in ("default_block", "default_fluid"))
    assert not any("/worldgen/feature/" in p or "/worldgen/carver/" in p for p in data), "MC26.3-only worldgen schema"

    # Validate every Name blockstate explicitly using the exact target Blocks registry.
    block_refs = {}
    def walk(value, p):
        if isinstance(value, dict):
            if "Name" in value and isinstance(value["Name"], str):
                name = value["Name"]
                if name.startswith("minecraft:"):
                    short = name.partition(":")[2]
                    assert short in vanilla["blocks"], f"Unknown MC26.1.2 vanilla block: {p} {name}"
                block_refs[name] = block_refs.get(name, 0) + 1
            for nested in value.values(): walk(nested, p)
        elif isinstance(value, list):
            for nested in value: walk(nested, p)
    for path, obj in data.items():walk(obj, path)

    def item_present(item_id):
        if not item_id.startswith("minecraft:"):return True
        short = item_id.partition(":")[2]
        return short in vanilla["items"] or short in vanilla["blocks"] or f'"{short}"' in vanilla["items_src"]

    # Recipe results, direct ingredients and loot table item entries are item-only
    # registries; avoid false positives from potion IDs and sound/particle IDs.
    checked_items = 0
    def validate_items(value, path, parent_key=""):
        nonlocal checked_items
        if isinstance(value, dict):
            for key, item in value.items():
                if key == "id" and parent_key == "result" and isinstance(item, str):
                    assert item_present(item), f"{path}: invalid recipe result {item}"
                    checked_items += 1
                elif key == "name" and value.get("type") == "minecraft:item" and isinstance(item, str):
                    assert item_present(item), f"{path}: invalid loot item {item}"
                    checked_items += 1
                validate_items(item, path, key)
        elif isinstance(value, list):
            for item in value:validate_items(item, path, parent_key)
    for path, recipe in recipes.items(): validate_items(recipe, path)
    for path, table in loot.items(): validate_items(table, path)
    # The 26.1 codec silently ignores 26.2's namespaced entity sub-predicates.
    # Valid JSON alone is not evidence that these gameplay conditions survive.
    entity_conditions = 0
    def validate_entity_predicates(value, path):
        nonlocal entity_conditions
        if isinstance(value, dict):
            if value.get("condition") == "minecraft:entity_properties" and isinstance(value.get("predicate"), dict):
                entity_conditions += 1
                predicate = value["predicate"]
                assert not any(key.startswith("minecraft:") for key in predicate), (
                    f"MC26.2 entity-predicate field would be silently ignored on 26.1.2: {path}")
                if "type_specific" in predicate:
                    assert predicate["type_specific"].get("type") == "minecraft:sheep", path
            for child in value.values():validate_entity_predicates(child, path)
        elif isinstance(value, list):
            for child in value:validate_entity_predicates(child, path)
    for path, value in data.items():validate_entity_predicates(value, path)
    return {
        "valid_json_and_mcmeta": len(parsed),
        "valid_data_json": len(data),
        "biomes": len(biomes),
        "noise": len(noise),
        "density_functions": len(density),
        "configured_features": len(configured),
        "configured_carvers": len(carvers),
        "placed_features": len(placed),
        "recipes": len(recipes),
        "loot_tables": len(loot),
        "checked_direct_recipe_and_loot_items": checked_items,
        "checked_worldgen_blockstate_references": sum(block_refs.values()),
        "checked_target_entity_predicate_objects": entity_conditions,
        "schema_reference": "Exact MC26.1.2.126 patched Minecraft.jar built-in 26.1.2 JSON schemas",
    }


def validate_nbt(resources: dict[str, bytes], original: ZipFile, vanilla: dict) -> dict:
    keys = sorted(p for p in resources if p.startswith("src/main/resources/data/") and p.endswith(".nbt"))
    assert len(keys) == 299, f"Expected 299 complete original structure templates: {len(keys)}"
    versions = Counter()
    lair = 0
    blocks_total = 0
    palette_total = 0
    deviations = []
    for path in keys:
        rel = path.removeprefix("src/main/resources/")
        current = parse_nbt(resources[path])
        upstream = parse_nbt(original.read(rel))
        assert "size" in current and "blocks" in current and "palette" in current, path
        assert len(current["blocks"]) == len(upstream["blocks"]) > 0, path
        assert list(current["size"]) == list(upstream["size"]), path
        palette = current["palette"]
        assert len(palette) == len(upstream["palette"]) > 0, path
        assert all(0 <= int(block["state"]) < len(palette) for block in current["blocks"]), path
        assert all("Name" in entry and "id" not in entry and "properties" not in entry for entry in palette), path
        for entry in palette:
            name = str(entry["Name"])
            if name.startswith("minecraft:"):
                block_id = name.partition(":")[2]
                assert block_id in vanilla["blocks"], f"Unknown target MC26.1.2 block ID {name} in {path}"
        blocks_total += len(current["blocks"])
        palette_total += len(palette)
        versions[int(current["DataVersion"])] += 1
        if rel.startswith(LAIR_PREFIX):
            lair += 1
            assert int(upstream["DataVersion"]) == 5023
            assert int(current["DataVersion"]) == 4903
            expected = parse_nbt(original.read(rel))
            for entry in expected["palette"]:
                if "id" in entry:
                    entry["Name"] = entry.pop("id")
                if "properties" in entry:
                    entry["Properties"] = entry.pop("properties")
            expected["DataVersion"] = nbtlib.Int(4903)
            assert expected == current, f"Lair content changed beyond verified MC26.2 palette normalization: {path}"
        elif rel == SULFUR_REPAIRED:
            assert str(current["palette"][6]["Name"]) == "the_sift:chiseled_soulshale"
            assert upstream == current, "Official Fabric262 2.0.1 already corrected this sulfur palette"
        elif upstream != current:
            deviations.append(rel)
    assert lair == 79
    assert versions == Counter({4903: 299}), f"Unexpected existing 262 NBT versions: {versions}"
    assert not deviations, f"Unexpected NBT gameplay changes vs original262: {deviations[:12]}"
    return {
        "parsed_nbt": len(keys),
        "data_versions": dict(versions),
        "lair_templates": lair,
        "palette_entries": palette_total,
        "structure_blocks": blocks_total,
        "lair_check": "79 templates are exact logical originals except validated id/properties -> Name/Properties and DataVersion 5023 -> 4903",
        "dome_check": "official Fabric262 2.0.1 already has the_sift:chiseled_soulshale instead of sulfur; all bytes retained",
        "future_version_note": (
            "MC26.1.2 current world_version=4790, inherited data NBT version=4903. "
            "Source parser reads Name/Properties through DFU. Independent native GameTest now verifies all299 templates, non-AIR results, registered block IDs and every property on MC26.1.2; see runtime logs."
        ),
    }


def rationale(path: str, differs_from_original: bool, overlay: bool) -> str:
    if overlay:
        if "easymagic/" in path:
            return "Preserve accepted optional Easy Magic catalyst overlay; exact charoite tag unchanged."
        if "strippables.json" in path:
            return "Preserve NeoForge26.1.2 native Strippable data map with 4 willow/ash transformations."
        return "Preserve NeoForge26.1.2 native Compostable data map and all 21 original Fabric262 gameplay values."
    if path.endswith(".nbt"):
        if LAIR_PREFIX in path:
            return "Accepted262 ALPHA4 NBT palette normalization Name/Properties DV4903; target2612 parser requires Name/Properties; preserve geometry."
        if SULFUR_REPAIRED in path:
            return "Original Fabric262 2.0.1 already corrects former chiseled_sulfur to chiseled_soulshale; target2612 lacks sulfur."
        return "Retain exact original Fabric262 structure bytes; target2612 NBT parser consumes Name/Properties."
    if "/worldgen/noise_settings/" in path or "/worldgen/noise/" in path:
        return "Preserve terrain density/noise parameter values; schemas match exact MC26.1.2.126 built-in JSON."
    if "/worldgen/" in path or "/dimension/" in path:
        return "Preserve accepted262 worldgen/biome/feature graph; path schema exists in exact2612 registry."
    if "/recipe/" in path or "/loot_table/" in path or "/advancement/" in path:
        return "Keep full 2.0.1 item/loot/recipe content; JSON and target2612 recipe fields statically checked."
    if path.startswith("src/main/resources/assets/"):
        if differs_from_original:
            return "Accepted262 ALPHA5 adapted asset/model retained; no format incompatibility demonstrated in2612."
        return "Original262 mod asset, preserved unchanged; no generic-media modifications."
    if differs_from_original:
        return "Existing accepted262 community fix retained; no target2612 JSON codec incompatibility demonstrated."
    return "Original262 mod resource preserved; no target2612 JSON codec incompatibility demonstrated."


def build_manifest_and_report():
    original_pin = artifact_sha(ORIGINAL_262, ORIGINAL_262_SHA)
    official_pin = artifact_sha(OFFICIAL_1211, OFFICIAL_1211_SHA)
    base_pin = artifact_sha(ACCEPTED_BASE, ACCEPTED_BASE_SHA)
    resources = owned_resources()
    target = json.loads((WORK / "TARGET-2612-PROVENANCE.json").read_text(encoding="utf8"))
    reviewed = target["target_resource_changes_from_accepted_base"]
    additions = target.get("target_resource_additions", {})
    removals = target.get("target_resource_removals", {})
    vanilla = load_target_sources()
    native_maps = validate_native_maps(resources)
    schemas = validate_json(resources, vanilla)
    with ZipFile(ORIGINAL_262) as source, ZipFile(ACCEPTED_BASE) as accepted:
        orig_names = {p for p in source.namelist() if p.startswith(("data/", "assets/")) and not p.endswith("/")}
        local = {
            p.removeprefix("src/main/resources/")
            for p in resources if p.startswith("src/main/resources/")
        }
        # Excluded shaders belong exclusively to worker1; compare only owned paths.
        upstream_owned = {p for p in orig_names if not p.startswith("assets/the_sift/shaders/")}
        assert (upstream_owned - set(removals)).issubset(local), (
            f"Unreviewed missing original2.0.1 resources: {sorted(upstream_owned - set(removals) - local)[:15]}"
        )
        nbt = validate_nbt(resources, source, vanilla)
        entries = []
        orig_changed = Counter()
        overlay_paths = []
        for path, data in resources.items():
            rel = path.removeprefix("src/main/resources/")
            if rel == path:
                rel = path.removeprefix("src/client/resources/")
            base_data = accepted.read(rel) if rel in accepted.namelist() else None
            changed = base_data != data
            if base_data is None:
                assert rel in additions and sha(data) == additions[rel]["after_sha256"], f"Unreviewed new resource: {rel}"
            elif changed:
                assert rel in reviewed, f"Unreviewed change from accepted ALPHA5: {rel}"
                assert sha(base_data) == reviewed[rel]["before_sha256"] and sha(data) == reviewed[rel]["after_sha256"], rel
            upstream = source.read(rel) if rel in orig_names and path.startswith("src/main/resources/") else None
            differs = upstream is not None and upstream != data
            overlay = rel.startswith(("data/neoforge/", "data/easymagic/"))
            if differs:
                orig_changed["assets" if "/assets/" in path else "data"] += 1
            if overlay: overlay_paths.append(path)
            digest = sha(data)
            entries.append({
                "path": path,
                "source_sha256": sha(base_data) if base_data is not None else None,
                "target_sha256": digest,
                "changed": changed,
                "reason": (additions[rel]["reason"] if base_data is None else reviewed[rel]["reason"] if changed else rationale(path, differs, overlay)),
                "byte_size": len(data),
                "origin": "reviewed_2612_backport" if changed else ("accepted_262_alpha5" if differs else ("nf2612_overlay" if overlay else "fabric262_2.0.1")),
                "upstream262_sha256": sha(upstream) if upstream is not None else None,
            })
    assert len(overlay_paths) == 3 and all(
        p.startswith("src/main/resources/data/") for p in overlay_paths
    ), f"Unexpected non-original resources: {overlay_paths}"
    assert len(entries) > 2500
    manifest = {
        "schema": "prime2-resource-adapt-2612-v1",
        "minecraft": "26.1.2",
        "target_mod_version": "2.0.1-neoforge.1-mc26.1.2-alpha.1",
        "target_neoforge_test": "26.1.2.126",
        "target_neoforge_minimum": "26.1.2.100",
        "base_git_commit": BASE_COMMIT,
        "source": {
            "accepted_base": "MC26.2 ALPHA5 resource tree at base commit",
            "accepted_base_jar_sha256": base_pin,
            "official_fabric262_2_0_1_jar": str(ORIGINAL_262),
            "official_fabric262_sha256": original_pin,
            "official_nf1211_2_0_1_jar": str(OFFICIAL_1211),
            "official_nf1211_sha256": official_pin,
            "nf1211_role": "Read-only original2.0.1 mechanism reference, not bulk asset donor",
            "target_minecraft_source_jar": vanilla["source_jar"],
            "target_mc_sources_sha256": vanilla["patched_sources_sha256"],
            "target_mc_patched_jar_sha256": vanilla["patched_jar_sha256"],
            "target_mc_world_version": vanilla["world_version"],
        },
        "scope": list(RESOURCE_PREFIXES),
        "excluded_globs": ["src/main/resources/assets/the_sift/shaders/**"],
        "summary": {
            "owned_resource_files": len(entries),
            "actual_changed_resource_count": sum(entry["changed"] for entry in entries),
            "changed_paths": [entry["path"] for entry in entries if entry["changed"]],
            "explicit_removed_resources": removals,
            "historical_deltas_from_original262_by_family": dict(orig_changed),
            "preserved_neoforge_overlays": sorted(overlay_paths),
            "json": schemas,
            "nbt": nbt,
            "native_neoforge": native_maps,
        },
        # Exhaustive per-file evidence relative to accepted ALPHA5, not fabricated
        # transformations from the official 1.21.1 resource tree.
        "entries": entries,
    }
    assert all(e["changed"] == (e["source_sha256"] != e["target_sha256"]) for e in entries)

    report = [
        "# The Sift 2.0.1 → MC26.1.2 NeoForge — audyt zasobów",
        "",
        "Data: 2026-11-08. **Zakres:** przydzielone data/ i assets/ poza shaderami. "
        f"**Wynik:** {manifest['summary']['actual_changed_resource_count']} jawnie udokumentowanych adaptacji plików; pozostałe zasoby zachowują bajty zaakceptowanej MC26.2 ALPHA5. "
        "Ten skrypt audytuje parsery/formaty i sumy, nie uruchamia gry. Oddzielne rzeczywiste testy odtworzyły i potwierdziły poprawkę cichych błędów predykatów oraz ładowanie299NBT.",
        "",
        "## Źródła i ograniczenia",
        "",
        f"- Bazowy commit zaakceptowanej ALPHA5: `{BASE_COMMIT}`.",
        f"- Oryginał 2.0.1 Fabric MC26.2: SHA-256 `{original_pin}`.",
        f"- Oficjalny 2.0.1 NeoForge 1.21.1: SHA-256 `{official_pin}`; używany jedynie do rekonstrukcji mechanik, nie jako hurtowy pakiet danych.",
        f"- Exact target: patched Minecraft 26.1.2.126 (source SHA-256 `{vanilla['patched_sources_sha256']}`); world_version **4790**.",
        "- Dziedziczone oryginalne dane Fabric26.2: **2555** nazw zasobów; trzy zachowane nakładki NeoForge/Easy Magic. "
        "Stare zaakceptowane adaptacje assetów i receptur nie zostały wycofane.",
        "",
        "## Zgodność formatów",
        "",
        f"- JSON / .mcmeta: **{schemas['valid_json_and_mcmeta']}** parsuje się poprawnie, w tym **{schemas['valid_data_json']}** danych JSON; "
        f"11 biome, {schemas['noise']} noise, {schemas['density_functions']} density_function, "
        f"{schemas['configured_features']} configured_feature, {schemas['configured_carvers']} configured_carver, "
        f"{schemas['placed_features']} placed_feature, **{schemas['recipes']} receptury**, "
        f"{schemas['loot_tables']} loot_table.",
        "- Dokładny target Minecraft 26.1.2.126 ma te same pola formatu JSON co zachowane dane: "
        "`firstOctave/amplitudes`, `noise_settings.noise.size_horizontal/size_vertical`, "
        "`default_block.Name`, `biome.attributes/carvers/features`, `recipe.result.id`. "
        "To **nie** jest nowszy format MC26.3 `worldgen/feature` albo `transformables`.",
        f"- Zweryfikowano {schemas['checked_direct_recipe_and_loot_items']} jawnych pozycji itemów w recepturach/loot "
        f"i {schemas['checked_worldgen_blockstate_references']} odniesień do stanów bloków.",
        f"- Sprawdzono {schemas['checked_target_entity_predicate_objects']} obiektów entity_properties. "
        "51 predykatów w11plikach osiągnięć/loot wymagało przejścia z26.2namespacedmap na26.1recordfields. "
        "Warunki, nagrody, liczności, komponenty przedmiotów i identyfikatory zachowano. Natywne testy "
        "potwierdzają negatywne/pozytywne warunki wyposażenia, biomów, typu ofiary, ognia i16kolorówwełny/strzyżenia.",
        f"- Wykorzystano NeoForge26.1.2.126 builtin datamaps: `Compostable` ({native_maps['compostables']} pozycji, szanse 0.3/0.5/0.65) "
        f"i `Strippable` ({native_maps['strippables']} pary). Zachowano opcjonalny tag Easy Magic `the_sift:charoite`.",
        "",
        "## Struktury NBT i nieobecne bloki",
        "",
        f"- **{nbt['parsed_nbt']}/{nbt['parsed_nbt']}** NBT dekoduje się, "
        f"{nbt['structure_blocks']} bloków i {nbt['palette_entries']} wpisów palet; "
        "wszystkie pozycje palet mają `Name` i opcjonalne `Properties`.",
        f"- **{nbt['lair_templates']}** szablonów Sculkers Lair: odziedziczona poprawka 26.2 "
        "normalizuje palety `id/properties` do `Name/Properties`, DataVersion **5023→4903**; "
        "sprawdzenie logicznego NBT wobec oryginału wykazało brak zmian geometrii i danych.",
        "- Dome `ichor_dome/organic_desert_2.nbt` ma już w **oficjalnym Fabric262 2.0.1** "
        "prawidłowy blok `the_sift:chiseled_soulshale` zamiast starego `minecraft:chiseled_sulfur`; "
        "w bieżącym porcie bajty są identyczne. Target MC26.1.2 nie zawiera vanilla sulfur.",
        "- Target `NbtUtils.readBlockState` odczytuje dokładnie `Name` / `Properties`; "
        "`TemplateSource.readStructure` stosuje `DataFixTypes.STRUCTURE.updateToCurrentVersion`.",
        "- MC26.1.2 ma world_version **4790**, a odziedziczone299NBT mają DataVersion **4903**. "
        "Nie uznano zgodności na podstawie samego parsowania: rzeczywisty nativeGameTest załadował299szablonów, "
        "sprawdził non-AIR i każdy identyfikator/właściwość palety. "
        "Nie zmieniono DataVersion globalnie, aby uniknąć nieuzasadnionego downgrade'u.",
        "",
        "## Zmiany i dalsze bramki",
        "",
        f"- **Liczba faktycznie zmienionych plików data/assets poza shaderami: {manifest['summary']['actual_changed_resource_count']}.** "
        f"Manifest `{MANIFEST.name}` zawiera {len(entries)} wpisy `entries` ze "
        "`source_sha256`, `target_sha256`, `reason` i stanem `changed` "
        "dla każdego przydzielonego pliku. Punktem odniesienia jest accepted262 ALPHA5, "
        "a historia odchyleń od oryginalnego Fabric262 jest oznaczona osobno.",
        "- Nie zmieniano shaderów, Java, Gradle, metadata, mixin konfiguracji, "
        "generic media i innych worktree. Nie usuwano feature/biome/density content.",
        "- Wyniki bieżących kompilacji, testów faktycznego JAR na minimalnym loaderze, klienta "
        "i pozostałe ograniczenia są w końcowym raporcie portu. Ręczne QA właściciela pozostaje odrębne.",
        "",
    ]
    return manifest, "\n".join(report)


def main(write: bool):
    manifest, report = build_manifest_and_report()
    if write:
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        REPORT.parent.mkdir(parents=True, exist_ok=True)
        REPORT.write_text(report, encoding="utf-8")
    print(
        f"MC26.1.2 resource audit {'WRITTEN' if write else 'DRY_RUN'} "
        f"entries={len(manifest['entries'])} json={manifest['summary']['json']['valid_json_and_mcmeta']} "
        f"nbt={manifest['summary']['nbt']['parsed_nbt']} resource_changes={manifest['summary']['actual_changed_resource_count']}"
    )
    if write:
        print(f"MANIFEST={MANIFEST}\nREPORT={REPORT}")


if __name__ == "__main__":
    options = argparse.ArgumentParser()
    options.add_argument("--write", action="store_true", help="Write manifest and report; never alter assets/data")
    main(options.parse_args().write)
