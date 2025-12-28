package de.kokoio01.spawnglider.commands;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import de.kokoio01.spawnglider.util.States;

import static net.minecraft.server.command.CommandManager.literal;

public class ToggleGliderCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("spawnglider")
                .then(literal("toggle")
                        .executes(ToggleGliderCommand::execute))));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command can only be used by players")
                    .formatted(Formatting.RED));
            return 0;
        }

        boolean newValue = States.isGlidingEnabled(player.getUuid());
        States.setGlidingEnabled(player.getUuid(), newValue);

        String status = newValue ? "enabled" : "disabled";
        Formatting color = newValue ? Formatting.GREEN : Formatting.RED;
        
        ctx.getSource().sendFeedback(() -> Text.literal("Spawn Glider is now " + status + "!")
                .formatted(color), true);

        return 1;
    }

}