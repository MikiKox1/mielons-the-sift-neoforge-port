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

/** Credits natural bedrock only, not blocks placed later by players or commands. */
public final class BedrockAdvancement {
   private static final Identifier ADVANCEMENT = Identifier.fromNamespaceAndPath("the_sift", "the_sift/make_it_for_bedrock");
   private static final Identifier FLOOR_RANDOM = Identifier.fromNamespaceAndPath("the_sift", "bedrock_floor");
   private static final int FLOOR_DEPTH = 5;

   private BedrockAdvancement() {
   }

   public static void tick(MinecraftServer server) {
      ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (level == null || level.players().isEmpty()) {
         return;
      }

      AdvancementHolder advancement = server.getAdvancements().get(ADVANCEMENT);
      if (advancement == null) {
         return;
      }
      for (ServerPlayer player : level.players()) {
         if (!player.isAlive() || player.isSpectator()) {
            continue;
         }
         double reach = player.blockInteractionRange();
         Vec3 eyes = player.getEyePosition();
         if (eyes.y > level.getMinY() + FLOOR_DEPTH + reach
            || player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            continue;
         }
         if (player.pick(reach, 1.0F, false) instanceof BlockHitResult hit
            && hit.getType() == Type.BLOCK && isNaturalFloor(level, hit.getBlockPos())) {
            Vec3 end = hit.getLocation();
            if (ProjectileUtil.getEntityHitResult(
               player, eyes, end, player.getBoundingBox().expandTowards(end.subtract(eyes)).inflate(1.0),
               entity -> !entity.isSpectator() && entity.isPickable(), eyes.distanceToSqr(end)
            ) == null) {
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
      if (depth < 0 || depth >= FLOOR_DEPTH) {
         return false;
      }
      double sample = depth == 0 ? 0.0 : level.getChunkSource().randomState().getOrCreateRandomFactory(FLOOR_RANDOM).at(pos).nextFloat();
      return BedrockFloorRules.matches(depth, sample);
   }

   public static void blockChanged(ServerLevel level, BlockPos pos, BlockState state) {
      if (!level.dimension().equals(TheSiftDimension.LEVEL_KEY) || !matchesFloorRule(level, pos)) {
         return;
      }
      CommandStorage storage = level.getServer().getCommandStorage();
      Identifier chunkKey = changedFloorKey(pos);
      CompoundTag changes = storage.get(chunkKey);
      String key = key(pos);
      if (state.is(Blocks.BEDROCK)) {
         changes.putBoolean(key, true);
      } else {
         if (!changes.contains(key)) {
            return;
         }
         changes.remove(key);
      }
      storage.set(chunkKey, changes);
   }

   private static Identifier changedFloorKey(BlockPos pos) {
      return Identifier.fromNamespaceAndPath("the_sift", "placed_bedrock/" + (pos.getX() >> 4) + "/" + (pos.getZ() >> 4));
   }

   private static String key(BlockPos pos) {
      return Long.toString(pos.asLong());
   }
}
