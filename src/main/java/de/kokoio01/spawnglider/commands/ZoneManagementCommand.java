package de.kokoio01.spawnglider.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.config.SpawnElytraConfig.Region;

import static net.minecraft.server.command.CommandManager.*;

public class ZoneManagementCommand {
    private static SpawnElytraConfig config;

    public static void register(SpawnElytraConfig configInstance) {
        config = configInstance;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("spawnglider")
                .then(literal("zone")
                        .requires(source -> source.hasPermissionLevel(2))
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

    private static int setZone(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command can only be used by players")
                    .formatted(Formatting.RED));
            return 0;
        }

        String dimension = player.getEntityWorld().getRegistryKey().getValue().toString();
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

        ctx.getSource().sendFeedback(() -> Text.literal("Zone set for dimension " + dimension + 
                " from (" + minX + "," + minY + "," + minZ + ") to (" + maxX + "," + maxY + "," + maxZ + ")")
                .formatted(Formatting.GREEN), true);

        return 1;
    }

    private static int removeZone(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command can only be used by players")
                    .formatted(Formatting.RED));
            return 0;
        }

        String dimension = player.getEntityWorld().getRegistryKey().getValue().toString();

        boolean removed = config.regions.removeIf(region -> region.dimension.equals(dimension));
        
        if (removed) {
            config.save();
            ctx.getSource().sendFeedback(() -> Text.literal("Zone removed for dimension " + dimension)
                    .formatted(Formatting.GREEN), true);
        } else {
            ctx.getSource().sendError(Text.literal("No zone found for dimension " + dimension)
                    .formatted(Formatting.RED));
        }

        return removed ? 1 : 0;
    }

    private static int listZones(CommandContext<ServerCommandSource> ctx) {
        if (config.regions.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("No zones configured")
                    .formatted(Formatting.YELLOW), false);
            return 1;
        }

        ctx.getSource().sendFeedback(() -> Text.literal("Configured zones:")
                .formatted(Formatting.GOLD), false);

        for (int i = 0; i < config.regions.size(); i++) {
            Region region = config.regions.get(i);
            Text zoneText = Text.literal((i + 1) + ". " + region.dimension + 
                    " from (" + region.minX + "," + region.minY + "," + region.minZ + 
                    ") to (" + region.maxX + "," + region.maxY + "," + region.maxZ + ")")
                    .formatted(Formatting.WHITE);
            ctx.getSource().sendFeedback(() -> zoneText, false);
        }

        return 1;
    }

    private static int zoneInfo(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command can only be used by players")
                    .formatted(Formatting.RED));
            return 0;
        }

        String dimension = player.getEntityWorld().getRegistryKey().getValue().toString();

        Region region = config.getRegion(net.minecraft.util.Identifier.tryParse(dimension));
        
        if (region == null) {
            ctx.getSource().sendError(Text.literal("No zone found for dimension " + dimension)
                    .formatted(Formatting.RED));
            return 0;
        }

        ctx.getSource().sendFeedback(() -> Text.literal("Zone info for " + dimension + ":")
                .formatted(Formatting.GOLD), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Min: (" + region.minX + "," + region.minY + "," + region.minZ + ")")
                .formatted(Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Max: (" + region.maxX + "," + region.maxY + "," + region.maxZ + ")")
                .formatted(Formatting.WHITE), false);

        return 1;
    }

    private static int setZoneHere(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command can only be used by players")
                    .formatted(Formatting.RED));
            return 0;
        }

        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        String dimension = player.getEntityWorld().getRegistryKey().getValue().toString();
        
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

        ctx.getSource().sendFeedback(() -> Text.literal("Zone set around your position (" + x + "," + y + "," + z + 
                ") with radius " + radius + " in dimension " + dimension)
                .formatted(Formatting.GREEN), true);

        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("SpawnGlider Zone Commands:")
                .formatted(Formatting.GOLD), false);
        ctx.getSource().sendFeedback(() -> Text.literal("/spawnglider zone set <minX> <minY> <minZ> <maxX> <maxY> <maxZ> - Set a zone in current dimension")
                .formatted(Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> Text.literal("/spawnglider zone remove - Remove zone in current dimension")
                .formatted(Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> Text.literal("/spawnglider zone list - List all zones")
                .formatted(Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> Text.literal("/spawnglider zone info - Show zone info for current dimension")
                .formatted(Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> Text.literal("/spawnglider zone sethere <radius> - Set zone around your position")
                .formatted(Formatting.WHITE), false);

        return 1;
    }
}
