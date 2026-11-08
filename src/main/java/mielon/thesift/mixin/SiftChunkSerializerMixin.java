package mielon.thesift.mixin;

import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftTerrainUpgrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public abstract class SiftChunkSerializerMixin {
   @Unique
   private CompoundTag theSift$generationTag;

   @Inject(method = "parse", at = @At("RETURN"), require = 1)
   private static void theSift$parseGeneration(
      LevelHeightAccessor height, PalettedContainerFactory factory, CompoundTag tag, CallbackInfoReturnable<SerializableChunkData> cir
   ) {
      if (cir.getReturnValue() != null) {
         CompoundTag marker = new CompoundTag();
         if (tag.contains("the_sift:terrain_generation")) {
            marker.putInt("the_sift:terrain_generation", tag.getIntOr("the_sift:terrain_generation", 1));
         }

         if (tag.contains("the_sift:surface_transition")) {
            marker.putInt("the_sift:surface_transition", tag.getIntOr("the_sift:surface_transition", 0));
         }

         ((SiftChunkSerializerMixin)(Object)cir.getReturnValue()).theSift$generationTag = marker;
      }
   }

   @Inject(method = "read", at = @At("RETURN"), cancellable = true, require = 1)
   private void theSift$readGeneration(ServerLevel level, PoiManager poi, RegionStorageInfo storage, ChunkPos pos, CallbackInfoReturnable<ProtoChunk> cir) {
      if (cir.getReturnValue() != null) {
         cir.setReturnValue(
            SiftTerrainUpgrade.load(level, this.theSift$generationTag == null ? new CompoundTag() : this.theSift$generationTag, cir.getReturnValue())
         );
      }
   }

   @Inject(method = "copyOf", at = @At("RETURN"), require = 1)
   private static void theSift$copyGeneration(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         CompoundTag marker = new CompoundTag();
         SiftTerrainUpgrade.write(level, marker, chunk);
         ((SiftChunkSerializerMixin)(Object)cir.getReturnValue()).theSift$generationTag = marker;
      }
   }

   @Inject(method = "write", at = @At("RETURN"), require = 1)
   private void theSift$writeGeneration(CallbackInfoReturnable<CompoundTag> cir) {
      if (this.theSift$generationTag != null && this.theSift$generationTag.contains("the_sift:terrain_generation")) {
         cir.getReturnValue().putInt("the_sift:terrain_generation", this.theSift$generationTag.getIntOr("the_sift:terrain_generation", 1));
      }

      if (this.theSift$generationTag != null && this.theSift$generationTag.contains("the_sift:surface_transition")) {
         cir.getReturnValue().putInt("the_sift:surface_transition", this.theSift$generationTag.getIntOr("the_sift:surface_transition", 0));
      }
   }
}
