package mielon.thesift.client.compat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;

public final class SiftSkyShaderCompat {
   private static long nextCheck;
   private static boolean customSky;
   private static Object lastPack;

   private SiftSkyShaderCompat() {
   }

   public static boolean restoreSky() {
      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      if (level == null || !level.dimension().equals(TheSiftDimension.LEVEL_KEY) || !SiftShaderCompat.isShaderPackInUse()) {
         return false;
      } else if (client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
         return false;
      } else {
         return client.player == null || !client.player.hasEffect(MobEffects.BLINDNESS) && !client.player.hasEffect(MobEffects.DARKNESS)
            ? !supportsCustomSky()
            : false;
      }
   }

   public static boolean supportsCustomSky() {
      long now = System.nanoTime();
      if (now < nextCheck) {
         return customSky;
      }

      nextCheck = now + 1000000000L;

      try {
         String[] ignored = new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"};
         int field = ignored.length;
         int pack = 0;

         while (true) {
            if (pack < field) {
               String name = ignored[pack];

               Class<?> iris;
               try {
                  iris = Class.forName(name);
               } catch (ClassNotFoundException ignoredx) {
                  pack++;
                  continue;
               }

               Object packx = ((Optional)iris.getMethod("getCurrentPack").invoke(null)).orElse(null);
               if (packx == lastPack) {
                  return customSky;
               }

               lastPack = packx;
               customSky = false;
               if (packx == null) {
                  return false;
               }

               try {
                  Map<?, ?> dimensions = (Map<?, ?>)packx.getClass().getMethod("getDimensionMap").invoke(packx);

                  for (Entry<?, ?> entry : dimensions.entrySet()) {
                     if ("the_sift:the_sift".equals(entry.getKey().toString())) {
                        Object folder = entry.getValue();
                        boolean dedicated = folder != null && !folder.toString().isBlank() && !"world0".equals(folder.toString());

                        for (Entry<?, ?> other : dimensions.entrySet()) {
                           if (!"the_sift:the_sift".equals(other.getKey().toString()) && Objects.equals(folder, other.getValue())) {
                              dedicated = false;
                           }
                        }

                        if (dedicated) {
                           customSky = true;
                           return true;
                        }
                     }
                  }
               } catch (ReflectiveOperationException var25) {
               }

               Path directory = (Path)iris.getMethod("getShaderpacksDirectory").invoke(null);
               String selected = (String)iris.getMethod("getCurrentPackName").invoke(null);
               Path path = directory.resolve(selected).normalize();
               if (!path.startsWith(directory.normalize())) {
                  return false;
               }

               if (Files.isDirectory(path)) {
                  Path props = path.resolve("shaders/shaders.properties");
                  if (Files.isRegularFile(props)) {
                     try (InputStream in = Files.newInputStream(props)) {
                        customSky = declaresSky(in);
                     }
                  }
               } else if (Files.isRegularFile(path)) {
                  try (ZipFile zip = new ZipFile(path.toFile())) {
                     Enumeration<? extends ZipEntry> entries = zip.entries();

                     while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().equals("shaders/shaders.properties") || entry.getName().endsWith("/shaders/shaders.properties")) {
                           try (InputStream in = zip.getInputStream(entry)) {
                              if (declaresSky(in)) {
                                 customSky = true;
                              }
                           }
                        }
                     }
                  }
               }

               return customSky;
            }

            Class<?> shaders = Class.forName("net.optifine.shaders.Shaders");
            Field fieldx = shaders.getDeclaredField("shaderPack");
            fieldx.setAccessible(true);
            Object packx = fieldx.get(null);
            if (packx == lastPack) {
               return customSky;
            }

            lastPack = packx;
            customSky = false;

            try (InputStream in = (InputStream)shaders.getMethod("getShaderPackResourceStream", String.class).invoke(null, "/shaders/shaders.properties")) {
               customSky = in != null && declaresSky(in);
               break;
            }
         }
      } catch (ReflectiveOperationException | IOException | RuntimeException | LinkageError ignored) {
         customSky = false;
      }

      return customSky;
   }

   private static boolean declaresSky(InputStream in) throws IOException {
      Properties properties = new Properties();
      properties.load(in);
      return Boolean.parseBoolean(properties.getProperty("the_sift.customSky", "false"));
   }
}
