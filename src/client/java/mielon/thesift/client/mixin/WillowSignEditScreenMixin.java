package mielon.thesift.client.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignEditScreen.class)
public abstract class WillowSignEditScreenMixin {
   @Unique
   private WoodType theSift$willowSignType;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void theSift$willowBoard(SignBlockEntity sign, boolean frontText, boolean filtered, CallbackInfo ci) {
      if (sign.getBlockState().is(ModBlocks.OVERGROWN_WILLOW_SIGN) || sign.getBlockState().is(ModBlocks.OVERGROWN_WILLOW_WALL_SIGN)) {
         this.theSift$willowSignType = ModBlocks.OVERGROWN_WILLOW_WOOD_TYPE;
      }

      if (sign.getBlockState().is(ModBlocks.ASH_WILLOW_SIGN) || sign.getBlockState().is(ModBlocks.ASH_WILLOW_WALL_SIGN)) {
         this.theSift$willowSignType = ModBlocks.ASH_WILLOW_WOOD_TYPE;
      }
   }

   @ModifyArg(
      method = "extractSignBackground",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"
      ),
      index = 2
   )
   private WoodType theSift$renderWillowSign(WoodType vanilla) {
      return this.theSift$willowSignType != null ? this.theSift$willowSignType : vanilla;
   }
}
