package mielon.thesift.client.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.quad.ModelQuad", remap = false)
public interface SodiumIchorQuadAccessor {
   // 0.9.3 renamed getters, but legacy and namespaced APIs use this same 8-int layout.
   @Accessor("data")
   int[] theSift$vertexData();

   @Accessor("sprite")
   TextureAtlasSprite theSift$getSprite();

   @Invoker("setTexU")
   void theSift$setTexU(int vertex, float coordinate);

   @Invoker("setTexV")
   void theSift$setTexV(int vertex, float coordinate);
}
