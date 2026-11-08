package mielon.thesift.entity;

import java.util.EnumSet;
import mielon.thesift.block.BlubbyBerriesBushBlock;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class BlubEatBerriesGoal extends Goal {
   private final BlubEntity blub;
   private long nextSearchTick;
   private BlockPos berryPos;
   private Path path;
   private int eatingTicks;
   private int elapsedTicks;
   private boolean ate;

   public BlubEatBerriesGoal(BlubEntity blub) {
      this.blub = blub;
      this.nextSearchTick = blub.level().getGameTime() + 200L + blub.getRandom().nextInt(401);
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
   }

   private boolean mayForage() {
      return this.blub.isAlive()
         && !this.blub.isOrderedToSit()
         && !this.blub.isBlubSitting()
         && this.blub.getTarget() == null
         && !this.blub.isAggressive()
         && !this.blub.isPassenger()
         && !this.blub.isInBlubTower()
         && !this.blub.isInWater()
         && !this.blub.isInLava()
         && !this.blub.isOnFire()
         && this.blub.level() instanceof ServerLevel level
         && level.getGameRules().get(GameRules.MOB_GRIEFING);
   }

   private boolean isRipe(BlockPos pos) {
      BlockState state = this.blub.level().getBlockState(pos);
      return state.is(ModBlocks.BLUBBY_BERRIES_BUSH) && state.getValue(BlubbyBerriesBushBlock.GROWN);
   }

   private BlockPos stemBase(BlockPos pos) {
      return this.blub.level().getBlockState(pos).getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
   }

   @Override
   public boolean canUse() {
      if (this.mayForage() && this.blub.onGround() && this.blub.level().getGameTime() >= this.nextSearchTick) {
         this.nextSearchTick = this.blub.level().getGameTime() + 400L + this.blub.getRandom().nextInt(801);
         this.berryPos = null;
         this.path = null;
         double closest = Double.MAX_VALUE;
         BlockPos origin = this.blub.blockPosition();

         for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-8, -2, -8), origin.offset(8, 2, 8))) {
            if (this.blub.level().hasChunkAt(candidate) && this.isRipe(candidate)) {
               double distance = this.blub.distanceToSqr(Vec3.atCenterOf(candidate));
               if (!(distance >= closest)) {
                  Path route = this.blub.getNavigation().createPath(this.stemBase(candidate), 1);
                  if (route != null && route.canReach()) {
                     closest = distance;
                     this.berryPos = candidate.immutable();
                     this.path = route;
                  }
               }
            }
         }

         return this.berryPos != null;
      } else {
         return false;
      }
   }

   @Override
   public boolean canContinueToUse() {
      return !this.ate && this.elapsedTicks < 240 && this.berryPos != null && this.mayForage() && this.isRipe(this.berryPos);
   }

   @Override
   public void start() {
      this.eatingTicks = 0;
      this.elapsedTicks = 0;
      this.ate = false;
      this.blub.getNavigation().moveTo(this.path, 1.0);
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   private boolean canReachFruit() {
      Vec3 fruit = Vec3.atCenterOf(this.berryPos);
      double dx = fruit.x - this.blub.getX();
      double dz = fruit.z - this.blub.getZ();
      if (!(dx * dx + dz * dz > 2.25) && !(Math.abs(fruit.y - this.blub.getEyeY()) > 1.75)) {
         HitResult hit = this.blub.level().clip(new ClipContext(this.blub.getEyePosition(), fruit, Block.COLLIDER, Fluid.NONE, this.blub));
         return hit.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      this.elapsedTicks++;
      if (this.berryPos != null && this.mayForage() && this.isRipe(this.berryPos)) {
         Vec3 fruit = Vec3.atCenterOf(this.berryPos);
         this.blub.getLookControl().setLookAt(fruit.x, fruit.y, fruit.z, 30.0F, 30.0F);
         if (!this.canReachFruit()) {
            this.eatingTicks = 0;
            if (this.elapsedTicks % 20 == 0) {
               BlockPos stem = this.stemBase(this.berryPos);
               this.blub.getNavigation().moveTo(stem.getX() + 0.5, stem.getY(), stem.getZ() + 0.5, 1.0);
            }
         } else {
            this.blub.getNavigation().stop();
            if (++this.eatingTicks >= 40) {
               BlockState harvested = this.blub.level().getBlockState(this.berryPos).setValue(BlubbyBerriesBushBlock.GROWN, false);
               this.blub.level().setBlock(this.berryPos, harvested, 2);
               this.blub.heal(4.0F);
               this.blub.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 0.9F + this.blub.getRandom().nextFloat() * 0.2F);
               this.blub.level().gameEvent(GameEvent.EAT, this.berryPos, Context.of(this.blub, harvested));
               this.blub.triggerAnim("body", "happy");
               this.blub.triggerAnim("ears", "happy");
               this.ate = true;
            }
         }
      }
   }

   @Override
   public void stop() {
      this.blub.getNavigation().stop();
      this.nextSearchTick = Math.max(this.nextSearchTick, this.blub.level().getGameTime() + 400L + this.blub.getRandom().nextInt(801));
      this.berryPos = null;
      this.path = null;
      this.eatingTicks = 0;
   }
}
