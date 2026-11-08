package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import mielon.thesift.block.OvergrownWillowVinesBlock;
import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class CampCoralVinesProcessor extends StructureProcessor {
   public static final MapCodec<CampCoralVinesProcessor> CODEC = MapCodec.unit(CampCoralVinesProcessor::new);

   @Override
   protected StructureProcessorType<?> getType() {
      return ModWorldgen.CAMP_CORAL_VINES;
   }

   @Override
   public List<StructureBlockInfo> finalizeProcessing(
      ServerLevelAccessor level,
      BlockPos origin,
      BlockPos pivot,
      List<StructureBlockInfo> original,
      List<StructureBlockInfo> processed,
      StructurePlaceSettings settings
   ) {
      HashMap<BlockPos, BlockState> states = new HashMap<>();
      List<StructureBlockInfo> result = new ArrayList<>();
      List<StructureBlockInfo> vines = new ArrayList<>();

      for (StructureBlockInfo info : processed) {
         states.put(info.pos(), info.state());
         if (info.state().getBlock() instanceof OvergrownWillowVinesBlock) {
            vines.add(info);
         } else {
            result.add(info);
         }
      }

      for (int i = 0; i < result.size(); i++) {
         StructureBlockInfo info = result.get(i);
         BlockState state = info.state();
         if (info.nbt() != null && info.nbt().contains("LootTable") && IchorWaterlogging.canContain(state)) {
            boolean immersed = IchorWaterlogging.isIchor(level.getFluidState(info.pos()).getType());

            for (Direction direction : Plane.HORIZONTAL) {
               BlockState adjacent = states.get(info.pos().relative(direction));
               immersed |= adjacent != null && IchorWaterlogging.isIchor(adjacent.getFluidState().getType());
            }

            if (immersed) {
               result.set(i, new StructureBlockInfo(info.pos(), IchorWaterlogging.fillState(state), info.nbt()));
            }
         }
      }

      vines.sort(Comparator.<StructureBlockInfo>comparingInt(info -> info.pos().getY()).reversed());

      for (StructureBlockInfo vine : vines) {
         BlockState below = states.getOrDefault(vine.pos().below(), Blocks.AIR.defaultBlockState());
         BlockState state = OvergrownWillowVinesBlock.refreshBottomFaces(vine.state(), below);
         result.add(new StructureBlockInfo(vine.pos(), state, vine.nbt()));
      }

      return result;
   }
}
