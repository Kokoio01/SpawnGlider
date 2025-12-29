package de.kokoio01.spawnglider.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SpawnElytraConfig {

	private static SpawnElytraConfig INSTANCE;

	public static class Region {
		@SerializedName("dimension")
		public String dimension;
		@SerializedName("minX")
		public int minX;
		@SerializedName("minY")
		public int minY;
		@SerializedName("minZ")
		public int minZ;
		@SerializedName("maxX")
		public int maxX;
		@SerializedName("maxY")
		public int maxY;
		@SerializedName("maxZ")
		public int maxZ;

		public boolean contains(Identifier worldId, double x, double y, double z) {
			if (worldId == null) return false;
			if (!worldId.toString().equals(this.dimension)) return false;
			return x >= Math.min(minX, maxX) && x <= Math.max(minX, maxX)
					&& y >= Math.min(minY, maxY) && y <= Math.max(minY, maxY)
					&& z >= Math.min(minZ, maxZ) && z <= Math.max(minZ, maxZ);
		}
	}

	@SerializedName("regions")
	public List<Region> regions = new ArrayList<>();

	@SerializedName("boosters")
	public int Boosters;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "spawnelytra.json";

	private SpawnElytraConfig() {
	}

	public static SpawnElytraConfig getInstance() {
		if (INSTANCE == null) {
			INSTANCE = loadOrCreate();
		}
		return INSTANCE;
	}

	public static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	private static SpawnElytraConfig loadOrCreate() {
		Path path = getConfigPath();
		if (Files.exists(path)) {
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				SpawnElytraConfig cfg = GSON.fromJson(reader, SpawnElytraConfig.class);
				if (cfg != null && cfg.regions != null) return cfg;
			} catch (IOException ignored) {
			}
		}
		SpawnElytraConfig def = new SpawnElytraConfig();
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(def, writer);
			}
		} catch (IOException ignored) {
		}
		return def;
	}

	public Region getRegion(Identifier worldId) {
		for (Region r : regions) {
			if (r.dimension.equals(worldId.toString())) {
				return r;
			}
		}
		return null;
	}

	public int getBoosters() {
		return Boosters;
	}

	public void setBoosters(int value) {
		Boosters = value;
	}

	public void save() {
		Path path = getConfigPath();
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}
}