package de.kokoio01.spawnglider.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.config.SpawnElytraConfig.Region;
import net.minecraft.server.permissions.Permissions;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ZoneManagementCommand {
    private static SpawnElytraConfig config;

    public static void register(SpawnElytraConfig configInstance) {
        config = configInstance;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("spawnglider")
                .then(literal("zone")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                        .then(literal("set")
                                .then(argument("minX", IntegerArgumentType.integer())
                                        .then(argument("minY", IntegerArgumentType.integer())
                                                .then(argument("minZ", IntegerArgumentType.integer())
                                                        .then(argument("maxX", IntegerArgumentType.integer())
                                                                .then(argument("maxY", IntegerArgumentType.integer())
                                                                        .then(argument("maxZ", IntegerArgumentType.integer())
                                                                                .executes(ZoneManagementCommand::setZone))))))))
                        .then(literal("remove")
                                .executes(ZoneManagementCommand::removeZone))
                        .then(literal("list")
                                .executes(ZoneManagementCommand::listZones))
                        .then(literal("info")
                                .executes(ZoneManagementCommand::zoneInfo))
                        .then(literal("sethere")
                                .then(argument("radius", IntegerArgumentType.integer())
                                        .executes(ZoneManagementCommand::setZoneHere)))
                        .executes(ZoneManagementCommand::help))));
    }

    private static int setZone(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String dimension = player.level().dimension().identifier().toString();
        int minX = IntegerArgumentType.getInteger(ctx, "minX");
        int minY = IntegerArgumentType.getInteger(ctx, "minY");
        int minZ = IntegerArgumentType.getInteger(ctx, "minZ");
        int maxX = IntegerArgumentType.getInteger(ctx, "maxX");
        int maxY = IntegerArgumentType.getInteger(ctx, "maxY");
        int maxZ = IntegerArgumentType.getInteger(ctx, "maxZ");

        config.regions.removeIf(region -> region.dimension.equals(dimension));

        Region region = new Region();
        region.dimension = dimension;
        region.minX = minX;
        region.minY = minY;
        region.minZ = minZ;
        region.maxX = maxX;
        region.maxY = maxY;
        region.maxZ = maxZ;

        config.regions.add(region);
        config.save();

        ctx.getSource().sendSuccess(() -> Component.literal("Zone set for dimension " + dimension +
                " from (" + minX + "," + minY + "," + minZ + ") to (" + maxX + "," + maxY + "," + maxZ + ")")
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int removeZone(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String dimension = player.level().dimension().identifier().toString();

        boolean removed = config.regions.removeIf(region -> region.dimension.equals(dimension));
        
        if (removed) {
            config.save();
            ctx.getSource().sendSuccess(() -> Component.literal("Zone removed for dimension " + dimension)
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("No zone found for dimension " + dimension)
                    .withStyle(ChatFormatting.RED));
        }

        return removed ? 1 : 0;
    }

    private static int listZones(CommandContext<CommandSourceStack> ctx) {
        if (config.regions.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No zones configured")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Configured zones:")
                .withStyle(ChatFormatting.GOLD), false);

        for (int i = 0; i < config.regions.size(); i++) {
            Region region = config.regions.get(i);
            Component zoneText = Component.literal((i + 1) + ". " + region.dimension +
                    " from (" + region.minX + "," + region.minY + "," + region.minZ + 
                    ") to (" + region.maxX + "," + region.maxY + "," + region.maxZ + ")")
                    .withStyle(ChatFormatting.WHITE);
            ctx.getSource().sendSuccess(() -> zoneText, false);
        }

        return 1;
    }

    private static int zoneInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String dimension = player.level().dimension().identifier().toString();

        Region region = config.getRegion(net.minecraft.resources.Identifier.tryParse(dimension));
        
        if (region == null) {
            ctx.getSource().sendFailure(Component.literal("No zone found for dimension " + dimension)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Zone info for " + dimension + ":")
                .withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Min: (" + region.minX + "," + region.minY + "," + region.minZ + ")")
                .withStyle(ChatFormatting.WHITE), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Max: (" + region.maxX + "," + region.maxY + "," + region.maxZ + ")")
                .withStyle(ChatFormatting.WHITE), false);

        return 1;
    }

    private static int setZoneHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        String dimension = player.level().dimension().identifier().toString();
        
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        config.regions.removeIf(region -> region.dimension.equals(dimension));

        Region region = new Region();
        region.dimension = dimension;
        region.minX = x - radius;
        region.minY = Math.max(y - radius, -64);
        region.minZ = z - radius;
        region.maxX = x + radius;
        region.maxY = Math.min(y + radius, 320); // Don't go above world top
        region.maxZ = z + radius;

        config.regions.add(region);
        config.save();

        ctx.getSource().sendSuccess(() -> Component.literal("Zone set around your position (" + x + "," + y + "," + z +
                ") with radius " + radius + " in dimension " + dimension)
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("SpawnGlider Zone Commands:")
                .withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/spawnglider zone set <minX> <minY> <minZ> <maxX> <maxY> <maxZ> - Set a zone in current dimension")
                .withStyle(ChatFormatting.WHITE), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/spawnglider zone remove - Remove zone in current dimension")
                .withStyle(ChatFormatting.WHITE), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/spawnglider zone list - List all zones")
                .withStyle(ChatFormatting.WHITE), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/spawnglider zone info - Show zone info for current dimension")
                .withStyle(ChatFormatting.WHITE), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/spawnglider zone sethere <radius> - Set zone around your position")
                .withStyle(ChatFormatting.WHITE), false);

        return 1;
    }
}
