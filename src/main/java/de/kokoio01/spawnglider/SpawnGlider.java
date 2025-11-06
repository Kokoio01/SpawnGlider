package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.commands.ToggleGliderCommand;
import de.kokoio01.spawnglider.commands.ZoneManagementCommand;
import net.fabricmc.api.ModInitializer;
import de.kokoio01.spawnglider.config.SpawnElytraConfig;

public class SpawnGlider implements ModInitializer {
    @Override
    public void onInitialize() {
        SpawnElytraConfig CONFIG = SpawnElytraConfig.loadOrCreate();
        ToggleGliderCommand.register();
        ZoneManagementCommand.register(CONFIG);
        new RegionFlightController(CONFIG).register();
        FallDamageProtection.register();
    }
}
