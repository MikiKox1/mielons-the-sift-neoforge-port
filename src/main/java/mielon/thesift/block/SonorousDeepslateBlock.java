package mielon.thesift.block;

import java.util.Map;
import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SonorousDeepslateBlock extends BaseEntityBlock {
   public static final EnumProperty<SonorousDeepslateBlock.Mode> MODE = EnumProperty.create("mode", SonorousDeepslateBlock.Mode.class);

   public SonorousDeepslateBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(MODE, SonorousDeepslateBlock.Mode.HORN));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(MODE);
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
      ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
      if (stack.isEmpty()) {
         stack = new ItemStack(ModBlocks.SONOROUS_DEEPSLATE_ITEM);
      }

      stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(MODE.getName(), state.getValue(MODE).getSerializedName())));
      return stack;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SonorousDeepslateBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModBlocks.SONOROUS_DEEPSLATE_BLOCK_ENTITY, SonorousDeepslateBlockEntity::tick);
   }

   public enum Mode implements StringRepresentable {
      HORN("horn"),
      NOTE("note");

      private final String name;

      Mode(String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
