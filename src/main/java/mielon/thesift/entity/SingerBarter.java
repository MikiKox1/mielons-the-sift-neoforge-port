package mielon.thesift.entity;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.enchantment.SoulAbsorption;
import mielon.thesift.item.ModItems;
import mielon.thesift.map.SiftExplorationMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class SingerBarter {
   private SingerBarter() {
   }

   public static InteractionResult interact(SingerEntity singer, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (!held.is(ModBlocks.SOUL_BLOCK.asItem())) {
         return InteractionResult.PASS;
      }

      if (!singer.canBarter()) {
         return InteractionResult.SUCCESS;
      }

      if (singer.level() instanceof ServerLevel) {
         singer.beginBarter(player instanceof ServerPlayer serverPlayer ? serverPlayer : null, player.position());
         if (!player.isCreative()) {
            held.shrink(1);
         }
      }

      return InteractionResult.SUCCESS;
   }

   public static void pickUp(SingerEntity singer, ItemEntity dropped) {
      if (singer.canBarter() && dropped.isAlive() && dropped.getItem().is(ModBlocks.SOUL_BLOCK.asItem())) {
         ServerPlayer owner = dropped.getOwner() instanceof ServerPlayer player ? player : null;
         singer.beginBarter(owner, owner != null ? owner.position() : dropped.position());
         singer.take(dropped, 1);
         dropped.getItem().shrink(1);
         if (dropped.getItem().isEmpty()) {
            dropped.discard();
         } else {
            dropped.setItem(dropped.getItem().copy());
         }
      }
   }

   public static void reward(SingerEntity singer, ServerLevel level, ServerPlayer player, Vec3 target) {
      ItemStack stack = rollReward(singer, level);
      Vec3 direction = target.subtract(singer.position()).multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 0.001) {
         direction = singer.getLookAngle().multiply(1.0, 0.0, 1.0);
      }

      direction = direction.normalize();
      ItemEntity reward = new ItemEntity(level, singer.getX() + direction.x * 0.6, singer.getY() + 1.2, singer.getZ() + direction.z * 0.6, stack);
      reward.setDeltaMovement(direction.scale(0.3).add(0.0, 0.2, 0.0));
      reward.setDefaultPickUpDelay();
      level.addFreshEntity(reward);
      if (player != null) {
         ModAdvancements.award(player, "the_sift/pitch_perfect");
      }
   }

   private static ItemStack rollReward(SingerEntity singer, ServerLevel level) {
      return chooseReward(singer.getRandom().nextInt(200), singer.getRandom(), level, singer.blockPosition());
   }

   private static ItemStack chooseReward(int roll, RandomSource random, ServerLevel level, BlockPos origin) {
      roll -= 24;
      Item item;
      int min;
      int max;
      if (roll < 0) {
         item = Items.IRON_BLOCK;
         min = 1;
         max = 2;
      } else {
         roll -= 14;
         if (roll < 0) {
            item = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
            min = 1;
            max = 1;
         } else {
            roll -= 14;
            if (roll < 0) {
               item = Items.ANCIENT_DEBRIS;
               min = 1;
               max = 4;
            } else {
               roll -= 3;
               if (roll < 0) {
                  item = ModItems.MUSIC_DISC_WELLSPRING;
                  min = 1;
                  max = 1;
               } else {
                  roll -= 14;
                  if (roll < 0) {
                     item = Items.GOLD_BLOCK;
                     min = 1;
                     max = 2;
                  } else {
                     roll -= 8;
                     if (roll < 0) {
                        item = Items.WHEAT_SEEDS;
                        min = 2;
                        max = 4;
                     } else {
                        roll -= 8;
                        if (roll < 0) {
                           item = Items.BEETROOT_SEEDS;
                           min = 4;
                           max = 8;
                        } else {
                           roll -= 10;
                           if (roll < 0) {
                              item = Items.POTATO;
                              min = 4;
                              max = 14;
                           } else {
                              roll -= 10;
                              if (roll < 0) {
                                 item = Items.CARROT;
                                 min = 4;
                                 max = 12;
                              } else {
                                 roll -= 8;
                                 if (roll < 0) {
                                    item = Items.NETHERITE_INGOT;
                                    min = 1;
                                    max = 1;
                                 } else {
                                    roll -= 12;
                                    if (roll < 0) {
                                       item = Items.BOOKSHELF;
                                       min = 4;
                                       max = 12;
                                    } else {
                                       roll -= 6;
                                       if (roll < 0) {
                                          item = Items.END_STONE;
                                          min = 2;
                                          max = 6;
                                       } else {
                                          roll -= 5;
                                          if (roll < 0) {
                                             item = Items.CHORUS_FLOWER;
                                             min = 1;
                                             max = 1;
                                          } else {
                                             roll -= 5;
                                             if (roll < 0) {
                                                item = Items.ECHO_SHARD;
                                                min = 1;
                                                max = 3;
                                             } else {
                                                roll -= 6;
                                                if (roll < 0) {
                                                   item = Items.REDSTONE_BLOCK;
                                                   min = 3;
                                                   max = 12;
                                                } else {
                                                   roll -= 8;
                                                   if (roll < 0) {
                                                      item = Items.STRING;
                                                      min = 4;
                                                      max = 16;
                                                   } else {
                                                      roll -= 8;
                                                      if (roll < 0) {
                                                         return SoulAbsorption.book(level.registryAccess(), 1 + random.nextInt(2));
                                                      }

                                                      roll -= 6;
                                                      if (roll < 0) {
                                                         return campMap(level, origin, random);
                                                      }

                                                      roll -= 10;
                                                      if (roll < 0) {
                                                         item = Items.WATER_BUCKET;
                                                         min = 1;
                                                         max = 1;
                                                      } else {
                                                         roll -= 6;
                                                         if (roll < 0) {
                                                            item = Items.PUMPKIN_SEEDS;
                                                            min = 4;
                                                            max = 8;
                                                         } else {
                                                            roll -= 6;
                                                            if (roll < 0) {
                                                               item = Items.COPPER_BLOCK.weathering().unaffected();
                                                               min = 3;
                                                               max = 8;
                                                            } else {
                                                               item = Items.LAPIS_BLOCK;
                                                               min = 1;
                                                               max = 3;
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return new ItemStack(item, min + random.nextInt(max - min + 1));
   }

   private static ItemStack campMap(ServerLevel level, BlockPos origin, RandomSource random) {
      SiftExplorationMaps.Kind[] kinds = new SiftExplorationMaps.Kind[]{
         SiftExplorationMaps.Kind.WASTES, SiftExplorationMaps.Kind.OVERGROWN, SiftExplorationMaps.Kind.CORAL
      };
      int first = random.nextInt(kinds.length);

      for (int offset = 0; offset < kinds.length; offset++) {
         ItemStack map = SiftExplorationMaps.create(level, kinds[(first + offset) % kinds.length], origin);
         if (!map.isEmpty()) {
            return map;
         }
      }

      return new ItemStack(Items.LAPIS_BLOCK, 1 + random.nextInt(3));
   }
}
