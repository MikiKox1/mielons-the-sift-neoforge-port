"""Offline MC26.1.2 Willow sign regression against the exact target source/JAR.

Run: py -3.14 -B -m unittest discover -s tests -p test_sign_2612_compat.py -v
No game, Gradle, launcher or resource mutation. GUI success is still a runtime gate.
"""

from __future__ import annotations

import json
import io
from pathlib import Path
import re
import struct
import unittest
from zipfile import ZipFile

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/the_sift"
MIXINS = ROOT / "src/client/java/mielon/thesift/client/mixin"
TARGET = ROOT / "build/moddev/artifacts/minecraft-patched-26.1.2.126-sources.jar"
TARGET_BINARY = ROOT / "build/moddev/artifacts/minecraft-patched-26.1.2.126.jar"
WOODS = ("overgrown_willow", "ash_willow")


def json_file(rel: str):
    return json.loads((ASSETS / rel).read_text(encoding="utf-8"))


class Sign2612CompatibilityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        assert TARGET.is_file(), "Exact MC26.1.2.126 sources must be available"
        assert TARGET_BINARY.is_file(), "Exact MC26.1.2.126 game data must be available"
        with ZipFile(TARGET) as source:
            cls.standing_source = source.read(
                "net/minecraft/client/gui/screens/inventory/SignEditScreen.java"
            ).decode("utf-8")
            cls.hanging_source = source.read(
                "net/minecraft/client/gui/screens/inventory/HangingSignEditScreen.java"
            ).decode("utf-8")

    def test_01_actual_2612_apis(self):
        self.assertIn("protected void extractSignBackground(GuiGraphicsExtractor graphics)", self.standing_source)
        self.assertIn("graphics.sign(this.signModel", self.standing_source)
        self.assertNotRegex(self.standing_source, r"private\s+(?:final\s+)?Identifier\s+texture\b")
        self.assertRegex(self.hanging_source, r"private final Identifier texture\s*=")
        self.assertIn("graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture", self.hanging_source)

    def test_02_standing_gui_uses_2612_extraction_without_nonexistent_shadow(self):
        source = (MIXINS / "WillowSignEditScreenMixin.java").read_text(encoding="utf-8")
        self.assertIn("@Mixin(SignEditScreen.class)", source)
        self.assertNotRegex(source, r"@Shadow\s+(?:@Final\s+)?(?:@Mutable\s+)?private Identifier texture")
        self.assertIn("@ModifyArg(", source)
        self.assertIn('method = "extractSignBackground"', source)
        self.assertIn(
            'Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign('
            'Lnet/minecraft/client/model/Model$Simple;'
            'FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V',
            source,
        )
        self.assertIn("index = 2", source)
        self.assertIn("WoodType theSift$renderWillowSign(WoodType vanilla)", source)
        self.assertNotIn("graphics.blit(", source, "Standing Willow GUI must keep native 3D sign preview")
        for wood in WOODS:
            self.assertIn(f"ModBlocks.{wood.upper()}_WOOD_TYPE", source)
            self.assertIn(f"ModBlocks.{wood.upper()}_SIGN", source)
            self.assertIn(f"ModBlocks.{wood.upper()}_WALL_SIGN", source)

    def test_03_hanging_gui_shadow_exists_and_has_both_willow_types(self):
        source = (MIXINS / "WillowHangingSignEditScreenMixin.java").read_text(encoding="utf-8")
        self.assertIn("@Mixin(HangingSignEditScreen.class)", source)
        self.assertRegex(source, r"@Shadow\s+@Final\s+@Mutable\s+private Identifier texture")
        for wood in WOODS:
            self.assertIn(f"textures/gui/hanging_signs/{wood}.png", source)
            self.assertIn(f"ModBlocks.{wood.upper()}_HANGING_SIGN", source)
            self.assertIn(f"ModBlocks.{wood.upper()}_WALL_HANGING_SIGN", source)

    def test_04_gui_textures_are_real_and_sized_for_2612_blits(self):
        for wood in WOODS:
            for rel, dimensions in (
                (f"textures/gui/signs/{wood}.png", (24, 26)),
                (f"textures/gui/hanging_signs/{wood}.png", (16, 16)),
            ):
                image = (ASSETS / rel).read_bytes()
                self.assertEqual(b"\x89PNG\r\n\x1a\n", image[:8], rel)
                self.assertEqual(dimensions, struct.unpack(">II", image[16:24]), rel)

    def test_05_all_eight_blockstates_use_real_block_entity_particle_models(self):
        with ZipFile(TARGET_BINARY) as game:
            for vanilla_sign in ("oak_sign", "oak_wall_sign", "oak_hanging_sign", "oak_wall_hanging_sign"):
                state = json.loads(game.read(f"assets/minecraft/blockstates/{vanilla_sign}.json"))
                self.assertEqual([""], list(state["variants"]), vanilla_sign)

        for wood in WOODS:
            for suffix in ("sign", "wall_sign", "hanging_sign", "wall_hanging_sign"):
                path = f"blockstates/{wood}_{suffix}.json"
                data = json_file(path)
                model_name = f"{wood}_{'hanging_sign' if 'hanging' in suffix else 'sign'}"
                self.assertEqual({"variants": {"": {"model": f"the_sift:block/{model_name}"}}}, data, path)
                self.assertTrue((ASSETS / f"models/block/{model_name}.json").is_file(), model_name)

    def test_06_only_particle_models_no_obsolete_missing_vanilla_template_parents(self):
        with ZipFile(TARGET_BINARY) as game:
            for model in ("oak_sign", "oak_hanging_sign"):
                template = json.loads(game.read(f"assets/minecraft/models/block/{model}.json"))
                self.assertEqual({"textures"}, set(template), model)
                self.assertEqual({"particle"}, set(template["textures"]), model)

        for wood in WOODS:
            for suffix, expected_particle in (
                ("sign", f"the_sift:block/{wood}_planks"),
                ("hanging_sign", f"the_sift:block/stripped_{wood}_log"),
            ):
                path = f"models/block/{wood}_{suffix}.json"
                model = json_file(path)
                self.assertEqual({"textures": {"particle": expected_particle}}, model, path)

        for file in (ASSETS / "models/block").glob("*sign*.json"):
            text = file.read_text(encoding="utf-8")
            self.assertNotIn("minecraft:block/template_", text, str(file.relative_to(ASSETS)))

    def test_07_mandatory_mixin_registration_remains(self):
        config = json.loads((ROOT / "src/main/resources/the_sift.client.mixins.json").read_text(encoding="utf-8"))
        mixins = set(config["client"])
        self.assertIn("WillowSignEditScreenMixin", mixins)
        self.assertIn("WillowHangingSignEditScreenMixin", mixins)

    def test_08_world_sign_wood_types_are_custom_for_all_eight_sign_blocks(self):
        blocks = (ROOT / "src/main/java/mielon/thesift/block/ModBlocks.java").read_text(encoding="utf-8")
        for wood in WOODS:
            const = wood.upper() + "_WOOD_TYPE"
            self.assertRegex(
                blocks,
                r'public static final WoodType ' + const +
                r' = WoodType\.register\(new WoodType\("the_sift:' + wood +
                r'", BlockSetType\.OAK\)\);',
            )
            constructors = {
                "SIGN": "StandingSignBlock",
                "WALL_SIGN": "WallSignBlock",
                "HANGING_SIGN": "CeilingHangingSignBlock",
                "WALL_HANGING_SIGN": "WallHangingSignBlock",
            }
            for suffix, constructor in constructors.items():
                match = re.search(
                    r'public static final Block ' + wood.upper() + "_" + suffix +
                    r'\s*=\s*registerBlock\((.*?)\);',
                    blocks, flags=re.DOTALL,
                )
                self.assertIsNotNone(match, f"{wood}_{suffix} missing")
                self.assertIn(f"new {constructor}({const}, properties)", match.group(1))
                self.assertNotIn("new " + constructor + "(WoodType.OAK", match.group(1))

    def test_09_native_sprite_maps_and_dynamic_model_layers(self):
        client = (ROOT / "src/client/java/mielon/thesift/client/TheSiftClient.java").read_text(encoding="utf-8")
        lifecycle = (ROOT / "src/client/java/mielon/thesift/client/ClientModEvents.java").read_text(encoding="utf-8")
        self.assertIn("event.enqueueWork(TheSiftClient::initialize)", lifecycle)
        for wood in WOODS:
            self.assertIn(f"Sheets.addWoodType(ModBlocks.{wood.upper()}_WOOD_TYPE)", client)

        with ZipFile(TARGET) as source:
            definitions = source.read("net/minecraft/client/model/geom/LayerDefinitions.java").decode("utf-8")
            sheets = source.read("net/minecraft/client/renderer/Sheets.java").decode("utf-8")
            directory = source.read(
                "net/minecraft/client/renderer/texture/atlas/sources/DirectoryLister.java"
            ).decode("utf-8")
            self.assertIn("WoodType.values().forEach(woodType ->", definitions)
            self.assertIn("ModelLayers.createStandingSignModelName(woodType)", definitions)
            self.assertIn("ModelLayers.createWallSignModelName(woodType)", definitions)
            self.assertIn("ModelLayers.createHangingSignModelName(woodType, attachmentType)", definitions)
            self.assertIn("SIGN_SPRITES.put(woodType, createSignSprite(woodType))", sheets)
            self.assertIn("HANGING_SIGN_SPRITES.put(woodType, createHangingSignSprite(woodType))", sheets)
            self.assertIn('FileToIdConverter("textures/" + this.sourcePath, ".png")', directory)

        with ZipFile(TARGET_BINARY) as game:
            atlas = json.loads(game.read("assets/minecraft/atlases/signs.json"))
            self.assertTrue(any(
                x.get("type") == "minecraft:directory"
                and x.get("source") == "entity/signs"
                and x.get("prefix") == "entity/signs/"
                for x in atlas.get("sources", [])
            ))

    def test_10_entity_sign_atlas_uses_original_2_0_1_uv_pixels(self):
        for wood in WOODS:
            for hanging, original, target, source_face, target_face in (
                (
                    False, f"textures/block/{wood}_sign.png",
                    f"textures/entity/signs/{wood}.png",
                    (0, 16, 24, 28), (2, 2, 26, 14),
                ),
                (
                    True, f"textures/block/{wood}_hanging_sign.png",
                    f"textures/entity/signs/hanging/{wood}.png",
                    (18, 16, 32, 26), (2, 14, 16, 24),
                ),
            ):
                original_img = Image.open(ASSETS / original).convert("RGBA")
                target_img = Image.open(ASSETS / target).convert("RGBA")
                self.assertEqual((32, 32), original_img.size)
                self.assertEqual((64, 32), target_img.size)
                self.assertEqual(
                    original_img.crop(source_face).tobytes(),
                    target_img.crop(target_face).tobytes(),
                    f"{wood}: target {'hanging' if hanging else 'standing'} entity face altered original pixels",
                )


if __name__ == "__main__":
    unittest.main()
