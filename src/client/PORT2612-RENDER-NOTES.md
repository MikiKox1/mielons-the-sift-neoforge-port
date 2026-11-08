# The Sift 2.0.1 — native client/render port for Minecraft 26.1.2

## Source and API boundary

- Working branch: `port/26.1.2-2.0.1-alpha1`, inherited accepted MC26.2 ALPHA5 commit `22176ab7e71ad9a5c2c624cfc72015702bb98f22`.
- Target: Minecraft 26.1.2, NeoForge 26.1.2.126, Java 25, GeckoLib 5.5.2. Supported minimum remains a separate test gate at NeoForge 26.1.2.100.
- API evidence: **unchanged** `../neo-26.1.2-1.1.2.1-release/build/moddev/artifacts/minecraft-patched-26.1.2.126-sources.jar` and its compiled target JAR. The older accepted 1.1.2.1 client was consulted for signatures only. Exact 2.0.1 gameplay and the 26.2 ALPHA5 community fixes were retained.
- The official NeoForge 1.21.1 2.0.1 and decompiled Fabric 26.2 are behavioral references; neither supplies Minecraft 26.1.2 method descriptors.

## Java changes and native fidelity

The original **eight** `SiftProceduralSkyRenderer.BUFFERS` remain, each with `86,016` `POSITION_COLOR` vertices. Layers 0–5 retain the original daily/night ribbons; layer 6 is the original thrive overlay and layer 7 remains available in the restoration pass. The original `SiftTides` calculations, colors, and draw order remain intact. **No six-layer 1.x renderer or baked sky textures were imported.**

The 26.1.2 GPU renderer uses `VertexFormat.Mode.QUADS`, `AutoStorageIndexBuffer.getBuffer(int)`, `RenderPass.setVertexBuffer(int, GpuBuffer)`, `drawIndexed(firstIndex, baseVertex, count, instances)`, `OptionalInt.empty()` for color clear and `DynamicUniforms.writeTransform(modelView, color, offset, textureMatrix)`. The sky's GPU vertex/index buffers are created before its own `RenderPass` opens. `SkyRenderer` has no `renderTarget` field in 26.1.2, so the mod uses `Minecraft.getMainRenderTarget()` and keeps the four-direction thrive sunrise.

The Rift and Sonorous pipelines now use the **26.1.2** `RenderPipelines.MATRICES_PROJECTION_SNIPPET`/`GLOBALS_SNIPPET`, `withSampler` and `withVertexFormat(format, VertexFormat.Mode.QUADS)`. The Rift's **eight variants for each destination**, glow, animated shader, and the two depth modes of the Sonorous beam remain. The portal's depth mask uses `ColorTargetState.WRITE_NONE` and depth writes ON. All custom pipelines remain registered through NeoForge's `RegisterRenderPipelinesEvent`.

The client transition, intro readiness, and Monarch flight input retain their full 2.0 logic while using `Minecraft.screen` / `setScreen` rather than the later `Minecraft.gui.screen()` API. Native HUD mixins target `net.minecraft.client.gui.Gui`, where the target bytecode retains `extractAirBubbles` and `extractCrosshair`; original Ichor and Monarch sprites and behavior remain. The native camera getter is `GameRenderer.getMainCamera()`.

The 26.1.2 `LevelRenderer` entrypoint is `renderLevel(GraphicsResourceAllocator, DeltaTracker, boolean, CameraRenderState, Matrix4fc, GpuBufferSlice, Vector4f, boolean, ChunkSectionsToRender)`. Unlike MC26.2, there is no method named `render` in that class. The `SiftShaderSkyMixin` restoration callback therefore targets `renderLevel` and includes the final `ChunkSectionsToRender` parameter. This descriptor was verified against the exact target class bytecode; an incorrect callback is a required client mixin startup failure even if shaderpack testing is deferred.

For rain and snow, `WeatherEffectRenderer.getPrecipitationAt(Level, BlockPos)` is intercepted with the MixinExtras `@ModifyReturnValue` hook present on the **26.1.2** target. The separate texture `@ModifyArg` targets the full **three-argument** NeoForge `render(Vec3, WeatherRenderState, LevelRenderState)` descriptor, which contains both texture lookups. The two-argument compatibility shim only delegates. The special `the_sift:ichor_snowy_peaks` snowfall, other Sift rain, and vanilla Overworld remain distinct.

## Shader file integrity manifest

