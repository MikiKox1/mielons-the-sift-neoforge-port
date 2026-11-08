package mielon.thesift.client.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignEditScreen.class)
public abstract class WillowSignEditScreenMixin {
   @Shadow
   @Final
   @Mutable
   private Identifier texture;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void theSift$willowBoard(SignBlockEntity sign, boolean frontText, boolean filtered, CallbackInfo ci) {
      if (sign.getBlockState().is(ModBlocks.OVERGROWN_WILLOW_SIGN) || sign.getBlockState().is(ModBlocks.OVERGROWN_WILLOW_WALL_SIGN)) {
         this.texture = Identifier.fromNamespaceAndPath("the_sift", "textures/gui/signs/overgrown_willow.png");
      }

      if (sign.getBlockState().is(ModBlocks.ASH_WILLOW_SIGN) || sign.getBlockState().is(ModBlocks.ASH_WILLOW_WALL_SIGN)) {
         this.texture = Identifier.fromNamespaceAndPath("the_sift", "textures/gui/signs/ash_willow.png");
      }
   }
}
