"""Static regression checks for the 26.3 shaderpack portal/sky repair.

These checks do not claim that Photon or Complementary visuals are accepted.
"""

from pathlib import Path
import math
import re
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/mielon/thesift/client"
BASE = "66367e65c22742a7c109a5632b87cd3bb26734b8"


def client(relative):
    return (CLIENT / relative).read_text(encoding="utf-8")


class ShaderFixAlpha4263(unittest.TestCase):
    def test_shader_frame_has_non_coplanar_geometry_only_for_shaderpacks(self):
        geometry = client("render/PixelPortalSurface.java")
        renderer = client("render/SiftPortalRenderer.java")
        self.assertIn("RIM_SHADER_SPACING = 0.03125F", geometry)
        self.assertIn("RIM_SHADER_SIDE_INSET = 0.015625F", geometry)
        self.assertIn("void drawShaderRimGlow(", geometry)
        self.assertIn("void drawShaderRimCore(", geometry)
        self.assertIn("void sideShader(", geometry)
        self.assertIn("PixelPortalSurface.drawShaderRimGlow(", renderer)
        self.assertIn("PixelPortalSurface.drawShaderRimCore(", renderer)
    def test_opaque_shader_backing_replaces_iris_beacon_depth_mask(self):
        types = client("render/SiftPortalRenderTypes.java")
        renderer = client("render/SiftPortalRenderer.java")
        self.assertIn("SHADER_BACKING = RenderTypes.entityCutout(WHITE)", types)
        self.assertIn("shaderPack ? SiftPortalRenderTypes.SHADER_BACKING : SiftPortalRenderTypes.DEPTH", renderer)
        self.assertIn("PixelPortalSurface.drawDepthMask", renderer)
        self.assertIn("shaderPack ? SHADER_BACKING_COLOR : color", renderer)
        self.assertIn("SHADER_BACKING_COLOR", renderer)

    def test_shader_frame_uses_translucent_pass_instead_of_beacon_mapping(self):
        types = client("render/SiftPortalRenderTypes.java")
        renderer = client("render/SiftPortalRenderer.java")
        self.assertIn("SHADER_RIM = RenderTypes.entityTranslucent(WHITE)", types)
        self.assertIn("SHADER_CORE = RenderTypes.entityTranslucent(WHITE)", types)
        self.assertIn("SiftPortalRenderTypes.rim(shaderPack)", renderer)
        self.assertIn("SiftPortalRenderTypes.core(shaderPack)", renderer)
        self.assertIn("RIM = SURFACE", types)
        self.assertIn("CORE = RenderTypes.beaconBeam(WHITE, false)", types)

    def test_postfinal_sky_blends_instead_of_erasing_pack_sky_and_clouds(self):
        shader_sky = client("render/SiftShaderSkyRenderer.java")
        self.assertIn("new ColorTargetState(BlendFunction.TRANSLUCENT)", shader_sky)
        self.assertIn("PHOTON_SKY_COVERAGE = 0.72F", shader_sky)
        self.assertIn("Math.pow(1.0F - PHOTON_SKY_COVERAGE", shader_sky)
        self.assertIn("private static float photonCompositeOpacity(", shader_sky)
        self.assertIn("new DynamicTexture[8]", shader_sky)
        self.assertIn("0, 1, 6, 2, 3, 4, 5, 7", shader_sky)
        self.assertIn("SiftProceduralSkyRenderer.shaderLayerColor", shader_sky)

    def test_postfinal_all_eight_layers_obey_normalized_phase_coverage(self):
        sky = client("render/SiftShaderSkyRenderer.java")
        # layer -> (phase, exponent budget share, legacy far/near brightness)
        layers = {
            0: ("base", .40, 1.0), 1: ("night", .20, 1.0),
            2: ("day", .36, .96), 3: ("night", .24, .96),
            4: ("day", .24, .78), 5: ("night", .16, .82),
            6: ("thrive", .42, 1.0), 7: ("thrive", .18, 1.0),
        }
        for layer, (phase, share, brightness) in layers.items():
            transform = re.search(r"transforms\[" + str(layer) + r"\]\s*=\s*SiftProceduralSkyRenderer\.transform\(pose,(.*?);", sky, re.DOTALL)
            self.assertIsNotNone(transform, f"Missing transform for layer {layer}")
            actual = transform.group(1)
            exponent = f"{share:.2f}F" if phase == "base" else f"{phase} * {share:.2f}F"
            self.assertIn(f"photonCompositeOpacity({exponent})", actual, f"Layer {layer} has no phase budget")
            if brightness != 1.0:
                self.assertIn(f"{brightness:.2f}F * photonCompositeOpacity(", actual, f"Layer {layer} lost native ribbon brightness")

        def phase_alpha(day, night, thrive):
            phase_weights = {"base": 1.0, "day": day, "night": night, "thrive": thrive}
            # Independent numerical replay of the static Java budget, with
            # intentionally pessimistic fully-opaque pixel textures.
            optical = 1.0
            total_exponent = 0.0
            for phase, share, brightness in layers.values():
                exponent = share * phase_weights[phase]
                total_exponent += exponent
                optical *= 1.0 - brightness * (1.0 - (1.0 - .72) ** exponent)
            self.assertAlmostEqual(total_exponent, 1.0, places=7)
            return 1.0 - optical

        # Every pure phase and all ordered tide crossfades, including center.
        phases = ((1., 0., 0.), (0., 1., 0.), (0., 0., 1.))
        for weights in phases:
            self.assertGreater(phase_alpha(*weights), .50)
            self.assertLessEqual(phase_alpha(*weights), .720001)
        for a in phases:
            for b in phases:
                for step in range(21):
                    t = step / 20.0
                    opacity = phase_alpha(*(a[i] * (1 - t) + b[i] * t for i in range(3)))
                    self.assertLessEqual(opacity, .720001)
                    self.assertLess(opacity, .80)
        self.assertGreater(1.0 - (1.0 - .42) * (1.0 - .96) * (1.0 - .78), .99)

    def test_sift_clouds_have_clear_weather_floor_and_shaderpack_passthrough(self):
        clouds = client("mixin/SiftCloudRendererMixin.java")
        method = re.search(r"private int theSift\$fadeCloudsWithRain\(int originalColor\)(.*?)\n   @ModifyVariable\(", clouds, re.DOTALL)
        self.assertIsNotNone(method)
        body = method.group(1)
        self.assertIn("if (SiftShaderCompat.isShaderPackInUse()) {", body)
        shader_branch = body.split("if (SiftShaderCompat.isShaderPackInUse()) {", 1)[1].split("}", 1)[0]
        self.assertIn("return originalColor;", shader_branch)
        self.assertLess(body.index("return originalColor;"), body.index("int originalAlpha"))
        self.assertIn("CLOUD_CLEAR_OPACITY = 0.44F", clouds)
        self.assertIn("CLOUD_RAIN_OPACITY = 0.66F", clouds)
        self.assertIn("originalAlpha * (CLOUD_CLEAR_OPACITY + (CLOUD_RAIN_OPACITY - CLOUD_CLEAR_OPACITY) * rain)", body)
        for original in (0, 1, 64, 128, 255):
            for rain in (0.0, 0.25, 0.5, 0.75, 1.0):
                changed = math.floor(original * (.44 + (.66 - .44) * rain) + .5)
                self.assertGreaterEqual(changed, 0)
                self.assertLessEqual(changed, original)
                if original >= 2:
                    self.assertGreater(changed, 0, f"Clear-weather cloud should not be transparent: {original=} {rain=}")

    def test_unrelated_native_sky_and_singer_remain_unchanged(self):
        for relative in (
            "src/client/java/mielon/thesift/client/render/SiftCloudRenderPipelines.java",
            "src/client/java/mielon/thesift/client/render/SiftProceduralSkyRenderer.java",
            "src/client/java/mielon/thesift/client/mixin/SiftSkyRendererMixin.java",
            "src/client/java/mielon/thesift/client/entity/SingerRenderer.java",
            "src/main/java/mielon/thesift/world/SiftTides.java",
        ):
            original = subprocess.check_output(["git", "-C", str(ROOT), "show", BASE + ":" + relative])
            self.assertEqual(original.replace(b"\r\n", b"\n"), (ROOT / relative).read_bytes().replace(b"\r\n", b"\n"), relative)
        relative = "src/client/java/mielon/thesift/client/render/PixelPortalSurface.java"
        original = subprocess.check_output(["git", "-C", str(ROOT), "show", BASE + ":" + relative], text=True, encoding="utf8")
        now = (ROOT / relative).read_text(encoding="utf8")
        def body(text, marker):
            i = text.index(marker)
            start = text.index("{", i)
            depth = 1
            for j in range(start + 1, len(text)):
                depth += (text[j] == "{") - (text[j] == "}")
                if depth == 0:
                    return text[i:j + 1]
            raise AssertionError(marker)
        for marker in ("public static void drawSurface(", "public static void drawRimGlow(",
                       "public static void drawRimCore(", "private static void side("):
            self.assertEqual(body(original, marker), body(now, marker), marker)

    def test_worldgen_code_and_data_are_unchanged(self):
        delta = subprocess.check_output([
            "git", "-C", str(ROOT), "diff", "--name-only", BASE, "--",
            "src/main/java/mielon/thesift/worldgen/",
            "src/main/resources/data/the_sift/worldgen/",
        ], text=True, encoding="utf8")
        self.assertEqual(delta.strip(), "")


if __name__ == "__main__":
    unittest.main()
