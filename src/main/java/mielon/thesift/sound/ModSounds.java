package mielon.thesift.sound;

import mielon.thesift.platform.ModRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_1 = register("block.sonorous_deepslate.sound_1");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_2 = register("block.sonorous_deepslate.sound_2");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_3 = register("block.sonorous_deepslate.sound_3");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_4 = register("block.sonorous_deepslate.sound_4");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_5 = register("block.sonorous_deepslate.sound_5");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_6 = register("block.sonorous_deepslate.sound_6");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_7 = register("block.sonorous_deepslate.sound_7");
   public static final SoundEvent SONOROUS_DEEPSLATE_SOUND_8 = register("block.sonorous_deepslate.sound_8");
   public static final SoundEvent SONOROUS_AUTOPLAY_GLITCH = register("sonorous_autoplay_glitch");
   public static final SoundEvent THE_SIFT_PORTAL_OPEN = register("the_sift_portal_open");
   public static final SoundEvent THE_SIFT_PORTAL_CLOSE = register("the_sift_portal_close");
   public static final SoundEvent RIFT_APPEAR = registerFixedRange("entity.rift.appear", 64.0F);
   public static final SoundEvent SINGER_SING = registerFixedRange("entity.singer.sing", 4096.0F);
   public static final SoundEvent SINGER_IDLE = register("entity.singer.idle");
   public static final SoundEvent SINGER_HURT = register("entity.singer.hurt");
   public static final SoundEvent SINGER_DEATH = register("entity.singer.death");
   public static final SoundEvent SINGER_HAPPY = register("entity.singer.happy");
   public static final SoundEvent SEEDLING_AMBIENT = register("entity.seedling.ambient");
   public static final SoundEvent SEEDLING_HURT = register("entity.seedling.hurt");
   public static final SoundEvent SEEDLING_DEATH = register("entity.seedling.death");
   public static final SoundEvent SEEDLING_STEP = register("entity.seedling.step");
   public static final SoundEvent SEEDLING_ATTACK = register("entity.seedling.attack");
   public static final SoundEvent BLUB_IDLE_WATER = register("entity.blub.idle_water");
   public static final SoundEvent BLUB_IDLE_AIR = register("entity.blub.idle_air");
   public static final SoundEvent BLUB_HURT = register("entity.blub.hurt");
   public static final SoundEvent BLUB_DEATH = register("entity.blub.death");
   public static final SoundEvent BLUB_SWIM = register("entity.blub.swim");
   public static final SoundEvent BLUB_SPLASH = register("entity.blub.splash");
   public static final SoundEvent BLUB_ATTACK = register("entity.blub.attack");
   public static final SoundEvent SINGER_EMERGE = register("entity.singer.emerge");
   public static final SoundEvent SINGER_DIG = register("entity.singer.dig");
   public static final SoundEvent DARK_SNIFFER_SNIFF = register("entity.dark_sniffer.sniff");
   public static final SoundEvent ECHO_GOLEM_SPAWN = register("entity.echo_golem.spawn");
   public static final SoundEvent ECHO_GOLEM_ITEM_DROP = register("entity.echo_golem.item_drop");
   public static final SoundEvent ECHO_GOLEM_ITEM_GET = register("entity.echo_golem.item_get");
   public static final SoundEvent ECHO_GOLEM_HURT = register("entity.echo_golem.hurt");
   public static final SoundEvent ECHO_GOLEM_DEATH = register("entity.echo_golem.death");
   public static final SoundEvent ECHO_GOLEM_STEP = register("entity.echo_golem.step");
   public static final SoundEvent SIFT_PORTAL_AMBIENT = register("block.sift_portal.ambient");
   public static final SoundEvent SIFT_PORTAL_TRIGGER = register("block.sift_portal.trigger");
   public static final SoundEvent DARK_SNIFFER_BLOOM = register("block.dark_sniffer.bloom");
   public static final SoundEvent ICHOR_BOTTLE_FILL = register("item.ichor_bottle.fill");
   public static final SoundEvent ICHOR_BOTTLE_EMPTY = register("item.ichor_bottle.empty");
   public static final SoundEvent ICHOR_BUCKET_FILL = register("item.ichor_bucket.fill");
   public static final SoundEvent ICHOR_BUCKET_EMPTY = register("item.ichor_bucket.empty");
   public static final SoundEvent SIFTITE_ARMOR_EQUIP = register("item.siftite_armor.equip");
   public static final SoundEvent MUSIC_DISC_RIFT = register("music_disc.rift");
   public static final SoundEvent MUSIC_DISC_WELLSPRING = register("music_disc.wellspring");
   public static final SoundEvent SIFT_INTRO = register("sift_intro");
   public static final SoundEvent[] BY_INDEX = new SoundEvent[]{
      SONOROUS_DEEPSLATE_SOUND_1,
      SONOROUS_DEEPSLATE_SOUND_2,
      SONOROUS_DEEPSLATE_SOUND_3,
      SONOROUS_DEEPSLATE_SOUND_4,
      SONOROUS_DEEPSLATE_SOUND_5,
      SONOROUS_DEEPSLATE_SOUND_6,
      SONOROUS_DEEPSLATE_SOUND_7,
      SONOROUS_DEEPSLATE_SOUND_8
   };
   public static final SoundEvent SEEDLING_AGGRESSIVE = register("entity.seedling.aggressive");
   public static final SoundEvent SEEDLING_OPEN_MOUTH = register("entity.seedling.open_mouth");
   public static final SoundEvent SEEDLING_RUN = register("entity.seedling.run");
   public static final SoundEvent BLUB_HOP = register("entity.blub.hop");
   public static final SoundEvent ECHO_GOLEM_AMBIENT = register("entity.echo_golem.ambient");
   public static final SoundEvent NESTER_AMBIENT = register("entity.nester.ambient");
   public static final SoundEvent NESTER_AGGRESSIVE = register("entity.nester.aggressive");
   public static final SoundEvent NESTER_ENRAGED = register("entity.nester.enraged");
   public static final SoundEvent NESTER_HURT = register("entity.nester.hurt");
   public static final SoundEvent NESTER_DEATH = register("entity.nester.death");
   public static final SoundEvent NESTER_STEP = register("entity.nester.step");
   public static final SoundEvent NESTER_ATTACK = register("entity.nester.attack");
   public static final SoundEvent NUZZLE_AMBIENT = register("entity.nuzzle.ambient");
   public static final SoundEvent NUZZLE_HURT = register("entity.nuzzle.hurt");
   public static final SoundEvent NUZZLE_DEATH = register("entity.nuzzle.death");
   public static final SoundEvent NUZZLE_STEP = register("entity.nuzzle.step");
   public static final SoundEvent POLLINATOR_AMBIENT = register("entity.pollinator.ambient");
   public static final SoundEvent POLLINATOR_AGGRESSIVE = register("entity.pollinator.aggressive");
   public static final SoundEvent POLLINATOR_HURT = register("entity.pollinator.hurt");
   public static final SoundEvent POLLINATOR_DEATH = register("entity.pollinator.death");
   public static final SoundEvent POLLINATOR_STEP = register("entity.pollinator.step");
   public static final SoundEvent POLLINATOR_INHALE = register("entity.pollinator.inhale");
   public static final SoundEvent POLLINATOR_EXHALE = register("entity.pollinator.exhale");
   public static final SoundEvent POLLINATOR_RUN = register("entity.pollinator.run");
   public static final SoundEvent POLLINATOR_DROOL_FIRE = register("entity.pollinator.drool_fire");
   public static final SoundEvent POLLINATOR_DROOL_LAND = register("entity.pollinator.drool_land");
   public static final SoundEvent POLLINATOR_DROOL_EXPLOSION = register("entity.pollinator.drool_explosion");
   public static final SoundEvent SCAVENGER_AMBIENT = register("entity.scavenger.ambient");
   public static final SoundEvent SCAVENGER_HURT = register("entity.scavenger.hurt");
   public static final SoundEvent SCAVENGER_DEATH = register("entity.scavenger.death");
   public static final SoundEvent SCAVENGER_MOVEMENT = register("entity.scavenger.movement");
   public static final SoundEvent SCAVENGER_PROJECTILE_LAUNCH = register("entity.scavenger.projectile_launch");
   public static final SoundEvent SCAVENGER_PROJECTILE_LOOP = register("entity.scavenger.projectile_loop");
   public static final SoundEvent SCAVENGER_PROJECTILE_EXPLODE = register("entity.scavenger.projectile_explode");
   public static final SoundEvent SLABBER_AMBIENT = register("entity.slabber.ambient");
   public static final SoundEvent SLABBER_HURT = register("entity.slabber.hurt");
   public static final SoundEvent SLABBER_DEATH = register("entity.slabber.death");
   public static final SoundEvent SLABBER_STEP = register("entity.slabber.step");
   public static final SoundEvent STALKER_AMBIENT = register("entity.stalker.ambient");
   public static final SoundEvent STALKER_HURT = register("entity.stalker.hurt");
   public static final SoundEvent STALKER_DEATH = register("entity.stalker.death");
   public static final SoundEvent MONARCH_DEATH = register("entity.monarch.death");
   public static final SoundEvent MONARCH_AMBIENT = register("entity.monarch.ambient");
   public static final SoundEvent MONARCH_HURT = register("entity.monarch.hurt");
   public static final SoundEvent MONARCH_STEP = register("entity.monarch.step");
   public static final SoundEvent MONARCH_ATTACK = register("entity.monarch.attack");
   public static final SoundEvent MONARCH_DASH = register("entity.monarch.dash");
   public static final SoundEvent MONARCH_PROJECTILE_LAUNCH = register("entity.monarch.projectile_launch");
   public static final SoundEvent MONARCH_PROJECTILE_LOOP = register("entity.monarch.projectile_loop");
   public static final SoundEvent MONARCH_PROJECTILE_EXPLODE = register("entity.monarch.projectile_explode");
   public static final SoundEvent ROYAL_SCULK_ACTIVATE = register("block.royal_sculk.activate");
   public static final SoundEvent ROYAL_SCULK_DEACTIVATE = register("block.royal_sculk.deactivate");
   public static final SoundEvent STALKER_STEP = register("entity.stalker.step");
   public static final SoundEvent STALKER_ATTACK = register("entity.stalker.attack");
   public static final SoundEvent STALKER_DASH = register("entity.stalker.dash");
   public static final SoundEvent SOULSHALE_BREAK = register("block.soulshale.break");
   public static final SoundEvent SOULSHALE_HIT = register("block.soulshale.hit");
   public static final SoundEvent SOULSHALE_PLACE = register("block.soulshale.place");
   public static final SoundEvent SOULSHALE_STEP = register("block.soulshale.step");

   private ModSounds() {
   }

   public static void initialize() {
   }

   private static SoundEvent register(String path) {
      ResourceKey<SoundEvent> key = key(path);
      return ModRegistration.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createVariableRangeEvent(key.identifier()));
   }

   private static SoundEvent registerFixedRange(String path, float range) {
      ResourceKey<SoundEvent> key = key(path);
      return ModRegistration.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createFixedRangeEvent(key.identifier(), range));
   }

   private static ResourceKey<SoundEvent> key(String path) {
      return ResourceKey.create(BuiltInRegistries.SOUND_EVENT.key(), Identifier.fromNamespaceAndPath("the_sift", path));
   }

   static {
      for (String biome : new String[]{
         "overgrown", "coral", "ichor_snowy_peaks", "sift_wastes", "siftslate_caves", "soulshale_caves", "corrupted_sift", "organic_desert"
      }) {
         register("music.biome." + biome);
      }
   }
}
