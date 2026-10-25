package com.nexusuniverse.survival.disease;

import com.nexusuniverse.survival.NexusSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

/**
 * Cure items are plain water-base potions (no vanilla effects of their
 * own) tagged with which Disease they cure. Drinking one that doesn't
 * match your current infection just does nothing special -- it's a wasted
 * potion, not an error, matching how a real "wrong medicine" would feel.
 */
public class DiseaseItems {

    private final NamespacedKey cureKey;
    private final NamespacedKey infectKey;

    public DiseaseItems(NexusSurvivalPlugin plugin) {
        this.cureKey = new NamespacedKey(plugin, "cure_for");
        this.infectKey = new NamespacedKey(plugin, "infect_with");
    }

    public ItemStack createCure(Disease disease) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        meta.setDisplayName("§aCure: " + disease.getDisplayName());
        meta.setLore(java.util.List.of("§7Drink while infected with", "§7" + disease.getDisplayName() + " to cure it."));
        meta.getPersistentDataContainer().set(cureKey, PersistentDataType.STRING, disease.name());
        item.setItemMeta(meta);
        return item;
    }

    public Disease readCureType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(cureKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return Disease.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * The inverse of a cure item: a self-inflicted infection. Same plain water-base-potion shape
     * (no vanilla effects of its own) as a cure, so it isn't secretly a free Nausea potion or
     * anything -- drinking it just runs the same infect() path /nexussurvival give's admin
     * command uses, with the same one-disease-at-a-time and post-cure-immunity guards. See
     * DiseaseListener#onConsume for where that actually happens, and NexusSurvivalApi for how a
     * plugin like NexusEconomy mints and sells these without needing this class as a
     * compile-time dependency.
     */
    public ItemStack createInfector(Disease disease) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        meta.setDisplayName("§4Pathogen Vial: " + disease.getDisplayName());
        meta.setLore(java.util.List.of(
                disease.getDescription(),
                "§7Drink to infect yourself with",
                "§7" + disease.getDisplayName() + ".",
                "§8Does nothing if you're already sick or freshly cured."
        ));
        meta.getPersistentDataContainer().set(infectKey, PersistentDataType.STRING, disease.name());
        item.setItemMeta(meta);
        return item;
    }

    public Disease readInfectorType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(infectKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return Disease.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
