package mielon.thesift.advancement;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

/** Mielon's 2.0.3 natural-bedrock criterion, with a persistent placed-block ledger. */
public final class BedrockAdvancement {
   private static final Identifier ADVANCEMENT = Identifier.fromNamespaceAndPath("the_sift", "the_sift/make_it_for_bedrock");
   private static final Identifier FLOOR_RANDOM = Identifier.fromNamespaceAndPath("the_sift", "bedrock_floor");
   private static final int FLOOR_DEPTH = 5;

   private BedrockAdvancement() { }

   public static void tick(MinecraftServer server) {
      ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (level == null || level.players().isEmpty()) return;
      AdvancementHolder advancement = server.getAdvancements().get(ADVANCEMENT);
      if (advancement == null) return;
      for (ServerPlayer player : level.players()) {
         if (player.isSpectator() || !player.isAlive()) continue;
         double reach = player.blockInteractionRange();
         Vec3 eyes = player.getEyePosition();
         if (eyes.y <= level.getMinY() + FLOOR_DEPTH + reach
            && !player.getAdvancements().getOrStartProgress(advancement).isDone()
            && player.pick(reach, 1.0F, false) instanceof BlockHitResult hit
            && hit.getType() == Type.BLOCK
            && isNaturalFloor(level, hit.getBlockPos())) {
            Vec3 end = hit.getLocation();
            if (ProjectileUtil.getEntityHitResult(player, eyes, end,
                  player.getBoundingBox().expandTowards(end.subtract(eyes)).inflate(1.0),
                  entity -> !entity.isSpectator() && entity.isPickable(), eyes.distanceToSqr(end)) == null) {
               player.getAdvancements().award(advancement, "event");
            }
         }
      }
   }

   private static boolean isNaturalFloor(ServerLevel level, BlockPos pos) {
      return level.getBlockState(pos).is(Blocks.BEDROCK)
         && matchesFloorRule(level, pos)
         && !level.getServer().getCommandStorage().get(changedFloorKey(pos)).contains(key(pos));
   }

   private static boolean matchesFloorRule(ServerLevel level, BlockPos pos) {
      int depth = pos.getY() - level.getMinY();
      if (depth < 0 || depth >= FLOOR_DEPTH) return false;
      float roll = depth == 0 ? 0.0F : level.getChunkSource().randomState()
         .getOrCreateRandomFactory(FLOOR_RANDOM).at(pos).nextFloat();
      return matchesFloorRoll(depth, roll);
   }

   static boolean matchesFloorRoll(int depth, float roll) {
      return depth >= 0 && depth < FLOOR_DEPTH && (depth == 0 || roll < 1.0 - depth / 5.0);
   }

   public static void blockChanged(ServerLevel level, BlockPos pos, BlockState state) {
      if (!level.dimension().equals(TheSiftDimension.LEVEL_KEY) || !matchesFloorRule(level, pos)) return;
      CommandStorage storage = level.getServer().getCommandStorage();
      Identifier chunkKey = changedFloorKey(pos);
      CompoundTag changes = storage.get(chunkKey);
      if (updatePlacedMarker(changes, pos, state.is(Blocks.BEDROCK))) storage.set(chunkKey, changes);
   }

   static boolean updatePlacedMarker(CompoundTag changes, BlockPos pos, boolean bedrock) {
      String key = key(pos);
      if (bedrock) {
         changes.putBoolean(key, true);
      } else {
         if (!changes.contains(key)) return false;
         changes.remove(key);
      }
      return true;
   }

   private static Identifier changedFloorKey(BlockPos pos) {
      return Identifier.fromNamespaceAndPath("the_sift", "placed_bedrock/" + (pos.getX() >> 4) + "/" + (pos.getZ() >> 4));
   }

   private static String key(BlockPos pos) {
      return Long.toString(pos.asLong());
   }
}
