package mielon.thesift.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mielon.thesift.platform.NeoEvents.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.saveddata.WeatherData;

public final class SiftWeatherCommand {
   private SiftWeatherCommand() {
   }

   public static void register() {
      CommandRegistrationCallback.EVENT
         .register((CommandRegistrationCallback)(dispatcher, registryAccess, environment) -> registerDimensionArguments(dispatcher));
   }

   private static void registerDimensionArguments(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("weather").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

      for (SiftWeatherCommand.Mode mode : SiftWeatherCommand.Mode.values()) {
         root.then(
            Commands.literal(mode.commandName)
               .then(
                  Commands.argument("dimension", DimensionArgument.dimension())
                     .executes(context -> set(context.getSource(), DimensionArgument.getDimension(context, "dimension"), mode, -1, true))
               )
               .then(
                  Commands.argument("duration", TimeArgument.time(1))
                     .then(
                        Commands.argument("dimension", DimensionArgument.dimension())
                           .executes(
                              context -> set(
                                 context.getSource(),
                                 DimensionArgument.getDimension(context, "dimension"),
                                 mode,
                                 IntegerArgumentType.getInteger(context, "duration"),
                                 true
                              )
                           )
                     )
               )
         );
      }

      dispatcher.register(root);
   }

   public static int setCurrent(CommandSourceStack source, SiftWeatherCommand.Mode mode, int requestedDuration) {
      return set(source, source.getLevel(), mode, requestedDuration, false);
   }

   private static int set(CommandSourceStack source, ServerLevel target, SiftWeatherCommand.Mode mode, int requestedDuration, boolean showDimension) {
      int duration = resolveDuration(target, mode, requestedDuration);
      WeatherData weather = target.getWeatherData();
      weather.setClearWeatherTime(mode == SiftWeatherCommand.Mode.CLEAR ? duration : 0);
      weather.setRainTime(mode == SiftWeatherCommand.Mode.CLEAR ? 0 : duration);
      weather.setThunderTime(mode == SiftWeatherCommand.Mode.THUNDER ? duration : 0);
      weather.setRaining(mode != SiftWeatherCommand.Mode.CLEAR);
      weather.setThundering(mode == SiftWeatherCommand.Mode.THUNDER);
      source.sendSuccess(() -> {
         MutableComponent message = Component.translatable("commands.weather.set." + mode.commandName);
         return showDimension ? message.append(" [" + target.dimension().identifier() + "]") : message;
      }, true);
      return duration;
   }

   private static int resolveDuration(ServerLevel target, SiftWeatherCommand.Mode mode, int requestedDuration) {
      if (requestedDuration >= 0) {
         return requestedDuration;
      }

      IntProvider provider = switch (mode) {
         case CLEAR -> ServerLevel.RAIN_DELAY;
         case RAIN -> ServerLevel.RAIN_DURATION;
         case THUNDER -> ServerLevel.THUNDER_DURATION;
      };
      return provider.sample(target.getRandom());
   }

   public enum Mode {
      CLEAR("clear"),
      RAIN("rain"),
      THUNDER("thunder");

      private final String commandName;

      Mode(String commandName) {
         this.commandName = commandName;
      }
   }
}
