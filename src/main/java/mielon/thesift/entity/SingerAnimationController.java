package mielon.thesift.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.AnimationController.AnimationStateHandler;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import java.util.function.DoubleSupplier;

public final class SingerAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
   private final DoubleSupplier phaseTick;
   private double phaseStartTick;
   private double phaseStartAge;

   public SingerAnimationController(String name, DoubleSupplier phaseTick, AnimationStateHandler<T> handler) {
      super(name, 0, handler);
      this.phaseTick = phaseTick;
   }

   @Override
   protected void initializeNewAnimation(T entity, GeoRenderState state, GeoModel<T> model, double speed, int transition) {
      super.initializeNewAnimation(entity, state, model, speed, transition);
      this.phaseStartTick = this.phaseTick.getAsDouble();
      this.phaseStartAge = Math.floor(this.lastAnimatableAge);
      if (this.synchronizePhase()) {
         this.animationPoint = this.timeline.createAnimationPoint(this.timelineTime, this.animationPoint, this.easingOverride);
      }
   }

   @Override
   protected void progressExistingAnimation(T entity, GeoRenderState state, double previousTime, double delta) {
      this.synchronizePhase();
      super.progressExistingAnimation(entity, state, previousTime, delta);
   }

   private boolean synchronizePhase() {
      if (!(this.phaseStartTick < 0.0) && this.timeline != null) {
         this.timelineTime = Math.min(
            (this.phaseStartTick + Math.max(0.0, this.lastAnimatableAge - this.phaseStartAge)) / 20.0,
            Math.max(0.0, this.timeline.lastAnimationEndTime() - 1.0E-6)
         );
         return true;
      } else {
         return false;
      }
   }
}
