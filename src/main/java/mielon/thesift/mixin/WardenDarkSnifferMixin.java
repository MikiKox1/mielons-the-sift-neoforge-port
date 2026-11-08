package mielon.thesift.mixin;

import java.util.Comparator;
import mielon.thesift.entity.DarkSnifferEntity;
import mielon.thesift.entity.ModAttachments;
import mielon.thesift.entity.SculkerFamily;
import mielon.thesift.entity.WardenPettingAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Warden.class)
public abstract class WardenDarkSnifferMixin implements WardenPettingAccess {
   @Unique
   private static final double THE_SIFT$SEARCH_RANGE = 18.0;
   @Unique
   private static final double THE_SIFT$PETTING_DISTANCE_SQUARED = 12.25;
   @Unique
   private static final int THE_SIFT$PETTING_DURATION = 100;
   @Unique
   private DarkSnifferEntity theSift$pettingTarget;
   @Unique
   private int theSift$pettingTicks;
   @Unique
   private int theSift$approachTicks;
   @Unique
   private int theSift$pettingSearchCooldown = -1;

   @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
   private void theSift$neverTargetSculkers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (SculkerFamily.contains(entity)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void theSift$tickDarkSnifferPetting(CallbackInfo ci) {
      Warden warden = (Warden)(Object)this;
      if (warden.level() instanceof ServerLevel serverLevel) {
         if (this.theSift$pettingTarget != null) {
            this.theSift$tickActivePetting(warden, serverLevel);
         } else {
            if (this.theSift$pettingSearchCooldown < 0) {
               this.theSift$pettingSearchCooldown = theSift$randomDelay(warden, 800, 1600);
            }

            if (this.theSift$pettingSearchCooldown-- <= 0 && theSift$isCompletelyCalm(warden)) {
               DarkSnifferEntity candidate = serverLevel.getEntitiesOfClass(
                     DarkSnifferEntity.class, warden.getBoundingBox().inflate(18.0), DarkSnifferEntity::isAvailableForWardenPetting
                  )
                  .stream()
                  .min(Comparator.comparingDouble(warden::distanceToSqr))
                  .orElse(null);
               if (candidate == null) {
                  this.theSift$pettingSearchCooldown = theSift$randomDelay(warden, 300, 300);
               } else {
                  this.theSift$pettingTarget = candidate;
                  this.theSift$approachTicks = 0;
                  this.theSift$pettingTicks = 0;
                  candidate.beginWardenPetting(warden, 260);
               }
            }
         }
      }
   }

   @Unique
   private void theSift$tickActivePetting(Warden warden, ServerLevel serverLevel) {
      DarkSnifferEntity target = this.theSift$pettingTarget;
      if (target != null && target.isAlive() && target.level() == warden.level() && !(warden.distanceToSqr(target) > 324.0) && theSift$isCompletelyCalm(warden)
         )
       {
         warden.getLookControl().setLookAt(target, 22.0F, 22.0F);
         target.beginWardenPetting(warden, 12);
         if (!(warden.distanceToSqr(target) > 12.25)) {
            warden.getNavigation().stop();
            this.theSift$setPettingFlag(warden, true);
            if (this.theSift$pettingTicks % 20 == 8) {
               serverLevel.sendParticles(
                  ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() + 0.35, target.getZ(), 1, 0.18, 0.08, 0.18, 0.0
               );
            }

            if (++this.theSift$pettingTicks >= 100) {
               this.theSift$stopPetting(warden, true);
            }
         } else if (++this.theSift$approachTicks > 240) {
            this.theSift$stopPetting(warden, false);
         } else {
            warden.getNavigation().moveTo(target, 0.55);
         }
      } else {
         this.theSift$stopPetting(warden, false);
      }
   }

   @Unique
   private static boolean theSift$isCompletelyCalm(Warden warden) {
      return warden.isAlive()
         && warden.getPose() == Pose.STANDING
         && warden.getAngerLevel() == AngerLevel.CALM
         && warden.getTarget() == null
         && warden.getEntityAngryAt().isEmpty()
         && warden.getVibrationData().getCurrentVibration() == null
         && !warden.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
         && !warden.getBrain().hasMemoryValue(MemoryModuleType.ROAR_TARGET)
         && !warden.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION)
         && !warden.getBrain().hasMemoryValue(MemoryModuleType.IS_SNIFFING)
         && !warden.getBrain().hasMemoryValue(MemoryModuleType.VIBRATION_COOLDOWN);
   }

   @Unique
   private void theSift$stopPetting(Warden warden, boolean completed) {
      if (this.theSift$pettingTarget != null) {
         this.theSift$pettingTarget.endWardenPetting(warden);
      }

      this.theSift$pettingTarget = null;
      this.theSift$pettingTicks = 0;
      this.theSift$approachTicks = 0;
      this.theSift$setPettingFlag(warden, false);
      this.theSift$pettingSearchCooldown = completed ? theSift$randomDelay(warden, 2400, 2400) : theSift$randomDelay(warden, 600, 600);
   }

   @Unique
   private void theSift$setPettingFlag(Warden warden, boolean petting) {
      boolean current = Boolean.TRUE.equals(warden.getExistingDataOrNull(ModAttachments.WARDEN_PETTING));
      if (current == petting) {
         return;
      }
      if (petting) {
         warden.setData(ModAttachments.WARDEN_PETTING, true);
      } else {
         warden.removeData(ModAttachments.WARDEN_PETTING);
      }
   }

   @Unique
   private static int theSift$randomDelay(Warden warden, int minimum, int variation) {
      return minimum + warden.getRandom().nextInt(variation + 1);
   }

   @Override
   public boolean theSift$isPettingDarkSniffer() {
      return Boolean.TRUE.equals(((Warden)(Object)this).getExistingDataOrNull(ModAttachments.WARDEN_PETTING));
   }
}
