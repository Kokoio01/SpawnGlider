package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.util.States;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;

public class FallDamageProtection {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!source.isOf(DamageTypes.FALL)) return true;
            return !States.isFlying(player.getUuid()) && !States.isInGracePeriod(player.getUuid());
        });
    }
}