Manifest scope is `src/main/resources/assets/the_sift/shaders/`; no shader files are present under `src/client/resources/assets/the_sift/shaders/`. SHA-256 values below are **expected post-port hashes** of the exact files on disk. All are unchanged from accepted ALPHA5 **except** `core/restore_tide_sky.vsh`. The source already used the 26.1.2-compatible `#moj_import` preprocessor, `gl_VertexID`, and non-explicit GLSL vertex attribute locations, so no blanket shader rewrite was needed.

| Shader path | Expected SHA-256 | Adaptation |
| --- | --- | --- |
| `core/rendertype_ichor_clouds.fsh` | `c1374569d1ac54eae51b6e923477d65a54251488099f54c5a42f47eb91824469` | Unchanged animated Ichor cloud fog |
| `core/rendertype_ichor_clouds.vsh` | `8e17eae576dd7cdc23da2ff739ea34340c883f9cc0e6f8d0f4a6b0b0deac3594` | Unchanged native cloud faces, uses `gl_VertexID` |
| `core/restore_tide_sky.fsh` | `b637d85d8b0ba4dfb36ca9717d181a657b0ae9e5f8443fa5b20f32d1ba410438` | Unchanged sky color fragment |
| `core/restore_tide_sky.vsh` | `25f99dc2cae59b89c760b7edcdefde25ff40e80ac523a285c2bdb5923a17475b` | **Changed** forward-Z depth placement, see below |
| `core/rift.fsh` | `925c97cb33f95f804942c95e1c98ca55548eeaffe4581c8974c5468e027cb133` | Unchanged new 2.0 Rift noise, bloom and destination sampling |
| `core/rift.vsh` | `9bb94c1cf3768ed62bdc1f7b427e45709947e94af14729a44676d2bee2a29699` | Unchanged `POSITION_TEX_COLOR` bindings |
| `core/sift_loading.fsh` | `c892ce72d5afe14192c72b869d3d75123c0a111cf520eadae3169441229ce562` | Unchanged moving Rift/loading background |
| `core/sift_portal.fsh` | `e6544ac7dd93c0ae42880464b301a03468fbeb438da10b2a8199177d0034f9f3` | Unchanged procedural portal field and fog |
| `core/sift_portal.vsh` | `554fc8bf911d2ce3847b937b7f2cb0b2bd2ece19a4fd160c0f0f8f24e1a6c9b0` | Unchanged projection and fog varyings |
| `core/sonorous_beam.fsh` | `587ac67cff4c036b8d844a2af9e2906740fbbc2afd06f55433b747a383cc91c2` | Unchanged beam texture/color |
| `core/sonorous_beam.vsh` | `4a808d5c920a90ba7a794af072ce73ea175553ee1670865a19b56e830c6e32b6` | Unchanged block vertex layout |
| `include/portal_fields.glsl` | `c7afc17a0ce583dafdeed5f7496d3aa3c62f0ed8e38349a5cbe5799576f3d05c` | Unchanged shared portal/Rift procedural noise |

The intentional shader delta is only:

- `core/restore_tide_sky.vsh` **before** `12703d676aabbf31009575b1329b362bf67de7edb74fb4960655cee3f032b63c`, **after** `25f99dc2cae59b89c760b7edcdefde25ff40e80ac523a285c2bdb5923a17475b`.
- The previous generic reversed-Z expression `ProjMat[2][2] > 0.5 ? -gl_Position.w : 0.0` places pixels at a mid-depth in MC26.1.2. The target has **forward-Z with far clear depth 1**, so the vertex shader now writes `gl_Position.z = gl_Position.w` and the restore pipeline uses `CompareOp.EQUAL`, depth writes OFF. This confines the restoration to the background. Visual approval with shaderpacks remains pending.

## Static checks and runtime boundaries

- Verify shader hashes with `Get-FileHash -Algorithm SHA256`. All 12 shader files should resolve, and none should contain `#include`, `gl_VertexIndex` or explicit vertex `layout(location = ...)` introduced for RenderPearl.
- Confirm all eight sky buffers and the 2.0.1 `SiftTides` API still exist in the client Java; inspect the compiled JAR for the final shader resources and NeoForge-specific mixin descriptors.
- Prime owns compilation and disposable client/server tests. The prime-reported full Java COMPILE2 was green **before** the subsequent `renderLevel` mixin signature correction, which still needs its own compile/runtime confirmation. At source review time there is **no in-world shader or native visual PASS**. Focused manual checks: eight-layer Sift sky day/night/thrive and reload; both kinds of Rift including eight destination textures and edge culling; portal depth; Sonorous beam in/out of Ichor; Ichor rain vs snowy peaks, Overworld unchanged; Monarch HUD/bossbar/flight; Singer and Monarch emissive; item trims, map colors and transition screens.
