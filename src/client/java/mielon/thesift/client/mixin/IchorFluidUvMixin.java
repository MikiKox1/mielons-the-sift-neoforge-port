package mielon.thesift.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import mielon.thesift.client.render.IchorQuadUv;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidRenderer.Output;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public abstract class IchorFluidUvMixin {
   @Unique
   private static final ThreadLocal<IchorFluidUvMixin.QuadContext> THE_SIFT$QUAD = new ThreadLocal<>();

   @Inject(method = "tesselate", at = @At("HEAD"))
   private void theSift$begin(BlockAndTintGetter level, BlockPos pos, Output output, BlockState block, FluidState state, CallbackInfo ci) {
      if (state.getType() != ModFluids.ICHOR && state.getType() != ModFluids.FLOWING_ICHOR) {
         THE_SIFT$QUAD.remove();
      } else {
         THE_SIFT$QUAD.set(new IchorFluidUvMixin.QuadContext(pos));
      }
   }

   @Inject(method = "tesselate", at = @At("RETURN"))
   private void theSift$end(BlockAndTintGetter level, BlockPos pos, Output output, BlockState block, FluidState state, CallbackInfo ci) {
      THE_SIFT$QUAD.remove();
   }

   @Unique
   private static void theSift$capture(TextureAtlasSprite sprite) {
      IchorFluidUvMixin.QuadContext context = THE_SIFT$QUAD.get();
      if (context != null) {
         context.sprite = sprite;
      }
   }

   @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU0()F"))
   private float theSift$getU0(TextureAtlasSprite sprite) {
      theSift$capture(sprite);
      return sprite.getU0();
   }

   @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU1()F"))
   private float theSift$getU1(TextureAtlasSprite sprite) {
      theSift$capture(sprite);
      return sprite.getU1();
   }

   @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV0()F"))
   private float theSift$getV0(TextureAtlasSprite sprite) {
      theSift$capture(sprite);
      return sprite.getV0();
   }

   @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV1()F"))
   private float theSift$getV1(TextureAtlasSprite sprite) {
      theSift$capture(sprite);
      return sprite.getV1();
   }

   @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU(F)F"))
   private float theSift$getU(TextureAtlasSprite sprite, float value) {
      theSift$capture(sprite);
      return sprite.getU(value);
   }

   @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV(F)F"))
   private float theSift$getV(TextureAtlasSprite sprite, float value) {
      theSift$capture(sprite);
      return sprite.getV(value);
   }

   @Inject(method = "vertex", at = @At("HEAD"), cancellable = true)
   private void theSift$vertex(VertexConsumer output, float x, float y, float z, int color, float u, float v, int light, CallbackInfo ci) {
      IchorFluidUvMixin.QuadContext context = THE_SIFT$QUAD.get();
      if (context != null && context.sprite != null) {
         Identifier name = context.sprite.contents().name();
         if (name.getNamespace().equals("the_sift") && (name.getPath().equals("block/ichor_still") || name.getPath().equals("block/ichor_flow"))) {
            int i = context.count++;
            context.x[i] = x;
            context.y[i] = y;
            context.z[i] = z;
            context.lights[i] = light;
            context.uv.sectionVertex(i, x, y, z);
            context.colors[i] = color;
            if (context.count == 4) {
               context.emit(output);
               context.count = 0;
            }

            ci.cancel();
         }
      }
   }

   @Unique
   private static final class QuadContext {
      private final IchorQuadUv uv;
      private final float[] x = new float[4];
      private final float[] y = new float[4];
      private final float[] z = new float[4];
      private final int[] lights = new int[4];
      private final int[] colors = new int[4];
      private TextureAtlasSprite sprite;
      private int count;

      private QuadContext(BlockPos pos) {
         this.uv = new IchorQuadUv(pos.getX(), pos.getY(), pos.getZ());
      }

      private void emit(VertexConsumer output) {
         for (int j = 0; j < 4; j++) {
            float coordinate = this.uv.u(j);
            float u = this.sprite.getU(coordinate);
            coordinate = this.uv.v(j);
            float v = this.sprite.getV(coordinate);
            output.addVertex(this.x[j], this.y[j], this.z[j], this.colors[j], u, v, OverlayTexture.NO_OVERLAY, this.lights[j], 0.0F, 1.0F, 0.0F);
         }
      }
   }
}
