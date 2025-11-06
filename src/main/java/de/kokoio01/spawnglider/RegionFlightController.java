package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.config.SpawnElytraConfig.Region;
import de.kokoio01.spawnglider.util.States;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class RegionFlightController {
    private static final int MIN_GRACE_TICKS = 5;
    private static final int MAX_GRACE_TICKS = 15;
    private static final int MIN_FLYING_TICKS_FOR_GRACE = 2 * 20;
    private static final double MIN_SPEED_FOR_GRACE = -0.35;
    private static final double SPEED_FOR_MAX_GRACE = -1.60;
    private static final int MIN_TICKS_BEFORE_GLIDE_TRIGGER = 12;
    private static final double MIN_DOWNWARD_SPEED_FOR_GLIDE = -0.35;
    private static final double LANDING_HORIZONTAL_DAMP = 0.2;
    private static final double LANDING_MIN_DOWNWARD_NUDGE = -0.08;

    private final SpawnElytraConfig config;

    public RegionFlightController(SpawnElytraConfig config) {
        this.config = config;
    }

    public void register() {
        ServerTickEvents.START_SERVER_TICK.register(this::onStartTick);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndTick);
    }

    private void onStartTick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        States.GracePeriodEndTimes.entrySet().removeIf(entry -> now >= entry.getValue());

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (!States.isGlidingEnabled(player.getUuid())) continue;

            double vy = player.getVelocity().y;
            boolean onSolidGround = isOnSolidGround(player) && vy <= 0.01;

            if (onSolidGround && player.isGliding()) {
                player.stopGliding();
                applyLandingDamping(player);
            }

            if (States.isFlying(player.getUuid()) && onSolidGround) {
                int flownTicks = States.getFlyingTicks(player.getUuid());
                States.setFlying(player.getUuid(), false);

                if (flownTicks >= MIN_FLYING_TICKS_FOR_GRACE) {
                    double peakVy = States.getPeakDownwardVy(player.getUuid());
                    int graceTicks = computeGraceTicksFromSpeed(peakVy);
                    States.startGracePeriod(player.getUuid(), graceTicks);
                }
                applyLandingDamping(player);
                States.resetFlyingTicks(player.getUuid());
                States.resetPeakDownwardVy(player.getUuid());
            }
        }
    }

    private void onEndTick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        States.GracePeriodEndTimes.entrySet().removeIf(entry -> now >= entry.getValue());

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (!States.isGlidingEnabled(player.getUuid())) continue;

            handlePlayerGliding(player);
        }
    }

    private void handlePlayerGliding(ServerPlayerEntity player) {
        Identifier worldId = player.getEntityWorld().getRegistryKey().getValue();
        Region region = config.getRegion(worldId);
        boolean insideRegion = region != null && region.contains(worldId, player.getX(), player.getY(), player.getZ());

        double vy = player.getVelocity().y;
        boolean onSolidGround = isOnSolidGround(player) && vy <= 0.01;
        boolean gliding = player.isGliding();
        boolean fallingFast = vy < MIN_DOWNWARD_SPEED_FOR_GLIDE;

        if (onSolidGround && gliding) {
            player.stopGliding();
            applyLandingDamping(player);
            gliding = false;
        }

        if ((insideRegion || States.isFlying(player.getUuid())) && !onSolidGround) {
            States.incrementFlyingTicks(player.getUuid());
            States.updatePeakDownwardVy(player.getUuid(), vy);

            if (!gliding && fallingFast && States.getFlyingTicks(player.getUuid()) >= MIN_TICKS_BEFORE_GLIDE_TRIGGER) {
                startGliding(player);
            }

            if (!States.isFlying(player.getUuid())) {
                States.resetPeakDownwardVy(player.getUuid());
                States.setFlying(player.getUuid(), true);
            }
            return;
        }

        if (States.isInGracePeriod(player.getUuid()) && !onSolidGround && !gliding) {
            States.resetFlyingTicks(player.getUuid());
            startGliding(player);
        }
    }

    private int computeGraceTicksFromSpeed(double peakVy) {
        if (peakVy >= 0) return MIN_GRACE_TICKS;
        double clamped = Math.max(SPEED_FOR_MAX_GRACE, Math.min(MIN_SPEED_FOR_GRACE, peakVy));
        double t = (clamped - MIN_SPEED_FOR_GRACE) / (SPEED_FOR_MAX_GRACE - MIN_SPEED_FOR_GRACE);
        int grace = (int) Math.round(MIN_GRACE_TICKS + t * (MAX_GRACE_TICKS - MIN_GRACE_TICKS));
        return Math.max(MIN_GRACE_TICKS, Math.min(MAX_GRACE_TICKS, grace));
    }

    private void startGliding(ServerPlayerEntity player) {
        player.startGliding();
        States.setFlying(player.getUuid(), true);
    }

    private void applyLandingDamping(ServerPlayerEntity player) {
        if (player.isTouchingWater() || player.isInLava()) return;
        Vec3d v = player.getVelocity();
        double vx = v.x * LANDING_HORIZONTAL_DAMP;
        double vz = v.z * LANDING_HORIZONTAL_DAMP;
        double vy = Math.min(v.y, LANDING_MIN_DOWNWARD_NUDGE);
        player.setVelocity(vx, vy, vz);
        player.velocityDirty = true;
    }

    private boolean isOnSolidGround(ServerPlayerEntity player) {
        if (!player.isOnGround()) return false;
        BlockPos below = player.getBlockPos().down();
        var world = player.getEntityWorld();
        BlockState state = world.getBlockState(below);
        VoxelShape shape = state.getCollisionShape(world, below);
        return !shape.isEmpty();
    }
}
