package de.kokoio01.spawnglider.mixin;

import de.kokoio01.spawnglider.util.States;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void canGlide(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity) {
            if (States.isFlying(self.getUuid())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "tickGliding", at = @At("HEAD"), cancellable = true)
    private void tickGliding(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity) {
            if (States.isFlying(self.getUuid())) {
                ci.cancel();
            }
        }
    }
}
