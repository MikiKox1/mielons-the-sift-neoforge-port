#!/usr/bin/env python3
"""Independent static/JAR regression for the staged MC26.2 F4 port.

The script checks the actual packaged JAR, not only source files. It does NOT
assert in-world gameplay, renderer visual parity, multiplayer or modpack QA.
"""
from __future__ import annotations

import hashlib
import json
import re
import tomllib
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
PREP = SIFT / "backports/26.2-1.1.2-prep"
ORIGINAL = PREP / "reference/upstream-mielon-1.1.2-fabric-26.2.jar"
NEO263 = PREP / "reference/our-port-1.1.2-neoforge-26.3.jar"
BIN = ROOT / "build/libs/the_sift-1.1.2-neoforge.26.2-alpha.1.jar"
SOURCES = ROOT / "build/libs/the_sift-1.1.2-neoforge.26.2-alpha.1-sources.jar"
CLIENT = ROOT / "src/client/java/mielon/thesift/client"
RESOURCE_MANIFEST = ROOT / "evidence/phase2-pack-262.json"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def verify(ok: bool, name: str) -> None:
    assert ok, name
    print("PASS " + name)


def main() -> None:
    verify(
        sha(ORIGINAL.read_bytes()) ==
        "1013807c2dea8f366f97347ab74cc895518799caf0e9513ff19a660567d33886",
        "upstream Mielon 1.1.2 Fabric26.2 release hash"
    )
    verify(
        sha(NEO263.read_bytes()) ==
        "d3bd9a6bf718e755e1da42190ff726a48d3fe6708245ed879a00883434e27cee",
        "our accepted NeoForge26.3 Port 1.1.2 reference hash"
    )
    verify(BIN.is_file() and SOURCES.is_file(), "build and source JARs exist")
    with ZipFile(ORIGINAL) as original, ZipFile(BIN) as binary, ZipFile(SOURCES) as sources:
        verify(
            all(z.testzip() is None for z in (original, binary, sources)),
            "source and binary ZIP/CRC validation"
        )
        info = tomllib.loads(binary.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        verify(info["mods"][0]["modId"] == "the_sift"
               and info["mods"][0]["version"] == "1.1.2-neoforge.26.2-alpha.1",
               "The Sift stage ID and 26.2 alpha version metadata")
        verify(info["loaderVersion"] == "[11,)",
               "FML Java language provider from 11 (NeoForge26.2)")
        verify(info["mods"][0].get("iconFile") == "assets/the_sift/icon.png"
               and "logoFile" not in info["mods"][0],
               "current FML 11.0 icon metadata without deprecated logoFile")
        verify(info["license"] == "MIT" and "LICENSE_the_sift" in binary.namelist(),
               "original MIT attribution preserved in JAR")
        deps = {d["modId"]: d for d in info["dependencies"]["the_sift"]}
        verify(deps["minecraft"]["versionRange"] == "[26.2]",
               "Minecraft dependency scoped to exact MC26.2")
        verify(deps["neoforge"]["versionRange"] == "[26.2.0.90,)",
               "NeoForge provisional minimum with no upper bound")
        verify(deps["geckolib"]["versionRange"] == "[5.5.6,6)",
               "GeckoLib MC26.2 required 5.5.6 through 5.x")
        verify(deps["lambdynlights"]["type"] == "optional" and "easymagic" not in deps,
               "no unrelated optional runtime dependency forced")

        original_wg = sorted(
            p for p in original.namelist()
            if p.startswith("data/the_sift/worldgen/") and p.endswith(".json")
        )
        packaged_wg = sorted(
            p for p in binary.namelist()
            if p.startswith("data/the_sift/worldgen/") and p.endswith(".json")
        )
        verify(len(original_wg) == len(packaged_wg) == 158
               and original_wg == packaged_wg
               and all(original.read(p) == binary.read(p) for p in original_wg),
               "158 worldgen JSON payloads byte-identical with original MC26.2")
        nbt_paths = sorted(p for p in original.namelist() if p.endswith(".nbt"))
        verify(len(nbt_paths) == 16 and
               all(binary.read(p) == original.read(p) for p in nbt_paths),
               "all 16 original 26.2 NBT structures preserved byte-for-byte")
        changes = json.loads(RESOURCE_MANIFEST.read_text(encoding="utf-8"))
        verify(len(changes["resources"]) == changes["restored_files"] == 147
               and all(
                   binary.read(row["name"]) == original.read(row["name"])
                   for row in changes["resources"]
               ), "147 26.2-specific advancement/loot/recipe/model/shader resources")

        tag = "data/the_sift/tags/block/sift_carver_replaceables.json"
        verify(
            json.loads(binary.read(tag)) == json.loads(original.read(tag)),
            "custom 26.2 carver replacement tag restored from upstream"
        )
        verify(
            "data/neoforge/data_maps/block/strippables.json" in binary.namelist()
            and "data/neoforge/data_maps/block/transformables.json" not in binary.namelist(),
            "NeoForge26.2 native strippables replacing unsupported transformables"
        )
        strippables = json.loads(binary.read("data/neoforge/data_maps/block/strippables.json"))
        verify(len(strippables["values"]) == 2,
               "both independent willow wood stripping transformations retained")
        compost = json.loads(binary.read("data/neoforge/data_maps/item/compostables.json"))
        verify(len(compost["values"]) == 16, "16 original compostables carried to NeoForge data map")
        verify(
            "the_sift.client.mixins.json" in binary.namelist()
            and "the_sift.mixins.json" in binary.namelist(),
            "client/server mixin configurations included"
        )
        for file_name in ("the_sift.client.mixins.json", "the_sift.mixins.json"):
            config = json.loads(binary.read(file_name))
            package = config["package"].replace(".", "/")
            names = config.get("client", []) + config.get("mixins", [])
            verify(bool(names) and all(
                package + "/" + name.replace(".", "/") + ".class" in binary.namelist()
                for name in names
            ), f"all declared {file_name} mixin classes are actually packaged")
        verify(
            not any(
                path.startswith(("fuzs/easymagic/", "fuzs/puzzleslib/", "net/caffeinemc/"))
                for path in binary.namelist()
            ), "standalone release does not bundle optional third-party mod classes"
        )

    src = {p.name: p.read_text(encoding="utf-8") for p in CLIENT.rglob("*.java")}
    all_client = "\n".join(src.values())
    verify(
        not re.search(r"com\.mojang\.renderpearl|OitPipelineSet|renderOit\s*\(", all_client),
        "removed absent 26.3 RenderPearl/OIT from all client Java"
    )
    def file(rel: str) -> str:
        return (CLIENT / rel).read_text(encoding="utf-8")

    event = file("ClientModEvents.java")
    verify("RegisterRenderPipelinesEvent" in event and
           event.count(".registerPipelines(event)") == 6,
           "NeoForge client event registers six pipeline owners")
    # Separate render owners with original 26.2 Blaze3D-style builders.
    owners = (
        ("render/RiftRenderTypes.java", 2),
        ("render/SiftPortalRenderTypes.java", 1),
        ("render/SonorousBeamRenderTypes.java", 2),
        ("render/SiftCloudRenderPipelines.java", 2),
        ("render/SiftProceduralSkyRenderer.java", 1),
        ("loading/SiftLoadingBackgroundRenderer.java", 2),
    )
    verify(all(file(path).count("event.registerPipeline(") == count
               for path, count in owners),
           "all ten custom Blaze3D pipelines registered exactly once")
    weather = file("mixin/SiftWeatherTextureMixin.java")
    verify(
        "method = \"render(Lnet/minecraft/world/phys/Vec3;" in weather
        and "TextureManager;getTexture(Lnet/minecraft/resources/Identifier;)" in weather
        and "rainTexture;" not in weather and "snowTexture;" not in weather
        and "ICHOR_SNOWY_PEAKS" in weather,
        "target 26.2 weather injector exact overload with Ichor biome rules"
    )
    air = file("mixin/IchorAirHudMixin.java")
    verify(
        "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;" in air
        and "hud/ichor_air" in air,
        "air bubble injector uses target 26.2 Blaze3D method descriptor"
    )
    sky = file("render/SiftProceduralSkyRenderer.java")
    verify(
        "shaderElevationSubdivisions" in sky and "SHADER_VERTEX_COUNT" in sky
        and "warpZenithGradient" in sky
        and "new RenderPass" not in sky
        and "createRenderPass(" in sky,
        "kept 26.3 sky tessellation and adapted 26.2 RenderTarget/RenderPass"
    )
    print("BINARY_SHA256=" + sha(BIN.read_bytes()).upper())
    print("SOURCES_SHA256=" + sha(SOURCES.read_bytes()).upper())
    print("F4_STATIC_JAR_QA=PASS; in-world visual/multiplayer gameplay NOT tested")


if __name__ == "__main__":
    main()
