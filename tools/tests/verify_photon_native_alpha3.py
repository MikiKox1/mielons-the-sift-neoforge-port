#!/usr/bin/env python3
"""Fail-closed ALPHA3 regression against pinned ALPHA2 and Photon v1.3b.

PASS means compiled/reviewed package and exact Iris pack selection, NOT proof
of the sky's appearance in-game. No user worlds or shaderpack ZIPs are edited.
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
OLD = SIFT / (
    "artifacts/builds/experimental-sky-alpha2-262/"
    "TheSift-1.1.2-MC26.2-NeoForge-26.2.0.90-ALPHA2-SKY-EXPERIMENTAL-STAGEONLY.jar"
)
NEW = ROOT / "build/libs/the_sift-1.1.2-neoforge.26.2-alpha.3-photonexp.jar"
SOURCES = ROOT / "build/libs/the_sift-1.1.2-neoforge.26.2-alpha.3-photonexp-sources.jar"
PHOTON = SIFT / (
    "backports/26.2-1.1.2-prep/reference/optional-shaders-photon-v1.3b/"
    "photon_v1.3b-OFFICIAL-REFERENCE-UNMODIFIED.zip"
)
SRC_COMPAT = ROOT / "src/client/java/mielon/thesift/client/compat/SiftShaderCompat.java"
SRC_RENDERER = ROOT / "src/client/java/mielon/thesift/client/render/SiftProceduralSkyRenderer.java"
OLD_SHA = "b583e143e5a8fd3e6ca3136efb91a9a112127004bff01f9c2052ff5f6268e71a"
PHOTON_SHA = "120897768eaa8cb1f14a3e98f88dff009ae3204ee3b96904a4fe8586edfc1ead"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def check(value: bool, label: str) -> None:
    assert value, label
    print("PASS " + label)


def main() -> None:
    check(OLD.is_file() and sha(OLD.read_bytes()) == OLD_SHA, "ALPHA2 binary protected by SHA")
    check(PHOTON.is_file() and sha(PHOTON.read_bytes()) == PHOTON_SHA,
          "official Photon v1.3b reference ZIP protected by SHA")
    check(NEW.is_file() and SOURCES.is_file(), "separate ALPHA3 binary/source JARs exist")

    with ZipFile(PHOTON) as photon:
        check(photon.testzip() is None, "Photon reference archive CRC")
        shader = photon.read("shaders/world0/gbuffers_textured.fsh").decode("utf-8")
        underlying = photon.read("shaders/program/gbuffers_all_translucent.fsh").decode("utf-8")
        check('#include "/program/gbuffers_all_translucent.fsh"' in shader
              and "RENDERTARGETS: 13" in underlying
              and "material_from(" in underlying
              and "get_diffuse_lighting(" in underlying,
              "Photon TEXTURED routes sky layers to lit-translucent material buffer 13")

    with ZipFile(OLD) as a, ZipFile(NEW) as b, ZipFile(SOURCES) as sources:
        check(all(z.testzip() is None for z in (a, b, sources)), "ALPHA2 / ALPHA3 / sources ZIP CRC")
        old_names = {k for k in a.namelist() if not k.endswith("/")}
        new_names = {k for k in b.namelist() if not k.endswith("/")}
        check(old_names == new_names, "ALPHA3 adds/removes no gameplay/assets/images/classes")
        changed = sorted(k for k in old_names if a.read(k) != b.read(k))
        classes = [k for k in changed if k.endswith(".class")]
        check(classes == [
            "mielon/thesift/client/compat/SiftShaderCompat.class",
            "mielon/thesift/client/render/SiftProceduralSkyRenderer.class",
        ], "only intended Photon guard and sky renderer bytecode changed")
        semantic_diff = []
        cosmetic_diff = []
        for key in changed:
            if key in classes:
                continue
            x, y = a.read(key), b.read(key)
            if key.endswith(".json"):
                try:
                    if json.loads(x) == json.loads(y):
                        cosmetic_diff.append(key)
                        continue
                except (UnicodeDecodeError, ValueError):
                    pass
            if x.replace(b"\r\n", b"\n") == y.replace(b"\r\n", b"\n"):
                cosmetic_diff.append(key)
                continue
            semantic_diff.append(key)
        check(semantic_diff == ["META-INF/neoforge.mods.toml"],
              "ALPHA1/2 datapack and shader assets byte/semantic equivalent")
        for key in old_names:
            if key.startswith("assets/the_sift/textures/sky/alpha2/") and key.endswith(".png"):
                assert a.read(key) == b.read(key), key
        check(len([p for p in old_names if p.startswith(
            "assets/the_sift/textures/sky/alpha2/") and p.endswith(".png")]) == 6,
            "six ALPHA2 texture assets remain exactly unchanged")
        toml = tomllib.loads(b.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        dependencies = {d["modId"]: d for d in toml["dependencies"]["the_sift"]}
        check(toml["mods"][0]["modId"] == "the_sift"
              and toml["mods"][0]["version"] == "1.1.2-neoforge.26.2-alpha.3-photonexp"
              and toml["license"] == "MIT"
              and "LICENSE_the_sift" in new_names,
              "ALPHA3 separate version, original mod ID and MIT attribution")
        check(dependencies["minecraft"]["versionRange"] == "[26.2]"
              and dependencies["neoforge"]["versionRange"] == "[26.2.0.90,)"
              and dependencies["geckolib"]["versionRange"] == "[5.5.6,6)",
              "Minecraft26.2, latest .90 minimum open upper, GeckoLib5.5.6")
        check(not any(p.startswith(("net/irisshaders/", "net/caffeinemc/"))
                      for p in new_names), "no Iris/Sodium/shaderpack binaries bundled")
        print(f"ALPHA3_COSMETIC_ONLY_ENTRIES={len(cosmetic_diff)}")

    compatible = SRC_COMPAT.read_text(encoding="utf-8")
    renderer = SRC_RENDERER.read_text(encoding="utf-8")
    check('Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName")' in compatible,
          "guarded optional Iris pack-name probe uses actual pinned Iris 1.11.4 symbol")
    check('name.toLowerCase(Locale.ROOT).startsWith("photon")' in compatible
          and "isPhotonShaderPackSelected()" in compatible
          and "getCurrentPackName" in compatible,
          "only matching Photon names select native compatibility candidate")
    check("PHOTON_NATIVE_SKY_PIPELINE" in renderer
          and "pipeline/photon_native_sky_alpha3" in renderer,
          "separate named Photon-specific pipeline exists")
    check("event.registerPipeline(PHOTON_NATIVE_SKY_PIPELINE);" in renderer,
          "Photon pipeline registered in NeoForge client event")
    check('SiftShaderCompat.assignPipeline(TEXTURED_SKY_PIPELINE, "TEXTURED")' in renderer
          and "SiftShaderCompat.assignPipeline(PHOTON_NATIVE_SKY_PIPELINE" not in renderer,
          "ALPHA2 Iris TEXTURED mapping preserved; Photon fallback intentionally unmapped")
    check("boolean photonNative = shadersActive && SiftShaderCompat.isPhotonShaderPackSelected()" in renderer
          and "(photonNative ? PHOTON_NATIVE_SKY_PIPELINE : TEXTURED_SKY_PIPELINE)" in renderer
          and "? RenderPipelines.DEBUG_QUADS" in renderer,
          "Iris off, non-Photon and Photon branches remain independent")
    check("pass.bindTexture(" in renderer and "getClampToEdge(FilterMode.LINEAR)" in renderer,
          "original interpolated six-layer UV texture sampling retained")
    check("The Sift ALPHA3 active sky render path" in renderer,
          "active in-world render path recorded once for shaderpack QA")
    print("ALPHA3_PACKAGE_SHA256=" + sha(NEW.read_bytes()).upper())
    print("ALPHA3_STATIC_QA=PASS; PHOTON_IN_WORLD_APPEARANCE=PENDING")


if __name__ == "__main__":
    main()
