package mielon.thesift.client.render;

public final class PixelPortalSurface {
   public static final int LEFT = 1;
   public static final int RIGHT = 2;
   public static final int BOTTOM = 4;
   public static final int TOP = 8;
   public static final int RIM_CORE_COLOR = color(242, 255, 255, 255);
   private static final float RIM_SHADER_SPACING = 0.03125F;
   private static final float RIM_SHADER_SIDE_INSET = 0.015625F;

   private static final int BACKING = color(38, 192, 197, 228);

   private PixelPortalSurface() {
   }

   public static void draw(
      boolean axisX, int worldU, int worldY, int edges, boolean positiveSide, double seconds, int resolution, PixelPortalSurface.QuadSink sink
   ) {
      drawSurface(axisX, worldU, worldY, positiveSide, seconds, resolution, sink);
      drawRim(axisX, edges, positiveSide, seconds, sink);
   }

   public static void drawDepthMask(boolean axisX, PixelPortalSurface.QuadSink sink) {
      face(sink, axisX, 0.0F, 0.0F, 1.0F, 1.0F, 0.5F, -1);
   }

   public static void drawSurface(boolean axisX, int worldU, int worldY, boolean positiveSide, double seconds, int resolution, PixelPortalSurface.QuadSink sink) {
      face(sink, axisX, 0.0F, 0.0F, 1.0F, 1.0F, positiveSide ? 0.505F : 0.495F, color(38, 192, 197, 228));
      partition(sink, axisX, positiveSide, seconds, resolution, worldU, worldY, Math.floorDiv(worldU, 2), Math.floorDiv(worldY, 2), 0, 0, 4, 4, 1, 0);
   }

   private static void partition(
      PixelPortalSurface.QuadSink sink,
      boolean axisX,
      boolean positive,
      double seconds,
      int quality,
      int worldU,
      int worldY,
      int macroU,
      int macroV,
      int u,
      int v,
      int width,
      int height,
      int node,
      int depth
   ) {
      double x = macroU * 2.0 + u * 0.5 - worldU;
      double y = macroV * 2.0 + v * 0.5 - worldY;
      if (!(x >= 1.0) && !(y >= 1.0) && !(x + width * 0.5 <= 0.0) && !(y + height * 0.5 <= 0.0)) {
         int seed = 1729 + node * 1931;
         boolean smallerTile = depth == 2 && width >= 2 && height >= 2 && hash(macroU, macroV, seed + 7) < 0.65;
         if ((depth < 2 || smallerTile) && (width > 1 || height > 1)) {
            boolean vertical = width > height || width == height && hash(macroU, macroV, seed) < 0.5;
            if (width == 1) {
               vertical = false;
            }

            if (height == 1) {
               vertical = true;
            }

            int size = vertical ? width : height;
            int cut = size / 2;
            if (depth == 0 && hash(macroU, macroV, seed + 11) < 0.22) {
               cut = hash(macroU, macroV, seed + 13) < 0.5 ? 1 : size - 1;
            }

            partition(
               sink,
               axisX,
               positive,
               seconds,
               quality,
               worldU,
               worldY,
               macroU,
               macroV,
               u,
               v,
               vertical ? cut : width,
               vertical ? height : cut,
               node * 2,
               depth + 1
            );
            partition(
               sink,
               axisX,
               positive,
               seconds,
               quality,
               worldU,
               worldY,
               macroU,
               macroV,
               vertical ? u + cut : u,
               vertical ? v : v + cut,
               vertical ? width - cut : width,
               vertical ? height : height - cut,
               node * 2 + 1,
               depth + 1
            );
         } else {
            double tone = 0.82 * field((macroU * 2.0 + (u + width * 0.5) * 0.5) / 4.0, (macroV * 2.0 + (v + height * 0.5) * 0.5) / 4.0)
               + 0.18 * hash(macroU, macroV, seed + 53);
            int rgba = surfaceColor(tone);
            double phase = seconds / (6.5 + hash(macroU, macroV, 71) * 3.0) + hash(macroU, macroV, 97) * 0.25 + hash(macroU, macroV, seed + 71);
            float sheet = positive ? 0.56F : 0.44F;
            tile(sink, axisX, x, y, width * 0.5, height * 0.5, sheet, fadeColor(rgba, phase));
            if (quality >= 4 && width * height >= 2 && hash(macroU, macroV, seed + 101) < 0.5) {
               double accentWidth = hash(macroU, macroV, seed + 113) < 0.5 ? 0.25 : 0.5;
               double accentHeight = hash(macroU, macroV, seed + 127) < 0.5 ? 0.25 : 0.5;
               double ax = x + (hash(macroU, macroV, seed + 131) < 0.5 ? 0.0 : width * 0.5 - accentWidth);
               double ay = y + (hash(macroU, macroV, seed + 137) < 0.5 ? 0.0 : height * 0.5 - accentHeight);
               tile(sink, axisX, ax, ay, accentWidth, accentHeight, positive ? 0.625F : 0.375F, fadeColor(color(196, 255, 246, 100), phase + 0.12));
            }
         }
      }
   }

