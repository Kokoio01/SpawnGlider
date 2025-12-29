package de.kokoio01.spawnglider.commands;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import de.kokoio01.spawnglider.util.States;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class ToggleGliderCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("spawnglider")
                .then(literal("toggle")
                        .executes(ToggleGliderCommand::execute))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean newValue = States.isGlidingDisabled(player.getUUID());
        States.setGlidingEnabled(player.getUUID(), newValue);

        String status = newValue ? "enabled" : "disabled";
        ChatFormatting color = newValue ? ChatFormatting.GREEN : ChatFormatting.RED;
        
        ctx.getSource().sendSuccess(() -> Component.literal("Spawn Glider is now " + status + "!")
                .withStyle(color), true);

        return 1;
    }

}