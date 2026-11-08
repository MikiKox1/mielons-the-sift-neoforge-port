package mielon.thesift.mixin;

import mielon.thesift.fluid.IchorState;
import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public abstract class IchorBlockFluidStateMixin {
   @Shadow
   @Final
   @Mutable
   private int lightEmission;
   @Unique
   private volatile byte theSift$fluidKind;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void theSift$ichorLight(Block block, Property<?>[] propertyKeys, Comparable<?>[] propertyValues, CallbackInfo ci) {
      if (propertyKeys != null && propertyValues != null) {
         if (IchorWaterlogging.isIchorlogged((BlockState)(Object)this)) {
            this.lightEmission = Math.max(this.lightEmission, 12);
         }
      } else {
         this.theSift$fluidKind = 1;
      }
   }

   @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
   private void theSift$actualFluid(CallbackInfoReturnable<FluidState> cir) {
      if (IchorState.source != null) {
         byte kind = this.theSift$fluidKind;
         if (kind == 0) {
            kind = (byte)(IchorWaterlogging.isIchorlogged((BlockState)(Object)this) ? 2 : 1);
            this.theSift$fluidKind = kind;
         }

         if (kind == 2) {
            cir.setReturnValue(IchorState.source);
         }
      }
   }
}
