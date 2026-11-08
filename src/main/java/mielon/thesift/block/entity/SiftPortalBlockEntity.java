package mielon.thesift.block.entity;

import java.util.List;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPortalBlock;
import mielon.thesift.portal.PortalFrameScanner;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.SiftPortalStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SiftPortalBlockEntity extends TheEndPortalBlockEntity {
   private static final int ANIMATION_NONE = 0;
   private static final int ANIMATION_GROWING = 1;
   private static final int ANIMATION_CLOSING = 2;
   private static final int ANIMATION_FORMING = 3;
   private boolean mainPortalAxisChecked;
   private int animationMode = 0;
   private long[] animationQueue = new long[0];
   private int animationIndex = 0;
   private int animationCooldown = 0;

   public SiftPortalBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlocks.SIFT_PORTAL_BLOCK_ENTITY, pos, state);
   }

   @Override
   public boolean shouldRenderFace(Direction direction) {
      return true;
   }

   public void startGrowth(List<BlockPos> queue, int nextIndex) {
      this.setAnimation(1, queue, nextIndex);
   }

   public void startDormant(List<BlockPos> queue, int nextIndex) {
      this.setAnimation(3, queue, nextIndex);
   }

   public void startClosing(List<BlockPos> queue) {
      this.setAnimation(2, queue, 0);
   }

   private void setAnimation(int mode, List<BlockPos> queue, int nextIndex) {
      this.animationMode = mode;
      this.animationQueue = new long[queue.size()];

      for (int i = 0; i < queue.size(); i++) {
         this.animationQueue[i] = queue.get(i).asLong();
      }

      this.animationIndex = Math.max(0, Math.min(nextIndex, this.animationQueue.length));
      this.animationCooldown = 0;
      this.setChanged();
   }

   public void stopPortalAnimation() {
      if (this.animationMode != 0) {
         this.clearAnimation();
         this.setChanged();
      }
   }

   public boolean isAnimatingPortal() {
      return this.animationMode != 0;
   }

   public boolean animationOverlaps(Set<BlockPos> positions) {
      if (!this.isAnimatingPortal()) {
         return false;
      }

      for (long packed : this.animationQueue) {
         if (positions.contains(BlockPos.of(packed))) {
            return true;
         }
      }

      return false;
   }

   public static void tick(Level level, BlockPos pos, BlockState state, SiftPortalBlockEntity blockEntity) {
      if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
         if (!blockEntity.mainPortalAxisChecked) {
            blockEntity.mainPortalAxisChecked = true;
            SiftPortalStructure.repairMainPortalAxis(serverLevel, pos, state);
         }

         blockEntity.tickAnimation(serverLevel);
      }
   }

   private void tickAnimation(ServerLevel level) {
      if (this.animationMode != 0) {
         if (this.animationQueue.length == 0 || this.animationIndex >= this.animationQueue.length) {
            this.clearAnimation();
            this.setChanged();
         } else if (this.animationMode == 3) {
            this.tickDormantFormation(level);
         } else if (this.animationCooldown > 0) {
            this.animationCooldown--;
            this.setChanged();
         } else {
            boolean closing = this.animationMode == 2;
            this.processOne(level, BlockPos.of(this.animationQueue[this.animationIndex]), closing);
            this.animationIndex++;
            if (this.animationIndex < this.animationQueue.length && this.deterministicChance(this.animationIndex, 55)) {
               BlockPos next = BlockPos.of(this.animationQueue[this.animationIndex]);
               if (!closing || !next.equals(this.worldPosition)) {
                  this.processOne(level, next, closing);
                  this.animationIndex++;
               }
            }

            if (this.animationIndex < this.animationQueue.length) {
               this.animationCooldown = this.deterministicChance(this.animationIndex + 31, 20) ? 1 : 0;
               this.setChanged();
            } else {
               if (!closing || level.getBlockState(this.worldPosition).is(ModBlocks.SIFT_PORTAL)) {
                  this.clearAnimation();
                  this.setChanged();
               }
            }
         }
      }
   }

   private void tickDormantFormation(ServerLevel level) {
      boolean axisX = this.getBlockState().getValue(SiftPortalBlock.AXIS) == Axis.X;
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;

      for (long packed : this.animationQueue) {
         BlockPos p = BlockPos.of(packed);
         int u = axisX ? p.getX() : p.getZ();
         min = Math.min(min, u);
         max = Math.max(max, u);
      }

      int centerTwice = min + max;
      BlockPos next = BlockPos.of(this.animationQueue[this.animationIndex]);
      int shell = Math.abs((axisX ? next.getX() : next.getZ()) * 2 - centerTwice);

      while (this.animationIndex < this.animationQueue.length) {
         BlockPos p = BlockPos.of(this.animationQueue[this.animationIndex]);
         if (Math.abs((axisX ? p.getX() : p.getZ()) * 2 - centerTwice) != shell) {
            break;
         }

         if (PortalFrameScanner.isAirLikeInterior(level, p)) {
            level.setBlockAndUpdate(
               p,
               ModBlocks.SIFT_PORTAL
                  .defaultBlockState()
                  .setValue(SiftPortalBlock.AXIS, this.getBlockState().getValue(SiftPortalBlock.AXIS))
                  .setValue(SiftPortalBlock.ACTIVE, false)
            );
         }

         this.animationIndex++;
      }

      if (this.animationIndex >= this.animationQueue.length) {
         this.clearAnimation();
      }

      this.setChanged();
   }

   private void processOne(ServerLevel level, BlockPos pos, boolean closing) {
      if (closing) {
         this.removeAt(level, pos);
      } else {
         this.placeAt(level, pos);
      }
   }

   private void placeAt(ServerLevel level, BlockPos pos) {
      if (PortalFrameScanner.isAirLikeInterior(level, pos)) {
         level.setBlockAndUpdate(
            pos, ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.AXIS, this.getBlockState().getValue(SiftPortalBlock.AXIS))
         );
         level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.32, 0.32, 0.32, 0.025);
         level.playSound(null, pos, ModSounds.SIFT_PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.15F);
      }
   }

   private void removeAt(ServerLevel level, BlockPos pos) {
      if (level.getBlockState(pos).is(ModBlocks.SIFT_PORTAL)) {
         level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.32, 0.32, 0.32, 0.025);
         level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
      }
   }

   private boolean deterministicChance(int salt, int percent) {
      long x = this.worldPosition.asLong();
      x ^= salt * -7046029254386353131L;
      x ^= x >>> 33;
      x *= -49064778989728563L;
      x ^= x >>> 33;
      return Math.floorMod(x, 100L) < percent;
   }

   private void clearAnimation() {
      this.animationMode = 0;
      this.animationQueue = new long[0];
      this.animationIndex = 0;
      this.animationCooldown = 0;
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("portal_animation_mode", this.animationMode);
      output.putInt("portal_animation_index", this.animationIndex);
      output.putInt("portal_animation_cooldown", this.animationCooldown);
      output.putInt("portal_animation_count", this.animationQueue.length);

      for (int i = 0; i < this.animationQueue.length; i++) {
         output.putLong("portal_animation_pos_" + i, this.animationQueue[i]);
      }
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.animationMode = input.getIntOr("portal_animation_mode", 0);
      if (this.animationMode != 1 && this.animationMode != 2 && this.animationMode != 3) {
         this.animationMode = 0;
      }

      int count = Math.max(0, Math.min(1024, input.getIntOr("portal_animation_count", 0)));
      this.animationQueue = new long[count];

      for (int i = 0; i < count; i++) {
         this.animationQueue[i] = input.getLong("portal_animation_pos_" + i).orElse(0L);
      }

      this.animationIndex = Math.max(0, Math.min(input.getIntOr("portal_animation_index", 0), count));
      this.animationCooldown = Math.max(0, input.getIntOr("portal_animation_cooldown", 0));
      if (count == 0 || this.animationIndex >= count) {
         this.clearAnimation();
      }
   }
}
