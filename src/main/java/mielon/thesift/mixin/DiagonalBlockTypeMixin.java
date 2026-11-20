package mielon.thesift.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import mielon.thesift.fluid.IchorState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockTypeImpl", remap = false)
public abstract class DiagonalBlockTypeMixin {
   @WrapOperation(method = "isTarget", at = @At(value = "INVOKE", target = "Ljava/util/Collection;size()I"), remap = false)
   private int theSift$countBaseProperties(Collection<?> properties, Operation<Integer> original) {
      int count = original.call(properties);
      return properties.contains(IchorState.ICHORLOGGED) ? count - 1 : count;
   }
}

