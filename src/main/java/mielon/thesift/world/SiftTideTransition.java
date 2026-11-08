package mielon.thesift.world;

public final class SiftTideTransition {
   private static final int EDGE = 60;
   private static final int RING = 180;
   private int position;
   private final int targetPosition;
   private final int direction;
   private final long target;
   private long time;

   public SiftTideTransition(long current, long target) {
      this.position = coordinate(current);
      this.targetPosition = coordinate(target);
      int distance = Math.floorMod(this.targetPosition - this.position + 90, 180) - 90;
      this.direction = Integer.signum(distance);
      this.target = target;
      this.time = this.done() ? target : current;
   }

   private static int coordinate(long ticks) {
      int tick = (int)Math.floorMod(ticks, 25200L);
      return (tick / 8400 * 60 + Math.max(0, tick % 8400 - 8340)) % 180;
   }

   public long advance() {
      if (!this.done()) {
         this.position = Math.floorMod(this.position + this.direction, 180);
      }

      int progress = this.position % 60;
      this.time = this.done() ? this.target : (progress == 0 ? this.position / 60 * 8400 : (this.position / 60 + 1L) * 8400L - 60L + progress);
      return this.time;
   }

   public boolean done() {
      return this.position == this.targetPosition;
   }

   public long time() {
      return this.time;
   }
}
