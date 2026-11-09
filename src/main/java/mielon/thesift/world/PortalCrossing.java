package mielon.thesift.world;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Mielon's 2.0.2 swept trigger geometry, shared with no-game regression tests. */
public final class PortalCrossing {
   private PortalCrossing() {
   }

   public static boolean isUsableMove(Vec3 from, Vec3 to) {
      double distance = from.distanceToSqr(to);
      return Double.isFinite(distance) && distance > 0.0 && distance <= 400.0;
   }

   public static boolean intersects(AABB portal, double width, double height, Vec3 from, Vec3 to) {
      AABB trigger = portal.inflate(width * 0.5, 0.0, width * 0.5).expandTowards(0.0, -height, 0.0);
      return !trigger.contains(from) && trigger.clip(from, to).isPresent();
   }
}
