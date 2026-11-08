package mielon.thesift.mixin;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import mielon.thesift.map.SiftMapLayer;
import mielon.thesift.map.SiftMapStorage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapItemSavedData.class)
public abstract class SiftMapDataMixin implements SiftMapStorage {
   @Unique
   private final SiftMapLayer theSift$mapColors = new SiftMapLayer();
   @Shadow
   @Final
   @Mutable
   public static Codec<MapItemSavedData> CODEC;

   @Override
   public SiftMapLayer theSift$colors() {
      return this.theSift$mapColors;
   }

   @Inject(method = "<clinit>", at = @At("TAIL"))
   private static void theSift$persistMapColors(CallbackInfo callback) {
      CODEC = Codec.pair(CODEC, Codec.INT_STREAM.optionalFieldOf("the_sift_colors").codec()).xmap(pair -> {
         pair.getSecond().ifPresent(stream -> ((SiftMapStorage)pair.getFirst()).theSift$colors().load(stream.toArray()));
         return pair.getFirst();
      }, map -> {
         int[] colors = ((SiftMapStorage)map).theSift$colors().snapshot();
         Optional<IntStream> stream = colors.length == 0 ? Optional.empty() : Optional.of(Arrays.stream(colors));
         return Pair.of(map, stream);
      });
   }

   @Inject(method = "locked", at = @At("RETURN"))
   private void theSift$keepLockedMapColors(CallbackInfoReturnable<MapItemSavedData> callback) {
      ((SiftMapStorage)callback.getReturnValue()).theSift$colors().load(this.theSift$mapColors.snapshot());
   }

   @Inject(method = "getUpdatePacket", at = @At("RETURN"))
   private void theSift$synchronizeMapColors(MapId id, Player player, CallbackInfoReturnable<Packet<?>> callback) {
      if (callback.getReturnValue() != null && player instanceof ServerPlayer serverPlayer) {
         this.theSift$mapColors.send(serverPlayer, id.id());
      }
   }
}
