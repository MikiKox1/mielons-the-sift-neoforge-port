package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class SculkersLairStructure extends Structure {
   public static final MapCodec<SculkersLairStructure> CODEC = simpleCodec(SculkersLairStructure::new);

   public SculkersLairStructure(StructureSettings settings) {
      super(settings);
   }

   @Override
   public StructureType<?> type() {
      return ModWorldgen.SCULKERS_LAIR;
   }

   @Override
   public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
      WorldgenRandom random = context.random();
      BlockPos origin = new BlockPos(context.chunkPos().getMinBlockX(), 24 + random.nextInt(17), context.chunkPos().getMinBlockZ());
      int[] corridors = permutation(6, random);
      int[] connectors = permutation(4, random);
      int[] rooms = permutation(6, random);
      Reference<StructureProcessorList> processors = context.registryAccess()
         .lookupOrThrow(Registries.PROCESSOR_LIST)
         .getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, Identifier.fromNamespaceAndPath("the_sift", "sculkers_lair")));
      StructureTemplateManager manager = context.structureTemplateManager();
      List<PoolElementStructurePiece> pieces = new ArrayList<>();
      BiFunction<String, Boolean, StructurePoolElement> element = (stem, crowded) -> StructurePoolElement.single(
            "the_sift:sculkers_lair/parts/" + stem + (crowded ? "_crowded" : ""), processors
         )
         .apply(Projection.RIGID);
      StructurePoolElement first = element.apply("first_corridor_" + corridors[0], random.nextBoolean());
      Rotation rotation = Rotation.getRandom(random);
      PoolElementStructurePiece root = new PoolElementStructurePiece(
         manager, first, origin, first.getGroundLevelDelta(), rotation, first.getBoundingBox(manager, origin, rotation), LiquidSettings.IGNORE_WATERLOGGING
      );
      pieces.add(root);
      join(context, pieces, 0, new BlockPos(4, 6, 16), Direction.SOUTH, element.apply("first_corridor_ending_1", false), new BlockPos(5, 6, 0), Direction.NORTH);
      join(
         context,
         pieces,
         0,
         new BlockPos(4, 5, 0),
         Direction.NORTH,
         element.apply("first_connector_" + connectors[0], random.nextBoolean()),
         new BlockPos(5, 5, 11),
         Direction.SOUTH
      );
      join(
         context,
         pieces,
         2,
         new BlockPos(11, 5, 6),
         Direction.EAST,
         element.apply("second_corridor_" + corridors[1], random.nextBoolean()),
         new BlockPos(5, 5, 16),
         Direction.SOUTH
      );
      join(
         context,
         pieces,
         3,
         new BlockPos(4, 5, 0),
         Direction.NORTH,
         element.apply("third_corridor_" + corridors[2], random.nextBoolean()),
         new BlockPos(5, 5, 16),
         Direction.SOUTH
      );
      join(
         context,
         pieces,
         4,
         new BlockPos(6, 5, 0),
         Direction.NORTH,
         element.apply("second_connector_" + connectors[1], random.nextBoolean()),
         new BlockPos(6, 5, 11),
         Direction.SOUTH
      );
      join(
         context,
         pieces,
         5,
         new BlockPos(0, 5, 6),
         Direction.WEST,
         element.apply("monarch_room_1", random.nextBoolean()),
         new BlockPos(18, 5, 27),
         Direction.SOUTH
      );

      for (int index = 0; index < 3; index++) {
         boolean mirrored = index == 2;
         join(
            context,
            pieces,
            new int[]{0, 3, 4}[index],
            new BlockPos(mirrored ? 10 : 0, 5, 8),
            mirrored ? Direction.EAST : Direction.WEST,
            element.apply("mini_room_" + rooms[index] + (index == 0 ? "_treasure" : ""), random.nextBoolean()),
            new BlockPos(17, 5, 8),
            Direction.EAST
         );
      }

      for (int i = 0; i < pieces.size(); i++) {
         for (int j = i + 1; j < pieces.size(); j++) {
            if (pieces.get(i).getBoundingBox().intersects(pieces.get(j).getBoundingBox())) {
               return Optional.empty();
            }
         }
      }

      return Optional.of(new GenerationStub(origin, builder -> pieces.forEach(builder::addPiece)));
   }

   private static int[] permutation(int size, RandomSource random) {
      int[] result = new int[size];

      for (int i = 0; i < size; i++) {
         result[i] = i + 1;
      }

      for (int i = size - 1; i > 0; i--) {
         int j = random.nextInt(i + 1);
         int t = result[i];
         result[i] = result[j];
         result[j] = t;
      }

      return result;
   }

   private static void join(
      GenerationContext context,
      List<PoolElementStructurePiece> pieces,
      int parentIndex,
      BlockPos exit,
      Direction facing,
      StructurePoolElement element,
      BlockPos entrance,
      Direction entranceFacing
   ) {
      PoolElementStructurePiece parent = pieces.get(parentIndex);
      Direction worldFacing = parent.getRotation().rotate(facing);
      Rotation rotation = Rotation.NONE;

      for (Rotation candidate : Rotation.values()) {
         if (candidate.rotate(entranceFacing) == worldFacing.getOpposite()) {
            rotation = candidate;
            break;
         }
      }

      BlockPos origin = parent.getPosition().offset(exit.rotate(parent.getRotation())).relative(worldFacing).subtract(entrance.rotate(rotation));
      StructureTemplateManager manager = context.structureTemplateManager();
      pieces.add(
         new PoolElementStructurePiece(
            manager,
            element,
            origin,
            element.getGroundLevelDelta(),
            rotation,
            element.getBoundingBox(manager, origin, rotation),
            LiquidSettings.IGNORE_WATERLOGGING
         )
      );
   }
}
