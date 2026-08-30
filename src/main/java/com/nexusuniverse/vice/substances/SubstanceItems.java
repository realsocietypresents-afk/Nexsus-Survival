package com.nexusuniverse.vice.substances;

import com.nexusuniverse.vice.ViceConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Each substance is now its own real item (sugar, snowballs, wheat,
 * etc. -- see Substance.material()), not a shared tinted potion bottle.
 * These aren't naturally consumable materials in vanilla, so they're
 * used via a plain right-click (see SubstanceUseListener), not the
 * vanilla eating/drinking animation.
 */
public class SubstanceItems {

    private final ViceConfig config;
    private final NamespacedKey substanceKey;

    public SubstanceItems(Plugin plugin, ViceConfig config) {
        this.config = config;
        this.substanceKey = new NamespacedKey(plugin, "vice_substance");
    }

    public ItemStack create(Substance substance) {
        ItemStack item = new ItemStack(substance.material());
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(colorFor(substance) + config.displayName(substance));
        meta.setLore(List.of("§7Right-click to use.", "§8Effects vary with how much you take."));
        meta.getPersistentDataContainer().set(substanceKey, PersistentDataType.STRING, substance.name());

        item.setItemMeta(meta);
        return item;
    }

    /** Returns the substance this item represents, or null if it isn't one of ours. */
    public Substance readSubstance(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(substanceKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return Substance.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String colorFor(Substance substance) {
        return switch (substance.category()) {
            case DEPRESSANT -> "§9";
            case STIMULANT -> "§c";
            case HALLUCINOGEN -> "§d";
            case MELLOW -> "§a";
            case DISSOCIATIVE -> "§3";
            case PERFORMANCE -> "§6";
            case EUPHORIC -> "§e";
        };
    }
}
