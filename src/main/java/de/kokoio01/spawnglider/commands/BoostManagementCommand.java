package de.kokoio01.spawnglider.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class BoostManagementCommand {
    private static SpawnElytraConfig config;

    public static void register(SpawnElytraConfig configInstance) {
        config = configInstance;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("spawnglider")
                .executes(BoostManagementCommand::help)
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .then(literal("boosters")
                        .then(argument("amount", IntegerArgumentType.integer())
                                .executes(BoostManagementCommand::setBoosters)))));
    }

    private static int setBoosters(CommandContext<CommandSourceStack> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        config.setBoosters(amount);
        config.save();

        ctx.getSource().sendSuccess(() -> Component.literal("Set allowed Boosters to " + amount)
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("Usage: /spawnglider boosters <amount of boosts>")
                .withStyle(ChatFormatting.WHITE), false);

        return 1;
    }
}
