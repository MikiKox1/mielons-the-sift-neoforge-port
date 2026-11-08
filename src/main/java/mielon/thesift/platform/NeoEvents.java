package mielon.thesift.platform;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native NeoForge listeners preserving the callback timing used by upstream. */
public final class NeoEvents {
   private static final List<PlayerBlockBreakEvents.After> BLOCK_BREAK_AFTER = new ArrayList<>();
   private static final List<ServerLivingEntityEvents.AfterDeath> LIVING_DEATH_AFTER = new ArrayList<>();
   private static final List<UseBlockCallback> USE_BLOCK = new ArrayList<>();
   private static final List<UseItemCallback> USE_ITEM = new ArrayList<>();
   private static final List<ServerTickEvents.StartTick> SERVER_TICK_START = new ArrayList<>();
   private static final List<ServerTickEvents.EndTick> SERVER_TICK_END = new ArrayList<>();
   private static final List<ServerLifecycleEvents.ServerStopping> SERVER_STOPPING = new ArrayList<>();
   private static final List<ServerLifecycleEvents.ServerStopped> SERVER_STOPPED = new ArrayList<>();
   private static final List<ServerLifecycleEvents.ServerStarted> SERVER_STARTED = new ArrayList<>();
   private static final List<ServerPlayerEvents.AfterRespawn> AFTER_RESPAWN = new ArrayList<>();
   private static final List<ServerPlayerEvents.Join> PLAYER_JOIN = new ArrayList<>();
   private static final List<CommandRegistrationCallback> COMMAND_REGISTRATION = new ArrayList<>();
   private static final Map<ServerPlayer, RespawnContext> RESPAWN_CONTEXTS = new IdentityHashMap<>();
   private static boolean bootstrapped;

   private NeoEvents() {
   }

   /** Installs all NeoForge game-bus listeners before the bus is started. */
   public static void bootstrap() {
      if (bootstrapped) {
         return;
      }

      bootstrapped = true;
      NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
         for (UseBlockCallback callback : USE_BLOCK) {
            InteractionResult result = callback.interact(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (result != InteractionResult.PASS) {
               event.setCancellationResult(result);
               event.setCanceled(true);
               return;
            }
         }
      });
      NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
         for (UseItemCallback callback : USE_ITEM) {
            InteractionResult result = callback.interact(event.getEntity(), event.getLevel(), event.getHand());
            if (result != InteractionResult.PASS) {
               event.setCancellationResult(result);
               event.setCanceled(true);
               return;
            }
         }
      });
      NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> SERVER_TICK_START.forEach(callback -> callback.onStartTick(event.getServer())));
      NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> SERVER_TICK_END.forEach(callback -> callback.onEndTick(event.getServer())));
      NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> SERVER_STARTED.forEach(callback -> callback.onServerStarted(event.getServer())));
      NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> SERVER_STOPPING.forEach(callback -> callback.onServerStopping(event.getServer())));
      NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
         SERVER_STOPPED.forEach(callback -> callback.onServerStopped(event.getServer()));
         RESPAWN_CONTEXTS.clear();
      });
      NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
         if (event.getOriginal() instanceof ServerPlayer oldPlayer && event.getEntity() instanceof ServerPlayer newPlayer) {
            RESPAWN_CONTEXTS.put(newPlayer, new RespawnContext(oldPlayer, !event.isWasDeath()));
         }
      });
      NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
         if (event.getEntity() instanceof ServerPlayer newPlayer) {
            RespawnContext context = RESPAWN_CONTEXTS.remove(newPlayer);
            if (context != null) {
               AFTER_RESPAWN.forEach(callback -> callback.afterRespawn(context.oldPlayer(), newPlayer, context.alive()));
            }
         }
      });
      NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
         if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_JOIN.forEach(callback -> callback.onJoin(player));
         }
      });
      NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> COMMAND_REGISTRATION.forEach(callback ->
         callback.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection())
      ));
   }

   public static void afterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
      BLOCK_BREAK_AFTER.forEach(callback -> callback.afterBlockBreak(level, player, pos, state, blockEntity));
   }

   public static void afterDeath(LivingEntity entity, DamageSource source) {
      LIVING_DEATH_AFTER.forEach(callback -> callback.afterDeath(entity, source));
   }

   public record Event<T>(Consumer<T> registrar) {
      public void register(T callback) {
         registrar.accept(callback);
      }
   }

   @FunctionalInterface
   public interface UseBlockCallback {
      Event<UseBlockCallback> EVENT = new Event<>(USE_BLOCK::add);

      InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit);
   }

   @FunctionalInterface
   public interface UseItemCallback {
      Event<UseItemCallback> EVENT = new Event<>(USE_ITEM::add);

      InteractionResult interact(Player player, Level level, InteractionHand hand);
   }

   public static final class PlayerBlockBreakEvents {
      public static final Event<After> AFTER = new Event<>(BLOCK_BREAK_AFTER::add);

      private PlayerBlockBreakEvents() {
      }

      @FunctionalInterface
      public interface After {
         void afterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity);
      }
   }

   public static final class ServerLivingEntityEvents {
      public static final Event<AfterDeath> AFTER_DEATH = new Event<>(LIVING_DEATH_AFTER::add);

      private ServerLivingEntityEvents() {
      }

      @FunctionalInterface
      public interface AfterDeath {
         void afterDeath(LivingEntity entity, DamageSource source);
      }
   }

   public static final class ServerTickEvents {
      public static final Event<StartTick> START_SERVER_TICK = new Event<>(SERVER_TICK_START::add);
      public static final Event<EndTick> END_SERVER_TICK = new Event<>(SERVER_TICK_END::add);

      private ServerTickEvents() {
      }

      @FunctionalInterface
      public interface StartTick {
         void onStartTick(MinecraftServer server);
      }

      @FunctionalInterface
      public interface EndTick {
         void onEndTick(MinecraftServer server);
      }
   }

   public static final class ServerLifecycleEvents {
      public static final Event<ServerStarted> SERVER_STARTED = new Event<>(NeoEvents.SERVER_STARTED::add);
      public static final Event<ServerStopping> SERVER_STOPPING = new Event<>(NeoEvents.SERVER_STOPPING::add);
      public static final Event<ServerStopped> SERVER_STOPPED = new Event<>(NeoEvents.SERVER_STOPPED::add);

      private ServerLifecycleEvents() {
      }

      @FunctionalInterface
      public interface ServerStarted {
         void onServerStarted(MinecraftServer server);
      }

      @FunctionalInterface
      public interface ServerStopping {
         void onServerStopping(MinecraftServer server);
      }

      @FunctionalInterface
      public interface ServerStopped {
         void onServerStopped(MinecraftServer server);
      }
   }

   public static final class ServerPlayerEvents {
      public static final Event<AfterRespawn> AFTER_RESPAWN = new Event<>(NeoEvents.AFTER_RESPAWN::add);
      public static final Event<Join> JOIN = new Event<>(PLAYER_JOIN::add);

      private ServerPlayerEvents() {
      }

      @FunctionalInterface
      public interface AfterRespawn {
         void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive);
      }

      @FunctionalInterface
      public interface Join {
         void onJoin(ServerPlayer player);
      }
   }

   @FunctionalInterface
   public interface CommandRegistrationCallback {
      Event<CommandRegistrationCallback> EVENT = new Event<>(COMMAND_REGISTRATION::add);

      void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, CommandSelection environment);
   }

   private record RespawnContext(ServerPlayer oldPlayer, boolean alive) {
   }
}
