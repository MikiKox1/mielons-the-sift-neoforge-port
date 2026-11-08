package mielon.thesift.entity;

import mielon.thesift.item.ModItems;
import mielon.thesift.platform.ModRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;

public final class ModEntities {
   public static final EntityType<SingerEntity> SINGER = register("singer", Builder.of(SingerEntity::new, MobCategory.MISC).sized(0.8F, 4.0F));
   public static final EntityType<EchoGolemEntity> ECHO_GOLEM = register(
      "echo_golem", Builder.of(EchoGolemEntity::new, MobCategory.CREATURE).sized(1.15F, 1.65F).eyeHeight(1.17F).clientTrackingRange(10)
   );
   public static final EntityType<DarkSnifferEntity> DARK_SNIFFER = register(
      "dark_sniffer",
      Builder.of(DarkSnifferEntity::new, MobCategory.MONSTER)
         .sized(1.9F, 1.75F)
         .eyeHeight(1.05F)
         .passengerAttachments(2.09375F)
         .nameTagOffset(2.05F)
         .clientTrackingRange(10)
   );
   public static final EntityType<BlubEntity> BLUB = register(
      "blub", Builder.of(BlubEntity::new, MobCategory.CREATURE).sized(0.665F, 0.875F).eyeHeight(0.4375F).clientTrackingRange(10)
   );
   public static final EntityType<SeedlingEntity> SEEDLING = register(
      "seedling", Builder.of(SeedlingEntity::new, MobCategory.CREATURE).sized(0.71999997F, 0.76F).eyeHeight(0.57600003F).clientTrackingRange(10)
   );
   public static final EntityType<ScavengerEntity> SCAVENGER = register(
      "scavenger",
      Builder.of(ScavengerEntity::new, MobCategory.MONSTER)
         .notInPeaceful()
         .sized(1.19F, 1.2750001F)
         .eyeHeight(1.0625F)
         .clientTrackingRange(10)
         .updateInterval(1)
   );
   public static final EntityType<ScavengerProjectileEntity> SCAVENGER_PROJECTILE = register(
      "scavenger_projectile", Builder.of(ScavengerProjectileEntity::new, MobCategory.MISC).sized(0.3125F, 0.3125F).clientTrackingRange(8).updateInterval(1)
   );
   public static final EntityType<MonarchEntity> MONARCH = register(
      "monarch", Builder.of(MonarchEntity::new, MobCategory.MONSTER).sized(1.45F, 4.8125F).eyeHeight(4.5F).clientTrackingRange(16).updateInterval(1)
   );
   public static final EntityType<MonarchEntity> MONARCH_ECHO = register(
      "monarch_echo", Builder.of(MonarchEntity::new, MobCategory.MONSTER).sized(1.45F, 4.8125F).eyeHeight(4.5F).clientTrackingRange(16).updateInterval(1)
   );
   public static final EntityType<MonarchProjectileEntity> MONARCH_SUMMON_PROJECTILE = register(
      "monarch_summon_projectile", Builder.of(MonarchProjectileEntity::new, MobCategory.MISC).sized(0.375F, 0.375F).clientTrackingRange(16).updateInterval(1)
   );
   public static final EntityType<MonarchProjectileEntity> MONARCH_EXPLOSION_PROJECTILE = register(
      "monarch_explosion_projectile",
      Builder.of(MonarchProjectileEntity::new, MobCategory.MISC).sized(0.375F, 0.375F).clientTrackingRange(16).updateInterval(1)
   );
   public static final EntityType<StalkerEntity> STALKER = register(
      "stalker",
      Builder.of(StalkerEntity::new, MobCategory.MONSTER)
         .notInPeaceful()
         .sized(0.90999997F, 2.375F)
         .eyeHeight(2.125F)
         .clientTrackingRange(10)
         .updateInterval(1)
   );
   public static final EntityType<NuzzleEntity> NUZZLE = register(
      "nuzzle", Builder.of(NuzzleEntity::new, MobCategory.CREATURE).sized(0.9F, 1.25F).eyeHeight(1.05F).clientTrackingRange(10)
   );
   public static final EntityType<SlabberEntity> SLABBER = register(
      "slabber", Builder.of(SlabberEntity::new, MobCategory.CREATURE).sized(1.5F, 1.6875F).eyeHeight(1.3125F).clientTrackingRange(10)
   );
   public static final EntityType<NesterEntity> NESTER = register(
      "nester", Builder.of(NesterEntity::new, MobCategory.CREATURE).sized(0.95F, 2.25F).eyeHeight(2.06F).clientTrackingRange(10)
   );
   public static final EntityType<PollinatorEntity> POLLINATOR = register(
      "pollinator", Builder.of(PollinatorEntity::new, MobCategory.CREATURE).sized(0.675F, 1.4375F).eyeHeight(1.25F).clientTrackingRange(10)
   );
   public static final EntityType<PollinatorProjectileEntity> POLLINATOR_PROJECTILE = register(
      "pollinator_projectile",
      Builder.of(PollinatorProjectileEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(Integer.MAX_VALUE)
   );
   public static final EntityType<RiftEntity> RIFT = register(
      "rift", Builder.of(RiftEntity::new, MobCategory.MISC).sized(1.0F, 4.25F).clientTrackingRange(12).updateInterval(1).noLootTable()
   );
   public static final EntityType<MiniRiftEntity> MINI_RIFT = register(
      "mini_rift", Builder.of(MiniRiftEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(8).updateInterval(1).noLootTable()
   );
   public static final EntityType<SiftiteReturnEntity> SIFTITE_RETURN = register(
      "siftite_return", Builder.of(SiftiteReturnEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).noLootTable()
   );
   public static final EntityType<Boat> ASH_WILLOW_BOAT = register(
      "ash_willow_boat",
      Builder.<Boat>of((type, level) -> new Boat(type, level, () -> ModItems.ASH_WILLOW_BOAT), MobCategory.MISC)
         .noLootTable()
         .sized(1.375F, 0.5625F)
         .eyeHeight(0.5625F)
         .clientTrackingRange(10)
   );
   public static final EntityType<ChestBoat> ASH_WILLOW_CHEST_BOAT = register(
      "ash_willow_chest_boat",
      Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, () -> ModItems.ASH_WILLOW_CHEST_BOAT), MobCategory.MISC)
         .noLootTable()
         .sized(1.375F, 0.5625F)
         .eyeHeight(0.5625F)
         .clientTrackingRange(10)
   );
   public static final EntityType<Boat> OVERGROWN_WILLOW_BOAT = register(
      "overgrown_willow_boat",
      Builder.<Boat>of((type, level) -> new Boat(type, level, () -> ModItems.OVERGROWN_WILLOW_BOAT), MobCategory.MISC)
         .noLootTable()
         .sized(1.375F, 0.5625F)
         .eyeHeight(0.5625F)
         .clientTrackingRange(10)
   );
   public static final EntityType<ChestBoat> OVERGROWN_WILLOW_CHEST_BOAT = register(
      "overgrown_willow_chest_boat",
      Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, () -> ModItems.OVERGROWN_WILLOW_CHEST_BOAT), MobCategory.MISC)
         .noLootTable()
         .sized(1.375F, 0.5625F)
         .eyeHeight(0.5625F)
         .clientTrackingRange(10)
   );

   private ModEntities() {
   }

   private static ResourceKey<EntityType<?>> key(String path) {
      return ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), Identifier.fromNamespaceAndPath("the_sift", path));
   }

   private static <T extends Entity> EntityType<T> register(String path, Builder<T> builder) {
      ResourceKey<EntityType<?>> key = key(path);
      return ModRegistration.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
   }

   public static void initialize() {
   }
}
