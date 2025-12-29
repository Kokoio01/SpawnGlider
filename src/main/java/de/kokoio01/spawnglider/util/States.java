package de.kokoio01.spawnglider.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class States {
	public static Map<UUID, Boolean> GlidingEnabled = new HashMap<>();
	public static Set<UUID> FlyingPlayers = new HashSet<>();
	public static Map<UUID, Integer> RemainingBoosters = new HashMap<>();

	public static boolean isGlidingDisabled(UUID uuid) {
		return !GlidingEnabled.getOrDefault(uuid, true);
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

	public static int getRemainingBoosters(UUID uuid) {
		return RemainingBoosters.getOrDefault(uuid, -1);
	}

	public static void setRemainingBoosters(UUID uuid, int value) {
		RemainingBoosters.put(uuid, Math.max(0, value));
	}

	public static void resetRemainingBoosters(UUID uuid) {
		RemainingBoosters.remove(uuid);
	}
}
