package mielon.thesift.mixin;

import mielon.thesift.world.SiftLocateCommand;
import mielon.thesift.world.SiftPortalVariants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {
   @Inject(method = "locateStructure", at = @At("HEAD"), cancellable = true)
   private static void theSift$locateDynamicLandmark(CommandSourceStack source, Result<Structure> result, CallbackInfoReturnable<Integer> callback) {
      String id = result.asPrintable();
      String portalBiome = SiftPortalVariants.abandonedLocateBiome(id);
      if (portalBiome != null) {
         callback.setReturnValue(SiftLocateCommand.locateAbandonedPortal(source, portalBiome));
      } else {
         if ("the_sift:soulshale_shelter".equals(id)) {
            callback.setReturnValue(SiftLocateCommand.locateSoulshaleShelter(source));
         } else if ("the_sift:main_portal".equals(id)) {
            callback.setReturnValue(SiftLocateCommand.locateMainPortal(source));
         } else if ("the_sift:abandoned_main_portal".equals(id)) {
            callback.setReturnValue(SiftLocateCommand.locateAbandonedPortal(source));
         }
      }
   }
}
