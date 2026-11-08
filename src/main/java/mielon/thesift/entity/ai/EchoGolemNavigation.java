package mielon.thesift.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EchoGolemNavigation extends GroundPathNavigation {
   public EchoGolemNavigation(Mob mob, Level level) {
      super(mob, level);
   }

   @Override
   protected void followThePath() {
      Vec3 position = this.getTempMobPos();
      Vec3 waypoint = this.path.getNextEntityPos(this.mob);
      float footprintWidth = (int)(this.mob.getBbWidth() + 1.0F);
      float clearance = (footprintWidth - this.mob.getBbWidth()) * 0.5F;
      this.maxDistanceToWaypoint = Math.max(0.1F, Math.min(0.3F, clearance - 0.05F));
      double rise = this.mob.getY() - waypoint.y;
      boolean reached = Math.abs(this.mob.getX() - waypoint.x) <= this.maxDistanceToWaypoint
         && Math.abs(this.mob.getZ() - waypoint.z) <= this.maxDistanceToWaypoint;
      boolean passed = false;
      if (this.path.getNextNodeIndex() + 1 < this.path.getNodeCount()) {
         Vec3 next = this.path.getEntityPosAtNode(this.mob, this.path.getNextNodeIndex() + 1);
         double dx = next.x - waypoint.x;
         double dz = next.z - waypoint.z;
         double along = (this.mob.getX() - waypoint.x) * dx + (this.mob.getZ() - waypoint.z) * dz;
         double cross = (this.mob.getX() - waypoint.x) * dz - (this.mob.getZ() - waypoint.z) * dx;
         passed = along > 0.0
            && cross * cross < 0.45 * (dx * dx + dz * dz)
            && waypoint.distanceToSqr(this.mob.position()) < 4.0
            && this.level.noCollision(this.mob, this.mob.getBoundingBox().expandTowards(new Vec3(dx, 0.0, dz).normalize().scale(0.4)).deflate(0.01));
      }

      double maxRise = this.mob.onGround() ? Math.max(0.65, this.mob.maxUpStep() + 0.05) : 0.65;
      if ((reached || passed) && rise > -0.65 && rise < maxRise) {
         this.path.advance();
      }

      this.doStuckDetection(position);
   }
}
