package mielon.thesift.network;

import mielon.thesift.world.SiftTeleportManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SiftIntroReadyPayload() implements CustomPacketPayload {
   public static final Type<SiftIntroReadyPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("the_sift", "intro_ready"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SiftIntroReadyPayload> CODEC = StreamCodec.unit(new SiftIntroReadyPayload());

   @Override
   public Type<SiftIntroReadyPayload> type() {
      return TYPE;
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      event.registrar("1").optional().playToServer(TYPE, CODEC, (payload, context) ->
         SiftTeleportManager.playFirstIntro((ServerPlayer)context.player())
      );
   }
}
