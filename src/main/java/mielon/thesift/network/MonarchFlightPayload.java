package mielon.thesift.network;

import mielon.thesift.item.MonarchFlightAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record MonarchFlightPayload(boolean held) implements CustomPacketPayload {
   public static final Type<MonarchFlightPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("the_sift", "monarch_flight"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MonarchFlightPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, MonarchFlightPayload::held, MonarchFlightPayload::new
   );

   @Override
   public Type<MonarchFlightPayload> type() {
      return TYPE;
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      event.registrar("1").optional().playToServer(TYPE, CODEC, (payload, context) -> {
         ServerPlayer player = (ServerPlayer)context.player();
         ((MonarchFlightAccess)player).theSift$monarchFlight().updateInput(player, payload.held());
      });
   }
}
