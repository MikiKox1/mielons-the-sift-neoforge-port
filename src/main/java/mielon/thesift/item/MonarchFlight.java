package mielon.thesift.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class MonarchFlight {
   public static final int COOLDOWN_TICKS = 200;
   public static final double MAX_RISE = 18.0;
   public static final double LIFT_PER_TICK = 0.65;
   public static final double DECELERATION = 0.065;
   private boolean held;
   private boolean pressed;
   private boolean wasGliding;
   private boolean active;
   private boolean coasting;
   private double startY;
   private double liftSpeed;
   private Object startLevel;
   private int cooldownDuration;
   private int pendingCooldown;

   public void setHeld(boolean value) {
      if (value && !this.held) {
         this.pressed = true;
      }

      if (!value) {
         this.pressed = false;
      }

      this.held = value;
   }

   public void updateInput(Player player, boolean value) {
      this.setHeld(value);
      if (!value && this.active && this.eligible(player) && player.level() == this.startLevel) {
         this.beginCoast(player);
      }
   }

   public boolean active() {
      return this.active;
   }

   public boolean coasting() {
      return this.coasting;
   }

   public double ceiling() {
      return this.startY + 18.0;
   }

   private boolean eligible(Player player) {
      ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
      return player.isAlive()
         && !player.isSpectator()
         && !player.getAbilities().flying
         && player.isFallFlying()
         && chest.is(ModItems.MONARCH_ELYTRA)
         && chest.getDamageValue() < chest.getMaxDamage() - 1;
   }

   public void tick(Player player) {
      if (this.pendingCooldown > 0 && player instanceof ServerPlayer) {
         this.startCooldown(player, this.pendingCooldown);
         this.pendingCooldown = 0;
      }

      boolean eligible = this.eligible(player);
      boolean newPress = this.pressed;
      this.pressed = false;
      if ((this.active || this.coasting) && (!eligible || player.level() != this.startLevel)) {
         if (this.active) {
            this.startCooldown(player);
         }

         this.active = this.coasting = false;
         this.liftSpeed = 0.0;
      }

      if (this.active && !this.held) {
         this.beginCoast(player);
      }

      if (newPress
         && this.held
         && eligible
         && this.wasGliding
         && !this.active
         && !this.coasting
         && !player.getCooldowns().isOnCooldown(new ItemStack(ModItems.MONARCH_ELYTRA))) {
         this.active = true;
         this.startY = player.getY();
         this.startLevel = player.level();
         this.liftSpeed = 0.65;
      }

      if (this.active && player instanceof ServerPlayer && this.ceiling() - player.getY() <= 0.125) {
         this.beginCoast(player);
      }

      if (this.active) {
         double remaining = Math.max(0.0, this.ceiling() - player.getY());
         if (remaining <= 0.001) {
            this.active = false;
            this.liftSpeed = 0.0;
            this.startCooldown(player);
            applySpeed(player, 0.0);
            this.clampServerHeight(player);
         } else {
            this.liftSpeed = speedForDistance(remaining);
            applySpeed(player, Math.min(this.liftSpeed, remaining));
         }
      } else if (this.coasting) {
         double remaining = Math.max(0.0, this.ceiling() - player.getY());
         this.liftSpeed = Math.min(Math.max(0.0, this.liftSpeed - 0.065), speedForDistance(remaining));
         applySpeed(player, Math.min(this.liftSpeed, remaining));
         if (this.liftSpeed <= 0.001 || remaining <= 0.001) {
            this.coasting = false;
            this.clampServerHeight(player);
         }
      }

      this.wasGliding = player.isFallFlying();
   }

   private static double speedForDistance(double remaining) {
      return Math.min(0.65, Math.sqrt(0.13 * remaining + 0.0010562500000000001) - 0.0325);
   }

   public Vec3 movement(Player player, Vec3 vanilla) {
      return (this.active || this.coasting) && this.eligible(player) && player.level() == this.startLevel
         ? new Vec3(vanilla.x, Math.min(this.liftSpeed, Math.max(0.0, this.ceiling() - player.getY())), vanilla.z)
         : vanilla;
   }

   private void beginCoast(Player player) {
      this.active = false;
      this.coasting = this.liftSpeed > 0.001;
      this.startCooldown(player);
   }

   public int cooldownToSave(Player player) {
      if (this.active) {
         return 200;
      }

      int remaining = Math.round(player.getCooldowns().getCooldownPercent(new ItemStack(ModItems.MONARCH_ELYTRA), 0.0F) * this.cooldownDuration);
      return Math.max(this.pendingCooldown, remaining);
   }

   public void loadCooldown(int ticks) {
      this.pendingCooldown = Mth.clamp(ticks, 0, 200);
   }

   private void startCooldown(Player player) {
      this.startCooldown(player, 200);
   }

   private void startCooldown(Player player, int ticks) {
      this.cooldownDuration = ticks;
      player.getCooldowns().addCooldown(new ItemStack(ModItems.MONARCH_ELYTRA), ticks);
   }

   private static void applySpeed(Player player, double speed) {
      Vec3 velocity = player.getDeltaMovement();
      player.setDeltaMovement(velocity.x, speed, velocity.z);
      if (player instanceof ServerPlayer) {
         player.hurtMarked = true;
      }
   }

   private void clampServerHeight(Player player) {
      if (player instanceof ServerPlayer serverPlayer && player.level() == this.startLevel && player.getY() > this.ceiling() + 0.001) {
         serverPlayer.teleportTo(player.getX(), this.ceiling(), player.getZ());
      }
   }
}
