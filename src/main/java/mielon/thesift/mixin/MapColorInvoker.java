package mielon.thesift.mixin;

import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MapColor.class)
public interface MapColorInvoker {
   @Invoker("<init>")
   static MapColor theSift$create(int id, int rgb) {
      throw new AssertionError("Map color constructor invoker was not applied");
   }
}
