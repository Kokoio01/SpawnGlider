package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.util.States;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoostController {
    private static SpawnElytraConfig config = SpawnElytraConfig.getInstance();
    private static final Map<UUID, Long> lastBoostTime = new HashMap<>();

    public BoostController(SpawnElytraConfig configInstance) {
        config = configInstance;
    }

    public static void tryApplyBoost(@UnknownNullability ServerPlayer player) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUUID();
        if (lastBoostTime.containsKey(playerId) && currentTime - lastBoostTime.get(playerId) < 200) return; // 200 is in ms anc cooldown to prevent double input
        lastBoostTime.put(playerId, currentTime);

        ServerLevel world = player.level();
        ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET);
        int remainingBoosters = States.getRemainingBoosters(playerId);
        System.out.println(remainingBoosters);
        if (States.isFlying(playerId) && player.isFallFlying() && (remainingBoosters > 0 || remainingBoosters == -1)) {
            if (remainingBoosters == -1) {
                States.setRemainingBoosters(playerId, config.getBoosters() - 1);
                Projectile.spawnProjectile(new FireworkRocketEntity(world, itemStack, player), world, itemStack);
                return;
            }
            States.setRemainingBoosters(playerId, remainingBoosters - 1);
            Projectile.spawnProjectile(new FireworkRocketEntity(world, itemStack, player), world, itemStack);
        }
    }
}
