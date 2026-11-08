#!/usr/bin/env python3
"""ALPHA4 static/JAR safety gate. Not a visual Photon in-world test."""

from __future__ import annotations

import hashlib
import json
import tomllib
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
OLD = SIFT / (
    "artifacts/builds/experimental-sky-alpha3-photon-262/"
    "TheSift-1.1.2-MC26.2-NeoForge-26.2.0.90-ALPHA3-PHOTON-EXPERIMENTAL-STAGEONLY.jar"
)
NEW = ROOT / "build/libs/the_sift-1.1.2-neoforge.26.2-alpha.4-postsky.jar"
SRC = ROOT / "build/libs/the_sift-1.1.2-neoforge.26.2-alpha.4-postsky-sources.jar"
VANILLA_SOURCES = ROOT / "build/moddev/artifacts/minecraft-patched-26.2.0.90-sources.jar"
JAVA = ROOT / "src/client/java/mielon/thesift/client"
ASSETS = "assets/the_sift/shaders/core/"
EXPECTED_OLD_SHA = "ccc5ae35d5eb250f42f4f24e5a58963e044d15080697ca965420165c51b423da"
ADDED_ENTRIES = {
    "mielon/thesift/client/mixin/IrisPhotonFinalSkyMixin.class",
    ASSETS + "photon_sky_overlay.vsh",
    ASSETS + "photon_sky_overlay.fsh",
}


def digest(blob: bytes) -> str:
    return hashlib.sha256(blob).hexdigest()


def ok(predicate: bool, label: str) -> None:
    assert predicate, label
    print("PASS", label)


