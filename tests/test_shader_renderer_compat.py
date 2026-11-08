from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/'src/client/java/mielon/thesift/client'
class ShaderRendererCompat(unittest.TestCase):
 def test_photon_beam_uses_supported_entity_path_without_native_changes(self):
  types=(ROOT/'render/SonorousBeamRenderTypes.java').read_text(encoding='utf8')
  render=(ROOT/'render/SonorousBeamRenderer.java').read_text(encoding='utf8')
  self.assertIn('RenderTypes.entityTranslucent(BEAM_TEXTURE)',types)
  self.assertIn('SiftShaderCompat.isShaderPackInUse() && SiftShaderCompat.isPhotonShaderPackSelected()',types)
  self.assertIn('? PHOTON_BEAM : BEAM',types)
  self.assertIn('SonorousBeamRenderTypes.beam()',render)
  self.assertIn('.setOverlay(655360)',render)
  self.assertIn('.setColor(red, green, blue, 0.55F)',render)
  self.assertIn('SonorousBeamRenderTypes.WATER_OVERLAY',render)
 def test_local_portal_depth_pipeline_is_mapped_before_rendering(self):
  types=(ROOT/'render/SiftPortalRenderTypes.java').read_text(encoding='utf8')
  client=(ROOT/'TheSiftClient.java').read_text(encoding='utf8')
  self.assertIn('SiftShaderCompat.assignPipeline(DEPTH_PIPELINE, "BEACON_BEAM")',types)
  self.assertIn('SiftPortalRenderTypes.initialize()',client)
  self.assertNotIn('geckolib',types.lower())
if __name__=='__main__':unittest.main()
