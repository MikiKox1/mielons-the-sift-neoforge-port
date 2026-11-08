package mielon.thesift.client.light;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior.BoundingBox;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import mielon.thesift.entity.EchoGolemEntity;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class TheSiftDynamicLightsInitializer implements DynamicLightsInitializer {
   @Override
   public void onInitializeDynamicLights(DynamicLightsContext context) {
      DynamicLightBehaviorManager manager = context.dynamicLightBehaviorManager();
      SingerSoulLightBridge.install(new TheSiftDynamicLightsInitializer.SingerLightHandler(manager));
      EchoGolemLightBridge.install(new TheSiftDynamicLightsInitializer.EchoLightHandler(manager));
   }

   private static final class EchoLightBehavior implements DynamicLightBehavior {
      private static final double LUMINANCE = 14.0;
      private final WeakReference<EchoGolemEntity> golem;
      private double x;
      private double y;
      private double z;
      private double previousX = Double.NaN;
      private double previousY = Double.NaN;
      private double previousZ = Double.NaN;

      private EchoLightBehavior(EchoGolemEntity golem, Vec3 position) {
         this.golem = new WeakReference<>(golem);
         this.setPosition(position);
      }

      private void setPosition(Vec3 position) {
         this.x = position.x;
         this.y = position.y;
         this.z = position.z;
      }

      @Override
      public double lightAtPos(BlockPos pos, double falloffRatio) {
         EchoGolemEntity entity = this.golem.get();
         if (entity != null && entity.hasSoulBlock()) {
            double dx = pos.getX() + 0.5 - this.x;
            double dy = pos.getY() + 0.5 - this.y;
            double dz = pos.getZ() + 0.5 - this.z;
            return Math.max(14.0 - Math.sqrt(dx * dx + dy * dy + dz * dz) * falloffRatio, 0.0);
         } else {
            return 0.0;
         }
      }

      @Override
      public BoundingBox getBoundingBox() {
         int blockX = Mth.floor(this.x);
         int blockY = Mth.floor(this.y);
         int blockZ = Mth.floor(this.z);
         return new BoundingBox(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
      }

      @Override
      public boolean hasChanged() {
         if (Double.compare(this.x, this.previousX) == 0 && Double.compare(this.y, this.previousY) == 0 && Double.compare(this.z, this.previousZ) == 0) {
            return false;
         }

         this.previousX = this.x;
         this.previousY = this.y;
         this.previousZ = this.z;
         return true;
      }

      @Override
      public boolean isRemoved() {
         EchoGolemEntity entity = this.golem.get();
         return entity == null || entity.isRemoved() || !entity.hasSoulBlock();
      }
   }

   private static final class EchoLightHandler implements EchoGolemLightBridge.Handler {
      private final DynamicLightBehaviorManager manager;
      private final Map<EchoGolemEntity, TheSiftDynamicLightsInitializer.EchoLightBehavior> sources = new WeakHashMap<>();

      private EchoLightHandler(DynamicLightBehaviorManager manager) {
         this.manager = manager;
      }

      @Override
      public void update(EchoGolemEntity golem, Vec3 position) {
         TheSiftDynamicLightsInitializer.EchoLightBehavior source = this.sources.get(golem);
         if (!golem.hasSoulBlock()) {
            if (source != null) {
               this.manager.remove(source);
               this.sources.remove(golem);
            }
         } else {
            if (source != null && !source.isRemoved()) {
               source.setPosition(position);
            } else {
               source = new TheSiftDynamicLightsInitializer.EchoLightBehavior(golem, position);
               this.sources.put(golem, source);
               this.manager.add(source);
            }
         }
      }
   }

   private static final class SingerLightHandler implements SingerSoulLightBridge.Handler {
      private final DynamicLightBehaviorManager manager;
      private final Map<SingerEntity, TheSiftDynamicLightsInitializer.SingerSoulLightBehavior> sources = new WeakHashMap<>();

      private SingerLightHandler(DynamicLightBehaviorManager manager) {
         this.manager = manager;
      }

      @Override
      public void update(SingerEntity singer, Vec3 position) {
         TheSiftDynamicLightsInitializer.SingerSoulLightBehavior source = this.sources.get(singer);
         if (!singer.isHoldingSoulBlock()) {
            if (source != null) {
               this.manager.remove(source);
               this.sources.remove(singer);
            }
         } else {
            if (source != null && !source.isRemoved()) {
               source.setPosition(position);
            } else {
               source = new TheSiftDynamicLightsInitializer.SingerSoulLightBehavior(singer, position);
               this.sources.put(singer, source);
               this.manager.add(source);
            }
         }
      }
   }

   private static final class SingerSoulLightBehavior implements DynamicLightBehavior {
      private static final double LUMINANCE = 15.0;
      private boolean removed;
      private final WeakReference<SingerEntity> singer;
      private double x;
      private double y;
      private double z;
      private double previousX = Double.NaN;
      private double previousY = Double.NaN;
      private double previousZ = Double.NaN;

      private SingerSoulLightBehavior(SingerEntity singer, Vec3 position) {
         this.singer = new WeakReference<>(singer);
         this.setPosition(position);
      }

      private void setPosition(Vec3 position) {
         this.x = position.x;
         this.y = position.y;
         this.z = position.z;
      }

      @Override
      public double lightAtPos(BlockPos pos, double falloffRatio) {
         SingerEntity entity = this.singer.get();
         if (entity != null && !entity.isRemoved() && entity.isHoldingSoulBlock()) {
            double dx = pos.getX() + 0.5 - this.x;
            double dy = pos.getY() + 0.5 - this.y;
            double dz = pos.getZ() + 0.5 - this.z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            return Math.max(15.0 - Math.sqrt(distanceSquared) * falloffRatio, 0.0);
         } else {
            return 0.0;
         }
      }

      @Override
      public BoundingBox getBoundingBox() {
         int blockX = Mth.floor(this.x);
         int blockY = Mth.floor(this.y);
         int blockZ = Mth.floor(this.z);
         return new BoundingBox(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
      }

      @Override
      public boolean hasChanged() {
         if (Double.compare(this.x, this.previousX) == 0 && Double.compare(this.y, this.previousY) == 0 && Double.compare(this.z, this.previousZ) == 0) {
            return false;
         }

         this.previousX = this.x;
         this.previousY = this.y;
         this.previousZ = this.z;
         return true;
      }

      @Override
      public boolean isRemoved() {
         SingerEntity entity = this.singer.get();
         this.removed = this.removed | (entity == null || entity.isRemoved() || !entity.isHoldingSoulBlock());
         return this.removed;
      }
   }
}
