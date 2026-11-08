package mielon.thesift.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class SiftMapEncoding {
   public static final int MAX_PACKET_BYTES = 131072;

   private SiftMapEncoding() {
   }

   public static byte[] encode(int[] colors) {
      if (colors.length == 0) {
         return new byte[0];
      }

      if (colors.length != 16384) {
         throw new IllegalArgumentException("Invalid map layer size");
      }

      try {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();

         try (DataOutputStream output = new DataOutputStream(new DeflaterOutputStream(bytes))) {
            for (int color : colors) {
               output.writeInt(color);
            }
         }

         return bytes.toByteArray();
      } catch (IOException failure) {
         throw new IllegalStateException("Could not encode map colors", failure);
      }
   }

   public static int[] decode(byte[] bytes) throws IOException {
      if (bytes.length == 0) {
         return new int[0];
      }

      if (bytes.length > 131072) {
         throw new IOException("Oversized map color packet");
      }

      int[] colors = new int[16384];

      try (DataInputStream input = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)))) {
         for (int i = 0; i < colors.length; i++) {
            colors[i] = input.readInt();
         }

         if (input.read() != -1) {
            throw new IOException("Oversized map color layer");
         }
      }

      return colors;
   }
}
