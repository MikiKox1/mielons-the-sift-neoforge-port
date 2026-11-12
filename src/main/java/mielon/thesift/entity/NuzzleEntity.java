package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

public final class NuzzleEntity extends Sheep implements GeoEntity {
   public static final float WIDTH = 0.9F;
   public static final float HEIGHT = 1.25F;
   public static final float EYE_HEIGHT = 1.05F;
   public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("the_sift", "nuzzle_food"));
   private static final TagKey<Block> GRAZING = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("the_sift", "nuzzle_grazing"));
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public NuzzleEntity(EntityType<? extends NuzzleEntity> type, Level level) {
      super(type, level);
      this.setColor(DyeColor.RED);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      if (input.read("Color", DyeColor.LEGACY_ID_CODEC).isEmpty()) {
         this.setColor(DyeColor.RED);
      }
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.removeAllGoals(goal -> goal instanceof TemptGoal);
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
      this.goalSelector.addGoal(5, new NuzzleEntity.SiftGrazeGoal());
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      DyeColor color = stack.get(DataComponents.DYE);
      if (color != null && this.isAlive() && !this.isSheared() && this.getColor() != color) {
         this.level().playSound(player, this, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
         if (!this.level().isClientSide()) {
            this.setColor(color);
            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }
         }
         return InteractionResult.SUCCESS;
      }
      return super.mobInteract(player, hand);
   }

   @Override
   public boolean isFood(ItemStack stack) {
      return stack.is(FOOD);
   }

   public static boolean checkNuzzleSpawnRules(
      EntityType<NuzzleEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      return SlabberEntity.checkSlabberSpawnRules(ModEntities.SLABBER, level, reason, pos, random);
   }

   public static DyeColor spawnColor(Holder<Biome> biome, RandomSource random) {
      int roll = random.nextInt(10);
      if (TheSiftDimension.isOvergrownBiome(biome)) {
         return roll == 0 ? DyeColor.CYAN : (roll == 1 ? DyeColor.GREEN : DyeColor.RED);
      } else if (TheSiftDimension.isCoralBiome(biome)) {
         return roll == 0 ? DyeColor.PINK : (roll == 1 ? DyeColor.MAGENTA : DyeColor.RED);
      } else if (biome.is(TheSiftDimension.ORGANIC_DESERT)) {
         return roll == 0 ? DyeColor.YELLOW : (roll == 1 ? DyeColor.WHITE : DyeColor.RED);
      } else {
         return DyeColor.RED;
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData group) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, group);
      this.setColor(spawnColor(level.getBiome(this.blockPosition()), this.random));
      return result;
   }

   public NuzzleEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
      Sheep inherited = super.getBreedOffspring(level, partner);
      NuzzleEntity child = ModEntities.NUZZLE.create(level, EntitySpawnReason.BREEDING);
      if (child != null && inherited != null) {
         child.setColor(inherited.getColor());
      }

      return child;
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose pose) {
      return this.getType().getDimensions().scale(this.isBaby() ? 0.5F : 1.0F);
   }

   @Override
   public SoundEvent getAmbientSound() {
      return ModSounds.NUZZLE_AMBIENT;
   }

   @Override
   public SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.NUZZLE_HURT;
   }

   @Override
   public SoundEvent getDeathSound() {
      return ModSounds.NUZZLE_DEATH;
   }

   @Override
   public void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.NUZZLE_STEP, 0.25F, 1.0F);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("movement", 0, state -> {
         state.controller().setAnimation(IDLE);
         return PlayState.CONTINUE;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private final class SiftGrazeGoal extends Goal {
      private int remaining;

      SiftGrazeGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
      }

      @Override
      public boolean canUse() {
         return NuzzleEntity.this.random.nextInt(NuzzleEntity.this.isBaby() ? 50 : 1000) == 0 && this.canGraze();
      }

      private boolean canGraze() {
         return NuzzleEntity.this.level().getBlockState(NuzzleEntity.this.blockPosition().below()).is(NuzzleEntity.GRAZING);
      }

      @Override
      public void start() {
         this.remaining = 40;
         NuzzleEntity.this.getNavigation().stop();
         NuzzleEntity.this.level().broadcastEntityEvent(NuzzleEntity.this, (byte)10);
      }

      @Override
      public void stop() {
         this.remaining = 0;
      }

      @Override
      public boolean canContinueToUse() {
         return this.remaining > 0;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         if (--this.remaining == 4 && this.canGraze()) {
            NuzzleEntity.this.ate();
         }
      }
   }
}
