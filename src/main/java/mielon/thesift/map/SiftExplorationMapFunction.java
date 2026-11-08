package mielon.thesift.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class SiftExplorationMapFunction implements LootItemFunction {
   public final SiftExplorationMaps.Kind destination;
   public static final MapCodec<SiftExplorationMapFunction> CODEC = Codec.STRING.comapFlatMap(value -> {
      try {
         return DataResult.success(SiftExplorationMaps.byId(value));
      } catch (IllegalArgumentException error) {
         return DataResult.error(error::getMessage);
      }
   }, kind -> kind.structureId).fieldOf("destination").xmap(SiftExplorationMapFunction::new, function -> function.destination);

   public SiftExplorationMapFunction(SiftExplorationMaps.Kind destination) {
      this.destination = destination;
   }

   @Override
   public MapCodec<SiftExplorationMapFunction> codec() {
      return CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN);
   }

   public ItemStack apply(ItemStack stack, LootContext context) {
      if (stack.isEmpty()) {
         return stack;
      }

      Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
      return origin == null ? ItemStack.EMPTY : SiftExplorationMaps.create(context.getLevel(), this.destination, BlockPos.containing(origin));
   }
}
