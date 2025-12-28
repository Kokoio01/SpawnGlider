package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.util.States;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class BoostController {
    private static SpawnElytraConfig config = SpawnElytraConfig.getInstance();

    public BoostController(SpawnElytraConfig configInstance) {
        config = configInstance;
    }

    public static void tryApplyBoost(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET);
        int remainingBoosters = States.getRemainingBoosters(player.getUuid());
        if (States.isFlying(player.getUuid()) && player.isGliding() && (remainingBoosters > 0 || remainingBoosters == -1)) {
            if (remainingBoosters == -1) {
                States.setRemainingBoosters(player.getUuid(), config.getBoosters() - 1);
                ProjectileEntity.spawn(new FireworkRocketEntity(world, itemStack, player), world, itemStack);
                return;
            }
            States.setRemainingBoosters(player.getUuid(), remainingBoosters - 1);
            ProjectileEntity.spawn(new FireworkRocketEntity(world, itemStack, player), world, itemStack);
        }
    }
}