"""Static regression contracts for MC26.2 shader renderer fixes.

Engine evidence (mapped MC26.2 client bytecode): LevelRenderer.render calls
addCloudsPass only for nonzero cloudColor alpha; DepthStencilState.DEFAULT
uses reversed-Z GREATER_THAN_OR_EQUAL; RenderPipelines' translucent beacon
explicitly installs ColorTargetState(BlendFunction.TRANSLUCENT).
No client/server or Gradle execution is required to run these tests.
"""
from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / 'src/client/java/mielon/thesift/client'


def source(name):
    return (CLIENT / name).read_text(encoding='utf-8')


def java_method(text, marker):
    start = text.index(marker)
    opening = text.index('{', start)
    depth = 1
    for i in range(opening + 1, len(text)):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    raise AssertionError('Unclosed method: ' + marker)


class ShaderFixAlpha8Tests(unittest.TestCase):
    def test_shader_portal_has_opaque_backing_and_no_depth_only_occluder(self):
        renderer = source('render/SiftPortalRenderer.java')
        types = source('render/SiftPortalRenderTypes.java')
        geometry = source('render/PixelPortalSurface.java')
        self.assertIn('if (state.active && !shaderPack)', renderer)
        self.assertIn('if (state.active && shaderPack)', renderer)
        self.assertIn('SiftPortalRenderTypes.BACKING', renderer)
        self.assertIn('PixelPortalSurface.drawShaderBacking', renderer)
        self.assertIn('PixelPortalSurface.drawSurfaceDetails', renderer)
        self.assertIn('RenderTypes.entitySolid(WHITE)', types)
        self.assertRegex(geometry, r'drawShaderBacking\([^)]*\)\s*\{[^}]*color\(38, 192, 197, 255\)')
        self.assertRegex(geometry, r'drawSurfaceDetails\([^)]*\)\s*\{[^}]*partition\(')

    def test_frame_surfaces_have_real_distance_from_neighbor_blocks(self):
        geometry = source('render/PixelPortalSurface.java')
        renderer = source('render/SiftPortalRenderer.java')
        types = source('render/SiftPortalRenderTypes.java')
        self.assertIn('RIM_SIDE_INSET = 0.015625F', geometry)
        self.assertIn('edge == 4 ? RIM_SIDE_INSET : 1.0F - RIM_SIDE_INSET', geometry)
        self.assertIn('edge == 1 ? RIM_SIDE_INSET : 1.0F - RIM_SIDE_INSET', geometry)
        self.assertIn('RIM_LAYER_SPACING = 0.03125F', geometry)
        self.assertIn('RIM_AURA_OFFSET = 0.03125F', geometry)
        self.assertIn('PixelPortalSurface.drawShaderRimGlow', renderer)
        self.assertIn('PixelPortalSurface.drawShaderRimCore', renderer)
        self.assertIn('SiftPortalRenderTypes.rim(shaderPack)', renderer)
        self.assertIn('SiftPortalRenderTypes.core(shaderPack)', renderer)
        self.assertIn('shaderPack ? SHADER_SURFACE : RIM', types)
        self.assertIn('shaderPack ? SHADER_SURFACE : CORE', types)

    def test_native_geometry_is_byte_for_byte_unchanged(self):
        rel = 'src/client/java/mielon/thesift/client/render/PixelPortalSurface.java'
        original = subprocess.check_output(
            ['git', '-C', str(ROOT), 'show', '9b485f5:' + rel], encoding='utf-8'
        )
        current = (ROOT / rel).read_text(encoding='utf-8')
        for marker in ('void drawSurface(', 'void drawRimGlow(', 'void drawRimCore(', 'void side('):
            self.assertEqual(java_method(original, marker), java_method(current, marker), marker)

    def test_clouds_remain_visible_without_rain_and_keep_shader_colors(self):
        mixin = source('mixin/SiftCloudRendererMixin.java')
        self.assertIn('if (SiftShaderCompat.isShaderPackInUse())', mixin)
        self.assertRegex(mixin, r'if \(SiftShaderCompat\.isShaderPackInUse\(\)\)\s*\{\s*return originalColor;')
        self.assertIn('CLOUD_CLEAR_OPACITY', mixin)
        self.assertIn('originalAlpha * (CLOUD_CLEAR_OPACITY', mixin)
        self.assertNotIn('originalAlpha * rain * 0.66F', mixin)
        self.assertIn('original == RenderPipelines.CLOUDS ? SiftCloudRenderPipelines.FANCY : original', mixin)

    def test_photon_preserves_underlying_pack_sky_with_alpha_composition(self):
        sky = source('render/SiftShaderSkyRenderer.java')
        self.assertIn('new ColorTargetState(BlendFunction.TRANSLUCENT)', sky)
        self.assertEqual(2, sky.count('new ColorTargetState(BlendFunction.TRANSLUCENT)'))
        self.assertIn('PHOTON_SKY_COVERAGE = 0.72F', sky)
        self.assertIn('postFinal ? photonCompositeOpacity(0.40F) : 1.0F', sky)
        self.assertIn('postFinal ? photonCompositeOpacity(night * 0.20F)', sky)
        self.assertIn('postFinal ? photonCompositeOpacity(thrive * 0.42F) : thrive', sky)
        self.assertIn('postFinal ? photonCompositeOpacity(thrive * 0.18F) : thrive', sky)
        self.assertIn('postFinal ? 0.96F * photonCompositeOpacity(day * 0.36F)', sky)
        self.assertIn('postFinal ? 0.96F * photonCompositeOpacity(night * 0.24F)', sky)
        self.assertIn('postFinal ? 0.78F * photonCompositeOpacity(day * 0.24F)', sky)
        self.assertIn('postFinal ? 0.82F * photonCompositeOpacity(night * 0.16F)', sky)
        self.assertIn('new DepthStencilState(CompareOp.EQUAL, false)', sky)
        self.assertIn('0, 1, 6, 2, 3, 4, 5, 7', sky)
        self.assertIn('SiftShaderCompat.assignPipeline(TEXTURED_PIPELINE, "TEXTURED")', sky)
        self.assertNotIn('SiftShaderCompat.assignPipeline(PHOTON_PIPELINE', sky)

    def test_photon_blend_budget_is_constant_for_flow_endure_and_thrive(self):
        sky = source('render/SiftShaderSkyRenderer.java')
        self.assertIn('1.0F - PHOTON_SKY_COVERAGE', sky)
        for day, night, thrive in ((1, 0, 0), (0, 1, 0), (0, 0, 1), (0.5, 0.5, 0), (0.3, 0.2, 0.5)):
            shares = (
                (0.40, 1.0),
                (night * 0.20, 1.0),
                (thrive * 0.42, 1.0),
                (day * 0.36, 0.96),
                (night * 0.24, 0.96),
                (day * 0.24, 0.78),
                (night * 0.16, 0.82),
                (thrive * 0.18, 1.0),
            )
            self.assertAlmostEqual(1.0, sum(weight for weight, _ in shares), places=6)
            remaining_photon = 1.0
            for share, native_strength in shares:
                remaining_photon *= 1.0 - native_strength * (1.0 - 0.28 ** share)
            self.assertGreaterEqual(remaining_photon, 0.28 - 1e-9)
            self.assertLess(1.0 - remaining_photon, 0.80)


if __name__ == '__main__':
    unittest.main()
