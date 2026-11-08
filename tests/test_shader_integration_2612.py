"""Focused source/API contracts for MC26.1.2 The Sift 2.0.1 shader integration.

Run without Gradle or a client:
  py -3.14 -B -m unittest discover -s tests -p test_shader_integration_2612.py -v
"""

import json
from pathlib import Path
import re
import subprocess
import unittest
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/mielon/thesift/client"
SHADERS = ROOT / "src/main/resources/assets/the_sift/shaders/core"
DEPENDENCIES = ROOT.parent.parent / "reports/qa/shader-integration-20261108/dependencies"
NATIVE = ROOT.parent / "neo-26.1.2-1.1.2.1-release/build/moddev/artifacts/minecraft-patched-26.1.2.126-sources.jar"


def read(relative):
    return (ROOT / relative).read_text(encoding="utf-8")


class Shader2612IntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sky = read("src/client/java/mielon/thesift/client/render/SiftProceduralSkyRenderer.java")
        cls.sky_mixin = read("src/client/java/mielon/thesift/client/mixin/SiftSkyRendererMixin.java")
        cls.tail = read("src/client/java/mielon/thesift/client/mixin/SiftShaderSkyMixin.java")
        cls.compat = read("src/client/java/mielon/thesift/client/compat/SiftShaderCompat.java")
        cls.sky_compat = read("src/client/java/mielon/thesift/client/compat/SiftSkyShaderCompat.java")
        cls.mixins = json.loads(read("src/main/resources/the_sift.client.mixins.json"))["client"]

    def test_01_preserve_original_eight_layers_and_thrive_sky_colors(self):
        self.assertIn("new GpuBuffer[8]", self.sky)
        self.assertIn("SiftTides.nightBlend(ticks)", self.sky)
        self.assertIn("SiftTides.flowBlend(ticks)", self.sky)
        self.assertIn("SiftTides.thriveBlend(ticks)", self.sky)
        self.assertIn("ThriveSkyColors.color(u, elevationDegrees)", self.sky)
        self.assertIn("ThriveSkyColors.sunrise(elevationDegrees)", self.sky)
        self.assertIn("Math.sin(time * 0.0065F + 1.8F)", self.sky)
        self.assertIn("renderSunriseAndSunset", self.sky_mixin)
        self.assertIn("direction * 90.0F", self.sky_mixin)

    def test_02_eight_shader_uv_layers_sample_current_exact_native_color_math(self):
        self.assertIn("new DynamicTexture[8]", self.sky)
        self.assertIn("new NativeImage(", self.sky)
        self.assertIn("new GpuBuffer[8]", self.sky)
        self.assertIn("sampleColor(u, v, night, layer)", self.sky)
        self.assertIn("sampleColor(u, v, night, originalLayer)", self.sky)
        self.assertIn("ThriveSkyColors.color(u, elevationDegrees)", self.sky)
        self.assertIn("ThriveSkyColors.sunrise(elevationDegrees)", self.sky)
        self.assertIn("DefaultVertexFormat.POSITION_TEX", self.sky)
        self.assertIn("RenderPipeline TEXTURED_SKY_PIPELINE", self.sky)
        self.assertNotIn('texture("base_day")', self.sky)
        self.assertNotIn("new GpuBuffer[6]", self.sky)

    def test_03_shader_resource_uploads_before_render_pass_and_release_on_close(self):
        self.assertIn("ensureShaderTextures()", self.sky)
        self.assertIn("TextureManager", self.sky)
        self.assertIn("pass.bindTexture(\"Sampler0\"", self.sky)
        shader_method = self.sky[self.sky.index("private static void renderTextured("):]
        self.assertLess(shader_method.index("ensureShaderTextures()"), shader_method.index(".createRenderPass("))
        self.assertLess(shader_method.index("getTextureView()"), shader_method.index(".createRenderPass("))
        self.assertIn("textureManager.release(", self.sky)
        self.assertIn("TEXTURED_DOME_BUFFER.close()", self.sky)

    def test_04_photon_final_stage_is_optional_and_uses_forward_depth(self):
        self.assertIn("IrisPhotonFinalSkyMixin", self.mixins)
        source = read("src/client/java/mielon/thesift/client/mixin/IrisPhotonFinalSkyMixin.java")
        self.assertIn("@Pseudo", source)
        self.assertIn("net.irisshaders.iris.pipeline.IrisRenderingPipeline", source)
        self.assertIn('finalizeLevelRendering()V', source)
        self.assertIn("renderAfterPhotonFinalPass()", source)
        self.assertIn("new DepthStencilState(CompareOp.EQUAL, false)", self.sky)
        self.assertIn("gl_FragDepth = 1.0", (SHADERS / "photon_sky_overlay.fsh").read_text(encoding="utf-8"))
        self.assertIn("clip.z = clip.w", (SHADERS / "photon_sky_overlay.vsh").read_text(encoding="utf-8"))
        iris = DEPENDENCIES / "26.1.2/iris-neoforge-1.11.4+mc26.1.2.jar"
        with ZipFile(iris) as archive:
            bytecode = archive.read("net/irisshaders/iris/pipeline/IrisRenderingPipeline.class")
            self.assertIn(b"finalizeLevelRendering", bytecode)

    def test_05_no_double_generic_restoration_for_photon_or_complementary(self):
        self.assertIn("isPhotonShaderPackSelected()", self.compat)
        self.assertIn("isComplementaryShaderPackSelected()", self.compat)
        self.assertIn("isPhotonShaderPackSelected()", self.tail)
        self.assertIn("isComplementaryShaderPackSelected()", self.tail)
        self.assertIn("return;", self.tail)
        self.assertIn("SiftProceduralSkyRenderer.restoreAfterShaders(view)", self.tail)
        self.assertIn("queuePhoton", self.sky)
        self.assertIn("renderAfterPhotonFinalPass", self.sky)

    def test_06_only_shader_sky_when_allowed_and_native_default_remains(self):
        self.assertIn("SiftSkyShaderCompat.restoreSky()", self.sky_mixin)
        self.assertIn("SiftSkyShaderCompat.restoreSky()", self.sky)
        self.assertIn("!SiftShaderCompat.isShaderPackInUse()", self.sky_mixin)
        self.assertIn("if (!SiftShaderCompat.isShaderPackInUse())", self.sky_mixin)
        self.assertIn("supportsCustomSky()", self.sky_compat)
        self.assertIn("getFluidInCamera() != FogType.NONE", self.sky_compat)
        self.assertIn("MobEffects.BLINDNESS", self.sky_compat)
        self.assertIn("MobEffects.DARKNESS", self.sky_compat)

    def test_07_optional_iris_reflection_not_linked_in_server_classes(self):
        self.assertIn("Class.forName(\"net.irisshaders.iris.api.v0.IrisApi\")", self.compat)
        self.assertIn("Class.forName(\"net.irisshaders.iris.Iris\")", self.compat)
        self.assertIn("assignPipeline(TEXTURED_SKY_PIPELINE, \"TEXTURED\")", self.sky)
        self.assertNotIn("import net.irisshaders.", self.compat)
        self.assertIn("remap = false", read("src/client/java/mielon/thesift/client/mixin/IrisPhotonFinalSkyMixin.java"))

    def test_08_portal_rift_sonorous_existing_shader_paths_preserved(self):
        portal = read("src/client/java/mielon/thesift/client/render/SiftPortalRenderer.java")
        rift = read("src/client/java/mielon/thesift/client/entity/RiftRenderer.java")
        beam = read("src/client/java/mielon/thesift/client/render/SonorousBeamRenderTypes.java")
        self.assertIn("SiftPortalRenderTypes.surface(shaderPack)", portal)
        self.assertIn("drawDepthMask", portal)
        self.assertIn("mesh.drawShaderpack", rift)
        self.assertIn("getBackgroundVariant()", rift)
        self.assertIn("assignPipeline(BEAM_PIPELINE, \"BEACON_BEAM\")", beam)

    def test_09_singer_glow_source_unmodified_against_alpha2_commit(self):
        paths = sorted((CLIENT / "entity").glob("Singer*"))
        self.assertGreater(len(paths), 0)
        for path in paths:
            relative = path.relative_to(ROOT).as_posix()
            original = subprocess.check_output(
                ["git", "show", "693b2fae4f3c45d491067138e39730eb7133a7bf:" + relative],
                cwd=ROOT,
            )
            # Windows checkout uses CRLF while git objects are LF.
            self.assertEqual(
                original.replace(b"\r\n", b"\n"),
                path.read_bytes().replace(b"\r\n", b"\n"),
                f"Singer must not receive 1.1.2 glow fixes: {relative}",
            )

    def test_10_changes_are_scoped_to_alpha3_and_shader_only(self):
        self.assertIn("new GpuBuffer[8]", self.sky)
        self.assertNotIn("SINGER_EMISSIVE", self.sky)
        changed = subprocess.check_output(
            [
                "git", "diff", "--name-only", "693b2fae4f3c45d491067138e39730eb7133a7bf",
                "--", "src/client/java", "src/main/resources/the_sift.client.mixins.json",
                "src/main/resources/assets/the_sift/shaders/core",
            ],
            cwd=ROOT, text=True,
        ).splitlines()
        for path in changed:
            self.assertTrue(
                path.startswith("src/client/java/mielon/thesift/client/compat/")
                or path.startswith("src/client/java/mielon/thesift/client/render/")
                or path.startswith("src/client/java/mielon/thesift/client/mixin/")
                or path in {
                    "src/client/java/mielon/thesift/client/TheSiftClient.java",
                    "src/main/resources/the_sift.client.mixins.json",
                    "src/main/resources/assets/the_sift/shaders/core/photon_sky_overlay.vsh",
                    "src/main/resources/assets/the_sift/shaders/core/photon_sky_overlay.fsh",
                },
                f"Out-of-scope tracked source edit: {path}",
            )
        client_path = "src/client/java/mielon/thesift/client/TheSiftClient.java"
        before = subprocess.check_output(
            ["git", "show", "693b2fae4f3c45d491067138e39730eb7133a7bf:" + client_path],
            cwd=ROOT, text=True,
        )
        expected = before.replace(
            "import mielon.thesift.client.render.SiftPortalRenderer;",
            "import mielon.thesift.client.render.SiftPortalRenderer;\nimport mielon.thesift.client.render.SiftPortalRenderTypes;",
        ).replace("SiftShaderCompat.initialize();", "SiftShaderCompat.initialize();\n      SiftPortalRenderTypes.initialize();")
        self.assertEqual((ROOT / client_path).read_text(encoding="utf8"), expected)


if __name__ == "__main__":
    unittest.main()
