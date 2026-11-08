package mielon.thesift.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementProgress.class)
public abstract class SeedlingAdvancementMigrationMixin {
   @Shadow
   @Final
   private Map<String, CriterionProgress> criteria;

   @Inject(method = "update", at = @At("HEAD"), require = 1)
   private void theSift$preserveSeedlingCriterion(AdvancementRequirements requirements, CallbackInfo ci) {
      for (String previous : List.copyOf(this.criteria.keySet())) {
         String target = previous.replace("grimstone", "soulshale");
         if (!previous.equals(target) && requirements.names().contains(target)) {
            CriterionProgress legacyProgress = this.criteria.remove(previous);
            CriterionProgress canonical = this.criteria.get(target);
            if (canonical == null || !canonical.isDone() && legacyProgress.isDone()) {
               this.criteria.put(target, legacyProgress);
            }
         }
      }

      if (requirements.names().contains("the_sift:seedling")) {
         CriterionProgress legacy = this.criteria.remove("the_sift:sifter");
         if (legacy != null) {
            CriterionProgress current = this.criteria.get("the_sift:seedling");
            if (current == null || !current.isDone() && legacy.isDone()) {
               this.criteria.put("the_sift:seedling", legacy);
            }
         }
      }
   }
}
