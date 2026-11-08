package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftTerrainUpgrade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk.PostLoadProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class SiftLevelChunkGenerationMixin {
   @Inject(
      method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
      at = @At("RETURN"),
      require = 1
   )
   private void theSift$copyGeneration(ServerLevel level, ProtoChunk source, PostLoadProcessor processor, CallbackInfo ci) {
      SiftTerrainUpgrade.data((LevelChunk)(Object)this).theSift$generation(SiftTerrainUpgrade.data(source).theSift$generation());
      SiftTerrainUpgrade.data((LevelChunk)(Object)this).theSift$surfaceRevision(SiftTerrainUpgrade.data(source).theSift$surfaceRevision());
   }
}
