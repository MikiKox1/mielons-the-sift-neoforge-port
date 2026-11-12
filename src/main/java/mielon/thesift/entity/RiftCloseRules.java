package mielon.thesift.entity;

/** Pure 2.0.3 closing rules, invoked after the live server/player guard. */
public final class RiftCloseRules {
   private RiftCloseRules() {}

   public static boolean canClose(boolean creative, boolean attackable) {
      return creative && attackable;
   }

   public static long closeAt(long worldTime) {
      return worldTime + 16L;
   }
}
