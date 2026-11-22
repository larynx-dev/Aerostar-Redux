package larynx.dev.aerostar_redux.command;

import larynx.dev.aerostar_redux.AeroStarComp;
import larynx.dev.aerostar_redux.transfer.SubLevelDimensionTransfer;
import com.lightning.northstar.content.world.NorthstarDimensions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Arrays;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class DriveTransferCommand {
    private static final List<String> PLANET_NAMES = Arrays.asList("moon", "mars", "venus", "mercury", "earth_orbit", "earth");
    private static final SuggestionProvider<CommandSourceStack> PLANET_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(PLANET_NAMES, builder);

    private DriveTransferCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("drivetransfer")
                .requires(src -> src.hasPermission(2))
                .then(
                    Commands.argument("planet", StringArgumentType.word())
                        .suggests(PLANET_SUGGESTIONS)
                        .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "planet")))
                )
        );
        AeroStarComp.LOGGER.info("Registered /drivetransfer command.");
    }

    private static int execute(CommandSourceStack source, String planet) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception var8) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        ResourceKey<Level> targetDimKey = resolveDimension(planet);
        if (targetDimKey == null) {
            source.sendFailure(Component.literal("Unknown planet '" + planet + "'. Valid: moon, mars, venus, mercury, earth_orbit, earth"));
            return 0;
        } else {
            ServerSubLevel subLevel = findSubLevel(player);
            if (subLevel == null) {
                source.sendFailure(Component.literal("You are not on a physics ship."));
                return 0;
            } else {
                ServerLevel sourceLevel = (ServerLevel)player.level();
                ServerLevel targetLevel = player.getServer().getLevel(targetDimKey);
                if (targetLevel == null) {
                    source.sendFailure(Component.literal("Dimension '" + targetDimKey.location() + "' is not loaded on this server."));
                    return 0;
                } else if (sourceLevel.dimension().equals(targetDimKey)) {
                    source.sendFailure(Component.literal("You are already in that dimension."));
                    return 0;
                } else {
                    source.sendSuccess(() -> Component.literal("Initiating transfer to " + targetDimKey.location() + "..."), false);
                    boolean success = SubLevelDimensionTransfer.transfer(subLevel, sourceLevel, targetLevel);
                    if (!success) {
                        source.sendFailure(Component.literal("Transfer failed. Check server logs for details."));
                        return 0;
                    } else {
                        source.sendSuccess(() -> Component.literal("Transfer complete! Welcome to " + targetDimKey.location()), false);
                        return 1;
                    }
                }
            }
        }
    }

    private static ServerSubLevel findSubLevel(ServerPlayer player) {
        SubLevel tracking = Sable.HELPER.getTrackingSubLevel(player);
        if (tracking instanceof ServerSubLevel) {
            return (ServerSubLevel)tracking;
        } else {
            Entity vehicle = player.getVehicle();
            ServerSubLevelContainer container;
            if (vehicle != null && (container = SubLevelContainer.getContainer((ServerLevel)player.level())) != null) {
                for (ServerSubLevel ssl : container.getAllSubLevels()) {
                    if (vehicle.level() == ssl.getLevel()) {
                        return ssl;
                    }
                }
            }

            return null;
        }
    }

    public static ResourceKey<Level> resolveDimension(String planet) {
        String var1 = planet.toLowerCase();

        return switch (var1) {
            case "moon" -> NorthstarDimensions.THE_MOON;
            case "mars" -> NorthstarDimensions.MARS;
            case "venus" -> NorthstarDimensions.VENUS;
            case "mercury" -> NorthstarDimensions.MERCURY;
            case "earth_orbit" -> NorthstarDimensions.EARTH_ORBIT;
            case "earth" -> Level.OVERWORLD;
            default -> null;
        };
    }
}
