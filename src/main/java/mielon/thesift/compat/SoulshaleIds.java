package mielon.thesift.compat;

import net.minecraft.resources.Identifier;

public final class SoulshaleIds {
   private SoulshaleIds() {
   }

   public static Identifier upgrade(Identifier id) {
      return id != null && id.getNamespace().equals("the_sift") && id.getPath().contains("grimstone")
         ? Identifier.fromNamespaceAndPath("the_sift", id.getPath().replace("grimstone", "soulshale"))
         : id;
   }
}