   private static void tile(PixelPortalSurface.QuadSink sink, boolean axisX, double x, double y, double width, double height, float sheet, int rgba) {
      float x0 = (float)Math.max(0.0, x);
      float y0 = (float)Math.max(0.0, y);
      float x1 = (float)Math.min(1.0, x + width);
      float y1 = (float)Math.min(1.0, y + height);
      if (x1 > x0 && y1 > y0) {
         face(sink, axisX, x0, y0, x1, y1, sheet, rgba);
      }
   }

   private static int fadeColor(int rgba, double phase) {
      phase -= Math.floor(phase);
      double fade = 0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0);
      return rgba & 16777215 | (int)((rgba >>> 24) * fade) << 24;
   }

   private static double field(double x, double y) {
      int ix = (int)Math.floor(x);
      int iy = (int)Math.floor(y);
      double sx = smooth(x - ix);
      double sy = smooth(y - iy);
      double lower = hash(ix, iy, 3181) * (1.0 - sx) + hash(ix + 1, iy, 3181) * sx;
      double upper = hash(ix, iy + 1, 3181) * (1.0 - sx) + hash(ix + 1, iy + 1, 3181) * sx;
      return lower * (1.0 - sy) + upper * sy;
   }

   public static void drawRim(boolean axisX, int edges, boolean positiveSide, double seconds, PixelPortalSurface.QuadSink sink) {
      drawRimGlow(axisX, edges, positiveSide, seconds, sink);
      drawRimCore(axisX, edges, positiveSide, sink);
   }

   public static void drawRimCore(boolean axisX, int edges, boolean positiveSide, PixelPortalSurface.QuadSink sink) {
      float depth = positiveSide ? 0.7509F : 0.2491F;

      for (int edge = 1; edge <= 8; edge <<= 1) {
         if ((edges & edge) != 0) {
            rim(sink, axisX, edge, 0.125F, depth, RIM_CORE_COLOR);
         }
      }
   }

   /** Shaderpack-only: keep the bright core away from its translucent halo. */
   public static void drawShaderRimCore(boolean axisX, int edges, boolean positiveSide, PixelPortalSurface.QuadSink sink) {
      float depth = positiveSide ? 0.75F + 2.0F * RIM_SHADER_SPACING : 0.25F - 2.0F * RIM_SHADER_SPACING;
      for (int edge = 1; edge <= 8; edge <<= 1) {
         if ((edges & edge) != 0) {
            rim(sink, axisX, edge, 0.125F, depth, RIM_CORE_COLOR);
         }
      }
   }

   public static void drawRimGlow(boolean axisX, int edges, boolean positiveSide, double seconds, PixelPortalSurface.QuadSink sink) {
      float depth = positiveSide ? 0.75F : 0.25F;
      float pulse = (float)(0.94 + 0.06 * Math.sin(seconds * 1.35));
      int halo = color(24, 248, 239, (int)(71.0F * pulse));
      int glow = color(113, 255, 249, (int)(158.0F * pulse));

      for (int edge = 1; edge <= 8; edge <<= 1) {
         if ((edges & edge) != 0) {
            rim(sink, axisX, edge, 0.3125F, depth + (positiveSide ? 3.0E-4F : -3.0E-4F), halo);
            rim(sink, axisX, edge, 0.1875F, depth + (positiveSide ? 6.0E-4F : -6.0E-4F), glow);
            float auraDepth = positiveSide ? 1.004F : -0.004F;
            aura(sink, axisX, edge, 0.18F, auraDepth, color(39, 255, 241, 27));
            aura(sink, axisX, edge, 0.1F, auraDepth + (positiveSide ? 2.0E-4F : -2.0E-4F), color(112, 255, 250, 43));
            side(sink, axisX, edge, color(103, 244, 241, 108));
         }
      }
   }

   /** Shaderpack-only: remove sub-pixel z-fighting and frame-block coplanarity.
    * The unmodified drawRimGlow/drawRimCore/side stay in use without shaders.
    */
   public static void drawShaderRimGlow(boolean axisX, int edges, boolean positiveSide, double seconds, PixelPortalSurface.QuadSink sink) {
      float depth = positiveSide ? 0.75F : 0.25F;
      float pulse = (float)(0.94 + 0.06 * Math.sin(seconds * 1.35));
      int halo = color(24, 248, 239, (int)(71.0F * pulse));
      int glow = color(113, 255, 249, (int)(158.0F * pulse));
      for (int edge = 1; edge <= 8; edge <<= 1) {
         if ((edges & edge) != 0) {
            rim(sink, axisX, edge, 0.3125F, depth, halo);
            rim(sink, axisX, edge, 0.1875F, depth + (positiveSide ? RIM_SHADER_SPACING : -RIM_SHADER_SPACING), glow);
            float auraDepth = positiveSide ? 1.0F + RIM_SHADER_SPACING : -RIM_SHADER_SPACING;
            aura(sink, axisX, edge, 0.18F, auraDepth, color(39, 255, 241, 27));
            aura(sink, axisX, edge, 0.1F, auraDepth + (positiveSide ? RIM_SHADER_SIDE_INSET : -RIM_SHADER_SIDE_INSET), color(112, 255, 250, 43));
            sideShader(sink, axisX, edge, color(103, 244, 241, 108));
         }
      }
   }

   private static void aura(PixelPortalSurface.QuadSink sink, boolean axisX, int edge, float width, float depth, int color) {
      switch (edge) {
         case 1:
            face(sink, axisX, -width, 0.0F, width, 1.0F, depth, color);
            break;
         case 2:
            face(sink, axisX, 1.0F - width, 0.0F, 1.0F + width, 1.0F, depth, color);
         case 3:
         case 5:
         case 6:
         case 7:
         default:
            break;
         case 4:
            face(sink, axisX, 0.0F, -width, 1.0F, width, depth, color);
            break;
         case 8:
            face(sink, axisX, 0.0F, 1.0F - width, 1.0F, 1.0F + width, depth, color);
      }
   }

   private static void rim(PixelPortalSurface.QuadSink sink, boolean axisX, int edge, float width, float depth, int color) {
      switch (edge) {
         case 1:
            face(sink, axisX, 0.0F, 0.0F, width, 1.0F, depth, color);
            break;
         case 2:
            face(sink, axisX, 1.0F - width, 0.0F, 1.0F, 1.0F, depth, color);
         case 3:
         case 5:
         case 6:
         case 7:
         default:
            break;
         case 4:
            face(sink, axisX, 0.0F, 0.0F, 1.0F, width, depth, color);
            break;
         case 8:
            face(sink, axisX, 0.0F, 1.0F - width, 1.0F, 1.0F, depth, color);
      }
   }

   private static void side(PixelPortalSurface.QuadSink sink, boolean axisX, int edge, int color) {
      if (edge != 1 && edge != 2) {
         float v = edge == 4 ? 0.0F : 1.0F;
         quad(sink, axisX, edge == 8 ? axisX : !axisX, 0.0F, v, 0.25F, 1.0F, v, 0.25F, 1.0F, v, 0.75F, 0.0F, v, 0.75F, color);
      } else {
         float u = edge == 1 ? 0.0F : 1.0F;
         quad(sink, axisX, edge == 2 ? axisX : !axisX, u, 0.0F, 0.25F, u, 0.0F, 0.75F, u, 1.0F, 0.75F, u, 1.0F, 0.25F, color);
      }
   }

   private static void sideShader(PixelPortalSurface.QuadSink sink, boolean axisX, int edge, int color) {
      // Neighboring frame blocks are exactly coplanar with side() at u/v=0 or 1.
      // The tiny inward inset is applied only to the shaderpack rim.
      if (edge != 1 && edge != 2) {
         float v = edge == 4 ? RIM_SHADER_SIDE_INSET : 1.0F - RIM_SHADER_SIDE_INSET;
         quad(sink, axisX, edge == 8 ? axisX : !axisX, 0.0F, v, 0.25F, 1.0F, v, 0.25F, 1.0F, v, 0.75F, 0.0F, v, 0.75F, color);
      } else {
         float u = edge == 1 ? RIM_SHADER_SIDE_INSET : 1.0F - RIM_SHADER_SIDE_INSET;
         quad(sink, axisX, edge == 2 ? axisX : !axisX, u, 0.0F, 0.25F, u, 0.0F, 0.75F, u, 1.0F, 0.75F, u, 1.0F, 0.25F, color);
      }
   }

   private static void face(PixelPortalSurface.QuadSink sink, boolean axisX, float u0, float v0, float u1, float v1, float depth, int color) {
      quad(sink, axisX, axisX != depth >= 0.5F, u0, v0, depth, u1, v0, depth, u1, v1, depth, u0, v1, depth, color);
   }

   private static void quad(
      PixelPortalSurface.QuadSink sink,
      boolean axisX,
      boolean reverse,
      float u0,
      float v0,
      float d0,
      float u1,
      float v1,
      float d1,
      float u2,
      float v2,
      float d2,
      float u3,
      float v3,
      float d3,
      int color
   ) {
      if (reverse) {
         sink.quad(
            axisX ? u3 : d3,
            v3,
            axisX ? d3 : u3,
            axisX ? u2 : d2,
            v2,
            axisX ? d2 : u2,
            axisX ? u1 : d1,
            v1,
            axisX ? d1 : u1,
            axisX ? u0 : d0,
            v0,
            axisX ? d0 : u0,
            color
         );
      } else {
         sink.quad(
            axisX ? u0 : d0,
            v0,
            axisX ? d0 : u0,
            axisX ? u1 : d1,
            v1,
            axisX ? d1 : u1,
            axisX ? u2 : d2,
            v2,
            axisX ? d2 : u2,
            axisX ? u3 : d3,
            v3,
            axisX ? d3 : u3,
            color
         );
      }
   }

   public static int surfaceColor(double tone) {
      if (tone < 0.28) {
         return color(25, 156, 172, 150);
      } else if (tone < 0.4) {
         return color(40, 205, 204, 170);
      } else if (tone < 0.53) {
         return color(88, 234, 223, 182);
      } else {
         return tone < 0.65 ? color(153, 249, 239, 198) : color(216, 255, 251, 212);
      }
   }

   private static double smooth(double value) {
      return value * value * (3.0 - 2.0 * value);
   }

   private static int color(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static double hash(int x, int y, int seed) {
      int value = x * 521288629 ^ y * 1597334677 ^ seed;
      value ^= value >>> 16;
      value *= 2146121005;
      value ^= value >>> 15;
      return (value & 16777215) / 1.6777215E7;
   }

   @FunctionalInterface
   public interface QuadSink {
      void quad(
         float var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         float var10,
         float var11,
         float var12,
         int var13
      );
   }

   // Keep the accepted shader geometry above unchanged; only native output uses the 2.0.2 composition.
   public static void drawNativeSurface(boolean axisX, int worldU, int worldY, boolean positiveSide, double seconds, PixelPortalSurface.QuadSink sink) {
      partitionNative(sink, axisX, positiveSide, seconds, worldU, worldY, Math.floorDiv(worldU, 2), Math.floorDiv(worldY, 2), 0, 0, 4, 4, 1, 0);
   }

   private static void partitionNative(
      PixelPortalSurface.QuadSink sink,
      boolean axisX,
      boolean positive,
      double seconds,
      int worldU,
      int worldY,
      int macroU,
      int macroV,
      int u,
      int v,
      int width,
      int height,
      int node,
      int depth
   ) {
      double x = macroU * 2.0 + u * 0.5 - worldU;
      double y = macroV * 2.0 + v * 0.5 - worldY;
      if (!(x >= 1.0) && !(y >= 1.0) && !(x + width * 0.5 <= 0.0) && !(y + height * 0.5 <= 0.0)) {
         int seed = 1729 + node * 1931;
         boolean smallerTile = depth == 2 && width >= 2 && height >= 2 && hash(macroU, macroV, seed + 7) < 0.65;
         if ((depth < 2 || smallerTile) && (width > 1 || height > 1)) {
            boolean vertical = width > height || width == height && hash(macroU, macroV, seed) < 0.5;
            if (width == 1) {
               vertical = false;
            }

            if (height == 1) {
               vertical = true;
            }

            int size = vertical ? width : height;
            int cut = size / 2;
            if (depth == 0 && hash(macroU, macroV, seed + 11) < 0.22) {
               cut = hash(macroU, macroV, seed + 13) < 0.5 ? 1 : size - 1;
            }

            partitionNative(
               sink, axisX, positive, seconds, worldU, worldY, macroU, macroV, u, v, vertical ? cut : width, vertical ? height : cut, node * 2, depth + 1
            );
            partitionNative(
               sink,
               axisX,
               positive,
               seconds,
               worldU,
               worldY,
               macroU,
               macroV,
               vertical ? u + cut : u,
               vertical ? v : v + cut,
               vertical ? width - cut : width,
               vertical ? height : height - cut,
               node * 2 + 1,
               depth + 1
            );
         } else {
            double tone = 0.82 * field((macroU * 2.0 + (u + width * 0.5) * 0.5) / 4.0, (macroV * 2.0 + (v + height * 0.5) * 0.5) / 4.0)
               + 0.18 * hash(macroU, macroV, seed + 53);
            int rgba = surfaceColor(tone);
            double phase = seconds / (6.5 + hash(macroU, macroV, 71) * 3.0) + hash(macroU, macroV, 97) * 0.25 + hash(macroU, macroV, seed + 71);
            float sheet = positive ? 0.56F : 0.44F;
            int tileColor = over(fadeColor(rgba, phase), BACKING);
            if (width * height >= 2 && hash(macroU, macroV, seed + 101) < 0.5) {
               double accentWidth = hash(macroU, macroV, seed + 113) < 0.5 ? 0.25 : 0.5;
               double accentHeight = hash(macroU, macroV, seed + 127) < 0.5 ? 0.25 : 0.5;
               double ax = x + (hash(macroU, macroV, seed + 131) < 0.5 ? 0.0 : width * 0.5 - accentWidth);
               double ay = y + (hash(macroU, macroV, seed + 137) < 0.5 ? 0.0 : height * 0.5 - accentHeight);
               int accentColor = fadeColor(color(196, 255, 246, 100), phase + 0.12);
               tile(sink, axisX, ax, ay, accentWidth, accentHeight, sheet, over(accentColor, tileColor));
               double otherX = ax == x ? x + accentWidth : x;
               double otherY = ay == y ? y + accentHeight : y;
               tile(sink, axisX, otherX, y, width * 0.5 - accentWidth, height * 0.5, sheet, tileColor);
               tile(sink, axisX, ax, otherY, accentWidth, height * 0.5 - accentHeight, sheet, tileColor);
            } else {
               tile(sink, axisX, x, y, width * 0.5, height * 0.5, sheet, tileColor);
            }
         }
      }
   }

   private static int over(int foreground, int background) {
      int frontAlpha = foreground >>> 24;
      int backAlpha = (background >>> 24) * (255 - frontAlpha) / 255;
      int alpha = frontAlpha + backAlpha;
      return color(
         blendChannel(foreground, background, frontAlpha, backAlpha, alpha, 16),
         blendChannel(foreground, background, frontAlpha, backAlpha, alpha, 8),
         blendChannel(foreground, background, frontAlpha, backAlpha, alpha, 0),
         alpha
      );
   }

   private static int blendChannel(int foreground, int background, int frontAlpha, int backAlpha, int alpha, int shift) {
      return ((foreground >>> shift & 0xFF) * frontAlpha + (background >>> shift & 0xFF) * backAlpha) / alpha;
   }
}
