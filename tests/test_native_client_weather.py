"""Regression for the native NeoForge overload observed during the real client crash."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class NativeClientWeather(unittest.TestCase):
    def test_texture_hook_targets_the_actual_three_argument_renderer(self):
        body = (ROOT / "src/client/java/mielon/thesift/client/mixin/SiftWeatherTextureMixin.java").read_text(encoding="utf-8")
        descriptor = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V"
        self.assertIn('method = "' + descriptor + '"', body)
        self.assertNotIn('method = "render"', body)
        self.assertIn("textures/environment/ichor_rain.png", body)
        self.assertIn("textures/environment/ichor_snow.png", body)
        self.assertIn("TheSiftDimension.ICHOR_SNOWY_PEAKS", body)
        self.assertNotIn("require = 0", body)


if __name__ == "__main__":
    unittest.main()
