package mielon.thesift.entity;

import mielon.thesift.item.ModItems;
import mielon.thesift.particle.ModParticles;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class SifterFamily {
   private SifterFamily() {
   }

   public static boolean isMember(Entity entity) {
      return entity instanceof SeedlingEntity || entity instanceof NesterEntity || entity instanceof PollinatorEntity;
   }

   public static boolean isDisguised(LivingEntity entity) {
      return entity instanceof Player && entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SIFTER_CAP);
   }

   public static boolean isDroolDamage(DamageSource source) {
      return source.getDirectEntity() instanceof AreaEffectCloud cloud && cloud.getParticle().getType() == ModParticles.POLLINATOR_DROOL;
   }
}
