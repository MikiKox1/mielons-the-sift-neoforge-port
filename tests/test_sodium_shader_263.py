"""Sodium legacy/namespaced quad ABIs keep the same exact data layout."""
from pathlib import Path
import json
import re
import subprocess
import unittest

ROOT=Path(__file__).resolve().parents[1]
REFERENCE=ROOT.parents[1]/'reports/qa/shader-integration-20261108/api'
JAVAP=Path(r"C:\Program Files\Java\jdk-25.0.4.1\bin\javap.exe")

class SodiumShader263(unittest.TestCase):
    def test_single_optional_mixin_uses_shared_data_layout(self):
        source=(ROOT/'src/client/java/mielon/thesift/client/mixin/SodiumIchorQuadAccessor.java').read_text(encoding='utf8')
        self.assertIn('@Pseudo',source)
        self.assertIn('@Accessor("data")',source)
        self.assertIn('@Accessor("sprite")',source)
        caller=(ROOT/'src/client/java/mielon/thesift/client/mixin/SodiumIchorFluidUvMixin.java').read_text(encoding='utf8')
        self.assertIn('Float.intBitsToFloat',caller)
        self.assertIn('i * 8',caller)
        self.assertNotIn('default float',source)
        self.assertNotRegex(source,r'@Invoker\("(?:sodium\$)?get')
        config=json.loads((ROOT/'src/main/resources/the_sift.client.mixins.json').read_text())
        self.assertNotIn('plugin',config)
        self.assertIn('SodiumIchorQuadAccessor',config['client'])
        self.assertNotIn('SodiumIchorLegacyQuadAccessor',config['client'])

    def test_exact_pinned_legacy_and_namespaced_sodium_layouts(self):
        source=(ROOT/'src/client/java/mielon/thesift/client/mixin/SodiumIchorQuadAccessor.java').read_text(encoding='utf8')
        for jar in ('sodium-263-beta1-actual.jar','sodium-26.3-actual.jar'):
            file=REFERENCE/jar
            api=subprocess.check_output([str(JAVAP),'-p','-c','-classpath',str(file),'net.caffeinemc.mods.sodium.client.model.quad.ModelQuad'],text=True,encoding='utf8')
            self.assertIn('private final int[] data;',api)
            self.assertIn('TextureAtlasSprite sprite;',api)
            for setter in re.findall(r'@Invoker\("([^"]+)"\)',source):
                self.assertRegex(api,re.escape(setter)+r'\(')
            util=subprocess.check_output([str(JAVAP),'-p','-c','-classpath',str(file),'net.caffeinemc.mods.sodium.client.util.ModelQuadUtil'],text=True,encoding='utf8')
            body=util[util.index('public static int vertexOffset(int);'):]
            self.assertRegex(body,r'bipush\s+8[\s\S]+imul')
            for getter in ('getX','getY','getZ'):
                method=re.search(r'public float (?:sodium\$)?'+getter+r'\(int\);[\s\S]*?(?=\n  public |\n})',api)
                self.assertIsNotNone(method)
                self.assertIn('Field data:[I',method[0])
                self.assertIn('Float.intBitsToFloat',method[0])

    def test_current_ichor_mapping_algorithm_unchanged(self):
        for relative in ('src/client/java/mielon/thesift/client/render/IchorQuadUv.java',):
            before=subprocess.check_output(['git','-C',str(ROOT),'show','b23395351ff39c02acd4682fce4d6b6a5130e5d5:'+relative])
            self.assertEqual((ROOT/relative).read_bytes().replace(b'\r\n',b'\n'),before.replace(b'\r\n',b'\n'))
        relative='src/client/java/mielon/thesift/client/mixin/SodiumIchorFluidUvMixin.java'
        before=subprocess.check_output(['git','-C',str(ROOT),'show','b23395351ff39c02acd4682fce4d6b6a5130e5d5:'+relative],text=True,encoding='utf8')
        expected=before.replace('         for (int i = 0; i < 4; i++) {\n            uv.blockVertex(i, view.theSift$getX(i), view.theSift$getY(i), view.theSift$getZ(i));',
            '         int[] vertices = view.theSift$vertexData();\n         for (int i = 0; i < 4; i++) {\n            uv.blockVertex(i, Float.intBitsToFloat(vertices[i * 8]),\n               Float.intBitsToFloat(vertices[i * 8 + 1]), Float.intBitsToFloat(vertices[i * 8 + 2]));')
        self.assertNotEqual(expected,before)
        self.assertEqual((ROOT/relative).read_text(encoding='utf8'),expected)

if __name__=='__main__':unittest.main()
