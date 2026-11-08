#!/usr/bin/env python3
"""Fail-closed static and packaging regression checks for the 1.1.2 NeoForge port.

This is NOT a gameplay or mixin-injection runtime test.
Only built-in Python modules are required.
"""

from __future__ import annotations

import hashlib
import json
import tomllib
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
OLD = SIFT / "releases/v1.1.1-neoforge.2-community.3-fix.1/the_sift-1.1.1-neoforge.2-community.3-fix.1.jar"
RELEASE_VERSION = "1.1.2-neoforge.1"
NEW = ROOT / "build/libs" / f"the_sift-{RELEASE_VERSION}.jar"
ADDED_CLASSES = {
    "mielon/thesift/entity/SonicSniffMotionAccess.class",
    "mielon/thesift/fluid/IchorRegeneration.class",
    "mielon/thesift/mixin/IchorRegenerationMixin.class",
    "mielon/thesift/mixin/SonicSniffMotionMixin.class",
}
CHANGED_CLASSES = {
    "mielon/thesift/entity/DarkSnifferEntity.class",
    "mielon/thesift/fluid/IchorFluid.class",
    "mielon/thesift/fluid/IchorFluid$Source.class",
    "mielon/thesift/fluid/IchorFluid$Flowing.class",
    "mielon/thesift/client/mixin/SiftWeatherTextureMixin.class",
}
NEW_MIXINS = {"IchorRegenerationMixin", "SonicSniffMotionMixin"}


def require(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)
    print("PASS " + message)


def source(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def main() -> None:
    require(OLD.is_file() and NEW.is_file(), "both NeoForge 1.1.1 release and 1.1.2 stage JAR exist")
    with ZipFile(OLD) as before, ZipFile(NEW) as after:
        require(before.testzip() is None and after.testzip() is None, "both archives have valid ZIP CRCs")
        former = {name: hashlib.sha256(before.read(name)).digest() for name in before.namelist() if not name.endswith("/")}
        current = {name: hashlib.sha256(after.read(name)).digest() for name in after.namelist() if not name.endswith("/")}
        added = set(current) - set(former)
        removed = set(former) - set(current)
        changed = {name for name in set(former) & set(current) if former[name] != current[name]}

        require(added == ADDED_CLASSES, f"exactly four expected 1.1.2 classes added ({len(added)})")
        require(not removed, "all previous NeoForge entries retained")
        changed_classes = {p for p in changed if p.endswith(".class")}
        require(changed_classes == CHANGED_CLASSES,
                f"only five planned classes changed ({len(changed_classes)})")
        expected_nonclass = {
            "META-INF/neoforge.mods.toml", "the_sift.mixins.json",
            "assets/the_sift/sounds.json",
            "data/easymagic/tags/item/enchanting_catalysts.json",
            "data/minecraft/tags/block/washed_away_by_fluids.json",
        }
        require(changed - CHANGED_CLASSES <= expected_nonclass,
                f"no unexpected binary asset/config changes ({len(changed)})")
        require("assets/the_sift/textures/textures.zip" not in current,
                "upstream duplicate textures.zip excluded")
        require("fabric.mod.json" not in current, "NeoForge-only JAR (no Fabric metadata)")
        require(RELEASE_VERSION.encode("utf-8") in after.read("META-INF/neoforge.mods.toml"),
                "official port version without staging suffix is in NeoForge metadata")
        metadata = tomllib.loads(after.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        deps = metadata["dependencies"]["the_sift"]
        neo_deps = [dep for dep in deps if dep["modId"] == "neoforge"]
        require(len(neo_deps) == 1 and neo_deps[0]["type"] == "required",
                "exactly one required NeoForge dependency")
        require(neo_deps[0]["versionRange"] == "[26.3.0.64-beta,)",
                "NeoForge range is >=26.3.0.64-beta WITHOUT upper version bound")
        require(any(dep["modId"] == "minecraft" and dep["versionRange"] == "[26.3]" for dep in deps),
                "Minecraft version remains scoped to MC 26.3")
        properties = dict(
            line.split("=", 1)
            for line in source("gradle.properties").splitlines()
            if "=" in line and not line.startswith("#")
        )
        require(properties["neo_min_version"] == "26.3.0.64-beta"
                and properties["neo_version"] != properties["neo_min_version"],
                "build/test version is separate from historical compatibility minimum")
        require(properties["mod_version"] == RELEASE_VERSION, "release properties match the official mod version")
        require("assets/the_sift/sounds.json" in current, "sound resources preserved")

        for name in sorted(changed & {
            "assets/the_sift/sounds.json",
            "data/easymagic/tags/item/enchanting_catalysts.json",
            "data/minecraft/tags/block/washed_away_by_fluids.json",
        }):
            old_obj = json.loads(before.read(name))
            new_obj = json.loads(after.read(name))
            require(old_obj == new_obj, f"old and stage {name} are JSON-semantically identical")

        old_mixin_list = json.loads(before.read("the_sift.mixins.json"))["mixins"]
        new_mixin_list = json.loads(after.read("the_sift.mixins.json"))["mixins"]
        require(set(new_mixin_list) - set(old_mixin_list) == NEW_MIXINS,
                "exactly two new required mixins registered")
        require([m for m in new_mixin_list if m not in NEW_MIXINS] == old_mixin_list,
                "all original mixins and their order retained")
        require(before.read("the_sift.client.mixins.json") == after.read("the_sift.client.mixins.json"),
                "client mixin configuration remains unchanged")

        require(not {p for p in changed if p.startswith(("data/", "assets/"))} - expected_nonclass,
                "all textures, GeckoLib files, NBT structures and gameplay data remain byte-identical")

        print(f"ARCHIVE_DELTA old={len(former)} new={len(current)} added={len(added)} removed={len(removed)} changed={len(changed)}")
        print("RELEASE_BUILD_SHA256=" + hashlib.sha256(NEW.read_bytes()).hexdigest().upper())

    fluid = source("src/main/java/mielon/thesift/fluid/IchorFluid.java")
    regen = source("src/main/java/mielon/thesift/fluid/IchorRegeneration.java")
    sonic = source("src/main/java/mielon/thesift/entity/DarkSnifferEntity.java")
    velocity = source("src/main/java/mielon/thesift/mixin/SonicSniffMotionMixin.java")
    weather = source("src/client/java/mielon/thesift/client/mixin/SiftWeatherTextureMixin.java")
    require("addEffect(" not in fluid and "multiply(0.92, 0.94, 0.92)" in fluid,
            "fluid slowdown remains; regeneration exclusively in tick handler")
    require("hasChunkAt(pos)" in regen and "ModBlocks.ICHOR_CAULDRON" in regen,
            "Ichor contact includes cauldron with loaded-chunk guard")
    require("Set<UUID>" in sonic and "getUUID()" in sonic and "theSift$markSonicSniffMotion" in sonic,
            "Sonic Sniff uses UUIDs and notifies player motion marker")
    require("TheSiftSonicSniffMotion" in velocity and "Vec3.ZERO" in velocity and "getBooleanOr" in velocity,
            "only a tagged persisted Sonic Sniff motion is reset")
    require('@ModifyVariable(method = "extractRenderState", at = @At("STORE"), ordinal = 0)' in weather
            and "@Local MutableBlockPos pos" in weather
            and "theSift$restrictSnowColumns" in weather,
            "upstream weather extraction interception is present")
    print("STATIC_112_RELEASE_QA_PASS")


if __name__ == "__main__":
    main()
