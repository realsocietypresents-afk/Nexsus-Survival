package com.nexusuniverse.vice;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ViceDataManager {

    private final Plugin plugin;
    private final File statsFile;
    private final Map<UUID, VicePlayerData> data = new ConcurrentHashMap<>();
    // separate from the transient map above and persisted -- a lifetime achievement stat,
    // unlike active dose/blackout state, which is deliberately not saved (see NexusVicePlugin)
    private final Map<UUID, Integer> overdoseCounts = new HashMap<>();

    public ViceDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "vice-stats.yml");
        loadStats();
    }

    public VicePlayerData get(UUID playerId) {
        return data.computeIfAbsent(playerId, id -> new VicePlayerData());
    }

    public void remove(UUID playerId) {
        data.remove(playerId);
    }

    public Map<UUID, VicePlayerData> all() {
        return data;
    }

    public int getOverdoseCount(UUID playerId) {
        return overdoseCounts.getOrDefault(playerId, 0);
    }

    public void incrementOverdoseCount(UUID playerId) {
        overdoseCounts.merge(playerId, 1, Integer::sum);
        saveStats();
    }

    private void loadStats() {
        if (!statsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection section = config.getConfigurationSection("overdose-counts");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                overdoseCounts.put(UUID.fromString(key), section.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // skip a malformed entry
            }
        }
    }

    private void saveStats() {
        YamlConfiguration config = new YamlConfiguration();
        overdoseCounts.forEach((id, count) -> config.set("overdose-counts." + id, count));
        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "NexusVice: failed to save vice-stats.yml", e);
        }
    }
}
