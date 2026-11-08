package mielon.thesift.client.loading;

import mielon.thesift.network.RiftLoadingPayload;
import mielon.thesift.world.SiftTeleportManager;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class SiftClientPortalTransition {
   private static ResourceKey<Level> lastDimension;
   private static boolean contactLatched;
   private static boolean awaitingServer;
   private static long contactStartedAtNanos;
   private static boolean arrivalGuard;
   private static int clearContactTicks;

   private SiftClientPortalTransition() {
   }

   public static void handle(RiftLoadingPayload payload) {
      Minecraft minecraft = Minecraft.getInstance();
      if (!payload.start()) {
         awaitingServer = false;
      }

      if (payload.start()) {
         awaitingServer = false;
         if (minecraft.player == null) {
            return;
         }

         SiftLoadingScreenState.begin(minecraft.player.level().dimension(), payload.rift());
         if (!(minecraft.gui.screen() instanceof LevelLoadingScreen) && !(minecraft.gui.screen() instanceof SiftTransitionScreen)) {
            minecraft.gui.setScreen(new SiftTransitionScreen());
         }
      } else if (minecraft.gui.screen() instanceof SiftTransitionScreen) {
         awaitingServer = false;
         SiftLoadingScreenState.clear();
         minecraft.gui.setScreen(null);
      } else if (!(minecraft.gui.screen() instanceof LevelLoadingScreen)) {
         SiftLoadingScreenState.clear();
      }
   }

   public static void tick() {
      Minecraft minecraft = Minecraft.getInstance();
      checkPortalContact(minecraft);
      SiftIntroClient.tick();
      if (SiftLoadingScreenState.isActive()) {
         Screen screen = minecraft.gui.screen();
         if (screen instanceof SiftTransitionScreen
            && minecraft.level != null
            && minecraft.player != null
            && !minecraft.level.dimension().equals(SiftLoadingScreenState.sourceDimension())
            && minecraft.level.hasChunkAt(minecraft.player.blockPosition())) {
            awaitingServer = false;
            SiftLoadingScreenState.clear();
            minecraft.gui.setScreen(null);
         } else if (!(screen instanceof SiftTransitionScreen) && !(screen instanceof LevelLoadingScreen)) {
            SiftLoadingScreenState.clear();
         } else if (SiftLoadingScreenState.isOlderThanSeconds(35.0)) {
            SiftLoadingScreenState.clear();
            screen.onClose();
         }
      }
   }

   private static void checkPortalContact(Minecraft minecraft) {
      if (minecraft.player != null && minecraft.level != null) {
         ResourceKey<Level> dimension = minecraft.level.dimension();
         boolean touching = !minecraft.player.isSpectator()
            && (dimension.equals(Level.OVERWORLD) || dimension.equals(TheSiftDimension.LEVEL_KEY))
            && SiftTeleportManager.isTouchingPortal(minecraft.player);
         if (!dimension.equals(lastDimension)) {
            lastDimension = dimension;
            contactLatched = true;
            awaitingServer = false;
            arrivalGuard = true;
            clearContactTicks = 0;
         }

         if (!arrivalGuard) {
            if (!touching) {
               contactLatched = false;
            }

            if (awaitingServer && System.nanoTime() - contactStartedAtNanos > 2000000000L && minecraft.gui.screen() instanceof SiftTransitionScreen) {
               awaitingServer = false;
               SiftLoadingScreenState.clear();
               minecraft.gui.setScreen(null);
            }

            if (touching && !contactLatched && minecraft.gui.screen() == null && !SiftLoadingScreenState.isActive()) {
               contactLatched = true;
               awaitingServer = true;
               contactStartedAtNanos = System.nanoTime();
               SiftLoadingScreenState.begin(dimension, false);
               minecraft.gui.setScreen(new SiftTransitionScreen());
            }
         } else {
            if (touching
               || !minecraft.level.hasChunkAt(minecraft.player.blockPosition())
               || minecraft.gui.screen() != null
               || SiftLoadingScreenState.isActive()) {
               clearContactTicks = 0;
            } else if (++clearContactTicks >= 5) {
               arrivalGuard = false;
               contactLatched = false;
            }
         }
      } else {
         lastDimension = null;
         contactLatched = false;
         awaitingServer = false;
         arrivalGuard = false;
         clearContactTicks = 0;
      }
   }
}
