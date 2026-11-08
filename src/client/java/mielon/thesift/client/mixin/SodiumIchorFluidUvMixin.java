package mielon.thesift.client.mixin;

import mielon.thesift.client.render.IchorQuadUv;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public abstract class SodiumIchorFluidUvMixin {
   @Inject(method = "updateQuad", at = @At("TAIL"), require = 0, remap = false)
   private void theSift$map(
      @Coerce Object quad,
      @Coerce Object level,
      BlockPos pos,
      @Coerce Object lighter,
      Direction dir,
      @Coerce Object facing,
      float brightness,
      @Coerce Object provider,
      FluidState state,
      CallbackInfo ci
   ) {
      if (state.getType() == ModFluids.ICHOR || state.getType() == ModFluids.FLOWING_ICHOR) {
         SodiumIchorQuadAccessor view = (SodiumIchorQuadAccessor)quad;
         TextureAtlasSprite sprite = view.theSift$getSprite();
         IchorQuadUv uv = new IchorQuadUv(pos.getX(), pos.getY(), pos.getZ());

         for (int i = 0; i < 4; i++) {
            uv.blockVertex(i, view.theSift$getX(i), view.theSift$getY(i), view.theSift$getZ(i));
         }

         for (int i = 0; i < 4; i++) {
            float coordinate = uv.u(i);
            view.theSift$setTexU(i, sprite.getU(coordinate));
            coordinate = uv.v(i);
            view.theSift$setTexV(i, sprite.getV(coordinate));
         }
      }
   }
}
