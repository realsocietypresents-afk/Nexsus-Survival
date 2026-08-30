package com.nexusuniverse.vice.guidebook;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * "Has ever received one," not "currently has it in inventory" -- a genuine one-time give even if
 * a player later loses, sells, or drops the book. Anyone who hasn't gotten one yet gets one on
 * join, new account or ten-year veteran alike; nothing about it is new-player-specific.
 */
public class GuidebookManager {

    private final Plugin plugin;
    private final File file;
    private final Set<UUID> received = new HashSet<>();

    public GuidebookManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "guidebook-recipients.yml");
        load();
    }

    public boolean hasReceived(UUID playerId) {
        return received.contains(playerId);
    }

    public void markReceived(UUID playerId) {
        if (received.add(playerId)) save();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<String> ids = data.getStringList("received");
        for (String id : ids) {
            try {
                received.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // skip a malformed entry
            }
        }
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        List<String> ids = received.stream().map(UUID::toString).toList();
        data.set("received", ids);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "NexusVice: failed to save guidebook-recipients.yml", e);
        }
    }
}
