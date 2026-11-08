package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;

final class SiftBiomeSampler {
   private final WorldGenLevel level;
   private final int minX;
   private final int minY;
   private final int minZ;
   private final int sizeX;
   private final int sizeY;
   private final int sizeZ;
   private final Holder<Biome>[] corners;
   private final Holder<Biome>[] uniform;
   private final byte[] kind;

   SiftBiomeSampler(WorldGenLevel level, int x, int z, int low, int high) {
      this.level = level;
      this.minX = x - 2 >> 2;
      this.minZ = z - 2 >> 2;
      this.minY = low - 2 >> 2;
      this.sizeX = (x + 15 - 2 >> 2) - this.minX + 2;
      this.sizeZ = (z + 15 - 2 >> 2) - this.minZ + 2;
      this.sizeY = (high - 2 >> 2) - this.minY + 2;
      int count = this.sizeX * this.sizeY * this.sizeZ;
      this.corners = new Holder[count];
      this.uniform = new Holder[count];
      this.kind = new byte[count];
   }

   Holder<Biome> get(BlockPos pos) {
      int x = (pos.getX() - 2 >> 2) - this.minX;
      int y = (pos.getY() - 2 >> 2) - this.minY;
      int z = (pos.getZ() - 2 >> 2) - this.minZ;
      if (x >= 0 && y >= 0 && z >= 0 && x < this.sizeX - 1 && y < this.sizeY - 1 && z < this.sizeZ - 1) {
         int index = (x * this.sizeZ + z) * this.sizeY + y;
         if (this.kind[index] == 0) {
            Holder<Biome> first = this.corner(x, y, z);
            boolean same = true;

            for (int dx = 0; dx <= 1; dx++) {
               for (int dz = 0; dz <= 1; dz++) {
                  for (int dy = 0; dy <= 1; dy++) {
                     if (this.corner(x + dx, y + dy, z + dz) != first) {
                        same = false;
                     }
                  }
               }
            }

            this.kind[index] = (byte)(same ? 1 : 2);
            this.uniform[index] = first;
         }

         return this.kind[index] == 1 ? this.uniform[index] : this.level.getBiome(pos);
      } else {
         return this.level.getBiome(pos);
      }
   }

   private Holder<Biome> corner(int x, int y, int z) {
      int index = (x * this.sizeZ + z) * this.sizeY + y;
      Holder<Biome> holder = this.corners[index];
      if (holder == null) {
         this.corners[index] = holder = this.level.getNoiseBiome(x + this.minX, y + this.minY, z + this.minZ);
      }

      return holder;
   }
}
