package mielon.thesift.mixin;

import mielon.thesift.fluid.IchorFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class IchorFishingHookMixin extends Projectile {
   @Shadow
   @Final
   private static EntityDataAccessor<Boolean> DATA_BITING;
   @Shadow
   private boolean biting;
   @Shadow
   private boolean openWater;
   @Shadow
   private int nibble;
   @Shadow
   private int timeUntilLured;
   @Shadow
   private int timeUntilHooked;
   @Shadow
   private int life;
   @Shadow
   private Entity hookedIn;

   @Shadow
   public abstract Player getPlayerOwner();

   @Shadow
   private boolean shouldStopFishing(Player player) {
      throw new AssertionError();
   }

   protected IchorFishingHookMixin(EntityType<? extends Projectile> type, Level level) {
      super(type, level);
   }

   @Unique
   private boolean theSift$inIchor() {
      BlockPos pos = this.blockPosition();
      FluidState fluid = this.level().getFluidState(pos);
      return fluid.getType() instanceof IchorFluid && this.getY() <= pos.getY() + fluid.getHeight(this.level(), pos) + 0.05;
   }

   @Unique
   private void theSift$cancelBite() {
      this.biting = false;
      this.openWater = false;
      this.nibble = 0;
      this.timeUntilLured = 0;
      this.timeUntilHooked = 0;
      this.getEntityData().set(DATA_BITING, false);
   }

   @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
   private void theSift$sinkWithoutFishing(CallbackInfo ci) {
      if (this.hookedIn == null && this.theSift$inIchor()) {
         this.theSift$cancelBite();
         super.tick();
         Player owner = this.getPlayerOwner();
         if (owner == null) {
            this.discard();
            ci.cancel();
         } else if (!this.level().isClientSide() && this.shouldStopFishing(owner)) {
            ci.cancel();
         } else {
            if (this.onGround()) {
               if (++this.life >= 1200) {
                  this.discard();
               }
            } else {
               this.life = 0;
            }

            if (!this.isRemoved()) {
               Vec3 old = this.getDeltaMovement();
               Vec3 velocity = new Vec3(old.x * 0.8, Math.max(-0.035, Math.min(0.0, old.y * 0.8 - 0.005)), old.z * 0.8);
               this.setDeltaMovement(velocity);
               this.move(MoverType.SELF, velocity);
               this.updateRotation();
            }

            ci.cancel();
         }
      }
   }

   @Inject(method = "retrieve", at = @At("HEAD"))
   private void theSift$noCatchInIchor(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
      if (this.theSift$inIchor()) {
         this.theSift$cancelBite();
      }
   }
}
