"""Pin target processor dispatch and preserve the accepted generation algorithm."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = "22176ab7e71ad9a5c2c624cfc72015702bb98f22"
WG = ROOT / "src/main/java/mielon/thesift/worldgen"


def base_source(name):
    return subprocess.check_output(
        ["git", "-C", str(ROOT), "show", BASE + ":src/main/java/mielon/thesift/worldgen/" + name],
        text=True, encoding="utf8")


class TargetWorldgenApi(unittest.TestCase):
    def test_processors_use_2612_abstract_dispatch(self):
        for cls, field in (("SculkersLairProcessor", "SCULKERS_LAIR_PROCESSOR"),
                           ("CampCoralVinesProcessor", "CAMP_CORAL_VINES")):
            text = (WG / (cls + ".java")).read_text(encoding="utf8")
            self.assertIn("class " + cls + " extends StructureProcessor", text)
            self.assertIn("protected StructureProcessorType<?> getType()", text)
            self.assertIn("return ModWorldgen." + field + ";", text)
            # Only type dispatch may change: do not silently reshape structures.
            self.assertEqual(text.split("public List<StructureBlockInfo> finalizeProcessing", 1)[1],
                             base_source(cls + ".java").split("public List<StructureBlockInfo> finalizeProcessing", 1)[1])

    def test_processor_registry_wraps_codec_in_original_2612_type(self):
        text = (WG / "ModWorldgen.java").read_text(encoding="utf8")
        for cls, field in (("SculkersLairProcessor", "SCULKERS_LAIR_PROCESSOR"),
                           ("CampCoralVinesProcessor", "CAMP_CORAL_VINES")):
            self.assertIn("StructureProcessorType<" + cls + "> " + field, text)
            self.assertIn("() -> " + cls + ".CODEC", text)
        self.assertEqual(re.findall(r'register\("([a-z_]+)"', text),
                         re.findall(r'register\("([a-z_]+)"', base_source("ModWorldgen.java")))

    def test_old_vanilla_names_preserve_exact_allay_sniffer_and_candles(self):
        for file, constant in (("SiftAllaySpawner.java", "ALLAY"),
                               ("SiftDeferredSnifferSpawner.java", "SNIFFER")):
            text = (WG / file).read_text(encoding="utf8")
            self.assertIn("EntityType." + constant, text)
            self.assertNotIn("EntityTypes", text)
            self.assertEqual(text, base_source(file).replace("EntityTypes", "EntityType"))
        text = (WG / "SoulshaleShelterFeature.java").read_text(encoding="utf8")
        self.assertEqual(text, base_source("SoulshaleShelterFeature.java")
                         .replace("Blocks.DYED_CANDLE.black()", "Blocks.BLACK_CANDLE")
                         .replace("Blocks.DYED_CANDLE.white()", "Blocks.WHITE_CANDLE"))


if __name__ == "__main__":
    unittest.main()
