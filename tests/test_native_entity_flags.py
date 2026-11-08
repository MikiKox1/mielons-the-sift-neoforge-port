"""NeoForge forbids adding vanilla entity data IDs; preserve 2.0 AI via synced attachments."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "src/main/java/mielon/thesift"


class NativeEntityFlags(unittest.TestCase):
    def test_foreign_entities_use_native_sync_attachments(self):
        for name, key in (("SnifferInfectionMixin", "SNIFFER_INFECTED"), ("WardenDarkSnifferMixin", "WARDEN_PETTING")):
            body = (ROOT / "mixin" / (name + ".java")).read_text(encoding="utf-8-sig")
            with self.subTest(mixin=name):
                self.assertNotIn("SynchedEntityData.defineId", body)
                self.assertIn("ModAttachments." + key, body)
                self.assertIn("getExistingDataOrNull", body)
        attachments = (ROOT / "entity/ModAttachments.java").read_text(encoding="utf-8-sig")
        self.assertEqual(attachments.count("sync(ByteBufCodecs.BOOL)"), 2)

    def test_original_2_0_behavior_is_not_replaced_by_1_x_ai(self):
        sniffer = (ROOT / "mixin/SnifferInfectionMixin.java").read_text(encoding="utf-8-sig")
        self.assertIn("!sniffer.isNoAi()", sniffer)
        self.assertIn("10 + sniffer.getRandom().nextInt(5)", sniffer)
        self.assertIn("this.theSift$infectionTicks >= 600", sniffer)
        warden = (ROOT / "mixin/WardenDarkSnifferMixin.java").read_text(encoding="utf-8-sig")
        self.assertIn("SculkerFamily.contains(entity)", warden)
        self.assertIn("++this.theSift$pettingTicks >= 100", warden)


if __name__ == "__main__":
    unittest.main()
