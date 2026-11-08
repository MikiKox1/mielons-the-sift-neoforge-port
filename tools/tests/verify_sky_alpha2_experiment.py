#!/usr/bin/env python3
"""Fail-closed ALPHA2 static/JAR regression: mod-owned Iris textured sky.

Run AFTER Gradle clean build. This cannot validate in-world Photon/Complementary
color, but prevents accidental changes to gameplay or resource data from ALPHA1.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tomllib
from zipfile import ZipFile

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
OLD = SIFT / "artifacts/builds/TheSift-1.1.2-MC26.2-NeoForge-26.2.0.90-ALPHA1-STAGEONLY.jar"
NEW = ROOT / "build/libs/the_sift-1.1.2-neoforge.26.2-alpha.2-skyexp.jar"
GENERATOR_MANIFEST = ROOT / "evidence/alpha2-baked-sky-textures.json"
SKY = ROOT / "src/client/java/mielon/thesift/client/render/SiftProceduralSkyRenderer.java"
NAMES = [
    "base_day", "base_night",
    "ribbons_far_day", "ribbons_far_night",
    "ribbons_near_day", "ribbons_near_night",
]
EXPECTED_OLD_SHA = "6bcbd85fb607c421ecd522e0cacb0b8aef59a38c0337859e1fb2bc2514985032"


def sha(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def check(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)
    print("PASS " + message)


def main() -> None:
    check(OLD.is_file() and sha(OLD.read_bytes()) == EXPECTED_OLD_SHA,
          "accepted ALPHA1 baseline pinned by SHA256")
    check(NEW.is_file(), "compiled independent ALPHA2 experiment exists")
    assets = {
        f"assets/the_sift/textures/sky/alpha2/{name}.png" for name in NAMES
    }
    with ZipFile(OLD) as a, ZipFile(NEW) as b:
        check(a.testzip() is None and b.testzip() is None, "both JAR ZIP CRC passes")
        before = {p for p in a.namelist() if not p.endswith("/")}
        after = {p for p in b.namelist() if not p.endswith("/")}
        check(not (before - after) and after - before == assets,
              "only the six sky texture assets added, no deleted entries")
        modified = [p for p in sorted(before & after) if a.read(p) != b.read(p)]
        modified_classes = [p for p in modified if p.endswith(".class")]
        check(modified_classes == ["mielon/thesift/client/render/SiftProceduralSkyRenderer.class"],
              "only intended renderer class changed")
        semantic_changes = []
        formatting_only = 0
        for path in modified:
            if path in modified_classes:
                continue
            former, current = a.read(path), b.read(path)
            if path.endswith(".json"):
                try:
                    if json.loads(former) == json.loads(current):
                        formatting_only += 1
                        continue
                except ValueError:
                    pass
            if former.replace(b"\r\n", b"\n") == current.replace(b"\r\n", b"\n"):
                formatting_only += 1
                continue
            semantic_changes.append(path)
        check(semantic_changes == ["META-INF/neoforge.mods.toml"],
              "worldgen, gameplay JSON, shaders, models, tags only differ in line endings")
        meta = tomllib.loads(b.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        deps = {d["modId"]: d for d in meta["dependencies"]["the_sift"]}
        check(meta["mods"][0]["modId"] == "the_sift"
              and meta["mods"][0]["version"] == "1.1.2-neoforge.26.2-alpha.2-skyexp"
              and meta["license"] == "MIT",
              "separate experiment metadata, original mod ID and MIT attribution")
        check(deps["minecraft"]["versionRange"] == "[26.2]"
              and deps["neoforge"]["versionRange"] == "[26.2.0.90,)"
              and deps["geckolib"]["versionRange"] == "[5.5.6,6)",
              "Minecraft 26.2, open-ended NeoForge from 26.2.0.90, GeckoLib 5.5.6")
        check("LICENSE_the_sift" in after, "MIT license embedded")
        check("fabric.mod.json" not in after, "NeoForge-only JAR")

        report = json.loads(GENERATOR_MANIFEST.read_text(encoding="utf-8"))
        check(len(report["images"]) == 6 and report["dimensions"] == [2048, 768],
              "six static filtered day/night layer textures declared")
        for name in NAMES:
            path = f"assets/the_sift/textures/sky/alpha2/{name}.png"
            pixels = b.read(path)
            file_pixels = (ROOT / "src/main/resources" / path).read_bytes()
            check(pixels == file_pixels and sha(pixels) == report["images"][name + ".png"]["sha256"],
                  f"{name}: exact verified texture asset in packaged JAR")
            from io import BytesIO
            with Image.open(BytesIO(pixels)) as img:
                check(img.size == (2048, 768) and img.mode == "RGBA", f"{name}: RGBA8 correct")
                image = np.asarray(img, dtype=np.int16)
                check(np.array_equal(image[:, 0], image[:, -1]), f"{name}: seamless azimuth U=0/U=1")
                alpha = image[:, :, 3]
                max_change = max(
                    int(np.abs(np.diff(alpha, axis=0)).max()),
                    int(np.abs(np.diff(alpha, axis=1)).max()),
                )
                check(max_change <= 3, f"{name}: alpha texture varies smoothly (max neighbor delta {max_change})")
                if name.startswith("base"):
                    check(int(alpha.min()) == 255, f"{name}: base layer stays opaque")
                else:
                    check(0 < int(alpha.max()) < 128, f"{name}: ribbons stay softly translucent")

        source = SKY.read_text(encoding="utf-8")
        check('SiftShaderCompat.assignPipeline(TEXTURED_SKY_PIPELINE, "TEXTURED")' in source,
              "Iris shaderpack pipeline explicitly uses TEXTURED, not BASIC")
        check('DefaultVertexFormat.POSITION_TEX' in source
              and '.withBindGroupLayout(BindGroupLayouts.SAMPLER0)' in source
              and 'withFragmentShader("core/position_tex")' in source,
              "Blaze3D typed UV shader and texture sampler bound")
        check('FilterMode.LINEAR' in source and 'getClampToEdge(' in source,
              "texture is bilinear filtered and clamped at poles/seam")
        check('TEXTURED_DOME_BUFFER' in source
              and 'pass.bindTexture(' in source
              and 'getTexture(SKY_TEXTURES[layer])' in source,
              "one shared textured mesh with six cached vanilla-managed texture resources")
        check('pass.setPipeline(shadersActive ? TEXTURED_SKY_PIPELINE : RenderPipelines.DEBUG_QUADS)' in source
              and 'buildDome(false, 0, false)' in source,
              "no-shaderpack original POSITION_COLOR path retained")
        check('pass.setUniform("DynamicTransforms", transforms[layer])' in source
              and 'SiftDayNightCycle.nightBlend(ticks)' in source,
              "original six layer motion, day/night and transparency modulation retained")
        check('if (TEXTURED_DOME_BUFFER != null)' in source and 'TEXTURED_DOME_BUFFER.close()' in source,
              "GPU buffer cleanup on renderer close")
        check(not any(
            p.startswith("net/irisshaders/") or p.startswith("net/caffeinemc/")
            for p in after
        ), "no Iris, Sodium or shaderpack binaries bundled into The Sift")
        print(f"PACKAGE_DELTA new_png=6 changed_class=1 formatting_only={formatting_only} meta=1")
        print("EXPERIMENT_JAR_SHA256=" + sha(NEW.read_bytes()).upper())
        print("ALPHA2_STATIC_AND_BINARY_QA=PASS; SHADERPACK_VISUALS=USER_TEST_PENDING")


if __name__ == "__main__":
    main()
