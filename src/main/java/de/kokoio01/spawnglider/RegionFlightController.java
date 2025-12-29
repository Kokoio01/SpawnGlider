package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.config.SpawnElytraConfig.Region;
import de.kokoio01.spawnglider.util.States;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionFlightController {

    public static Map<UUID, Long> GracePeriodEndTimes = new HashMap<>();
    public static Map<UUID, Integer> FlyingTicks = new HashMap<>();
    public static Map<UUID, Double> PeakDownwardVy = new HashMap<>();

    private static final int MIN_GRACE_TICKS = 5;
    private static final int MAX_GRACE_TICKS = 15;
    private static final int MIN_FLYING_TICKS_FOR_GRACE = 2 * 20;

    private static final double MIN_SPEED_FOR_GRACE = -0.35;
    private static final double SPEED_FOR_MAX_GRACE = -1.60;

    private static final int MIN_TICKS_BEFORE_GLIDE_TRIGGER = 12;
    private static final double MIN_DOWNWARD_SPEED_FOR_GLIDE = -0.35;

    private static final double LANDING_HORIZONTAL_DAMP = 0.2;
    private static final double LANDING_MIN_DOWNWARD_NUDGE = -0.08;

    private static SpawnElytraConfig config = SpawnElytraConfig.getInstance();

    public RegionFlightController(SpawnElytraConfig configInstance) {
        config = configInstance;
    }

    public void register() {
        ServerTickEvents.START_SERVER_TICK.register(this::onStartTick);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onDamage);
    }

    private void onStartTick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        GracePeriodEndTimes.entrySet().removeIf(entry -> now >= entry.getValue());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (States.isGlidingDisabled(player.getUUID())) continue;

            double vy = player.getDeltaMovement().y;
            boolean onSolidGround = isOnSolidGround(player) && vy <= 0.01;

            if (onSolidGround && player.isFallFlying()) {
                player.stopFallFlying();
                applyLandingDamping(player);
            }

            if (States.isFlying(player.getUUID()) && onSolidGround) {
                int flownTicks = FlyingTicks.getOrDefault(player.getUUID(), 0);
                States.setFlying(player.getUUID(), false);

                if (flownTicks >= MIN_FLYING_TICKS_FOR_GRACE) {
                    double peakVy = PeakDownwardVy.getOrDefault(player.getUUID(), 0.0);
                    int graceTicks = computeGraceTicksFromSpeed(peakVy);
                    startGracePeriod(player.getUUID(), graceTicks);
                }

                applyLandingDamping(player);
                FlyingTicks.remove(player.getUUID());
                PeakDownwardVy.remove(player.getUUID());
                States.resetRemainingBoosters(player.getUUID());
            }
        }
    }

    private void onEndTick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        GracePeriodEndTimes.entrySet().removeIf(entry -> now >= entry.getValue());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (States.isGlidingDisabled(player.getUUID())) continue;

            handlePlayerGliding(player);
        }
    }

    private boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (!source.is(DamageTypes.FALL)) return true;
        return !States.isFlying(player.getUUID()) && !isInGracePeriod(player.getUUID());
    }

    private void handlePlayerGliding(ServerPlayer player) {
        Identifier worldId = player.level().dimension().identifier();
        Region region = config.getRegion(worldId);
        boolean insideRegion = region != null && region.contains(worldId, player.getX(), player.getY(), player.getZ());

        double vy = player.getDeltaMovement().y;
        boolean onSolidGround = isOnSolidGround(player) && vy <= 0.01;
        boolean gliding = player.isFallFlying();
        boolean fallingFast = vy < MIN_DOWNWARD_SPEED_FOR_GLIDE;

        if (onSolidGround && gliding) {
            player.stopFallFlying();
            applyLandingDamping(player);
            gliding = false;
        }

        if ((insideRegion || States.isFlying(player.getUUID())) && !onSolidGround) {

            FlyingTicks.put(
                    player.getUUID(),
                    FlyingTicks.getOrDefault(player.getUUID(), 0) + 1
            );

            PeakDownwardVy.put(
                    player.getUUID(),
                    Math.min(PeakDownwardVy.getOrDefault(player.getUUID(), 0.0), vy)
            );

            if (!gliding
                    && fallingFast
                    && FlyingTicks.get(player.getUUID()) >= MIN_TICKS_BEFORE_GLIDE_TRIGGER) {
                startGliding(player);
            }

            if (!States.isFlying(player.getUUID())) {
                States.setFlying(player.getUUID(), true);
            }
            return;
        }

        if (isInGracePeriod(player.getUUID()) && !onSolidGround && !gliding) {
            FlyingTicks.remove(player.getUUID());
            startGliding(player);
        }
    }

    public static boolean isInGracePeriod(UUID uuid) {
        Long endTime = GracePeriodEndTimes.get(uuid);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public static void startGracePeriod(UUID uuid, long ticks) {
        GracePeriodEndTimes.put(uuid, System.currentTimeMillis() + ticks * 50L);
    }

    private int computeGraceTicksFromSpeed(double peakVy) {
        if (peakVy >= 0) return MIN_GRACE_TICKS;

        double clamped = Math.max(SPEED_FOR_MAX_GRACE, Math.min(MIN_SPEED_FOR_GRACE, peakVy));
        double t = (clamped - MIN_SPEED_FOR_GRACE) / (SPEED_FOR_MAX_GRACE - MIN_SPEED_FOR_GRACE);

        int grace = (int) Math.round(
                MIN_GRACE_TICKS + t * (MAX_GRACE_TICKS - MIN_GRACE_TICKS)
        );

        return Math.max(MIN_GRACE_TICKS, Math.min(MAX_GRACE_TICKS, grace));
    }

    private void startGliding(ServerPlayer player) {
        player.startFallFlying();
        States.setFlying(player.getUUID(), true);
    }

    private void applyLandingDamping(ServerPlayer player) {
        if (player.isInWater() || player.isInLava()) return;

        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(
                v.x * LANDING_HORIZONTAL_DAMP,
                Math.min(v.y, LANDING_MIN_DOWNWARD_NUDGE),
                v.z * LANDING_HORIZONTAL_DAMP
        );
        player.needsSync = true;
    }

    private boolean isOnSolidGround(ServerPlayer player) {
        if (!player.onGround()) return false;

        BlockPos below = player.blockPosition().below();
        BlockState state = player.level().getBlockState(below);
        VoxelShape shape = state.getCollisionShape(player.level(), below);

        return !shape.isEmpty();
    }
}
