package mielon.thesift.entity.ai;

import mielon.thesift.entity.MonarchEntity;
import mielon.thesift.fluid.IchorRegeneration;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class MonarchNavigation extends GroundPathNavigation {
   public MonarchNavigation(Mob mob, Level level) {
      super(mob, level);
   }

   @Override
   protected double getGroundY(Vec3 point) {
      return IchorRegeneration.isTouching(this.mob) ? point.y : super.getGroundY(point);
   }

   @Override
   protected void followThePath() {
      if (this.path != null && !this.path.isDone()) {
         for (int skip = Math.min(this.path.getNodeCount() - 1, this.path.getNextNodeIndex() + 8); skip > this.path.getNextNodeIndex() + 1; skip--) {
            Vec3 point = this.path.getEntityPosAtNode(this.mob, skip);
            if (point.distanceToSqr(this.mob.position()) < 64.0 && ((MonarchEntity)this.mob).canWalkDirectly(this.mob.position(), point)) {
               this.path.setNextNodeIndex(skip);
               break;
            }
         }

         Vec3 position = this.getTempMobPos();
         Vec3 waypoint = this.path.getNextEntityPos(this.mob);
         this.maxDistanceToWaypoint = Math.max(0.3F, Math.min(0.55F, this.mob.getBbWidth() * 0.5F));
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

         if ((reached || passed) && rise > -0.65 && rise < Math.max(1.75, this.mob.maxUpStep() + 0.25)) {
            this.path.advance();
         }

         this.doStuckDetection(position);
      }
   }
}
