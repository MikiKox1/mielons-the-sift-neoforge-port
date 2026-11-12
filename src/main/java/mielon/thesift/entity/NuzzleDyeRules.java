package mielon.thesift.entity;

import net.minecraft.world.item.DyeColor;

/** Original 2.0.3 eligibility test used in the actual Nuzzle interaction. */
public final class NuzzleDyeRules {
   private NuzzleDyeRules() {}

   public static boolean canRecolor(DyeColor requested, DyeColor current, boolean alive, boolean sheared) {
      return requested != null && requested != current && alive && !sheared;
   }
}
