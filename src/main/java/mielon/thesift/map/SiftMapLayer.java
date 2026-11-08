package mielon.thesift.map;

import java.util.Map;
import java.util.WeakHashMap;
import mielon.thesift.network.MapColorsPayload;
import net.minecraft.server.level.ServerPlayer;

public final class SiftMapLayer {
   public static final int SIZE = 16384;
   private int[] pixels;
   private int populated;
   private long revision;
   private long encodedRevision = -1L;
   private byte[] encoded = new byte[0];
   private final Map<ServerPlayer, Long> sent = new WeakHashMap<>();

   public synchronized boolean set(int x, int y, int color) {
      if (this.pixels == null && color == 0) {
         return false;
      }

      if (this.pixels == null) {
         this.pixels = new int[16384];
      }

      int index = x + y * 128;
      int old = this.pixels[index];
      if (old == color) {
         return false;
      }

      if (old == 0) {
         this.populated++;
      }

      if (color == 0) {
         this.populated--;
      }

      this.pixels[index] = color;
      if (this.populated == 0) {
         this.pixels = null;
      }

      this.revision++;
      return true;
   }

   public synchronized int get(int index) {
      return this.pixels == null ? 0 : this.pixels[index];
   }

   public synchronized int[] snapshot() {
      return this.pixels == null ? new int[0] : (int[])this.pixels.clone();
   }

   public synchronized void load(int[] values) {
      if (values.length == 0 || values.length == 16384) {
         this.pixels = values.length == 0 ? null : (int[])values.clone();
         this.populated = 0;
         if (this.pixels != null) {
            for (int color : this.pixels) {
               if (color != 0) {
                  this.populated++;
               }
            }
         }

         if (this.populated == 0) {
            this.pixels = null;
         }

         this.revision++;
      }
   }

   public synchronized void send(ServerPlayer player, int mapId) {
      Long previous = this.sent.get(player);
      if (previous == null || previous != this.revision) {
         if (previous != null || this.pixels != null) {
            if (this.encodedRevision != this.revision) {
               this.encoded = SiftMapEncoding.encode(this.snapshot());
               this.encodedRevision = this.revision;
            }

            MapColorsPayload.send(player, mapId, this.encoded);
            this.sent.put(player, this.revision);
         }
      }
   }
}
