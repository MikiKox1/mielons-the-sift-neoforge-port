package mielon.thesift.client.light;

import mielon.thesift.entity.SingerEntity;
import net.minecraft.world.phys.Vec3;

public final class SingerSoulLightBridge {
   private static final SingerSoulLightBridge.Handler NO_OP = (singer, position) -> {};
   private static volatile SingerSoulLightBridge.Handler handler = NO_OP;

   private SingerSoulLightBridge() {
   }

   public static void install(SingerSoulLightBridge.Handler newHandler) {
      handler = newHandler != null ? newHandler : NO_OP;
   }

   public static void update(SingerEntity singer, Vec3 position) {
      handler.update(singer, position);
   }

   @FunctionalInterface
   public interface Handler {
      void update(SingerEntity var1, Vec3 var2);
   }
}