def main() -> None:
    ok(OLD.is_file() and digest(OLD.read_bytes()) == EXPECTED_OLD_SHA,
       "frozen ALPHA3 file SHA verified")
    ok(NEW.is_file() and SRC.is_file(), "ALPHA4 binary and sources JAR exist")
    with ZipFile(OLD) as older, ZipFile(NEW) as newer, ZipFile(SRC) as sources:
        ok(all(p.testzip() is None for p in (older, newer, sources)), "all ZIP CRC pass")
        before = set(n for n in older.namelist() if not n.endswith("/"))
        after = set(n for n in newer.namelist() if not n.endswith("/"))
        ok(after - before == ADDED_ENTRIES and not before - after,
           "exactly 1 optional mixin class + 2 mod-owned shader sources added")
        modified = {
            name for name in before & after if older.read(name) != newer.read(name)
        }
        expected_changed_classes = {
            "mielon/thesift/client/render/SiftProceduralSkyRenderer.class",
        }
        ok({p for p in modified if p.endswith(".class")} == expected_changed_classes,
           "only intended renderer existing bytecode changed")
        for name in before & after:
            if name in modified:
                continue
            if name.startswith(("data/", "assets/")) or name.endswith(".nbt"):
                assert older.read(name) == newer.read(name), name
        modified_other = {p for p in modified if not p.endswith(".class")}
        semantic = []
        for name in modified_other:
            x, y = older.read(name), newer.read(name)
            if name.endswith(".json"):
                try:
                    if json.loads(x) == json.loads(y):
                        continue
                except ValueError:
                    pass
            if x.replace(b"\r\n", b"\n") == y.replace(b"\r\n", b"\n"):
                continue
            semantic.append(name)
        ok(set(semantic) == {"META-INF/neoforge.mods.toml", "the_sift.client.mixins.json"},
           "only intended metadata and optional mixin configuration changed")
        info = tomllib.loads(newer.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        dep = {x["modId"]: x for x in info["dependencies"]["the_sift"]}
        ok(info["mods"][0]["version"] == "1.1.2-neoforge.26.2-alpha.4-postsky"
           and info["mods"][0]["modId"] == "the_sift"
           and info["license"] == "MIT" and "LICENSE_the_sift" in after,
           "original mod identity and license intact, experimental version distinct")
        ok(dep["minecraft"]["versionRange"] == "[26.2]"
           and dep["neoforge"]["versionRange"] == "[26.2.0.90,)"
           and dep["geckolib"]["versionRange"] == "[5.5.6,6)",
           "NeoForge minimum .90 upper bound OPEN, target 26.2 GeckoLib pinned")
        config = json.loads(newer.read("the_sift.client.mixins.json"))
        ok(config["client"].count("IrisPhotonFinalSkyMixin") == 1,
           "optional Iris final-pass mixin added exactly once")
        ok(not any("photon_v1.3b" in p or p.startswith(("net/irisshaders/", "net/caffeinemc/"))
                   for p in after), "no Photon/Iris/Sodium third-party assets included")
        ok(all(older.read(p) == newer.read(p) for p in before if p.startswith(
            "assets/the_sift/textures/sky/alpha2/")), "all six accepted gradient PNGs unchanged")

    renderer = (JAVA / "render/SiftProceduralSkyRenderer.java").read_text(encoding="utf-8")
    iris_mixin = (JAVA / "mixin/IrisPhotonFinalSkyMixin.java").read_text(encoding="utf-8")
    vsh = (ROOT / "src/main/resources" / (ASSETS + "photon_sky_overlay.vsh")).read_text()
    fsh = (ROOT / "src/main/resources" / (ASSETS + "photon_sky_overlay.fsh")).read_text()
    ok('@Pseudo' in iris_mixin
       and 'net.irisshaders.iris.pipeline.IrisRenderingPipeline' in iris_mixin
       and 'finalizeLevelRendering()V' in iris_mixin
       and 'at = @At("TAIL")' in iris_mixin
       and 'require = 0' in iris_mixin,
       "Iris post-final client hook optional and precise")
    ok('PHOTON_POST_FINAL_SKY_PIPELINE' in renderer
       and 'new DepthStencilState(CompareOp.EQUAL, false)' in renderer
       and 'renderAfterPhotonFinalPass()' in renderer,
       "native sky overlay after final pass with depth equality / no depth writes")
    ok('photonSkyQueued = true;' in renderer
       and 'PHOTON_SKY_MODELVIEW.set(RenderSystem.getModelViewStack())' in renderer
       and 'modelView.set(PHOTON_SKY_MODELVIEW)' in renderer,
       "original camera pose/modelview snapshot reused after final pass")
    ok('if (SiftShaderCompat.isShaderPackInUse() && SiftShaderCompat.isPhotonShaderPackSelected())' in renderer
       and 'SiftShaderCompat.assignPipeline(TEXTURED_SKY_PIPELINE, "TEXTURED")' in renderer
       and 'RenderPipelines.DEBUG_QUADS' in renderer,
       "Photon deferred, Complementary TEXTURED and shader-off original routes retained")
    ok('float photonBaseBlend = photonPostFinal ? 0.72F : 1.0F;' in renderer
       and 'TEXTURED_DOME_BUFFER' in renderer, "sky alpha leaves part of Photon clouds/moon visible")
    ok("clip.z = 0.0;" in vsh and "gl_Position = clip;" in vsh
       and "texture(Sampler0, texCoord0)" in fsh
       and "ColorModulator" in fsh,
       "final overlay vertex explicitly at far plane and fragment retains alpha")
    with ZipFile(VANILLA_SOURCES) as src:
        gl = src.read("com/mojang/blaze3d/opengl/GlDevice.java").decode()
        ds = src.read("com/mojang/blaze3d/pipeline/DepthStencilState.java").decode()
        ok("ARBClipControl.glClipControl(36001, 37727)" in gl
           and "CompareOp.GREATER_THAN_OR_EQUAL" in ds,
           "26.2 OpenGL ZERO_TO_ONE and reversed-Z verified; far clip Z=0, not -W")
    print("FINAL_ALPHA4_JAR_SHA256=" + digest(NEW.read_bytes()).upper())
    print("ALPHA4_STATIC_PACKAGE_QA=PASS; PHOTON_IN_WORLD_STILL_PENDING")


if __name__ == "__main__":
    main()
