package mielon.thesift.particle;

import mielon.thesift.platform.ModRegistration;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModParticles {
   public static final SimpleParticleType POLLINATOR_DROOL = new SimpleParticleType(false);
   public static final SimpleParticleType SIFT_PARALLAX = new SimpleParticleType(false);
   public static final SimpleParticleType SIFT_NOTE = new SimpleParticleType(false);
   public static final SimpleParticleType SOUND_WAVE = new SimpleParticleType(false);
   public static final SimpleParticleType SINGER_SOUND_WAVE = new SimpleParticleType(false);
   public static final SimpleParticleType CANYON_SOUL = new SimpleParticleType(false);
   public static final SimpleParticleType SOUL_FRAGMENT = new SimpleParticleType(false);
   public static final SimpleParticleType ICHOR_SURFACE_MIST = new SimpleParticleType(false);
   public static final SimpleParticleType ICHOR_BUBBLE = new SimpleParticleType(false);
   public static final SimpleParticleType ICHOR_SPLASH = new SimpleParticleType(false);
   public static final SimpleParticleType ICHOR_RAIN_SPLASH = new SimpleParticleType(false);
   public static final SimpleParticleType ICHOR_SOUL_SURFACE = new SimpleParticleType(false);
   public static final SimpleParticleType ICHOR_SOUL_RISING = new SimpleParticleType(false);
   public static final SimpleParticleType ICHOR_SOUL_BODY = new SimpleParticleType(false);

   private ModParticles() {
   }

   public static void initialize() {
   }

   private static void register(String name, SimpleParticleType type) {
      ModRegistration.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("the_sift", name), type);
   }

   static {
      register("ichor_soul_surface", ICHOR_SOUL_SURFACE);
      register("ichor_soul_rising", ICHOR_SOUL_RISING);
      register("ichor_soul_body", ICHOR_SOUL_BODY);
      register("pollinator_drool", POLLINATOR_DROOL);
      register("sift_parallax", SIFT_PARALLAX);
      register("sift_note", SIFT_NOTE);
      register("sound_wave", SOUND_WAVE);
      register("singer_sound_wave", SINGER_SOUND_WAVE);
      register("canyon_soul", CANYON_SOUL);
      register("soul_fragment", SOUL_FRAGMENT);
      register("ichor_surface_mist", ICHOR_SURFACE_MIST);
      register("ichor_bubble", ICHOR_BUBBLE);
      register("ichor_splash", ICHOR_SPLASH);
      register("ichor_rain_splash", ICHOR_RAIN_SPLASH);
   }
}
