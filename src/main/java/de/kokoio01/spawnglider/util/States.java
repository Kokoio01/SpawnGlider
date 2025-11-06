package de.kokoio01.spawnglider.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class States {
	public static Map<UUID, Boolean> GlidingEnabled = new HashMap<>();
	public static Set<UUID> FlyingPlayers = new HashSet<>();
	public static Map<UUID, Long> GracePeriodEndTimes = new HashMap<>();
	public static Map<UUID, Integer> FlyingTicks = new HashMap<>();
	public static Map<UUID, Double> PeakDownwardVy = new HashMap<>();

	public static boolean isGlidingEnabled(UUID uuid) {
		return GlidingEnabled.getOrDefault(uuid, true);
	}

	public static void setGlidingEnabled(UUID uuid, boolean value) {
		GlidingEnabled.put(uuid, value);
	}

	public static boolean isFlying(UUID uuid) {
		return FlyingPlayers.contains(uuid);
	}

	public static void setFlying(UUID uuid, boolean flying) {
		if (flying) {
			FlyingPlayers.add(uuid);
		} else {
			FlyingPlayers.remove(uuid);
		}
	}

	public static int getFlyingTicks(UUID uuid) {
		return FlyingTicks.getOrDefault(uuid, 0);
	}

	public static void incrementFlyingTicks(UUID uuid) {
		FlyingTicks.put(uuid, getFlyingTicks(uuid) + 1);
	}

	public static void resetFlyingTicks(UUID uuid) {
		FlyingTicks.remove(uuid);
	}

	public static boolean isInGracePeriod(UUID uuid) {
		Long endTime = GracePeriodEndTimes.get(uuid);
		return endTime != null && System.currentTimeMillis() < endTime;
	}

	public static void startGracePeriod(UUID uuid, long ticks) {
		long durationMs = ticks * 50;
		GracePeriodEndTimes.put(uuid, System.currentTimeMillis() + durationMs);
	}

    public static void resetPeakDownwardVy(UUID uuid) {
		PeakDownwardVy.remove(uuid);
	}

	public static void updatePeakDownwardVy(UUID uuid, double vy) {
		if (vy >= 0) return;
		Double current = PeakDownwardVy.get(uuid);
		if (current == null || vy < current) {
			PeakDownwardVy.put(uuid, vy);
		}
	}

	public static double getPeakDownwardVy(UUID uuid) {
		return PeakDownwardVy.getOrDefault(uuid, 0.0);
	}
}
