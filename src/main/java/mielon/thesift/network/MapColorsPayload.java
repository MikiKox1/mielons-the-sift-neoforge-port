package mielon.thesift.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record MapColorsPayload(int mapId, byte[] colors) implements CustomPacketPayload {
   public static final Type<MapColorsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("the_sift", "map_colors"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MapColorsPayload> CODEC = new StreamCodec<RegistryFriendlyByteBuf, MapColorsPayload>() {
      public MapColorsPayload decode(RegistryFriendlyByteBuf buffer) {
         return new MapColorsPayload(buffer.readVarInt(), buffer.readByteArray(131072));
      }

      public void encode(RegistryFriendlyByteBuf buffer, MapColorsPayload payload) {
         buffer.writeVarInt(payload.mapId);
         buffer.writeByteArray(payload.colors);
      }
   };

   @Override
   public Type<MapColorsPayload> type() {
      return TYPE;
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      event.registrar("1").optional().playToClient(TYPE, CODEC);
   }

   public static void send(ServerPlayer player, int id, byte[] colors) {
      if (player.connection.hasChannel(TYPE)) {
         PacketDistributor.sendToPlayer(player, new MapColorsPayload(id, colors));
      }
   }
}
