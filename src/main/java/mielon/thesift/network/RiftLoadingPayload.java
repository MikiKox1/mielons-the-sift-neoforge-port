package mielon.thesift.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RiftLoadingPayload(boolean start, boolean rift) implements CustomPacketPayload {
   public static final Type<RiftLoadingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("the_sift", "rift_loading"));
   public static final StreamCodec<RegistryFriendlyByteBuf, RiftLoadingPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, RiftLoadingPayload::start, ByteBufCodecs.BOOL, RiftLoadingPayload::rift, RiftLoadingPayload::new
   );

   @Override
   public Type<RiftLoadingPayload> type() {
      return TYPE;
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      event.registrar("1").optional().playToClient(TYPE, CODEC);
   }

   public static void send(ServerPlayer player, boolean start) {
      send(player, start, true);
   }

   public static void send(ServerPlayer player, boolean start, boolean rift) {
      if (player.connection.hasChannel(TYPE)) {
         PacketDistributor.sendToPlayer(player, new RiftLoadingPayload(start, rift));
      }
   }
}
