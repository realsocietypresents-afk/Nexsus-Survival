package com.nexusuniverse.vice;

import com.nexusuniverse.vice.alcohol.AlcoholBrand;
import com.nexusuniverse.vice.alcohol.AlcoholType;
import com.nexusuniverse.vice.substances.Substance;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ViceConfig {

    private final JavaPlugin plugin;

    public ViceConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    // --- Substances ---

    /** The name shown to players -- change this in config.yml, nothing in code needs to change. */
    public String displayName(Substance substance) {
        return plugin.getConfig().getString("substances." + substance.configKey() + ".display-name", titleCase(substance.name()));
    }

    public double dosePerItem(Substance substance) {
        return plugin.getConfig().getDouble("substances." + substance.configKey() + ".dose-per-item", substance.defaultDosePerItem());
    }

    public double overdoseThreshold(Substance substance) {
        return plugin.getConfig().getDouble("substances." + substance.configKey() + ".overdose-threshold", substance.defaultOverdoseThreshold());
    }

    public boolean hasOverdoseRisk(Substance substance) {
        return plugin.getConfig().getBoolean("substances." + substance.configKey() + ".has-overdose-risk", substance.defaultHasOverdoseRisk());
    }

    // --- Alcohol ---

    public String displayName(AlcoholType alcohol) {
        return plugin.getConfig().getString("alcohol." + alcohol.configKey() + ".display-name", titleCase(alcohol.name()));
    }

    public double dosePerItem(AlcoholType alcohol) {
        return plugin.getConfig().getDouble("alcohol." + alcohol.configKey() + ".dose-per-item", alcohol.defaultDosePerItem());
    }

    // --- Alcohol brands (specific drinkable products) ---

    public String displayName(AlcoholBrand brand) {
        return plugin.getConfig().getString("alcohol-brands." + brand.configKey() + ".display-name", titleCase(brand.name()));
    }

    /** Defaults to that brand's base type dose scaled by its quality tier's multiplier -- override per-brand in config to break from that. */
    public double dosePerItem(AlcoholBrand brand) {
        double defaultDose = dosePerItem(brand.type()) * brand.quality().doseMultiplier();
        return plugin.getConfig().getDouble("alcohol-brands." + brand.configKey() + ".dose-per-item", defaultDose);
    }

    // --- Shared tuning ---

    public int tickIntervalSeconds() {
        return Math.max(1, plugin.getConfig().getInt("tick-interval-seconds", 5));
    }

    public double decayPerTick() {
        return plugin.getConfig().getDouble("decay-per-tick", 3.0);
    }

    public double vomitSevereChance() {
        return plugin.getConfig().getDouble("vomit.severe-chance", 0.3);
    }

    public double vomitOverdoseChance() {
        return plugin.getConfig().getDouble("vomit.overdose-chance", 1.0);
    }

    public int vomitCooldownSeconds() {
        return plugin.getConfig().getInt("vomit.cooldown-seconds", 30);
    }

    public double alcoholBlackoutThreshold() {
        return plugin.getConfig().getDouble("blackout.alcohol-threshold", 90.0);
    }

    public double blackoutDamagePerPulse() {
        return plugin.getConfig().getDouble("blackout.damage-per-pulse", 2.0);
    }

    public int blackoutPulseIntervalSeconds() {
        return plugin.getConfig().getInt("blackout.pulse-interval-seconds", 5);
    }

    public double comboDepressantAlcoholMultiplier() {
        return plugin.getConfig().getDouble("combo.depressant-alcohol-bonus-multiplier", 1.5);
    }

    public int rehabCooldownHours() {
        return plugin.getConfig().getInt("rehab.cooldown-hours", 24);
    }

    /** Weighted item pool for what flies out of a player's head on a vomit trigger -- duplicates make an entry more common. Falls back to plain DIRT if config is missing/empty/invalid. */
    public List<Material> vomitBlocks() {
        List<String> raw = plugin.getConfig().getStringList("vomit.blocks");
        List<Material> materials = new ArrayList<>();
        for (String name : raw) {
            try {
                materials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // skip a bad entry rather than failing the whole list
            }
        }
        if (materials.isEmpty()) materials.add(Material.DIRT);
        return materials;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    private String titleCase(String enumName) {
        String raw = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
