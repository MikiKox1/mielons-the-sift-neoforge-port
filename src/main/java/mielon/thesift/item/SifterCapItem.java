package mielon.thesift.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.util.GeckoLibUtil;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

public final class SifterCapItem extends Item implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private static Supplier<?> rendererFactory;
   private static Object renderProvider;

   public SifterCapItem(Properties properties) {
      super(properties);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public static void installRenderer(Supplier<?> factory) {
      rendererFactory = factory;
   }

   @Override
   public Object getRenderProvider() {
      if (renderProvider == null && rendererFactory != null) {
         renderProvider = rendererFactory.get();
      }

      return renderProvider;
   }
}
