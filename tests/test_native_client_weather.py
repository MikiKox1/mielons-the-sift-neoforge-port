"""MC26.3 weather preparation must load textures before the active RenderPass."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class NativeClientWeather(unittest.TestCase):
    def test_texture_hook_targets_native_prepare_and_render_weather(self):
        body = (ROOT / "src/client/java/mielon/thesift/client/mixin/SiftWeatherTextureMixin.java").read_text(encoding="utf-8")
        descriptor = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V"
        self.assertNotIn(descriptor, body)
        self.assertIn('@Inject(method = "prepare", at = @At("HEAD"))', body)
        self.assertIn('@ModifyVariable(method = "renderWeather", at = @At("HEAD"), argsOnly = true, ordinal = 0)', body)
        self.assertIn("Vec3 cameraPos, WeatherRenderState renderState, CallbackInfo ci", body)
        render_body = body.split("private AbstractTexture theSift$useIchorWeatherTexture", 1)[1]
        self.assertNotIn("getTexture(", render_body)
        self.assertNotIn('method = "render"', body)
        self.assertIn("textures/environment/ichor_rain.png", body)
        self.assertIn("textures/environment/ichor_snow.png", body)
        self.assertIn("TheSiftDimension.ICHOR_SNOWY_PEAKS", body)
        self.assertNotIn("require = 0", body)


if __name__ == "__main__":
    unittest.main()
