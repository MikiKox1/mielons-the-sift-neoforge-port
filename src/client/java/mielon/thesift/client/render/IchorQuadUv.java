package mielon.thesift.client.render;

public final class IchorQuadUv {
   private final int blockX;
   private final int blockY;
   private final int blockZ;
   private final float baseU;
   private final float baseV;
   private final float[] x = new float[4];
   private final float[] y = new float[4];
   private final float[] z = new float[4];

   public IchorQuadUv(int blockX, int blockY, int blockZ) {
      this.blockX = blockX;
      this.blockY = blockY;
      this.blockZ = blockZ;
      this.baseU = Math.floorMod(blockX, 16) / 16.0F;
      this.baseV = Math.floorMod(blockZ, 16) / 16.0F;
   }

   public void sectionVertex(int i, float vx, float vy, float vz) {
      this.blockVertex(i, blockLocal(vx, this.blockX), blockLocal(vy, this.blockY), blockLocal(vz, this.blockZ));
   }

   public static float blockLocal(float coordinate, int blockCoordinate) {
      float local = coordinate - (blockCoordinate & 15);
      return local - 16.0F * Math.round(local / 16.0F);
   }

   public void blockVertex(int i, float vx, float vy, float vz) {
      this.x[i] = snap(vx);
      this.y[i] = vy;
      this.z[i] = snap(vz);
   }

   public float u(int i) {
      if (spans(this.x) && spans(this.z)) {
         return this.baseU + this.x[i] / 16.0F;
      }

      float minX = min(this.x);
      float minZ = min(this.z);
      int origin = this.blockX + this.blockZ + Math.round(minX + minZ);
      return (Math.floorMod(origin, 16) + this.x[i] - minX + this.z[i] - minZ) / 16.0F;
   }

   public float v(int i) {
      return spans(this.x) && spans(this.z) ? this.baseV + this.z[i] / 16.0F : (Math.floorMod(-this.blockY - 1, 16) + 1 - snap(this.y[i])) / 16.0F;
   }

   private static float min(float[] values) {
      float result = values[0];

      for (int i = 1; i < 4; i++) {
         result = Math.min(result, values[i]);
      }

      return result;
   }

   private static boolean spans(float[] values) {
      float min = values[0];
      float max = values[0];

      for (int i = 1; i < 4; i++) {
         min = Math.min(min, values[i]);
         max = Math.max(max, values[i]);
      }

      return max - min > 0.5F;
   }

   private static float snap(float v) {
      return v < 0.01F ? 0.0F : (v > 0.99F ? 1.0F : v);
   }
}
