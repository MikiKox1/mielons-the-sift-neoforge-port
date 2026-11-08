"""Shader integration source contracts; runtime/visual evidence is separate."""
from pathlib import Path
import json
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = 'cfc09025c4a9d5dc4de86f248f83370c23c0a2fb'
JAVA = ROOT/'src/client/java/mielon/thesift/client'


def source(relative):
    file = JAVA/relative
    return file.read_text(encoding='utf8') if file.is_file() else ''


def method(text, marker):
    start = text.index(marker)
    opening = text.index('{', start)
    depth = 1
    for index in range(opening+1, len(text)):
        if text[index] == '{': depth += 1
        if text[index] == '}': depth -= 1
        if depth == 0: return text[start:index+1]
    raise AssertionError('Unclosed method '+marker)


class ShaderIntegration262(unittest.TestCase):
    def test_pack_classification_is_optional_and_uses_live_name(self):
        text = source('compat/SiftShaderCompat.java')
        self.assertIn('isPhotonShaderPackSelected', text)
        self.assertIn('isComplementaryShaderPackSelected', text)
        self.assertIn('getCurrentPackName', text)
        self.assertIn('Locale.ROOT', text)

    def test_eight_live_textures_use_current_color_sampler(self):
        text = source('render/SiftShaderSkyRenderer.java')
        self.assertIn('new DynamicTexture[8]', text)
        self.assertIn('SiftProceduralSkyRenderer.shaderLayerColor', text)
        self.assertIn('SiftTides.thriveBlend', text)
        self.assertIn('SiftTides.flowBlend', text)
        self.assertIn('0, 1, 6, 2, 3, 4, 5, 7', text)
        self.assertNotIn('textures/sky/alpha2', text)

    def test_optional_iris_final_hook_is_registered(self):
        text = source('mixin/IrisPhotonFinalSkyMixin.java')
        self.assertIn('@Pseudo', text)
        self.assertIn('finalizeLevelRendering()V', text)
        self.assertIn('require = 0', text)
        self.assertIn('SiftShaderSkyRenderer.afterPhotonFinal', text)
        configs = list((ROOT/'src').rglob('the_sift.client.mixins.json'))
        self.assertEqual(len(configs),1)
        self.assertIn('IrisPhotonFinalSkyMixin', json.loads(configs[0].read_text(encoding='utf8'))['client'])

    def test_gpu_allocation_is_before_open_pass_and_close_releases(self):
        text = source('render/SiftShaderSkyRenderer.java')
        self.assertIn('void prepare(', text)
        body = method(text, 'void draw(')
        for forbidden in ('getTexture(', 'new NativeImage', 'new DynamicTexture', 'createBuffer(', '.upload('):
            self.assertNotIn(forbidden, body)
        body = method(text, 'void close(')
        self.assertIn('.close()', body)
        self.assertIn('queuedLevel = null', body)

    def test_correct_depth_and_no_global_pipeline_mapping(self):
        text = source('render/SiftShaderSkyRenderer.java')
        self.assertIn('new DepthStencilState(CompareOp.EQUAL, false)', text)
        self.assertIn('assignPipeline(TEXTURED_PIPELINE, "TEXTURED")', text)
        self.assertNotIn('assignPipeline(PHOTON_PIPELINE', text)
        shader = ROOT/'src/main/resources/assets/the_sift/shaders/core/photon_tide_sky.fsh'
        self.assertTrue(shader.is_file())
        self.assertIn('gl_FragDepth = 0.0', shader.read_text(encoding='utf8'))

    def test_native_color_functions_and_singer_are_preserved(self):
        relative = 'src/client/java/mielon/thesift/client/render/SiftProceduralSkyRenderer.java'
        old = subprocess.check_output(['git','-C',str(ROOT),'show',BASE+':'+relative], text=True, encoding='utf8')
        new = (ROOT/relative).read_text(encoding='utf8')
        for marker in ('private static float[] baseColor(', 'private static float[] convergeLowerHemisphere(',
                       'private static float[] ribbonColor(', 'private static void addVertex('):
            self.assertEqual(method(new,marker),method(old,marker),marker)
        for relative in ('src/client/java/mielon/thesift/client/entity/SingerRenderer.java',
                         'src/main/java/mielon/thesift/world/SiftTides.java',
                         'src/main/java/mielon/thesift/world/ThriveSkyColors.java',
                         'src/main/java/mielon/thesift/world/SafePortalArrival.java'):
            old = subprocess.check_output(['git','-C',str(ROOT),'show',BASE+':'+relative])
            self.assertEqual((ROOT/relative).read_bytes().replace(b'\r\n',b'\n'),old.replace(b'\r\n',b'\n'),relative)

    def test_specialized_frame_has_same_sky_guards_and_no_second_restore(self):
        text = source('render/SiftShaderSkyRenderer.java')
        self.assertIn('SiftSkyShaderCompat.restoreSky()', text)
        self.assertIn('queuedLevel != client.level', text)
        self.assertIn('void finishFrame(', text)
        self.assertIn('frameDrawn', text)
        mixin = source('mixin/SiftShaderSkyMixin.java')
        self.assertIn('SiftShaderSkyRenderer.finishFrame', mixin)
        self.assertIn('SiftShaderSkyRenderer.isManagedPack()', mixin)


    def test_photon_queue_consumed_only_after_actual_iris_final(self):
        text = source('render/SiftShaderSkyRenderer.java')
        body = method(text, 'void finishFrame(')
        self.assertNotIn('afterPhotonFinal();', body)
        self.assertIn('if (SiftShaderCompat.isPhotonShaderPackSelected()) return;', body)


if __name__ == '__main__':
    unittest.main()
