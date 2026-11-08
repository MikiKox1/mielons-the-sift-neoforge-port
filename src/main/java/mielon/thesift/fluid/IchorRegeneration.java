package mielon.thesift.fluid;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.enchantment.SoulAbsorption;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

public final class IchorRegeneration {
   public static final ResourceKey<DamageType> SOUL_DRAIN = ResourceKey.create(
      Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("the_sift", "ichor_soul_drain")
   );
   public static final TagKey<EntityType<?>> ICHOR_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("the_sift", "ichor_immune"));
   private static final int DURATION = 101;
   private static final int BOTTLE_DURATION = 100;
   private MobEffectInstance bottleEffect;
   private MobEffectInstance contactEffect;
   private boolean wasTouching;
   private boolean rainRegeneration;
   private int damageTicks = -1;

   public static int absorptionLevel(LivingEntity entity) {
      return Math.max(SoulAbsorption.level(entity), entity.getType().builtInRegistryHolder().is(ICHOR_IMMUNE) ? 1 : 0);
   }

   public void tick(LivingEntity entity) {
      if (entity.level() instanceof ServerLevel level) {
         if (this.damageTicks >= 0) {
            this.damageTicks = Math.min(10, this.damageTicks + 1);
         }

         int absorption = absorptionLevel(entity);
         if (absorption > 0) {
            this.damageTicks = -1;
         }

         MobEffectInstance current = entity.getEffect(MobEffects.REGENERATION);
         if (SoulAbsorption.level(entity) < 2 && isContactEffect(current)) {
            entity.removeEffect(MobEffects.REGENERATION);
            current = null;
            this.wasTouching = false;
         }

         boolean touching = isTouching(entity);
         boolean rain = !touching && isExposedToRain(entity);
         this.contactEffect = isContactEffect(current) ? current : null;
         if (this.rainRegeneration && (current == null || current != this.contactEffect || current.getDuration() > 101)) {
            this.rainRegeneration = false;
         }

         if (this.rainRegeneration && !rain) {
            if (!touching && current == this.contactEffect) {
               entity.removeEffect(MobEffects.REGENERATION);
               current = null;
               this.contactEffect = null;
            }

            this.rainRegeneration = false;
         }

         if (this.wasTouching && !touching && current != null && current == this.contactEffect) {
            refresh(entity);
            current = entity.getEffect(MobEffects.REGENERATION);
            this.contactEffect = current;
         }

         this.wasTouching = false;
         if (this.bottleEffect != null) {
            if (current != this.bottleEffect || absorption < 2) {
               this.bottleEffect = null;
            } else if (!touching && !rain && entity.isAlive() && (entity.tickCount + entity.getId() & 3) == 0) {
               bodySouls(level, entity, true, 1);
            }
         }

         if (touching || rain) {
            if (absorption < 2) {
               if (current != null && current == this.contactEffect) {
                  entity.removeEffect(MobEffects.REGENERATION);
               }

               this.contactEffect = null;
               this.rainRegeneration = false;
               this.wasTouching = false;
               if (absorption == 0 && this.damageTicks < 0) {
                  this.damageTicks = 1;
               }

               if (absorption == 0 && this.damageTicks >= 10) {
                  this.damageTicks = 0;
                  float damage = contactDamage(level);
                  if (rain) {
                     damage *= 0.5F;
                  }

                  DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SOUL_DRAIN));
                  entity.hurtServer(level, source, damage);
               } else if (absorption > 0) {
                  this.damageTicks = -1;
               }
            } else {
               this.damageTicks = -1;
               this.wasTouching = touching;
               if (current == null) {
                  refresh(entity);
                  this.contactEffect = entity.getEffect(MobEffects.REGENERATION);
                  this.rainRegeneration = rain;
               } else if (current == this.contactEffect && current.getDuration() <= 51 && (touching || this.rainRegeneration)) {
                  refresh(entity);
               }
            }
         }
      }
   }

   private static float contactDamage(ServerLevel level) {
      return switch (level.getDifficulty()) {
         case PEACEFUL -> 0.5F;
         case EASY -> 1.0F;
         case NORMAL -> 1.5F;
         case HARD -> 2.0F;
      };
   }

   public static void hitBySnowball(LivingEntity entity) {
      if (entity.level() instanceof ServerLevel level && !entity.getType().builtInRegistryHolder().is(ICHOR_IMMUNE)) {
         int absorption = SoulAbsorption.level(entity);
         if (absorption == 0) {
            DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SOUL_DRAIN));
            entity.hurtServer(level, source, 1.0F);
            bodySouls(level, entity, false, 4);
         } else if (absorption >= 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
            bodySouls(level, entity, true, 4);
            awardAbsorption(entity);
         }
      }
   }

   private static void bodySouls(ServerLevel level, LivingEntity entity, boolean absorbed, int count) {
      RandomSource random = entity.getRandom();

      for (int i = 0; i < count; i++) {
         level.sendParticles(
            ModParticles.ICHOR_SOUL_BODY,
            entity.getX() + (random.nextDouble() - 0.5) * entity.getBbWidth(),
            entity.getY() + entity.getBbHeight() * (0.2 + random.nextDouble() * 0.65),
            entity.getZ() + (random.nextDouble() - 0.5) * entity.getBbWidth(),
            0,
            entity.getId(),
            absorbed ? 1.0 : 0.0,
            0.0,
            1.0
         );
      }
   }

   public static void drink(LivingEntity entity) {
      if (entity.level() instanceof ServerLevel level) {
         int absorption = SoulAbsorption.level(entity);
         if (absorption == 0) {
            float before = entity.getHealth();
            MobEffects.INSTANT_DAMAGE.value().applyInstantenousEffect(level, null, null, entity, 0, 1.0);
            if (entity.getHealth() < before) {
               bodySouls(level, entity, false, 8);
            }
         } else if (absorption >= 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
            MobEffectInstance effect = entity.getEffect(MobEffects.REGENERATION);
            if (entity instanceof IchorRegeneration.Carrier carrier && isContactEffect(effect)) {
               carrier.theSift$ichorState().bottleEffect = effect;
            }

            awardAbsorption(entity);
         }
      }
   }

   private static boolean isContactEffect(MobEffectInstance effect) {
      return effect != null
         && effect.getAmplifier() == 0
         && effect.isAmbient()
         && !effect.isVisible()
         && effect.showIcon()
         && effect.getDuration() > 0
         && effect.getDuration() <= Math.max(101, 100);
   }

   private static void awardAbsorption(LivingEntity entity) {
      if (entity instanceof ServerPlayer player && SoulAbsorption.level(entity) >= 2 && entity.hasEffect(MobEffects.REGENERATION)) {
         ModAdvancements.award(player, "the_sift/soul_absorption");
      }
   }

   private static void refresh(LivingEntity entity) {
      entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 101, 0, true, false, true));
      awardAbsorption(entity);
   }

   public static boolean isExposedToRain(LivingEntity entity) {
      if (entity instanceof Player player && !player.isSpectator() && entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
         Level level = entity.level();
         if (level.dimension().equals(TheSiftDimension.LEVEL_KEY) && level.isRaining()) {
            BlockPos head = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
            if (level.hasChunkAt(head) && level.canSeeSky(head) && level.getHeight(Types.MOTION_BLOCKING, head.getX(), head.getZ()) <= head.getY()) {
               Holder<Biome> biome = level.getBiome(head);
               return biome.is(TheSiftDimension.ICHOR_SNOWY_PEAKS) || biome.value().hasPrecipitation();
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isTouching(LivingEntity entity) {
      Level level = entity.level();
      AABB bounds = entity.getBoundingBox().deflate(0.001);

      for (BlockPos pos : BlockPos.betweenClosed(
         BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)
      )) {
         if (level.hasChunkAt(pos)) {
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (fluid.getType() instanceof IchorFluid && bounds.minY < pos.getY() + fluid.getHeight(level, pos)) {
               return true;
            }

            if (state.is(ModBlocks.ICHOR_CAULDRON)) {
               double height = (6.0 + 3.0 * state.getValue(LayeredCauldronBlock.LEVEL).intValue()) / 16.0;
               if (bounds.intersects(pos.getX() + 0.125, pos.getY() + 0.25, pos.getZ() + 0.125, pos.getX() + 0.875, pos.getY() + height, pos.getZ() + 0.875)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public interface Carrier {
      IchorRegeneration theSift$ichorState();
   }
}
