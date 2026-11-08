package mielon.thesift.mixin;

import java.util.List;
import mielon.thesift.worldgen.CorruptedSiftStone;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureStart.class)
public abstract class SculkersLairSealMixin {
   @Inject(method = "placeInChunk", at = @At("RETURN"), require = 1)
   private void theSift$sealLair(
      WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunk, CallbackInfo ci
   ) {
      StructureStart start = (StructureStart)(Object)this;
      Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
      if (id != null && id.toString().equals("the_sift:sculkers_lair")) {
         List<StructurePiece> pieces = start.getPieces();
         MutableBlockPos cursor = new MutableBlockPos();

         for (StructurePiece piece : pieces) {
            BoundingBox box = piece.getBoundingBox();
            int minX = Math.max(chunkBox.minX(), box.minX() - 1);
            int maxX = Math.min(chunkBox.maxX(), box.maxX() + 1);
            int minZ = Math.max(chunkBox.minZ(), box.minZ() - 1);
            int maxZ = Math.min(chunkBox.maxZ(), box.maxZ() + 1);
            int minY = Math.max(chunkBox.minY(), box.minY() - 1);
            int maxY = Math.min(chunkBox.maxY(), box.maxY() + 1);

            for (int x = minX; x <= maxX; x++) {
               for (int z = minZ; z <= maxZ; z++) {
                  for (int y = minY; y <= maxY; y++) {
                     if (x < box.minX() || x > box.maxX() || y < box.minY() || y > box.maxY() || z < box.minZ() || z > box.maxZ()) {
                        cursor.set(x, y, z);
                        boolean inside = false;

                        for (StructurePiece other : pieces) {
                           if (other.getBoundingBox().isInside(cursor)) {
                              inside = true;
                              break;
                           }
                        }

                        if (!inside) {
                           BlockState state = level.getBlockState(cursor);
                           if (state.isAir() || !state.getFluidState().isEmpty()) {
                              level.setBlock(cursor, CorruptedSiftStone.at(cursor, level.getSeed()), 2);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
