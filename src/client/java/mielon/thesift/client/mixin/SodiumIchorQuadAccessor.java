package mielon.thesift.client.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.quad.ModelQuad", remap = false)
public interface SodiumIchorQuadAccessor {
   @Invoker("getX")
   float theSift$getX(int var1);

   @Invoker("getY")
   float theSift$getY(int var1);

   @Invoker("getZ")
   float theSift$getZ(int var1);

   @Invoker("getSprite")
   TextureAtlasSprite theSift$getSprite();

   @Invoker("setTexU")
   void theSift$setTexU(int var1, float var2);

   @Invoker("setTexV")
   void theSift$setTexV(int var1, float var2);
}
