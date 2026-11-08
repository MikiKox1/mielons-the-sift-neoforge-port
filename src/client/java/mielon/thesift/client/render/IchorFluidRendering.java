package mielon.thesift.client.render;

import mielon.thesift.fluid.ModFluids;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel.Unbaked;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

public final class IchorFluidRendering {
   private IchorFluidRendering() {
   }

   public static void register(RegisterFluidModelsEvent event) {
      Material still = new Material(Identifier.fromNamespaceAndPath("the_sift", "block/ichor_still"), true);
      Material flowing = new Material(Identifier.fromNamespaceAndPath("the_sift", "block/ichor_flow"), true);
      Material overlay = flowing;
      Unbaked model = new Unbaked(still, flowing, overlay, new IchorFluidRendering.IchorTint());
      event.register(model, ModFluids.ICHOR, ModFluids.FLOWING_ICHOR);
   }

   private static final class IchorTint implements FluidTintSource {
      @Override
      public int color(FluidState state) {
         return -1;
      }

      @Override
      public int colorInWorld(FluidState fluidState, BlockState state, BlockAndTintGetter level, BlockPos pos) {
         return -1;
      }
   }
}
