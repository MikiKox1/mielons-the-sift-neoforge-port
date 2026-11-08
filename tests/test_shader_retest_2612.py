"""Owner-reported portal/Photon THRIVE regressions, source-only until manual QA."""
import pathlib
import subprocess
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/mielon/thesift/client"
BASE = "b926094c72e50adcfdb4291008d6b17c8305c334"


def source(name):
    return (CLIENT / name).read_text(encoding="utf-8")


def method_body(text, marker):
    start = text.index(marker)
    begin = text.index("{", start)
    level = 1
    for i in range(begin + 1, len(text)):
        level += (text[i] == "{") - (text[i] == "}")
        if level == 0:
            return text[start:i + 1]
    raise AssertionError("Unterminated method: " + marker)


class ShaderRetest2612(unittest.TestCase):
    def test_no_prewritten_portal_depth_under_shaderpacks(self):
        text = source("render/SiftPortalRenderer.java")
        # When translucent fragments arrive in Iris they must not be rejected by
        # a previously rendered full-portal depth plane at position 0.5.
        self.assertIn("if (state.active && !shaderPack)", text)
        self.assertIn("SiftPortalRenderTypes.DEPTH", text)

    def test_portal_sheet_rim_and_core_share_stable_shaderpack_type(self):
        types = source("render/SiftPortalRenderTypes.java")
        renderer = source("render/SiftPortalRenderer.java")
        self.assertIn("RenderType rim(boolean shaderPack)", types)
        self.assertIn("RenderType core(boolean shaderPack)", types)
        self.assertIn("return shaderPack ? SHADER_SURFACE : RIM;", types)
        self.assertIn("return shaderPack ? SHADER_SURFACE : CORE;", types)
        self.assertIn("SiftPortalRenderTypes.rim(shaderPack)", renderer)
        self.assertIn("SiftPortalRenderTypes.core(shaderPack)", renderer)

    def test_only_shader_frame_geometry_separates_coplanar_rim_and_border(self):
        surface = source("render/PixelPortalSurface.java")
        renderer = source("render/SiftPortalRenderer.java")
        self.assertIn("RIM_SHADER_SPACING = 0.03125F", surface)
        self.assertIn("RIM_SHADER_SIDE_INSET = 0.015625F", surface)
        self.assertIn("void drawShaderRimGlow(", surface)
        self.assertIn("void drawShaderRimCore(", surface)
        self.assertIn("void sideShader(", surface)
        self.assertIn("if (shaderPack)", renderer)
        self.assertIn("PixelPortalSurface.drawShaderRimGlow(", renderer)
        self.assertIn("PixelPortalSurface.drawShaderRimCore(", renderer)

    def test_shaderpack_portal_has_opaque_non_beacon_continuous_backdrop(self):
        types = source("render/SiftPortalRenderTypes.java")
        renderer = source("render/SiftPortalRenderer.java")
        # Actual Complementary r5.9.3 BEACON_BEAM shader alpha dithers
        # translucent fragments. The sheet needs a fully covered non-beacon
        # background while its mosaics continue animating above that plane.
        self.assertIn("SHADER_BACKDROP = RenderTypes.entityCutout(WHITE)", types)
        self.assertIn("if (state.active && shaderPack)", renderer)
        self.assertIn("SiftPortalRenderTypes.SHADER_BACKDROP", renderer)
        self.assertIn("PixelPortalSurface.drawDepthMask", renderer)
        self.assertIn("0xFF26C0C5", renderer)

    def test_thrive_photon_uses_partial_alpha_for_pack_contribution(self):
        sky = source("render/SiftProceduralSkyRenderer.java")
        method = method_body(sky, "private static void renderTextured(")
        self.assertIn("photonThriveBlend", method)
        self.assertIn("thrive * 0.48F", method)
        self.assertIn("thrive * 0.36F", method)
        self.assertIn("0.72F - 0.20F * thrive", method)
        self.assertIn("transforms[6] = transform", method)
        self.assertIn("transforms[7] = transform", method)

    def test_native_sky_and_portal_art_and_singer_unchanged(self):
        for relative in (
            "src/client/java/mielon/thesift/client/entity/SingerRenderer.java",
            "src/main/java/mielon/thesift/world/SiftTides.java",
            "src/main/java/mielon/thesift/world/SafePortalArrival.java",
        ):
            original = subprocess.check_output(
                ["git", "-C", str(ROOT), "show", BASE + ":" + relative]
            )
            now = (ROOT / relative).read_bytes()
            self.assertEqual(now.replace(b"\r\n", b"\n"), original.replace(b"\r\n", b"\n"), relative)
        relative = "src/client/java/mielon/thesift/client/render/PixelPortalSurface.java"
        original = subprocess.check_output(
            ["git", "-C", str(ROOT), "show", BASE + ":" + relative], text=True, encoding="utf-8"
        )
        current = (ROOT / relative).read_text(encoding="utf-8")
        for marker in ("public static void drawSurface(", "public static void drawRimGlow(",
                       "public static void drawRimCore(", "private static void side("):
            self.assertEqual(method_body(original, marker), method_body(current, marker), marker)
        relative = "src/client/java/mielon/thesift/client/render/SiftProceduralSkyRenderer.java"
        original = subprocess.check_output(
            ["git", "-C", str(ROOT), "show", BASE + ":" + relative], text=True, encoding="utf-8"
        )
        current = (ROOT / relative).read_text(encoding="utf-8")
        self.assertEqual(
            method_body(current, "public static void render(PoseStack"),
            method_body(original, "public static void render(PoseStack"),
            "Shader fix changed native sky branch",
        )


if __name__ == "__main__":
    unittest.main()
