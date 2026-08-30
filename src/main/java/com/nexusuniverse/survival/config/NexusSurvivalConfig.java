package com.nexusuniverse.survival.config;

import org.bukkit.plugin.java.JavaPlugin;

public class NexusSurvivalConfig {

    private final JavaPlugin plugin;

    public NexusSurvivalConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        // saveDefaultConfig() only writes config.yml the very first time this plugin is
        // installed -- copyDefaults(true) + saveConfig() merges in anything a later update adds
        // to an already-existing config.yml, instead of it silently never showing up.
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    public double getDouble(String path, double fallback) {
        return plugin.getConfig().getDouble(path, fallback);
    }

    /**
     * Master dial for disease rarity: every infection roll in the plugin
     * (biome/block/mob contact, person-to-person contagion, plague sites,
     * dirty-armor hygiene infection, raw water, and Plague Carrier bite/
     * proximity) is multiplied by this on top of its own individual
     * chance. 1.0 = no change from the rates below; lower = rarer overall
     * without having to retune every individual number by hand.
     */
    public double diseaseChanceMultiplier() {
        return getDouble("disease.chance-multiplier", 0.35);
    }

    /**
     * The top of the thirst bar -- how much water a player can hold before needing to drink
     * again. Raised from the plugin's original fixed 180 to a configurable value (default 267)
     * per direct request. Read fresh from config on every call (not cached) so /nexussurvival
     * reload picks up a change immediately, same as every other number in this class.
     */
    public double thirstMax() {
        return getDouble("thirst.max", 267.0);
    }

    public double thirstDrinkRestore() {
        return getDouble("thirst.drink-restore", 54.0);
    }

    public double thirstRawDrinkRestore() {
        return getDouble("thirst.raw-drink-restore", 27.0);
    }

    /**
     * NOTE: this is NOT automatically rescaled when thirst.max changes. The original design had
     * ThirstItems.CANTEEN_MAX_CHARGES (6) * this exactly equal the old max (180) -- a full
     * Canteen was worth exactly one full bar. With thirst.max raised to 267 by default, that
     * invariant no longer holds on its own (6 * 30 = 180, not 267); raise this to 267/6 = 44.5
     * yourself in config.yml if you want "6 sips = one full bar" to still be true.
     */
    public double thirstCanteenSipRestore() {
        return getDouble("thirst.canteen-sip-restore", 30.0);
    }

    public int thirstDecayIntervalSeconds() {
        return getInt("thirst.decay-interval-seconds", 5);
    }

    public int thirstDehydrationDamageIntervalSeconds() {
        return getInt("thirst.dehydration-damage-interval-seconds", 4);
    }

    public boolean thirstAlertEnabled() {
        return getBoolean("thirst.alert.enabled", true);
    }

    /** Fires one highlighted chat message the moment thirst crosses down to this value. See ThirstManager#tick. */
    public int thirstAlertThreshold() {
        return getInt("thirst.alert.threshold", 67);
    }

    public int getInt(String path, int fallback) {
        return plugin.getConfig().getInt(path, fallback);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return plugin.getConfig().getBoolean(path, fallback);
    }

    public String getString(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }

    public void reload() {
        plugin.reloadConfig();
    }
}
