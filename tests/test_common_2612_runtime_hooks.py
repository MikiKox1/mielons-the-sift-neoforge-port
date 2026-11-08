"""Regressions for target descriptors that javac cannot validate in mixin strings."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVAP = Path(r"C:\Program Files\Java\jdk-25.0.4.1\bin\javap.exe")


class TargetRuntimeHooks(unittest.TestCase):
    def test_stalker_first_claw_uses_real_target_knockback_descriptor(self):
        target = ROOT / "build/moddev/artifacts/minecraft-patched-26.1.2.126-merged.jar"
        text = subprocess.check_output([str(JAVAP), "-p", "-s", "-classpath", str(target),
                                        "net.minecraft.world.entity.LivingEntity"], text=True,
                                       encoding="utf8", errors="replace")
        self.assertRegex(text, r"public void knockback\(double, double, double\);\s*descriptor: \(DDD\)V")
        hook = (ROOT / "src/main/java/mielon/thesift/mixin/StalkerClawKnockbackMixin.java").read_text(encoding="utf8")
        self.assertIn('method = "knockback(DDD)V"', hook)
        self.assertIn("argsOnly = true, ordinal = 0", hook)
        self.assertIn("StalkerEntity.firstClawKnockbackStrength((LivingEntity)(Object)this, strength)", hook)
        self.assertNotIn("require = 0", hook)


if __name__ == "__main__":
    unittest.main()
