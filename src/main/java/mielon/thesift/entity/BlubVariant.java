package mielon.thesift.entity;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public enum BlubVariant {
   DEFAULT(0, "default"),
   CARAPACE(1, "carapace"),
   MEADOWS(2, "meadows");

   private final int id;
   private final String serializedName;

   BlubVariant(int id, String serializedName) {
      this.id = id;
      this.serializedName = serializedName;
   }

   public int id() {
      return this.id;
   }

   public String serializedName() {
      return this.serializedName;
   }

   public String resourceSuffix() {
      return this == DEFAULT ? "" : "_" + this.serializedName;
   }

   public static BlubVariant fromId(int id) {
      return switch (id) {
         case 1 -> CARAPACE;
         case 2 -> MEADOWS;
         default -> DEFAULT;
      };
   }

   public static BlubVariant fromName(String name) {
      for (BlubVariant variant : values()) {
         if (variant.serializedName.equals(name)) {
            return variant;
         }
      }

      return DEFAULT;
   }

   public static BlubVariant forBiome(Holder<Biome> biome, RandomSource random) {
      if (biome.is(TheSiftDimension.ORGANIC_DESERT)) {
         return CARAPACE;
      } else if (biome.is(TheSiftDimension.SOULSHALE_CAVES)) {
         return MEADOWS;
      } else if (!TheSiftDimension.isOvergrownBiome(biome) && !biome.is(TheSiftDimension.SIFT_WASTES) && !biome.is(TheSiftDimension.ICHOR_SNOWY_PEAKS)) {
         return DEFAULT;
      } else {
         return random.nextInt(4) == 0 ? DEFAULT : MEADOWS;
      }
   }
}
