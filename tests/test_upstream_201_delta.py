"""Bound the 2.0.1 backport to the verified same-Minecraft upstream delta."""
from pathlib import Path
from zipfile import ZipFile
import gzip
import io
import json
import re
import unittest

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/mielon/thesift"
RESOURCE = ROOT / "src/main/resources"
BASE = ROOT.parents[1] / "reference/upstream-2.0.0-20261107/mielon's-the-sift-2.0.0-mc26.3-fabric.jar"
NEW = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
STRUCTURE = "data/the_sift/structure/ichor_dome/organic_desert_2.nbt"
LANG = "assets/the_sift/lang/en_us.json"


def parse_nbt(data):
    raw = gzip.decompress(data) if data.startswith(b"\x1f\x8b") else data
    return nbtlib.File.parse(io.BytesIO(raw))


class Upstream201Delta(unittest.TestCase):
    def test_native_gamerule_is_exact_and_registered_in_its_event(self):
        manager = (JAVA / "world/RiftManager.java").read_text(encoding="utf-8")
        entry = (JAVA / "TheSiftMod.java").read_text(encoding="utf-8")
        self.assertRegex(manager, r'GameRules\.registerBoolean\("the_sift:spawn_rifts",\s*GameRuleCategory\.SPAWNING,\s*true\)')
        self.assertRegex(entry, r'BuiltInRegistries\.GAME_RULE\.key\(\)\)\)\s*\{\s*RiftManager\.registerGameRules\(\)')
        constructor = entry.split("public TheSiftMod(IEventBus modBus)", 1)[1].split("private void registerContent", 1)[0]
        self.assertNotIn("RiftManager.registerGameRules", constructor)
        self.assertNotIn("net.fabricmc", manager)

    def test_disable_only_natural_spawning_not_rift_transit(self):
        manager = (JAVA / "world/RiftManager.java").read_text(encoding="utf-8")
        self.assertIn("tickNaturalSpawns(server, now);", manager)
        self.assertRegex(manager, r'if \(!server\.getGameRules\(\)\.get\(spawnRifts\)\)\s*\{\s*NEXT_SPAWN\.clear\(\)')
        tick = manager.split("public static void tick(MinecraftServer server)", 1)[1].split("private static void tickNaturalSpawns", 1)[0]
        self.assertIn("RiftLightCleanup.tick(server)", tick)
        self.assertIn("finish(server, request)", tick)
        self.assertIn("releaseRiftExitLatches(server)", tick)
        self.assertIn("RiftDirectory.loaded()", tick)
        self.assertNotIn("getGameRules().get(spawnRifts)", tick)

    def test_invalid_melody_keeps_the_rolling_window(self):
        body = (JAVA / "portal/SonorousConsoles.java").read_text(encoding="utf-8")
        self.assertRegex(body, r'if \(matches\(attempt, expected\)\)\s*\{\s*BUFFERS\.remove\(key\);')
        self.assertIn("notes.get(index).soundIndex1to8() != sequence[index]", body)
        self.assertIn("new int[]{1, 3, 7, 6, 5, 2, 4, 8}", body)

    def test_all_five_new_chunk_guards_are_retained(self):
        checks = (
            ("block/entity/SonorousDeepslateBlockEntity.java", "level.hasChunkAt(notePos) && level.getBlockState(notePos)"),
            ("portal/PortalFrameScanner.java", "closing && level.hasChunkAt(pos) && level.getBlockState(pos)"),
            ("portal/SonorousIgnition.java", "!this.level.hasChunkAt(note.notePos())"),
        )
        for path, expected in checks:
            with self.subTest(path=path):
                self.assertIn(expected, (JAVA / path).read_text(encoding="utf-8"))
        auto = (JAVA / "portal/PortalAutoActivation.java").read_text(encoding="utf-8")
        self.assertEqual(auto.count("level.hasChunkAt(candidate)"), 2)

    def test_null_properties_do_not_trigger_ichor_state_reads(self):
        body = (JAVA / "mixin/IchorBlockFluidStateMixin.java").read_text(encoding="utf-8")
        self.assertIn("Block block, Property<?>[] propertyKeys, Comparable<?>[] propertyValues, CallbackInfo ci", body)
        self.assertIn("propertyKeys != null && propertyValues != null", body)
        self.assertIn("this.theSift$fluidKind = 1;", body)
        self.assertIn("Math.max(this.lightEmission, 12)", body)
        self.assertIn("(BlockState)(Object)this", body)

    def test_structure_keeps_the_26_3_format_and_fixed_original_block(self):
        with ZipFile(BASE) as original:
            expected = parse_nbt(original.read(STRUCTURE))
        self.assertEqual(str(expected["palette"][6]["id"]), "minecraft:chiseled_sulfur")
        expected["palette"][6]["id"] = nbtlib.String("the_sift:chiseled_soulshale")
        actual = parse_nbt((RESOURCE / STRUCTURE).read_bytes())
        self.assertEqual(int(actual["DataVersion"]), 5023)
        self.assertEqual(actual, expected)

    def test_translations_are_the_exact_upstream_2_0_1_file(self):
        with ZipFile(NEW) as upstream:
            self.assertEqual((RESOURCE / LANG).read_bytes(), upstream.read(LANG))
        data = json.loads((RESOURCE / LANG).read_bytes())
        self.assertEqual(data["gamerule.the_sift.spawn_rifts"], "Spawn Rifts")


if __name__ == "__main__":
    unittest.main()
