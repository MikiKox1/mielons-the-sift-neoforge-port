package mielon.thesift.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import mielon.thesift.TheSiftMod;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.storage.LevelResource;

public final class SiftTideResumeMigration {
   private SiftTideResumeMigration() {
   }

   public static void resumeRequestedClock(MinecraftServer server) {
      Path marker = server.getWorldPath(LevelResource.ROOT).resolve("the_sift_resume_tides");
      if (Files.isRegularFile(marker)) {
         ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
         if (level != null) {
            Reference<WorldClock> clock = level.registryAccess()
               .lookupOrThrow(Registries.WORLD_CLOCK)
               .getOrThrow(ResourceKey.create(Registries.WORLD_CLOCK, SiftTides.CLOCK_ID));
            server.clockManager().setPaused(clock, false);

            try {
               Files.delete(marker);
            } catch (IOException error) {
               TheSiftMod.LOGGER.warn("Could not remove the Sift clock repair marker", error);
            }

            TheSiftMod.LOGGER.info("Resumed the saved Sift Tide clock after the requested repair.");
         }
      }
   }
}
