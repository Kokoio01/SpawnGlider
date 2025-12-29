package de.kokoio01.spawnglider.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BoostManagementCommand {
    private static SpawnElytraConfig config;

    public static void register(SpawnElytraConfig configInstance) {
        config = configInstance;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("spawnglider")
                .executes(BoostManagementCommand::help)
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("boosters")
                        .then(argument("amount", IntegerArgumentType.integer())
                            .executes(BoostManagementCommand::setBoosters)))));
    }

    private static int setBoosters(CommandContext<ServerCommandSource> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        config.setBoosters(amount);
        config.save();

        ctx.getSource().sendFeedback(() -> Text.literal("Set allowed Boosters to " + amount)
                .formatted(Formatting.GREEN), true);

        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("Usage: /spawnglieder boosters <amount of boosts>")
                .formatted(Formatting.WHITE), false);

        return 1;
    }
}
